MikroAnalyzer{

	var maxdur, numcoef;
	var eventResponder, <recBuffer, <events, <synth, <krbus;
	var eventOn = false, currentEvent;
	var eventResponderFunctions, <elapsedTime, startTime;
	var <>onsetAction, <>offAction, timeStamp;
	
	var savePath = "/Users/alo/Data/mikro/";
	
	*new{|maxdur=60, numcoef=8|
		^super.newCopyArgs(maxdur, numcoef).init
	}
	
	init{
		
		timeStamp = Date.getDate.stamp;
		
		fork{
			
			recBuffer = Buffer.alloc(Server.default, maxdur * Server.default.sampleRate);
			events = Array();
			
			Server.default.sync;
									
			SynthDef(\eventDetector, {|krout, in, onsetth, lag, msgrate|
				var input, onsets, chain, isOn, amp, off, local, mfcc, event, flat;
				input = In.ar(in);
				RecordBuf.ar(input, recBuffer, loop: 0);
				amp = Amplitude.kr(input, lag);
				chain = FFT(LocalBuf(1024), input);
				onsets = Onsets.kr(chain, onsetth);
				mfcc = MFCC.kr(chain, numcoef);
				flat = SpecFlatness.kr(chain);
				off = LagUD.kr(Trig.kr(amp < onsetth, lag), 0.01, 0.02);
				local = LocalIn.kr;
				SendReply.kr(local, '/event', onsets, 1);
				SendReply.kr(1.0 - local, '/event', off, 0);
				isOn = SetResetFF.kr(onsets, off);
				LocalOut.kr(isOn);
				event = Impulse.kr(msgrate) * isOn;
				SendReply.kr(event, '/event', amp, 2);
				SendReply.kr(event, '/event', mfcc, 3);
				SendReply.kr(event, '/event', flat, 4);
				Out.kr(krout, [event, onsets, off, flat]);
			}).add;
						
			eventResponder = OSCresponderNode(Server.default.addr, '/event', {|ti, re, ms|
				elapsedTime = ti - startTime;
				ms[2].switch(
					0, {
						if (currentEvent.notNil)
						{
							if (currentEvent.amps.size > 0) 
							{
								currentEvent.setDuration(elapsedTime);
								currentEvent.loadBuffer(recBuffer, offAction);
								events = events.add(currentEvent)
							};
							currentEvent = nil;
						}
					},
					1, {
						currentEvent = MikroEvent(elapsedTime);
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
					},
					4, {
						if (currentEvent.notNil)
						{
							currentEvent.addFlat([elapsedTime, ms[3]])
						}
					}
				);
				
				eventResponderFunctions.do({|func|
					func.value(elapsedTime, re, ms)
				});
			});
			
			eventResponderFunctions = ();
						
		}

	}
	
	putEventResponderFunction{|key, func|
		eventResponderFunctions[key] = func
	}

	removeEventResponderFunction{|key|
		eventResponderFunctions[key] = nil
	}
			
	start{|bus, target, addAction, onsetth=0.0001, lag=0.05, msgrate=30|
		if (krbus.isNil) { krbus = Bus.control(Server.default, 5) };
		synth = Synth(\eventDetector, [\krout, krbus, \in, bus, \onsetth, onsetth, 
			\lag, lag, \msgrate, msgrate], target, addAction);
		startTime = AppClock.seconds;
		eventResponder.add;
	}
	
	free{
		synth.free;
		synth = nil;
		eventResponder.remove;
		eventResponderFunctions.clear;
	}
	
	writeRecBuffer{|path|
		if (recBuffer.notNil)
		{
			recBuffer.write(
				path ? (thisProcess.platform.recordingsDir +/+ "mikroInput_" ++ timeStamp ++ ".aif")
			)
		}
	}
	
	saveEvents{
		var file, path;
		path = savePath ++ Date.getDate.stamp ++ ".events";
		file = File(path, "wb");
		file.putFloat(events.size.asFloat);
		file.putFloat(maxdur);
		file.putFloat(numcoef);
		file.putChar($\n);
		events.do({|event|
			file.putFloat(event.start);
			file.putFloat(event.duration);
			file.putFloat(event.amps.size.asFloat);
			file.putChar($\n);
			event.amps.do({|amp|
				file.putFloat(amp[0]);
				file.putFloat(amp[1]);
				file.putChar($\n);
			});
			event.mfcs.do({|mfc|
				file.putFloat(mfc[0]);
				mfc[1].do({|coef| file.putFloat(coef) });
				file.putChar($\n);
			});
			event.flts.do({|flt|
				file.putFloat(flt[0]);
				file.putFloat(flt[1]);
				file.putChar($\n);
			})
		});
		file.close;
		("events saved to file " ++ path).inform;
	}
	
	loadEvents{|path|
		var file;
		file = File(path, "rb");
		events = Array.newClear(file.getFloat.asInt);
		maxdur = file.getFloat;
		numcoef = file.getFloat;
		file.getChar;
		events.size.do({|i|
			var event, size, amps, mfcs, flts;
			event = MikroEvent(file.getFloat).duration_(file.getFloat);
			size = file.getFloat.asInt;
			file.getChar;
			amps = Array.newClear(size);
			size.do({|j|
				amps[j] = [file.getFloat, file.getFloat];
				file.getChar;
			});
			event.amps = amps;
			mfcs = Array.newClear(size);
			size.do({|j|
				mfcs[j] = [file.getFloat, Array.fill(numcoef, { file.getFloat })];
				file.getChar;
			});
			flts = Array.newClear(size);
			size.do({|j|
				flts[j] = [file.getFloat, file.getFloat];
				file.getChar;
			});
			event.flts = flts;			
			events[i] = event
		});
		file.close;
		("events loaded from file " ++ path).inform;
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
					var xc = event.start.linlin(0, totaldur, 0, 1000).asInt;
					Pen.color = Color.green;
					Pen.line(Point(xc,90), Point(xc,0));
					Pen.stroke;
					event.amps.do({|amp|
						xc = amp[0].linlin(0, totaldur, 0, 1000).asInt;
						Pen.color = Color.red;
						Pen.line(Point(xc,90), Point(xc,90-(amp[1]**0.5*90)));
						Pen.stroke;
					})
				})
			});
			
		mfcview = UserView(window, Rect(10, 300, 1000, 90))
			.drawFunc_({
				events.do({|event|
					event.mfcs.do({|mfc|
						var xc = mfc[0].linlin(0, totaldur, 0, 1000).asInt;						mfc[1].reverse.do({|coef, i|
							Pen.color = Color.blue(1.0 - coef, coef);
							Pen.line(Point(xc,10*i),Point(xc,10*i+10));
							Pen.stroke;
						})
					})
				})
			});
		
		writepath = thisProcess.platform.recordingsDir +/+ "mikroInput_" ++ timeStamp ++ ".aif";
		fork{
			this.writeRecBuffer(writepath);
			Server.default.sync;
			{ 
			sfview.soundfile = SoundFile.openRead(writepath);
			sfview.read(0, recBuffer.numFrames);
			}.defer;
		}
	}

}

MikroEvent{
	
	var <start, <>duration, <>amps, <>mfcs, <>flts, <buffer, <ampBuffer;
	
	*new{|start|
		^super.newCopyArgs(start).init
	}
	
	init{
		amps = Array();
		mfcs = Array();
		flts = Array();
	}
	
	addAmp{|amp|
		amps = amps.add(amp)
	}
	
	addMfc{|mfc|
		mfcs = mfcs.add(mfc)
	}
	
	addFlat{|flat|
		flts = flts.add(flat)
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
	
	loadAmpBuffer{|doneAction|
		ampBuffer = Buffer.loadCollection(Server.default, amps.collect(_.at(1)), action: doneAction)
	}
	
	ampsToEnv{|maxseg=8|
		
	}
	
}
