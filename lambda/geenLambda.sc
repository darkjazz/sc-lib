GeenLambda{
	
	var mikro, decoder, dur, procs, <composer, rotation, stats, isActive = false, <geen;
	var cameraActionParams, cameraWeights, cameraActions, patterns, activePatterns;

	*new{|mikro, decoder, dur|
		^super.newCopyArgs(mikro, decoder, dur).init
	}	
	
	init{
		
		if (decoder.isNil) {
			decoder = FoaDecoder();
		};
				
		stats = ();

		stats.mfc = (0.25 ! 8);
		stats.amps = (0 ! 20);
		stats.freqs = (0 ! 20);
		stats.flats = (0 ! 20);
		
		procs = "/Users/alo/Development/mikro/audio/procs01.scd".load;
		
		composer = MikroComposer(mikro, procs, useEventData: false);
		
		activePatterns = Array.fill(6, false)
		
	}
	
	prepare{
		{
			MikroData.loadPath = "/Users/alo/Data/mikro/lib000/";
//			MikroData.loadPath = "/Users/alo/Data/mikro/lib001/";
			Post << "Initialising MikroGeen.." << Char.nl;
			geen = MikroGeen();
			Post << "Updating clusters.." << Char.nl;
			geen.updateClusters;
//			geen.loadClusters("/Users/alo/Data/mikro/130214_194521.kmeans");
			Post << "Loading event data.." << Char.nl;
			geen.loadEventData(doneAction: { 
				Post << "Training sets.." << Char.nl;
				geen.trainSets;
//				geen.loadSets;
				Post << "Training sets finished..." << Char.nl;
				Post << "Setup complete, ready for launch..." << Char.nl;
			});
		}.fork
	}

	start{
		mikro.start(-60.dbamp, 0.05, 30);
		mikro.input.mainamp_(-3.dbamp);
		mikro.input.auxamp_(-6.dbamp);				

//		Tdef(\liveprocs, {
//			var names;
//			names = #[ fbgverb, cliq, grains, arhythmic, latch, fbgverb, streamverb ];
//			3.wait;
//			mikro.analyzer.addMFCCResponderFunction({|ti, re, ms, an|
//				var argstr = Pseq(ms[3..mikro.analyzer.numcoef+2].clip(0.0, 1.0), inf).asStream;
//				composer.activeSynths.do({|synth|
//					var args;
//					args = composer.descLib[synth.defName.asSymbol].metadata.specs
//						.collect(_.map(argstr.next));
//					synth.set(*args.asKeyValuePairs)
//				});
//				
//			});
//			names.do({|name|
//				var id;
//				id = composer.play(name, argstream: Pn(0.25, inf).asStream);
//				(mikro.analyzer.maxdur / names.size - 15).wait;
//				composer.releaseSynth(id, 15);
//			})
//		}).play;

		#[xang,yang,zang].do({|name|
			Tdef(name, {
				var angle, time;
				angle = Pbrown(-pi, pi, 64.reciprocal, inf).asStream;
				time = Pbrown(0.01, 0.1, 0.01, inf).asStream;
				loop({
					mikro.input.synth.set(name, angle.next);
					time.next.wait;
				})
			}).play
		});				
		
