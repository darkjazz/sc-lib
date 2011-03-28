MikroAnalyzer{

	var maxdur, numcoef, somsize, traindur, numdim, nhood, initweight, somBuffer;
	var somResponder, eventResponder, <recBuffer, <events, <synth;
	var eventOn = false, currentEvent, <somError, <somRemaining, <bmuIndex;
	var somResponderFunctions, eventResponderFunctions, <elapsedTime, startTime;
	var <>onsetAction, <>offAction;
	
	var savePath = "/Users/alo/Data/mikro/";
	
	*new{|maxdur=60, numcoef=8, somsize=40, traindur=1000, numdim=2, nhood=0.5, initw=0.5, somBuffer|
		^super.newCopyArgs(maxdur, numcoef, somsize, traindur, numdim, nhood, initw, somBuffer).init
	}
	
	init{
		
		fork{

			if (somBuffer.isNil)
			{
				somBuffer = SOMTrain.allocBuf(Server.default, somsize, numdim, numcoef, \rand)
			};
			
			recBuffer = Buffer.alloc(Server.default, maxdur * Server.default.sampleRate);
			events = Array();
			
			Server.default.sync;
			
			SynthDef(\eventDetector, {|in, onsetth, lag, msgrate|
				var input, onsets, chain, isOn, amp, off, local, mfcc, som, event;
				input = In.ar(in);
				RecordBuf.ar(input, recBuffer, loop: 0);
				amp = Amplitude.kr(input, lag);
				chain = FFT(LocalBuf(1024), input);
				onsets = Onsets.kr(chain, onsetth);
				off = amp < onsetth;
				local = LocalIn.kr;
				SendReply.kr(local, '/event', onsets, 1);
				SendReply.kr(1.0 - local, '/event', off, 0);
				isOn = SetResetFF.kr(onsets, off);
				LocalOut.kr(isOn);
				mfcc = MFCC.kr(chain, numcoef);
				event = Impulse.kr(msgrate) * isOn;
				som = SOMTrain.kr(somBuffer, mfcc, somsize, numdim, traindur, 
					nhood, event, initweight);
				SendReply.kr(event, '/event', amp, 2);
				SendReply.kr(event, '/event', mfcc, 3);
				SendReply.kr(event, '/som', som);
			}).add;
			
			eventResponder = OSCresponderNode(Server.default.addr, '/event', {|ti, re, ms|
				elapsedTime = ti - startTime;
				ms[2].switch(
					0, {
						if (currentEvent.notNil)
						{
							currentEvent.setDuration(elapsedTime);
							currentEvent.loadBuffer(recBuffer, offAction);
							events = events.add(currentEvent);
							currentEvent = nil;
						}
					},
					1, {
						currentEvent = MikroEvent(elapsedTime, somBuffer);
						onsetAction.value(elapsedTime, re, currentEvent);
					},
					2, {
						if (currentEvent.notNil)
						{
							currentEvent.addAmp([elapsedTime, ms[3]])
						}
					},
					3, {
						if (currentEvent.notNil)
						{
							currentEvent.addMfc([elapsedTime, ms[3..numcoef+2]])
						}
					}
				);
				
				eventResponderFunctions.do({|func|
					func.value(elapsedTime, re, ms)
				});
			});
			
			eventResponderFunctions = ();
			
			somResponder = OSCresponderNode(Server.default.addr, '/som', {|ti, re, ms|
				somRemaining = ms[3];
				somError = ms[4];
				bmuIndex = ms[5];
				somResponderFunctions.do({|func|
					func.value(ti, re, ms)
				})	
			});
			
			somResponderFunctions = ();

		}

	}
	
	putSOMResponderFunction{|key, func|
		somResponderFunctions[key] = func
	}

	removeSOMResponderFunction{|key|
		somResponderFunctions[key] = nil
	}

	putEventResponderFunction{|key, func|
		eventResponderFunctions[key] = func
	}

	removeEventResponderFunction{|key|
		eventResponderFunctions[key] = nil
	}
		
	start{|bus, target, addAction, onsetth=0.001, lag=0.1, msgrate=10|
		synth = Synth(\eventDetector, [\in, bus, \onsetth, onsetth, \lag, lag, \msgrate, msgrate], 
			target, addAction);
		startTime = AppClock.seconds;
		eventResponder.add;
	}
	
	free{
		synth.free;
		synth = nil;
		eventResponder.remove;
		eventResponder = nil;
		eventResponderFunctions.clear;
		somResponder.remove;
		somResponder = nil;
		somResponderFunctions.clear;
	}
	
	saveEvents{
		var file;
		file = File(savePath ++ Date.getDate.stamp ++ ".events");
		events.do({|event|
			
		})
	}
	
	visualize{
		var window, sfview, ampview, mfcview, writepath, totaldur;
		totaldur = events.last.start + events.last.duration;
		window =  Window("--:--:--", Rect(100, 100, 1020, 400)).background_(Color.grey(0.2)).front;
		sfview = SoundFileView(window, Rect(10, 10, 1000, 180))
			.gridOn_(false)
			.background_(Color.grey(0.4, 0.8))
			.waveColors_([Color.yellow]);
		
		ampview = UserView(window, Rect(10, 200, 1000, 90))
			.drawFunc_({
				events.do({|event|
					event.amps.do({|amp|
						var xc = amp[0].linlin(0, totaldur, 0, 1000).asInt;
						Pen.color = Color.red;
						Pen.line(Point(xc,90), Point(xc,90-(amp[1]**0.5*90)));
						Pen.stroke;
					})
				})
			});
			
		mfcview = UserView(window, Rect(10, 300, 1000, 90))
			.drawFunc_({
				events.do({|event|
					event.mfcs.reverse.do({|mfc|
						var xc = mfc[0].linlin(0, totaldur, 0, 1000).asInt;						mfc[1].do({|coef, i|
							Pen.color = Color.blue(coef);
							Pen.line(Point(xc,10*i),Point(xc,10*i+10));
							Pen.stroke;
						})
					})
				})
			});
		
		writepath = thisProcess.platform.recordingsDir +/+ "mikro_" ++ Date.localtime.stamp ++ ".aif";
		fork{
			recBuffer.write(writepath);
			Server.default.sync;
			{ 
			sfview.soundfile = SoundFile.openRead(writepath);
			sfview.read(0, recBuffer.numFrames);
			}.defer;
		}
	}

}

MikroEvent{
	
	var <start, <duration, <amps, <mfcs, <buffer;
	
	*new{|start|
		^super.newCopyArgs(start).init
	}
	
	init{
		amps = Array();
		mfcs = Array();
	}
	
	addAmp{|amp|
		amps = amps.add(amp)
	}
	
	addMfc{|mfc|
		mfcs = mfcs.add(mfc)
	}
	
	setDuration{|time|
		duration = time - start
	}
	
	startFrame{ ^(start*Server.default.sampleRate) }
	
	numFrames{ ^(duration*Server.default.sampleRate) }
	
	loadBuffer{|recBuffer, doneAction| 
		fork{
			buffer = Buffer.alloc(Server.default, this.numFrames);
			Server.default.sync;
			recBuffer.copyData(buffer, 0, this.startFrame, this.numFrames);
			Server.default.sync;
			doneAction.value(this);
		}
		 
	}
	
}
