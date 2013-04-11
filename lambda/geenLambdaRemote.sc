GeenLambdaRemote{

	var ncoef, msgrate, lag, threshold;
	var cameraActionParams, cameraWeights, cameraActions, patterns, activePatterns;
	var graphics, stats, synth, eventResponder, onsetResponder, rotation;
	
	*new{|ncoef=20, msgrate=20, lag=0.05, threshold=0.01, screenX=800, screenY=600, mode=0|
		^super.newCopyArgs(ncoef, msgrate, lag, threshold).init(screenX, screenY, mode)
	}

	init{|screenX, screenY, mode|
		
		if (Server.default.serverRunning.not) {
//			Server.default.options.inDevice = "Built-in Microphone";
			Server.default.waitForBoot({
				this.addSynthDef
			}) 
		} {
			this.addSynthDef
		};
		
		graphics = CinderApp(screenX, screenY, mode: mode);
		activePatterns = Array.fill(6, false);

		stats = ();

		stats.mfc = (0.25 ! 8);
		stats.amps = (0 ! 20);

		cameraActions = (
			rotateCam: {|rhoRate, rhoMin, rhoRange, thetaRate, phiRate|
				rotation = Rotation(rrand(rhoMin, rhoMin + rhoRange), rhoRate, rhoMin, rhoRange, 
					2pi.rand, thetaRate, 2pi.rand, phiRate);
				graphics.setCameraRotation(rotation, 200.0, 200.0, 200.0, 200.0, 200.0, 200.0);
				graphics.rotateCamera;
			},
			attachCam: {|lookAtCenter=true|
				graphics.setBoidCam(true, lookAtCenter);
			},
			stopCam: {|x, y, z|
				graphics.setBoidCam(false, false);
				graphics.stopRotation;
				graphics.stopMove;
				graphics.setViewpoint(x, y, z, 200.0, 200.0, 200.0);
			},
			moveCam: {|startcoords, endcoords, time, rate, done|
				graphics.moveCamera(startcoords, endcoords, time, rate, done)
			}
		);
		cameraWeights = (
			0.5: \rotateCam,
			0.3: \attachCam,
			0.08: \stopCam,
			0.12: \moveCam
		);
		cameraActionParams = (
			rotateCam: {  
				[ 
				stats.amps.mean.explin(0.001, 1.0, 50.0, 500.0), 
				stats.amps.mean.explin(0.001, 1.0, 50.0, 500.0), 
				stats.amps.mean.explin(0.001, 1.0, 50.0, 500.0),
				stats.amps.mean.explin(0.001, 1.0, 50.0, 500.0),
				stats.amps.mean.explin(0.001, 1.0, 50.0, 500.0)
				]
			},
			attachCam: { [true] },
			stopCam: { 
				var sp = Spherical(stats.amps.mean.explin(0.001, 1.0, 50.0, 200.0) + 200.0, 2pi.rand, 2pi.rand);
				[sp.x, sp.y, sp.z]
			},
			moveCam: {  
				[
					[500.0.rand, 500.0.rand, 500.0.rand, 200.0, 200.0, 200.0], 
					[400.0.rand, 400.0.rand, 400.0.rand, 200.0, 200.0, 200.0],
					rrand(10, 30),
					10
				]
			}
		);
				
		graphics.open;
	}
	
	addSynthDef{
		SynthDef(\analyze, {
			var input, chain, amp, mfcc, event;
			input = Mix(SoundIn.ar([0, 1]));
			amp = Amplitude.kr(input, lag);
			chain = FFT(LocalBuf(1024), input);
			mfcc = MFCC.kr(chain, ncoef);
			event = Impulse.kr(msgrate);
			SendReply.kr(Onsets.kr(chain, threshold), '/onsets', amp);
			SendReply.kr(event, '/event', amp, 2);
			SendReply.kr(event, '/event', mfcc, 3);
		}).add;		
	}
	
	start{
		synth = Synth(\analyze);
						
		Tdef(\initGraphics, {
			graphics.setFrameRate(16.0);
			1.wait;
			graphics.initWorld(20, 20, 20, ncoef);
			1.wait;
			graphics.setViewpoint(500.0, 400.0, 600.0, 200.0, 200.0, 200.0);
			1.wait;
			graphics.activateSwarm(16, 400.0, 400.0, 400.0, 0.9, 50.0, 8.0, 5.0, 100.0);
			1.wait;
			graphics.sendBoidPattern(2, 1, 0);
			this.startResponders;
		}).play;
				
	}
	
	startResponders{
		eventResponder = OSCresponderNode(Server.default.addr, '/event', {|ti, re, ms|
			ms[2].switch(
				2, {
					stats.amps.pop;
					stats.amps.insert(0, ms[3]);
					if (stats.amps.keep(5).mean < 0.001) {
						graphics.setBoidCam(false, false);
						graphics.stopRotation;
						graphics.stopMove;			
					}
				},
				3, {
					stats.mfc = ms[3..ncoef+2];
					graphics.sendSOMVector(stats.mfc);
					graphics.setSwarm(0.9, 50.0, 8.0, (0.25 - stats.mfc[0] ).abs.linlin(0.1, 1.0, 5.0, 40.0), 100.0)
				}
			);
		}).add;
		
		onsetResponder = OSCresponderNode(Server.default.addr, '/onsets', {|ti, re, ms|
			var action;
			if (stats.amps.mean.coin) {
				action = cameraWeights.values.wchoose(cameraWeights.keys(Array));
				if (action != 'stopCam') {  
					graphics.setBoidCam(false, false);
					graphics.stopRotation;
					graphics.stopMove;			
				};
				cameraActions[action].(*cameraActionParams[action].())
			};
			
			if (stats.amps.mean > 0.5) {
				graphics.activateSwarm(rrand(16, 32), 400.0, 400.0, 400.0, 0.9, 50.0, 8.0, 5.0, 100.0);
			};

			this.changePattern			
		}).add;		
		
	}

	changePattern{
		var params;
		if (activePatterns.collect(_.asInt).sum > 2) {
			var inds, index;
			inds = activePatterns.selectIndices({|is| is });
			inds.remove(2);
			index = inds.choose;
			("turning off BOID pattern " ++ index.asString).postln;
			graphics.sendBoidPattern(index, 0, 0);
			activePatterns[index] = false;
		};
		if (stats.amps.mean.coin) {
			params = [
				[0, 1, (0..5).choose],
				[1, 1, 0],
				[2, 1, 0],
				[3, 1, [0, 3, 4].choose],
				[4, 1, [0, 4].choose],
				[5, 1, 4]
			].choose;
			activePatterns[params.first] = true;
			("activating BOID pattern " ++ params.first.asString).postln;
			graphics.sendBoidPattern(*params)
		}
		
	}
	
	clear{
		synth.free;
		
		eventResponder.remove;
		onsetResponder.remove;
		
		Tdef(\clearGraphics, {
			6.do({|i|
				graphics.sendBoidPattern(i, 0, 0);
			});
			graphics.stopRotation;
			0.5.wait;
			graphics.killSwarm;
			0.5.wait;
			graphics.setViewpoint(0.0, 0.0, 60.0, 0.0, 0.0, 0.0);
			0.5.wait;
			graphics.setFrameRate(32.0);
		}).play;
		
	}
	
	
}