//		Tdef(\space, {
//			loop({
//				mikro.input.synth.set(\xang, rrand(-pi, pi), \yang, rrand(-pi, pi), \zang, rrand(-pi, pi));
//				rrand(0.5, 3.0).wait;
//			})
//		}).play;

		cameraActions = (
			rotateCam: {|rhoRate, rhoMin, rhoRange, thetaRate, phiRate|
				rotation = Rotation(rrand(rhoMin, rhoMin + rhoRange), rhoRate, rhoMin, rhoRange, 
					2pi.rand, thetaRate, 2pi.rand, phiRate);
				mikro.graphics.setCameraRotation(rotation, 20.0.rand, 20.0.rand, 20.0.rand, 0.0, 0.0, 0.0);
				mikro.graphics.rotateCamera;
			},
			attachCam: {|lookAtCenter=true|
				mikro.graphics.setBoidCam(true, lookAtCenter);
			},
			stopCam: {|x, y, z|
				mikro.graphics.setBoidCam(false, false);
				mikro.graphics.stopRotation;
				mikro.graphics.stopMove;
				mikro.graphics.setViewpoint(x, y, z, 20.0.rand, 20.0.rand, 20.0.rand);
			},
			moveCam: {|startcoords, endcoords, time, rate, done|
				mikro.graphics.moveCamera(startcoords, endcoords, time, rate, done)
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
				stats.flats.mean.explin(0.001, 0.6, 1/31, 1/11), 
				stats.amps.mean.explin(0.001, 1.0, 50.0, 500.0), 
				stats.amps.mean.explin(0.001, 1.0, 50.0, 300.0),
				stats.freqs.mean.explin(20.0, 20000.0, 1/31, 1/11),
				stats.flats.mean.explin(0.001, 0.6, 0.0, 1/23)
				]
			},
			attachCam: { [false] },
			stopCam: { 
				var sp = Spherical(stats.amps.mean.explin(0.001, 1.0, 50.0, 200.0) + 200.0, 2pi.rand, 2pi.rand);
				[sp.x, sp.y, sp.z]
			},
			moveCam: {  
				[
					[200.0.rand, 200.0.rand, 200.0.rand, 0.0, 0.0, 0.0], 
					[100.0.rand, 100.0.rand, 100.0.rand, 0.0, 0.0, 0.0],
					rrand(10, 30),
					10
				]
			}
		);
				
		Tdef(\initGraphics, {
			mikro.graphics.setFrameRate(16.0);
			mikro.graphics.setBackground(0.1, 0.1, 0.1);
			mikro.graphics.initWorld(20, 20, 20, mikro.analyzer.numcoef);
			1.wait;
			mikro.graphics.initContinuous;
			mikro.graphics.setViewpoint(200.0, 150.0, 180.0, 0.0, 0.0, 0.0);
			1.wait;
			mikro.graphics.setAdd(rrand(0.005, 0.01));
			mikro.graphics.activateSwarm(16, 400.0, 400.0, 400.0, 0.9, 50.0, 8.0, 5.0, 100.0);
			1.wait;
			mikro.graphics.sendBoidPattern(2, 1, 0);
		}).play;
		
		SystemClock.sched(10, {
			
			mikro.analyzer.addMFCCResponderFunction({|ti, re, ms, an|
				stats.mfc = ms[3..mikro.analyzer.numcoef+2];
				mikro.graphics.sendSOMVector(stats.mfc);
				mikro.graphics.setSwarm(0.9, 50.0, 8.0, (0.25 - stats.mfc[0] ).abs.linlin(0.1, 1.0, 5.0, 40.0), 100.0)
			});
		
			mikro.analyzer.putEventResponderFunction(\geenstats, {|ti, re, ms, an|
				if (ms[2] == 2) {
					stats.amps.pop;
					stats.amps.insert(0, ms[3]);
					if (stats.amps.keep(5).mean < 0.001) {
						mikro.graphics.setBoidCam(false, false);
						mikro.graphics.stopRotation;
						mikro.graphics.stopMove;			
					}
				};
				if (ms[2] == 5) {
					stats.freqs.pop;
					stats.freqs.insert(0, ms[3]);
				};
				if (ms[2] == 4) {
					stats.flats.pop;
					stats.flats.insert(0, ms[3])
				}
			});
			
			geen.prepareForPlay(false, 1, decoder.bus, {
				mikro.analyzer.onsetAction = {
					var action;
					if (stats.amps.mean.coin) {
						action = cameraWeights.values.wchoose(cameraWeights.keys(Array));
						Post << "Camera: " << action << Char.nl;
						if (action != 'stopCam') {  
							mikro.graphics.setBoidCam(false, false);
							mikro.graphics.stopRotation;
							mikro.graphics.stopMove;			
						};
						cameraActions[action].(*cameraActionParams[action].())
					};
					
					if (stats.amps.mean > 0.5) {
						mikro.graphics.activateSwarm(rrand(16, 32), 400.0, 400.0, 400.0, 0.9, 50.0, 8.0, 5.0, 100.0);
					};
					
					mikro.graphics.setPattern(4, [0, 1].choose, 0.4.rand, 0, 0, 0.7, 0.7, 0.3);
					mikro.graphics.setPattern(0, [0, 1].choose, 0.2.rand, 0, 0, rrand(0.3, 0.5), rrand(0.3, 0.5), rrand(0.1, 0.3));
					mikro.graphics.setAdd(1.0.rand);
					mikro.graphics.setBackground(0.1.rand, 0.1.rand, 0.1.rand);

					if ((isActive.not) and: { mikro.analyzer.events.size > 4 }) {
						isActive = true;
						Post << "playing MikroGeen prepared sequence" << Char.nl;
						
						geen.playPreparedSequence( 
							(3..8).wchoose(Array.geom(6,16,0.8).normalizeSum), 
							stats.amps.mean.explin(0.001, 1.0, 0.3, 4.0), 
							mikro.analyzer.events.last, mikro.analyzer.eventIntervals.last, {
								isActive = false
							}
						);
					};
					this.changePattern
				};
				
			}, -6.dbamp);
			
			Post << "activating MikroGeen player" << Char.nl;
					
			nil
		});
		
	}	
	
	changePattern{
		var params;
		if (activePatterns.collect(_.asInt).sum > 2) {
			var inds, index;
			inds = activePatterns.selectIndices({|is| is });
			inds.remove(2);
			index = inds.choose;
			Post << "turning off BOID pattern " << index << Char.nl;
			mikro.graphics.sendBoidPattern(index, 0, 0);
			activePatterns[index] = false;
		};
		if (stats.amps.mean.coin) {
			params = [
				[0, 1, (0..6).choose],
				[1, 1, 0],
				[2, 1, 0],
				[3, 1, [0, 3, 4].choose],
				[4, 1, [0, 4].choose],
				[5, 1, 4]
			].choose;
			activePatterns[params.first] = true;
			Post << "activating BOID pattern " << params.first << Char.nl;
			mikro.graphics.sendBoidPattern(*params);
		}
		
	}
	
	clear{
		Post << "Clearing GeenLambda.." << Char.nl;
//		Tdef(\space).clear;
		#[xang,yang,zang].do({|name|
			Tdef(name).clear
		});
		mikro.analyzer.onsetAction = nil;
		mikro.analyzer.removeEventResponderFunction(\geenstats);
		mikro.analyzer.removeMFCCResponderFunction;
		geen.stop;
//		Tdef(\liveprocs).clear;
//		composer.releaseAllSynths(5);
		Tdef(\clearGraphics, {
			6.do({|i|
				mikro.graphics.sendBoidPattern(i, 0, 0);
			});
			mikro.graphics.stopRotation;
			0.5.wait;
			mikro.graphics.killSwarm;
			0.5.wait;
			mikro.graphics.setViewpoint(0.0, 0.0, 60.0, 0.0, 0.0, 0.0);
			0.5.wait;
			mikro.graphics.setFrameRate(32.0);
		}).play;
		mikro.stop;
		mikro.quit(false, false);
	}
}

