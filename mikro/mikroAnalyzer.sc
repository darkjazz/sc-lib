MikroAnalyzer{

	var <maxdur, <numcoef, recordInput;
	var eventResponder, <recBuffer, <events, <synth, <krbus;
	var eventOn = false, <currentEvent, onsetResponder, <currentPitch;
	var eventResponderFunctions, <elapsedTime, startTime;
	var <>onsetAction, <>offAction, timeStamp, <>runningOnsetAction;
	
	var savePath = "/Users/alo/Data/mikro/";
	
	*new{|maxdur=60, numcoef=8, recordInput=true|
		^super.newCopyArgs(maxdur, numcoef, recordInput).init
	}
	
	init{
		
		timeStamp = Date.getDate.stamp;
		
		fork{
			
			if (recordInput)
			{
				recBuffer = Buffer.alloc(Server.default, maxdur * Server.default.sampleRate)
			};
			
			events = Array();
			
			Server.default.sync;
									
			SynthDef(\eventDetector, {|krout, in, onsetth, lag, msgrate|
				var input, onsets, chain, isOn, amp, off, local, mfcc, event, flat, pch;
				input = In.ar(in);
				if (recordInput) {
					RecordBuf.ar(input, recBuffer, loop: 0);
				};
				amp = Amplitude.kr(input, lag);
				pch = Pitch.kr(input).at(0);
				chain = FFT(LocalBuf(1024), input);
				onsets = Onsets.kr(chain, onsetth);
				mfcc = MFCC.kr(chain, numcoef);
				flat = SpecFlatness.kr(chain);
				off = LagUD.kr(Trig.kr(amp < onsetth, lag), 0.01, 0.02);
//				off = off + CheckBadValues.kr(flat); //InRange.kr(mfcc.mean, 0.25, 0.25);
				local = LocalIn.kr;
				SendReply.kr(Onsets.kr(chain, -40.dbamp), '/onsets', amp);
				SendReply.kr(local, '/event', onsets, 1);
				SendReply.kr(1.0 - local, '/event', off, 0);
				isOn = SetResetFF.kr(onsets, off);
				LocalOut.kr(isOn);
				event = Impulse.kr(msgrate) * isOn;
				SendReply.kr(event, '/event', amp, 2);
				SendReply.kr(event, '/event', mfcc, 3);
				SendReply.kr(event, '/event', flat, 4);
				SendReply.kr(event, '/event', pch, 5);
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
								if (recordInput)
								{
									currentEvent.loadBuffer(recBuffer, offAction)
								};
								events = events.add(currentEvent)
							};
							currentEvent = nil;
						};
						offAction.value(elapsedTime, re, this)
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
					},
					5, {
						currentPitch = ms[3];
						if (currentEvent.notNil)
						{
							currentEvent.addFreq([elapsedTime, ms[3]])
						}
					}
				);
				
				if (elapsedTime > maxdur) {
					currentEvent = nil;
					eventResponder.action = {|ti, re, ms|
						elapsedTime = ti - startTime;
						ms[2].switch(
							0, {
								offAction.value(elapsedTime, re, this)
							},
							1, {
								onsetAction.value(elapsedTime, re, events[events.lastIndex])
							}
						);
						eventResponderFunctions.do({|func|
							func.value(elapsedTime, re, ms, this)
						});
						
					};
				};
				
				eventResponderFunctions.do({|func|
					func.value(elapsedTime, re, ms, this)
				});
			});
			
			eventResponderFunctions = ();
			
			onsetResponder = OSCresponderNode(Server.default.addr, '/onsets', {|ti, re, ms|
				runningOnsetAction.value(ti, re, ms)
			});
						
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
		onsetResponder.add;
	}
	
	set{|name, value|
		
	}
	
	free{
		synth.free;
		synth = nil;
		eventResponder.remove;
		eventResponderFunctions.clear;
		onsetResponder.remove;
		runningOnsetAction = nil;
	}
	
	writeRecBuffer{|path|
		if (recBuffer.notNil)
		{
			recBuffer.write(
				path ? (thisProcess.platform.recordingsDir +/+ "mikroInput_" ++ timeStamp ++ ".aif")
			)
		}
	}
	
	eventIntervals{
		var intervals;
		intervals = Array();
		if (events.size > 1) {	
			events.doAdjacentPairs({|evA, evB|
				intervals = intervals.add(evB.start - evA.start)
			})
		}
		^intervals
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
			});
			event.frqs.do({|frq|
				file.putFloat(frq[0]);
				file.putFloat(frq[1]);
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
			var event, size, amps, mfcs, flts, frqs;
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
			frqs = Array.newClear(size);
			size.do({|j|
				frqs[j] = [file.getFloat, file.getFloat];
				file.getChar;
			});
			event.frqs = frqs;
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
	
	var <start, <>duration, <>amps, <>mfcs, <>flts, <>frqs, <buffer; 
	var <ampBuffer, <specFlatBuffer, <mfccBuffer, <atsFile;
	
	*new{|start|
		^super.newCopyArgs(start).init
	}
	
	init{
		amps = Array();
		mfcs = Array();
		flts = Array();
		frqs = Array();
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
	
	addFreq{|freq|
		frqs = frqs.add(freq)
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
	
	loadSpecFlatnessBuffer{|doneAction|
		specFlatBuffer = Buffer.loadCollection(Server.default, flts.collect(_.at(1)), action: doneAction)
	}
	
	loadMFCCBuffer{|doneAction|
		var numChan = mfcs.first.last.size;
		mfccBuffer = Buffer.loadCollection(Server.default, mfcs.collect(_.at(1)).flat, numChan, doneAction )
	}
	
	ampsToEnv{|maxseg=8, curve = 'lin', fix = false, normalize = false|
		var levels, times;
		levels = amps.collect(_.at(1));
		if (amps.size > maxseg) { 
			levels = levels.clump((amps.size / maxseg).floor).collect(_.mean) 
		};

		if (normalize) { levels.normalize(0.0, 1.0) };
		
		if (fix) { levels = [0.0] ++ levels ++ [0.0] };
		
		^Env(levels, levels.lastIndex.reciprocal ! levels.lastIndex, curve)
		
	}
	
	specFlatnessToEnv{|maxseg=8, curve = 'lin'|
		var levels, times;
		levels = flts.collect(_.at(1));
		if (flts.size > maxseg) { 
			levels = levels.clump((flts.size / maxseg).floor).collect(_.mean) 
		};
				
		^Env(levels, levels.lastIndex.reciprocal ! levels.lastIndex, curve)
	}
		
	bufferToAts{
		BufferToAts(buffer).convert({|ats| atsFile = ats })
	}
	
}
