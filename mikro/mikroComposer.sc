MikroComposer{
	var mikro, <procs, <recognizer, <timeQuant, <roundAmp, <descLib, <stateBuses, <activeSynths;
	var <timeChain, <durChain, <ampChain, <freqChain, <intChain, <envSet, <runningStats;
	var <liveprocs, <>limitLiveProcs, <graphControl, <eventData, <allEvents;
	
	var descFile = "/Users/alo/Development/mikro/audio/synthdesc.scd";
	
	*new{|mikro, procs, recognizer, libName, timeQuant, roundAmp|
		^super.newCopyArgs(mikro, procs, recognizer, timeQuant, roundAmp).init(libName)
	}
	
	init{|libName|
		descLib = SynthDescLib(libName ? \mikro);
		descFile.load.do(_.add(descLib.name));
		activeSynths = ();		
		timeQuant = timeQuant ? 64.reciprocal;
		roundAmp = roundAmp ? -60.dbamp;
		
		timeChain = MarkovSet();
		durChain = MarkovSet();
		ampChain = MarkovSet();
		freqChain = MarkovSet();
		intChain = MarkovSet();
		envSet = FuzzySet();
		
		mikro.analyzer.addEventFunc = {|events|
			this.updateEventChains(events)
		};
		
		liveprocs = descLib.synthDescs.select({|desc| desc.metadata.type == \live }).values.asArray.collect({|desc|
			if (desc.metadata.activeIndex.notNil) {
				MikroLiveProc(
					name: desc.name,
					activeIndex: desc.metadata.activeIndex,
					maxweight: desc.metadata.maxweight,
					mindur: desc.metadata.mindur,
					fadetime: desc.metadata.fadetime
				)
			}
			{
				MikroLiveProc(
					name: desc.name,
					maxweight: desc.metadata.maxweight,
					mindur: desc.metadata.mindur,
					fadetime: desc.metadata.fadetime
				).activationRange_(desc.metadata.activationRange)
			}
		});
		
		limitLiveProcs = 3;
		
		runningStats = (
			amps: (arr: (0 ! 20), avg: 0.0),
			flats: (arr: (0 ! 20), avg: 0.0),
			freqs: (arr: (0 ! 20), avg: 0.0)
		);
		
		graphControl = (
			activePatterns: [],
			maxCount: 3,
			locked: false,
			weight: 0.0,
			patStream: Pwxrand((0..15), 
				[9, 5, 8, 2, 5, 4, 7, 6, 10, 7, 5, 8, 6, 8, 10, 8].normalizeSum, inf).asStream
		);
		
		eventData = MikroData().loadPathMatch(doneAction: { this.trainSets });
	
	}
	
	roundFreq{|freq, octavediv=24, ref=440|
		^(2**(round(log2(freq/ref)*octavediv)/octavediv)*ref)
	}	
	
	trainSets{
		if (eventData.notNil) {
			eventData.datalib.keysValuesDo({|key, data|
				var intervals = Array(), freqIntervals = Array();
				data.events.doAdjacentPairs({|evA, evB|
					durChain.read(*[evA.duration, evB.duration].round(timeQuant));
					freqChain.read(*this.roundFreq([evA.meanFreq, evB.meanFreq]));
					ampChain.read(*[evA.peakAmp, evB.peakAmp].round(roundAmp));
					envSet.put(evA.duration.round(timeQuant), evA.ampsToEnv(8, 'sine', true, true, true));
					intervals = intervals.add(evB.start - evA.start);
					freqIntervals = freqIntervals.add(
						this.roundFreq(evA.meanFreq)/this.roundFreq(evB.meanFreq)
					);
				});
				envSet.put(data.events.last.duration.round(timeQuant), 
					data.events.last.ampsToEnv(8, 'sine', true, true, true));
				intervals.doAdjacentPairs({|intA, intB| timeChain.read(*[intA, intB].round(timeQuant))  });
				freqIntervals.doAdjacentPairs({|intA, intB| intChain.read(intA, intB)  })
				
			});
			
			allEvents = eventData.datalib.values.collect(_.events).flat;
		}
		{
			"No training data loaded. Execute loadEventData first.".warn
		}
		
	}
	
	start{|liveProcLag=10.0|
		this.activateLiveProcs(liveProcLag);
		recognizer.run;
	}
	
	stop{
		recognizer.stop;
		this.cleanupLiveProcs;
	}
				
	play{|defname, env, argstream, buffer, dur|
		var params, desc, synth;
		desc = descLib[defname.asSymbol];
		if (dur.isNil) {
			env = env ? Env([0.001, 1.0, 1.0, 0.001], [0.3, 0.4, 0.3], \sine, 2, 1);
		}
		{
			env = env ? Env([0.001, 1.0, 1.0, 0.001], [0.3, 0.4, 0.3]);
		};
		params = [\out, mikro.decoder.bus, \in, mikro.input.bus, \dur, dur ? 1.0];
		if (buffer.notNil) {
			params = params ++ [ \buf, buffer]
		};
		params = params ++ desc.metadata.specs.collect(_.map(argstream.next)).asKeyValuePairs;
		synth = Synth.tail(mikro.group, desc.name, params).setn(\env, env);
		Post << "Params for synth " << defname << ":" << synth.nodeID 
			<< " - " << params << Char.nl;
		activeSynths[synth.nodeID.asSymbol] = synth;
		if (env.loopNode.isNil) { this.clearSynth(dur) };
		^synth.nodeID
	}
	
	clearSynth{|id, time|
		SystemClock.sched(time + 0.5, { 
			this.unmapStates(id);
			mikro.graphics.removeStatesFunction(id.asSymbol);
			mikro.graphics.removeBmuFunction(id.asSymbol);
			activeSynths[id.asSymbol] = nil;
			Post << "node " << id << " cleared" << Char.nl;
			nil
		});
	}
	
	releaseSynth{|id, time, doneAction|
		activeSynths[id.asSymbol].set(\gate, time.neg);
		this.clearSynth(id, time);
		Post << "node " << id << " released in " << time << " seconds" << Char.nl;
		SystemClock.sched(time + 0.5, {
			doneAction.();
			Post << "done action for node " << id << " executed" << Char.nl;
			nil
		})
	}
		
	mapStates{|id, argMap|
		var desc, synth;
		synth = activeSynths[id.asSymbol];
		desc = descLib[synth.defName.asSymbol];
		mikro.graphics.putStatesFunction(synth.nodeID.asSymbol, {|states|
			var params = ();
			argMap.keysValuesDo({|name, index|
				params[name] = desc.metadata.specs[name].map(states[index])
			});
			synth.set(*params.asKeyValuePairs)
		});
	}
		
	unmapStates{|id|
		mikro.graphics.removeStatesFunction(id.asSymbol)
	}
	
	mapStatesAndRelease{|id, argMap, time|
		this.mapStates(id, argMap);
		SystemClock.sched(time, {
			this.unmapStates(id);
			nil
		});
	}	
	
	mapVector{|id, argMap|
		var desc, synth;
		synth = activeSynths[id.asSymbol];
		desc = descLib[synth.defName.asSymbol];
		mikro.graphics.putBmuFunction(synth.nodeID.asSymbol, {|bmu|
			var params = ();
			argMap.keysValuesDo({|name, index|
				params[name] = desc.metadata.specs[name].map(bmu.vector[index].clip(0.0, 1.0))
			});
			synth.set(*params.asKeyValuePairs)
		});
	}
	
	unmapVector{|id|
		mikro.graphics.removeBmuFunction(id.asSymbol)
	}
	
	mapVectorAndRelease{|id, argMap, time|
		this.mapVector(id, argMap);
		SystemClock.sched(time, {
			this.unmapVector(id);
			nil
		});
	}
		
	updateEventChains{|events|
		var evA, evB, inA, inB;
		if (events.size > 2) {
			evB = events.last;
			evA = events[events.lastIndex - 1];
			inB = evA.start - evB.start;
			inA = events[events.lastIndex-2].start - evA.start;
			timeChain.read(*[inA, inB].round(timeQuant));
			inB = this.roundFreq(evB.meanFreq) / this.roundFreq(evA.meanFreq);
			inA = this.roundFreq(evA.meanFreq) / 
				this.roundFreq(events[events.lastIndex-2].meanFreq);
			intChain.read(inA, inB);
		};

		if (events.size > 1) {
			evB = events.last;
			evA = events[events.lastIndex - 1];
			durChain.read(*[evA.duration, evB.duration].round(timeQuant));
			ampChain.read(*[evA.peakAmp, evB.peakAmp].round(roundAmp));
			freqChain.read(*this.roundFreq([evA.meanFreq, evB.meanFreq]));
			envSet.put(evB.duration.round(timeQuant), evB.ampsToEnv(8, 'sine', true, true, true));
			
		}
	}
	
	setStatSize{|which, size|
		if (runningStats[which].arr.size < size) {
			runningStats[which].arr = runningStats[which].arr ++ (0 ! runningStats[which].arr.size-size)
		};

		if (runningStats[which].arr.size > size) {
			runningStats[which].arr = runningStats[which].arr.keep(size)
		}
		
	}
	
	getAvg{|which| ^runningStats[which].avg }

	activateLiveProcs{|lag=1.0|
		SystemClock.sched(lag, {
			recognizer.addResponderFunction(\liveproc, {|rec, ti|
				var mostCommon = rec.mostCommonAsDigits;
				liveprocs.do({|proc|
					var patchActive;
					if (proc.activeIndex.notNil ) {
						patchActive = mostCommon[proc.activeIndex] == 1
					}
					{
						patchActive = (mostCommon.sum >= proc.activationRange.min)
							.and(mostCommon.sum <= proc.activationRange.max)
							.and(liveprocs.collect(_.activated).asInt.sum == 0)
					};
					
					if (proc.activated) {
						if (patchActive.not) {
							if ((mikro.analyzer.elapsedTime - proc.start > proc.mindur) and: { proc.released.not }) {
								proc.released = true;
//								this.releaseSynth(proc.id, proc.fadetime, {
//									proc.activated = false;
//									proc.start = -1;
//									proc.id = -1;
//									proc.released = false;
//								});
							}
						}
					}
					{	
						if (proc.activate.not) {
							if (patchActive) {
								if (proc.weight.coin) {
									proc.activate = true;
									proc.weight = 0;
								}
								{
									proc.weight = (proc.weight + 60.reciprocal).clip(0, proc.maxweight)
								}
							}
							{
								proc.weight = (proc.weight - 60.reciprocal).clip(0, proc.maxweight)
							}
						}
					};
									
				});
				
			});
			
			mikro.analyzer.onsetAction = {|ti, re, ms|
				var selectionweights, counts, activatingSound = false, selectedproc, numActive;
				
				counts = liveprocs.collect(_.activationCount);
				
				numActive = liveprocs.collect(_.activated).asInt.sum;
				
				selectionweights = liveprocs.collect(_.activate).asInt * (counts.maxItem + 1 - counts);
				
				if (selectionweights.sum > 0) {
					selectedproc = liveprocs.wchoose(selectionweights.normalizeSum)
				};
				
				if ((selectedproc.notNil).and(numActive < limitLiveProcs)) {
									
//					selectedproc.id = this.play(selectedproc.name, 
//						Env([0, 1, 1, 0], [2.0, 1.0, 1.0], 'sine', 2, 1), 
//						Pseq(mikro.graphics.states ? Array.rand(16, 0.2, 0.5), inf).asStream
//					);
//					this.mapStates(id);
					selectedproc.incrementCount;
					selectedproc.activated = true;
					selectedproc.activate = false;
					selectedproc.start = mikro.analyzer.elapsedTime;
//					Post << "Most common: " << recognizer.mostCommonAsDigits << Char.nl;
//					Post << ti << ": " << selectedproc.name << Char.nl;
					activatingSound = true;
				
				};
				
				liveprocs.select(_.activate).do({|proc| 
					proc.activate = false;
				});
			
//				Post << "Most common: " << recognizer.mostCommonAsDigits << Char.nl;
				
				if (graphControl.activePatterns.isEmpty) {
					this.activateGraphicsPattern;
				}
				{
					if (graphControl.locked.not) {
					
						if ((graphControl.activePatterns.size < graphControl.maxCount).and(activatingSound)) {
							if (graphControl.weight.coin) {
								this.activateGraphicsPattern;
								graphControl.weight = 0.0;
							}
							{
								graphControl.weight = graphControl.weight + runningStats.amps.avg;
							}
						}
						{
							this.dectivateGraphicsPattern;
							graphControl.weight = 0.0;
						}
					}
				};
							
			};
			
			mikro.analyzer.putEventResponderFunction(\runningAmp, {|ti, re, ms|
				if (ms[2] == 2) {
					runningStats.amps.arr.pop;
					runningStats.amps.arr.insert(0, ms[3]);
				};
				if (ms[2] == 4) {
					runningStats.flats.arr.pop;
					runningStats.flats.arr.insert(0, ms[3]);
				};
				if (ms[2] == 4) {
					runningStats.freqs.arr.pop;
					runningStats.freqs.arr.insert(0, ms[3]);
				};
				runningStats.do({|stat|
					stat.avg = stat.arr.mean
				});
			});
			
			nil
		})
				
	}
	
	cleanupLiveProcs{
		recognizer.removeResponderFunction(\liveproc);
		mikro.analyzer.onsetAction = nil;
		mikro.analyzer.removeEventResponderFunction(\runningAmp);
	}
	
	changeGlobalSettings{
		var flat;
		if (liveprocs.collect(_.activated).asInt.sum > 2) {
			mikro.graphics.settings['add'] = 0.01
		}
		{
			mikro.graphics.settings['add'] = 0.99
		};
		
		if (graphControl.locked.not and: { 0.2.coin }) {
			flat = runningStats.flats.avg;
			if (flat.isNaN.not) {
				if (mikro.graphics.settings['add'] > 0.5) {
					mikro.graphics.settings['add'] = flat.linlin(0.01, 0.6, 0.995, 0.7);
				}
				{
					mikro.graphics.settings['add'] = flat.linlin(0.01, 0.6, 0.005, 0.3)
				};
				mikro.graphics.settings['transz'] = [20, 30, 40, 50].wchoose([0.1, 0.2, 0.6, 0.1]);
	//			mikro.graphics.settings['phase'] = runningStats.freqs.avg.explin(10000.0, 20.0, 1, 4);
	//			mikro.graphics.settings['symmetry'] = [0, 1, 2].wchoose([0.1, 0.3, 0.6]);
				mikro.graphics.sendSettings;
			}
		}
		
	}

	activateGraphicsPattern{
		var pat, time;
		pat = graphControl.patStream.next;
		graphControl.locked = true;
		time = rrand(2.0, 4.0);
		Post << "fade in GRAPHICS pattern " << pat << " for " << time << " seconds" << Char.nl;
		graphControl.activePatterns = graphControl.activePatterns.add(pat);
		Post << "active patterns: " << graphControl.activePatterns << Char.nl;
		mikro.graphics.fadeInPattern(pat, runningStats.amps.avg.explin(0.001, 1.0, 0.7, 1.0), time );
		SystemClock.sched(time + 1.0, { 
			graphControl.locked = false;
			this.changeGlobalSettings;
			Post << "Pattern " << pat << " active!!!" << Char.nl;
			nil 
		})
	}
	
	dectivateGraphicsPattern{
		var pat, time;
		time = rrand(2.0, 4.0);
		graphControl.locked = true;
		Post << "fade out GRAPHICS pattern " << pat << " for " << time << " seconds" << Char.nl;
		pat = graphControl.activePatterns.wchoose((graphControl.activePatterns.size..1).normalizeSum);
		mikro.graphics.fadeOutPattern(pat, time );
		graphControl.activePatterns.removeAt(graphControl.activePatterns.indexOf(pat));
		Post << "active patterns: " << graphControl.activePatterns << Char.nl;
		SystemClock.sched(time + 1.0, { 
			graphControl.locked = false; 
			this.changeGlobalSettings;			
			Post << "Pattern " << pat << " deactivated!!!" << Char.nl;
			nil			
		})
	}
	
}

MikroLiveProc{
	var <name, <>activeIndex, <>maxweight, <>mindur, <>fadetime; 
	var <>activate, <>activated, <>released, <>weight;
	var <>id, <>start, <>activationRange, <activationCount;
	
	*new{|name, activeIndex, maxweight, mindur, fadetime|
		^super.newCopyArgs(name, activeIndex, maxweight, mindur, fadetime).init
	}
	
	init{
		activate = false;
		activated = false;
		released = false;
		weight = 0.0;
		id = -1;
		start = -1;
		activationCount = 0;
        
    }
    
    incrementCount{ activationCount = activationCount + 1 }
    
}