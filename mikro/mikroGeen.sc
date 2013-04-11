MikroGeen{
	
	classvar <>archPath = "/Users/alo/Data/mikro/sets/";
	
	var nclusters, timeQuant, <metadata, datasize = 20, <clusters;
	var <durSet, <freqSet, <ampSet, <intervalSet, <clusterSet, <envSet;
	var <eventData, <allEvents, <currentEvent, <defclusters, <group;
	var player, <globalbus, <dynsynth, <currentSequence, <currentSource;
	
	*new{|nclusters, timeQuant, defdir|
		^super.newCopyArgs(nclusters, timeQuant).init(defdir)
	}
	
	init{|defdir|
		Server.default.loadDirectory(defdir ? UGenExpressionTree.defDir);
		metadata = UGenExpressionTree.loadMetadataFromDir.select({|data|
			(data.stats.mfcc.size == datasize).and(data.stats.amp.mean <= 1.0)
				.and(data.stats.mfcc.collect(_.mean).sum.isNaN.not)
		});

		durSet = MarkovSet();
		freqSet = MarkovSet();
		ampSet = MarkovSet();
		intervalSet = MarkovSet();
		clusterSet = MarkovSet();
		envSet = FuzzySet();
		
		timeQuant = timeQuant ? (2**6).reciprocal;
		
		nclusters = nclusters ? 64;
		
		SynthDef(\dynamics, {|out, in, amp, ra, rt, er, tl|
			var eq, input, sig, rev;
			eq = (
				ugen: [BLowShelf, (BPeakEQ ! 3), BHiShelf].flat,
				freq: Array.geom(5, "c 3".notemidi.midicps, 3.6),
				bw: [0.65, 1, 3.5, 1.5, 1],
				db: [4, 0, -4, 2, 6]
			);
			input = Limiter.ar(In.ar(in, 4), -1.0.dbamp, 0.1);
			sig = Mix.fill(5, {|i| eq.ugen[i].ar(input, eq.freq[i], eq.bw[i], eq.db[i]) });
			rev = GVerb.ar(sig[0] * ra, 6, rt, drylevel: 0, earlyreflevel: er, taillevel: tl);
			Out.ar(out, (sig.pow(0.2) + rev.dup.flat) * amp)
		}).add;

		SynthDef(\procgen, {|out, in, dur, amp|
			var env, input, sig, fft, bf, rot, til, tum;
			env = EnvControl.kr(size: 16);
			input = Mix(In.ar(in, 2)).tanh * EnvGen.kr(env, timeScale: dur, levelScale: amp, doneAction: 3);
			fft = FFT(LocalBuf(1024), input);
			bf = FoaEncode.ar(Array.fill(4, {IFFT(PV_Diffuser(fft, Dust.ar(10.0))) }), FoaEncoderMatrix.newAtoB );
			rot = LFNoise2.kr(bf[0].explin(0.001, 1.0, 0.5, 20.0)).range(-pi, pi);
			til = LFNoise2.kr(bf[1].explin(0.001, 1.0, 0.5, 20.0)).range(-pi, pi);
			tum = LFNoise2.kr(bf[2].explin(0.001, 1.0, 0.5, 20.0)).range(-pi, pi);
			bf = FoaTransform.ar(bf, 'rtt', rot, til, tum );
			Out.ar(out, bf)
		}).add;
		
	}
	
	updateClusters{
//		clusters = KMeans(nclusters);
		clusters = MikroMeans(nclusters);
		metadata.do({|data|
			clusters.add(data.stats.mfcc.collect(_.mean))
		});
		clusters.update
	}
	
	saveClusters{ clusters.saveData }
	
	loadClusters{|path| 
		if (clusters.isNil) {
			clusters = MikroMeans(nclusters);
			metadata.do({|data|
				clusters.add(data.stats.mfcc.collect(_.mean))
			});		
		};
		clusters.loadData(path)
	}
	
	roundFreq{|freq, octavediv=24, ref=440|
		^(2**(round(log2(freq/ref)*octavediv)/octavediv)*ref)
	}
	
	loadEventData{|path, doneAction|
		eventData = MikroData().loadPathMatch(path, doneAction)
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
					intervals = intervals.add(min(evB.start - evA.start, 4.0));
				});
				envSet.put(data.events.last.duration.round(timeQuant), 
					data.events.last.ampsToEnv(8, 'sine', true, true, true));
				intervals.doAdjacentPairs({|intA, intB| intervalSet.read(*[intA, intB].round(timeQuant))  });
				
			});
			
			allEvents = eventData.datalib.values.collect(_.events).flat;
			"Training completed...".postln;
		}
		{
			"No training data loaded. Execute loadEventData first.".warn
		}
	}
	
	initializeChain{|source, firstInterval|
		if (source.isNil) {
			source = allEvents.choose
		};
		if (firstInterval.notNil) {
			firstInterval = firstInterval.round(timeQuant)
		}
		{
			firstInterval = intervalSet.dict.choose[0]
		};
		currentEvent = (
			dur: source.duration.round(timeQuant), 
			freq: this.roundFreq(source.meanFreq),
			amp: source.peakAmp.round(timeQuant),
			env: envSet[source.duration.round(timeQuant)],
			cluster: clusters.classify(source.meanMFCC),
			int: firstInterval
		);
		
		defclusters = ();
		
		clusterSet.dict.keys(Array).do({|num|
			defclusters[num] = clusters.assignments.selectIndices({|ind| num == ind})
		});
		
	}
	
	nextEvent{
		var dur, frq, amp, int;
		dur = durSet.next(currentEvent.dur); 
		if (dur.isNil) { 
			dur = this.getNearestMatch(durSet, currentEvent.dur)
		};
		frq = freqSet.next(currentEvent.freq);
		if ( frq.isNil) {
			frq = this.getNearestMatch(freqSet, currentEvent.freq)
		};
		amp = ampSet.next(currentEvent.amp);
		if (amp.isNil) { 
			amp = this.getNearestMatch(ampSet, currentEvent.amp)
		};
		int = intervalSet.next(currentEvent.int);
		if (int.isNil) {
			int = this.getNearestMatch(intervalSet, currentEvent.int)
		};
		currentEvent = (
			dur: dur, 
			freq: frq,
			amp: amp,
			env: envSet[dur],
			cluster: clusterSet.next(currentEvent.cluster),
			int: int
		)		
	}
	
	getNearestMatch{|set, value|
		var index, keys, diff;
		keys = set.dict.keys(Array);
		diff = keys.collect({|num| abs(num-value) });
		index = diff.indexOf(diff.minItem);
		^set.next(keys[index])
	}
	
	findNearestInCluster{|cluster|
		var clusterData, closest = inf, selectedIndex = 0;
		if (currentSource.notNil) {
			clusterData = defclusters[cluster].collect({|ind|
				clusters.data[ind]
			});
			clusterData.do({|data, i|
				var diff = (data - currentSource.meanMFCC).abs.sum;
				if (diff < closest) {
					closest = diff;
					selectedIndex = i;
				}
			});
			^defclusters[cluster][selectedIndex]
		}
		{
			^nil
		}
	}

	
	playCurrentEvent{|target, addAction, timeScale=1|
		var synth, defindex, data, bus, args, env;
		bus = Bus.audio(Server.default, 2);
		defindex = defclusters[currentEvent.cluster].choose;
		data = metadata[defindex];
		args = data.args;
		args.selectIndices({|item| item > this.roundFreq("c 0".notemidi.midicps) }).do({|argindex|
			args[argindex] = this.roundFreq(args[argindex], 24, currentEvent.freq)
		});
		if (currentEvent.env.notNil) {
			if (currentEvent.env.levels.size > 16) 
				{ env = Env([0.001, 1.0, 1.0, 0.001], [0.3, 0.4, 0.3], \sine) } 
				{ env = currentEvent.env }
		} {
			 env = Env([0.001, 1.0, 1.0, 0.001], [0.3, 0.4, 0.3], \sine)
		};
		synth = Synth(data.defname, [\out, bus] ++ args, target, addAction);
		Synth.after(synth, \procgen, [\out, globalbus, \in, bus, \dur, currentEvent.dur * timeScale, 
			\amp, currentEvent.amp])
			.setn(\env, env);
		SystemClock.sched(currentEvent.dur * timeScale, { bus.free; bus = nil; });
	}
	
	playEvent{|event, target, addAction, timeScale=1|
		var synth, defindex, data, bus, args, env, amp;
		bus = Bus.audio(Server.default, 2);
		defindex = this.findNearestInCluster(event.cluster);
		if (defindex.isNil) { 
			defclusters[event.cluster].choose
		};
		data = metadata[defindex];
		args = data.args;
		args.selectIndices({|item| item > this.roundFreq("c 0".notemidi.midicps) }).do({|argindex|
			args[argindex] = this.roundFreq(args[argindex], 24, currentEvent.freq)
		});
//		if (event.env.notNil) {
//			if (event.env.levels.size > 16) 
//				{ env = Env([0.001, 1.0, 1.0, 0.001], [0.3, 0.4, 0.3], \sine) } 
//				{ env = event.env }
//		} {
//			 env = Env([0.001, 1.0, 1.0, 0.001], [0.3, 0.4, 0.3], \sine)
//		};
		env = currentSource.ampsToEnv(8, 'sine', true, true, true, -60.dbamp);
		if (env.isNil or: { env.levels.size > 16 }) {
			 env = Env([0.001, 1.0, 1.0, 0.001], [0.3, 0.4, 0.3], \sine)
		};
		if (currentSource.notNil) {
			amp = currentSource.peakAmp
		}
		{
			amp = -6.dbamp
		};
		synth = Synth(data.defname, [\out, bus] ++ args, target, addAction);
		Synth.after(synth, \procgen, [\out, globalbus, \in, bus, \dur, event.dur * timeScale, 
			\amp, event.amp * amp ])
			.setn(\env, env);
		SystemClock.sched(event.dur * timeScale, { bus.free; bus = nil; });		
	}
		
	playSequence{|size=4, target, addAction, timeScale=1, source, doneAction|
		var seq;
		Routine({
			if (currentEvent.isNil) {
				this.initializeChain(source)
			};
			size.do({|i|
				this.playCurrentEvent(target, addAction, timeScale);
				currentEvent.int.wait;
				this.nextEvent;
			});
			doneAction.(this)
		}).play
	}
	
	playPreparedSequence{|size=4, timeScale=1, source, firstInterval, doneAction|
		var seqdur;
		"---".postln;
		(\dur: source.duration.round(timeQuant), \intr: firstInterval.round(timeQuant)).postln;
		currentSource = source;
		this.prepareSequence(size, source, firstInterval);
		seqdur = ([0.0] ++ currentSequence.keep(size-1).collect(_.int)).integrate + 
			currentSequence.collect(_.dur);
		seqdur = seqdur.maxItem;
		Post << "Sequence started with total duration: " << seqdur << Char.nl;
		currentSequence.do(_.postln);

		Tdef(\currentSequence, {
			currentSequence.do({|event|
				this.playEvent(event, dynsynth, \addBefore, timeScale);
				event.int.wait;
			});
			if (seqdur < 3.0) {
				[
				{
					Post << "Repeating sequence in reverse: " << seqdur << Char.nl;
					currentSequence.reverse.do({|event|
						this.playEvent(event, dynsynth, \addBefore, timeScale);
						event.int.wait;
					});
				},
				{
					Post << "Repeating sequence scrambled: " << seqdur << Char.nl;
					currentSequence.scramble.do({|event|
						this.playEvent(event, dynsynth, \addBefore, timeScale);
						event.int.wait;
					});
				},
				{
					Post << "Repeating sequence rotated: " << seqdur << Char.nl;
					currentSequence.rotate([-2, 2].choose).do({|event|
						this.playEvent(event, dynsynth, \addBefore, timeScale);
						event.int.wait;
					});
				},
				{
					Post << "Repeating sequence normal: " << seqdur << Char.nl;
					currentSequence.do({|event|
						this.playEvent(event, dynsynth, \addBefore, timeScale);
						event.int.wait;
					});
				}
				].choose.value
			};
			doneAction.()
		}).play
	}
	
	prepareSequence{|size=4, source, firstInterval|
		this.initializeChain(source, firstInterval);
		currentSequence = Array();
		size.do({
			currentSequence = currentSequence.add(currentEvent);
			this.nextEvent;
		})
	}
	
	prepareForPlay{|record=false, timeScale=1, decoderbus, doneFunc, amp=1|
		Routine({
			globalbus = Bus.audio(Server.default, 4);
			group = Group.before(1);
			Server.default.sync;
			dynsynth = Synth.tail(group, \dynamics, [\out, decoderbus, \in, globalbus, \amp, amp,
				\ra, 0.06, \rt, 3, \er, 0.7, \tl, 0.7]);
			if (record) { Server.default.record };
//			CmdPeriod.add({ this.stop });
			Server.default.sync;
			if (currentEvent.isNil) { 
				this.initializeChain
			};
			doneFunc.();
		}).play
	}
	
	play{|record=false, timeScale=1, bus, amp = 1|
		this.prepareForPlay(record, timeScale, bus, {
			player = Routine({
				loop({
					this.playCurrentEvent(dynsynth, \addBefore, timeScale);
					currentEvent.int.wait;
					this.nextEvent;
				})
			}).play
		}, amp);
	}
	
	stop{
		if (Server.default.recordNode.notNil) { Server.default.stopRecording }; 
		player.stop;
		SystemClock.sched(currentEvent.dur, {
			dynsynth.free;
			group.free; 
			globalbus.free;
			nil
		});
	}
	
	getDefArgs{|name|
		var ev = metadata.select({|data| data.defname == name.asString }).first;
		if (ev.notNil) { ^ev.args } { ^nil }
	}
	
	playGEPSynth{|name|
		var args, ev;
		ev = metadata.select({|data| data.defname == name }).first;
		if (ev.notNil) {
			^Synth(name, [\out, 0] ++ ev.args)
		}
		{
			"Data not found..".warn;
			^nil
		}
	}
	
	archiveSets{
		var path;
		path = this.getArchivePath;
		durSet.writeArchive(path ++ "durSet");
		freqSet.writeArchive(path ++ "freqSet");
		ampSet.writeArchive(path ++ "ampSet");
		intervalSet.writeArchive(path ++ "intervalSet");
		clusterSet.writeArchive(path ++ "clusterSet");
		envSet.writeArchive(path ++ "envSet");
	}
	
	loadSets{
		var path; 
		path = this.getArchivePath;
		durSet = MarkovSet.readArchive(path ++ "durSet");
		freqSet = MarkovSet.readArchive(path ++ "freqSet");
		ampSet = MarkovSet.readArchive(path ++ "ampSet");
		intervalSet = MarkovSet.readArchive(path ++ "intervalSet");
		clusterSet = MarkovSet.readArchive(path ++ "clusterSet");
		envSet = MarkovSet.readArchive(path ++ "envSet");
	}
	
	getArchivePath{
		var path, dir, temp;
		temp = MikroData.loadPath.split($/);
		temp.pop;
		dir = temp.last;
		path = this.class.archPath ++ dir ++ "/";
		if (File.exists(path).not) {
			File.mkdir(path)
		};
		^path;	
	}
		
}

LiveGeen : MikroGeen{
	
	activate{|decoderbus, amp|
		Routine({
			globalbus = Bus.audio(Server.default, 4);
			group = Group.before(1);
			Server.default.sync;
			dynsynth = Synth.tail(group, \dynamics, [\out, decoderbus, \in, globalbus, \amp, amp,
				\ra, 0.06, \rt, 3, \er, 0.7, \tl, 0.7]);
		}).play		
	}
	
}