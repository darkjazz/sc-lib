MikroGeen{
	
	var nclusters, timeQuant, metadata, datasize = 20, clusters;
	var durSet, freqSet, ampSet, intervalSet, clusterSet, envSet;
	var eventData, allEvents, <currentEvent, defclusters, <group, player, globalbus;
	
	*new{|nclusters, timeQuant, defdir|
		^super.newCopyArgs(nclusters, timeQuant).init(defdir)
	}
	
	init{|defdir|
		Server.default.loadDirectory(defdir ? UGenExpressionTree.defDir);
		metadata = UGenExpressionTree.loadMetadataFromDir.select({|data|
			data.stats.mfcc.size == datasize && data.stats.mfcc.collect(_.mean).sum.isNaN.not
		});
		
		durSet = MarkovSet();
		freqSet = MarkovSet();
		ampSet = MarkovSet();
		intervalSet = MarkovSet();
		clusterSet = MarkovSet();
		envSet = FuzzySet();
		
		timeQuant = timeQuant ? (2**6).reciprocal;
		
		nclusters = nclusters ? 64;
		
		SynthDef(\dynamics, {|in, ra, rt, er, tl|
			var eq, input, sig, rev;
			eq = (
				ugen: [BLowShelf, (BPeakEQ ! 3), BHiShelf].flat,
				freq: Array.geom(5, "c 3".notemidi.midicps, 3.6),
				bw: [0.65, 1, 3.5, 1.5, 1],
				db: [4, 0, -4, 2, 6]
			);
			input = Limiter.ar(In.ar(in, 2).tanh, -1.0.dbamp, 0.1);
			sig = Mix.fill(5, {|i| eq.ugen[i].ar(input, eq.freq[i], eq.bw[i], eq.db[i]) });
			rev = GVerb.ar(sig[0] * ra, 10, rt, drylevel: 0, earlyreflevel: er, taillevel: tl);
			Out.ar(0, ((sig**0.3) + rev) * -1.0.dbamp)
		}).add;

		SynthDef(\procgen, {|out, in, dur, amp|
			var env, input;
			env = EnvControl.kr(size: 16);
			input = In.ar(in, 2) * EnvGen.kr(env, timeScale: dur, levelScale: amp, doneAction: 3);
			Out.ar(out, input)
		}).add;
		
	}
	
	updateClusters{
		clusters = KMeans(nclusters);
		metadata.do({|data|
			clusters.add(data.stats.mfcc.collect(_.mean))
		});
		clusters.update
	}
	
	roundFreq{|freq, octavediv=24, ref=440|
		^(2**(round(log2(freq/ref)*octavediv)/octavediv)*ref)
	}
	
	loadEventData{|path|
		eventData = MikroData().loadPathMatch
	}
	
	trainSets{
		if (eventData.notNil) {
			eventData.datalib.keysValuesDo({|key, data|
				var intervals = Array();
				data.events.doAdjacentPairs({|evA, evB|
					durSet.read(*[evA.duration, evB.duration].round(timeQuant));
					freqSet.read(*this.roundFreq([evA.meanFreq, evB.meanFreq]));
					ampSet.read(*[evA.peakAmp, evB.peakAmp].round(timeQuant));
					envSet.put(evA.duration.round(timeQuant), evA.ampsToEnv(8, 'sine', true, true, true));
					clusterSet.read( clusters.classify(evA.meanMFCC), clusters.classify(evB.meanMFCC) );
					intervals = intervals.add(evB.start - evA.start);
				});
				envSet.put(data.events.last.duration.round(timeQuant), 
					data.events.last.ampsToEnv(8, 'sine', true, true, true));
				intervals.doAdjacentPairs({|intA, intB| intervalSet.read(*[intA, intB].round(timeQuant))  });
				
			});
			
			allEvents = eventData.datalib.values.collect(_.events).flat;
		}
		{
			"No training data loaded. Execute loadEventData first.".warn
		}
	}
	
	initializeChain{|source|
		source = source ? allEvents.choose;
		currentEvent = (
			dur: source.duration.round(timeQuant), 
			freq: this.roundFreq(source.meanFreq),
			amp: source.peakAmp.round(timeQuant),
			env: envSet[source.duration.round(timeQuant)],
			cluster: clusters.classify(source.meanMFCC),
			int: intervalSet.dict.choose[0];
		);
		
		defclusters = ();
		
		clusterSet.dict.keys(Array).do({|num|
			defclusters[num] = clusters.assignments.selectIndices({|ind| num == ind})
		});
		
	}
	
	nextEvent{
		var dur;
		dur = durSet.next(currentEvent.dur);
		currentEvent = (
			dur: dur, 
			freq: freqSet.next(currentEvent.freq),
			amp: ampSet.next(currentEvent.amp),
			env: envSet[dur],
			cluster: clusterSet.next(currentEvent.cluster),
			int: intervalSet.next(currentEvent.int) ? 1.0
		)		
	}
	
	play{|record=false, timeScale=1|
		player = Routine({
			var dynsynth;
			var defaultEnv = Env([0.001, 1.0, 1.0, 0.001], [0.3, 0.4, 0.3], \sine);
			globalbus = Bus.audio(Server.default, 2);
			group = Group();
			Server.default.sync;
			dynsynth = Synth.tail(group, \dynamics, [\in, globalbus, \ra, 0.06, \rt, 3, \er, 0.7, \tl, 0.7]);
			if (record) { Server.default.record };
			CmdPeriod.add({ this.stop });
			Server.default.sync;
			currentEvent ? this.initializeChain;
			loop({
				var synth, defindex, data, bus, args, env;
				bus = Bus.audio(Server.default, 2);
				defindex = defclusters[currentEvent.cluster].choose;
				data = metadata[defindex];
				args = data.args;
				args.selectIndices({|item| item > this.roundFreq("c 0".notemidi.midicps) }).do({|argindex|
					args[argindex] = this.roundFreq(args[argindex], 24, currentEvent.freq)
				});
				if (currentEvent.env.levels.size > 16) { env = defaultEnv } { env = currentEvent.env };
				synth = Synth.before(dynsynth, data.defname, [\out, bus] ++ args);
				Synth.after(synth, \procgen, [\out, globalbus, \in, bus, \dur, currentEvent.dur * timeScale, 
					\amp, currentEvent.amp])
					.setn(\env, env);
				SystemClock.sched(currentEvent.dur * timeScale, { bus.free; bus = nil; });
				currentEvent.int.wait;
				this.nextEvent;
			})
		}).play
	}
	
	stop{
		if (Server.default.recordNode.notNil) { Server.default.stopRecording }; 
		player.stop;
		SystemClock.sched(currentEvent.dur, {
			group.free; 
			globalbus.free;
			nil
		});
	}
	
}