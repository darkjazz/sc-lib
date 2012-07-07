MikroAnalyzer{

	var <>maxdur, <>numcoef, recordInput, isMono, clusterThreshold;
	var eventResponder, <recBuffer, <>events, <synth, <krbus;
	var eventOn = false, <currentEvent, onsetResponder, <currentPitch;
	var eventResponderFunctions, <elapsedTime, startTime;
	var <>onsetAction, <>offAction, timeStamp, <>runningOnsetAction;
	var <intMarkov, <durMarkov, <ampMarkov, <clusterBoundaries, <eventdiff;
	var <clusters;
	
	var savePath = "/Users/alo/Data/mikro/lib000/";
	
	*new{|maxdur=60, numcoef=8, recordInput=true, isMono=true, clusterThreshold=0.4|
		^super.newCopyArgs(maxdur, numcoef, recordInput, isMono, clusterThreshold).init
	}
	
	init{
		
		timeStamp = Date.getDate.stamp;
		
		intMarkov = MarkovSet();
		durMarkov = MarkovSet();
		ampMarkov = MarkovSet();
		
		events = Array();
	
		fork{
			
			if (recordInput)
			{
				recBuffer = Buffer.alloc(Server.default, maxdur * Server.default.sampleRate);
				Server.default.sync;
			};			
			
			this.sendSynthDef;
						
			this.prepareResponders;						
		}

	}
	
	sendSynthDef{
		SynthDef(\eventDetector, {|krout, in, onsetth, lag, msgrate|
			var input, onsets, chain, isOn, amp, off, local, mfcc, event, flat, pch;
			input = In.ar(in);
			if (recordInput) {
				RecordBuf.ar(input, recBuffer, loop: 0);
			};
			amp = Amplitude.kr(input, lag);
			pch = Tartini.kr(input).at(0);
			chain = FFT(LocalBuf(1024), input);
			onsets = Onsets.kr(chain, onsetth);
			mfcc = MFCC.kr(chain, numcoef);
			flat = SpecFlatness.kr(chain);
			if (isMono) {
				off = LagUD.kr(Trig.kr(amp < onsetth, lag), 0.01, 0.02);
				local = LocalIn.kr;
				SendReply.kr(local, '/event', onsets, 1);
				SendReply.kr(1.0 - local, '/event', off, 0);
				isOn = SetResetFF.kr(onsets, off);
				LocalOut.kr(isOn);
				event = Impulse.kr(msgrate) * isOn;
			}{
				SendReply.kr(onsets, '/event', onsets, 1);
				event = Impulse.kr(msgrate);
			};
			SendReply.kr(Onsets.kr(chain, -40.dbamp), '/onsets', amp);
			SendReply.kr(event, '/event', amp, 2);
			SendReply.kr(event, '/event', mfcc, 3);
			SendReply.kr(event, '/event', flat, 4);
			SendReply.kr(event, '/event', pch, 5);
		}).add;		
	}
	
	prepareResponders{
		eventResponder = OSCresponderNode(Server.default.addr, '/event', {|ti, re, ms|
			elapsedTime = ti - startTime;
			ms[2].switch(
				0, {
					this.addCurrentEvent;
					this.updateEventChains;
					offAction.value(elapsedTime, re, this)
				},
				1, {
					if (isMono.not) {
						this.addCurrentEvent;
						this.updateEventChains;
					};
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
	
	addCurrentEvent{
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
			this.detectClusterBoundary;
		};		
	}
	
	detectClusterBoundary{
		if (clusterBoundaries.isNil) { clusterBoundaries = Array.with(0) };
		if (events.size > 1) {
			if (this.compareEvents(events.last, events[events.lastIndex-1]) > clusterThreshold) {
				clusterBoundaries = clusterBoundaries.add(events.lastIndex)
			};
		}
	}
	
	compareEvents{|evA, evB|
		var diff = Array();
		diff = diff.add( evB.start - (evA.start + evA.duration) );
		diff = diff.add( abs(evA.mfcs.collect(_.at(1)).mean - evB.mfcs.collect(_.at(1)).mean).mean );
		diff = diff.add( abs(evA.flts.collect(_.at(1)) - evB.flts.collect(_.at(1))).mean );
		diff = diff.add( abs(evA.amps.collect(_.at(1)) - evB.amps.collect(_.at(1))).mean );
		diff = diff.add( abs(evA.frqs.collect(_.at(1)) - evB.frqs.collect(_.at(1))).mean / 10000.0);
		if (eventdiff.isNil) { eventdiff = Array() };
		eventdiff = eventdiff.add(diff.sum);
		^diff.sum
	}
	
	generateClusterBoundaries{|threshold|
		var diffs;
		clusterThreshold = threshold ? clusterThreshold;
		diffs = Array();
		eventdiff = Array();
		events.doAdjacentPairs({|evA, evB|
			diffs = diffs.add(this.compareEvents(evA, evB))
		});
		^diffs.selectIndices({|stats| stats > clusterThreshold })
	}
	
	clusterEvents{|threshold|
		var clumps = Array();
		(Array.with(0) ++ (this.generateClusterBoundaries(threshold) + 1)).doAdjacentPairs({|a, b|
			clumps = clumps.add(b - a)
		});
		clusters = events.clumps(clumps).collect({|cluster| MikroCluster(cluster) })
		^events.clumps(clumps)
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
	
	updateEventChains{
		var index, ampa, ampb, intv;
		index = events.lastIndex;
		if (events.size > 2) {
			intv = this.eventIntervals;
			intMarkov.read(intv[intv.lastIndex - 1], intv[intv.lastIndex]);
		};
		
		if (events.size > 1) {
			durMarkov.read(events[index - 1].duration, events[index].duration);
			ampa = events[index - 1].amps.collect(_.last).maxItem + 0.01 ** 0.5;
			ampb = events[index].amps.collect(_.last).maxItem + 0.01 ** 0.5;
			ampMarkov.read(ampa, ampb);
		}
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
	
	selectIntervals{|min = 0.1, max = 2.0|
		^this.eventIntervals.select({|intr| (intr > min).and(intr < max) })
	}
	
	selectEvents{|min = 0.1, max = 6.0|
		^events.select({|ev| (ev.duration > min).and( ev.duration < max ) })
	}
	
	saveEvents{|path| MikroData.saveEvents(this, path) }
	
	loadEvents{|path| MikroData.loadEvents(this, path) }
	
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
		if (flat[1].isNaN) { flat = 0.0 };
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
	
	ampsToEnv{|maxseg=8, curve = 'lin', fix = false, normalize = false, trim = false, trimGate = 0.0001|
		var levels, times;
		levels = amps.collect(_.at(1));
		if (amps.size > maxseg) { 
			levels = levels.clump((amps.size / maxseg).floor).collect(_.mean) 
		};
		
		if (trim) {	
			levels = levels.select({|am| am > trimGate });  
			if (levels.size < 2) { levels = [0, 1, 0]; fix = false; }
		};
		
		if (fix) { levels = [0.0] ++ levels ++ [0.0] };

		if (normalize) { levels = levels.normalize(0.0, 1.0) };
		
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
	
	meanAmp{ ^amps.collect(_.at(1)).mean }
	
	peakAmp{ ^amps.collect(_.at(1)).maxItem }
	
	meanMFCC{ ^mfcs.collect(_.at(1)).mean }
	
	stdMFCC{ ^mfcs.collect(_.at(1)).stdDev }
	
	meanFlatness{ ^flts.collect(_.at(1)).mean }
	
	meanFreq{ ^frqs.collect(_.at(1)).mean }
	
}

MikroCluster{
	
	var <events;
	
	*new{|events| ^super.newCopyArgs(events) }
	
	meanDuration{ ^this.eventDurations.mean }
	
	meanMFCC{	events.collect(_.meanMFCC) }
	
	eventDurations{ ^events.collect(_.duration) }

	duration{ this.eventDurations.sum }
	
	absStartTimes{ events.collect(_.start) }
	
	relStartTimes{ ^(this.absStartTimes - events.first.start) }
	
	size{ ^events.size }
	
}
