MikroComposer{
	var mikro, <procs, <recognizer, <timeQuant, <roundAmp, <descLib, <stateBuses, <activeSynths;
	var <timeChain, <durChain, <ampChain, <freqChain, <intChain, <runningStats;
	var <liveprocs, graphics;
	
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
		
		runningStats = (
			amps: (arr: (0 ! 20), avg: 0.0),
			flats: (arr: (0 ! 20), avg: 0.0),
			freqs: (arr: (0 ! 20), avg: 0.0)
		);
		
		graphics = (
			active: [],
			max: 3,
			locked: false,
			stream: Pshuf((0..15).clump(4).collect({|arr| Pwrand(arr, (4..1).normalizeSum, 1) }), inf).asStream
		);
	
	}
	
	start{|liveProcLag=10.0|
		this.activateLiveProcs(liveProcLag);
		recognizer.run;
	}
				
	play{|defname, env, argstream, buffer, dur|
		var params, desc, synth;
		desc = descLib[defname.asSymbol];
		if (dur.isNil) {
			env = env ? Env([0.001, 1.0, 1.0, 0.001], [0.3, 0.4, 0.3], \lin, 2, 1);
		}
		{
			env = env ? Env([0.001, 1.0, 1.0, 0.001], [0.3, 0.4, 0.3]);
		};
		params = [\out, mikro.decoder.bus, \in, mikro.input.bus, \buf, buffer, \dur, dur ? 1.0];
		params = params ++ desc.metadata.specs.collect(_.map(argstream.next)).asKeyValuePairs;
		synth = Synth.tail(mikro.group, desc.name, params).setn(\env, env);
		activeSynths[synth.nodeID.asSymbol] = synth;
		if (env.loopNode.isNil) { this.clearSynth(dur) };
		^synth.nodeID
	}
	
	clearSynth{|id, time|
		SystemClock.sched(time + 0.5, { 
//			this.unmapStates(id);
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
	
	roundFreq{|freq, octavediv=24, ref=440|
		^(2**(round(log2(freq/ref)*octavediv)/octavediv)*ref)
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
								this.releaseSynth(proc.id, proc.fadetime, {
									proc.activated = false;
									proc.start = -1;
									proc.id = -1;
									proc.released = false;
								});
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
				var pat, time;
				
				liveprocs.select(_.activate).do({|proc|
					proc.id = this.play(proc.name, argstream: Pseq(Array.rand(16, 0.0, 1.0), inf).asStream);
					proc.activated = true;
					proc.activate = false;
					proc.start = mikro.analyzer.elapsedTime;
					Post << "Most common: " << recognizer.mostCommonAsDigits << Char.nl;
					Post << ti << ": " << proc.name << Char.nl;
				});
			
				Post << "Most common: " << recognizer.mostCommonAsDigits << Char.nl;
				
				if (graphics.active.isEmpty) {
					pat = graphics.stream.next;
					graphics.locked = true;
					mikro.graphics.fadeInPattern(pat, runningStats.amps.avg.explin(0.001, 1.0, 0.3, 1.0) );
				}
				
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
	
}

MikroLiveProc{
	var <name, <>activeIndex, <>maxweight, <>mindur, <>fadetime; 
	var <>activate, <>activated, <>released, <>weight;
	var <>id, <>start, <>activationRange;
	
	*new{|name, activeIndex, maxweight, mindur, fadetime|
		^super.newCopyArgs(name, activeIndex, maxweight, mindur, fadetime).init
	}
	
	init{
		activate = false;
		activated = false;
		released = false;
		weight = 0.0;
		id = -1;
		start = -1
        
    }
    
}