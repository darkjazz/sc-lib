MikroComposer{
	var mikro, <descLib, <stateBuses, <activeSynths;
	
	var descFile = "/Users/alo/Development/mikro/audio/synthdesc.scd";
	
	*new{|mikro, libName|
		^super.newCopyArgs(mikro).init(libName)
	}
	
	init{|libName|
		descLib = SynthDescLib(libName ? \mikro);
		descFile.load.do(_.add(descLib.name));
		activeSynths = ();		
	}
			
	play{|defname, env, argstream, buffer, dur|
		var params, desc, synth;
		desc = descLib[defname];
		if (dur.isNil) {
			env = env ? Env([0.001, 1.0, 1.0, 0.001], [0.3, 0.4, 0.3], \lin, 2, 1);
		}
		{
			env = env ? Env([0.001, 1.0, 1.0, 0.001], [0.3, 0.4, 0.3]);
		};
		params = [\out, mikro.decoder.bus, \in, mikro.inputBus, \buf, buffer, \dur, dur ? 1.0];
		params = params ++ desc.metadata.specs.collect(_.map(argstream.next)).asKeyValuePairs;
		synth = Synth.tail(mikro.group, desc.name, params).setn(\env, env);
		activeSynths[synth.nodeID.asSymbol] = synth;
		if (env.loopNode.isNil) { this.clearSynth(dur) };
		^synth.nodeID
	}
	
	clearSynth{|id, time|
		SystemClock.sched(time, { 
			this.unmapStates(id);
			mikro.graphics.removeStatesFunction(id.asSymbol);
			mikro.graphics.removeBmuFunction(id.asSymbol);
			activeSynths[id.asSymbol] = nil;
		});
	}
	
	releaseSynth{|id, time|
		activeSynths[id.asSymbol].set(\gate, time.neg);
		this.clearSynth(id, time);
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
	
}

MikroComposerOld{
		
	var mikro, <liveProcs, <bufProcs, <synthProcs, <liveProcDict, <bufProcDict, <synthProcDict;
	var <activeLiveSynths, <activeBufSynths, <activeSynthSynths, <>synthLimit = 15, <>cpuLimit = 60.0;
	var lastDefEvent, defEvents, lastTime, <actions, synthSeq, <intervals;
	var <>liveProcFile = "mikroPROClive.scd", <>bufProcFile = "mikroPROCbuf.scd"; 
	var <>synthProcFile = "mikroPROCsynth.scd";
	
	var procPath = "/Users/alo/Development/mikro/audio/";
		
	*new{|mikro, map|
		^super.newCopyArgs(mikro).init
	}
	
	init{
		liveProcs = (procPath ++ liveProcFile).load;
		bufProcs = (procPath ++ bufProcFile).load;
		synthProcs = (procPath ++ synthProcFile).load;	

		activeLiveSynths = Event();
		activeBufSynths = Event();
		activeSynthSynths = Event();
		actions = (
			startSynth: (order: 0, weight: 0.1),
			setParams: (order: 1, weight: 0.8),
			freeSynth: (order: 2, weight: 0.1)
		);
				
		intervals = MarkovSet();
		
		defEvents = MarkovSet();
		
		synthSeq = 0;		
		
		this.sendSynthDefs;
				
		(bufProcDict ++ synthProcDict).scramble.doAdjacentPairs({|procA, procB|
			this.addEvent(procA, procA);
			this.addEvent(procA, procB);
			this.addEvent(procB, procA);
			this.addEvent(procB, procB);
//			(bufProcDict ++ synthProcDict).do({|procC|
//				this.addEvent(procC, procA);
//				this.addEvent(procC, procB)
//			})
		});
		
		Array.series(32, 0.0625, 0.0625).doAdjacentPairs({|intA, intB| 
			this.addInterval(intA, intA);
			this.addInterval(intA, intB);
			this.addInterval(intB, intA);
			this.addInterval(intB, intB);
		});
		
	}
	
	addInterval{|intA, intB| intervals.read(intA, intB) }
	
	addEvent{|evA, evB| defEvents.read(evA, evB) }
	
	sendSynthDefs{
		liveProcDict = Array();
		
		liveProcs.keysValuesDo({|name, proc|
			liveProcDict = liveProcDict.add(name);
			proc.def.add
		});
		
		bufProcDict = Array();
		
		bufProcs.keysValuesDo({|name, proc|
			bufProcDict = bufProcDict.add(name);
			proc.def.add
		});		
		
		synthProcDict = Array();
		synthProcs.keysValuesDo({|name, proc|
			synthProcDict = synthProcDict.add(name);
			proc.def.add
		})
	}			
		
	startSynth{
		var defEvent;
		if (this.countSynths < synthLimit )
		{
			defEvent = defEvents.next(lastDefEvent) ? lastDefEvent;
			if (this.isLiveSynth(defEvent))
			{
				this.startLiveSynth(defEvent, Env([0, 1, 1, 0], [0.1, 0.4, 0.5], \sine, 2, 1) )
			}
			{
				this.startBufSynth(defEvent, Env([0, 1, 1, 0], [0.1, 0.4, 0.5], \sine, 2, 1), 
					mikro.analyzer.events.choose.buffer
				)
			}
		}
	}
		
	startSequence{|length, events, firstEvent, firstInterval|
		var seq, int, defstream, intstream, lastEvent, lastInterval, ev, env;
		seq = Array.fill(length, { lastEvent = defEvents.next(lastEvent ? firstEvent) });
		int = Array.fill(length, { lastInterval = intervals.next(lastInterval ? firstInterval) });
		defstream = Pseq(seq, 1).asStream;
		intstream = Pseq(int, 1).asStream;
		Routine({
			length.do({|i|
				var defname = defstream.next;
				if ((this.countSynths < synthLimit).and(Server.default.avgCPU < cpuLimit)) {
					ev = events.wchoose(Array.geom(events.size, events.size, 1.3333.reciprocal));
					if (this.isBufSynth(defname)) {
						this.startBufSynth(
							defname, 
							Env([0, 1, 1, 0], [0, 1, 0], \sine, 2, 1), 
							ev.buffer, 
							events.choose.buffer, 
							if (ev.duration < 1.0) { ev.duration * [2, 4, 8].choose } { ev.duration }, 
							1.0, 
							Pseq(mikro.graphics.states, inf).asStream
						)
					}
					{
						env = ev.ampsToEnv(8, \sine, false, true);
						this.startSynthSynth(
							defname, 
							if (env.times.size < 2) { Env.sine } { env },
							if (ev.duration < 0.1) { ev.duration * 10 } { ev.duration }, 
							ev.frqs.choose.last, 
							Pseq(mikro.graphics.states, inf).asStream
						)
					}
				};
				intstream.next.wait;
			})
		}).play
	}
	
	countSynths{ ^(activeLiveSynths.size + activeBufSynths.size + activeSynthSynths.size) }
	
	isLiveSynth{|key| ^liveProcs.includesKey(key) }
	
	isBufSynth{|key| ^bufProcs.includesKey(key) } 
	
	startLiveSynth{|key, env|
		var abmu, params, proc, synth;
		env = env ? Env([0, 1, 1, 0], [0.3, 0.4, 0.3]);
		abmu = mikro.graphics.bmu.vector ? Array.rand(mikro.graphics.vectorSize, 0.0, 1.0);
		abmu = Pseq(abmu.clip(0.0, 1.0), inf).asStream;
		proc = liveProcs[key];
		params = [\out, mikro.decoder.bus, \in, mikro.inputBus];
		params = params ++	proc.specs.collect({|spec| spec.map(abmu.next) }).asKeyValuePairs;
		
		params.postln;
		
		synth = Synth.tail(mikro.group, key, params).setn(\env, env.asArray);
		this.setn(synth, proc);
		activeLiveSynths[synth.nodeID.asSymbol] = synth;
		mikro.graphics.putStatesFunction(synth.nodeID.asSymbol, {|states|
			var args = Pseq(states, inf).asStream;
			synth.set(*proc.specs.collect(_.map(args.next)).asKeyValuePairs)
		});
		^synth.nodeID
	}
		
	startBufSynth{|key, env, buffer, buffer2, dur, releaseTime = 1, argstream|
		var params, proc, synth, bufm;
				
		env = env ? Env([0, 1, 1, 0], [0.3, 0.4, 0.3]);
		proc = bufProcs[key];
		params = [\out, mikro.decoder.bus, \in, mikro.inputBus, \buf, buffer];
		if (proc.def.name == \bufmod) { params = params ++ [\bufm, buffer2] };
		params = params ++	proc.specs.collect({|spec| spec.map(argstream.next) }).asKeyValuePairs;
		synth = Synth.tail(mikro.group, key, params).setn(\env, env.asArray);
		this.setn(synth, proc);
		activeBufSynths[synth.nodeID.asSymbol] = synth;
		if (dur > 1) {
			mikro.analyzer.putEventResponderFunction(synth.nodeID.asSymbol, {
				var args, states = Pseq(mikro.graphics.states, inf).asStream;
				synth.set(*proc.specs.collect({|spec| spec.map(states.next) }).asKeyValuePairs )
			});
		};
		SystemClock.sched(dur - 0.1, {
			synth.set(\gate, releaseTime.neg);
			SystemClock.sched(releaseTime, { activeBufSynths[synth.nodeID.asSymbol] = nil; nil });
			mikro.analyzer.removeEventResponderFunction(synth.nodeID.asSymbol);
			nil
		})
		
	}
	
	startSynthSynth{|key, env, dur, frq, argstream|
		var synth, params, proc;
		proc = synthProcs[key];
		frq = this.transposeValueToRange(frq, proc.frange.min, proc.frange.max);
		params = [\ou, mikro.decoder.bus, \ea, 99, \dr, dur, \fr, frq];
		params = params ++ proc.specs.collect({|spec| spec.map(argstream.next) }).asKeyValuePairs;
		synth = Synth.tail(mikro.group, key, params).setn(\env, env.asArray);
		this.setn(synth, proc);
		activeSynthSynths[synth.nodeID.asSymbol] = synth;
		SystemClock.sched(dur, {
			activeSynthSynths[synth.nodeID.asSymbol] = nil;
			nil
		})
	}	
	
	transposeValueToRange{|value, min, max, depth = 6|
		if (value > max) { value = this.transposeValueToRange(value / 2, min, max, depth + 1) };
		if (value < min) { value = this.transposeValueToRange(value * 2, min, max, depth + 1) };
		if (depth > 6) { value = max + min / 2 };
		^value
	}
	
	setn{|synth, proc|
		if (proc.includesKey('setn')) {
			proc.setn.keysValuesDo({|key, val|
				synth.setn(key, 
					if (val.curve == \exp) 
						{ Array.geom(val['size'], val['start'], val['step']) }
						{ Array.series(val['size'], val['start'], val['step']) } 
				)
			});
		};
		if (proc.includesKey('array')) {
			proc['array'].keysValuesDo({|key, val|
				synth.setn(key, val)
			})
		};
		
	}
			
	freeSynth{|id, releaseTime = 1|
		var weights;
		if (id.isNil)
		{
			if (activeLiveSynths.size > activeBufSynths.size) {
				weights = activeLiveSynths.keys(Array).asInt - 1000;
				id = activeLiveSynths.keys(Array).wchoose(weights.reverse.normalizeSum);
			}
			{
				weights = activeBufSynths.keys(Array).asInt - 1000;
				id = activeBufSynths.keys(Array).wchoose(weights.reverse.normalizeSum);
			}	
		};
		if (activeLiveSynths.includesKey(id.asSymbol))
		{
			activeLiveSynths[id.asSymbol].set(\gate, releaseTime.neg);
			activeLiveSynths[id.asSymbol] = nil;
			mikro.graphics.removeBmuFunction(id.asSymbol);
			mikro.graphics.removeStatesFunction(id.asSymbol);
		};
		if (activeBufSynths.includesKey(id.asSymbol))
		{
			activeBufSynths[id.asSymbol].set(\gate, releaseTime.neg);
			activeBufSynths[id.asSymbol] = nil;		
		}
	}	
	
}
