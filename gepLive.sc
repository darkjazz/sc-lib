LiveGenetic{

	var <genetic, <input, <ncoef, <rate, namespace, isLive, <decoder, <graphics, <cospecies, <>resetInterval=10;
	var statRoutine, <inputAnalyzer, <gepAnalyzer, <def, <defs, <args, <>fitnessFunc, <distances, <comaps;
	var <group, <synths, <store, <codewindow, defStrings, <triggerSynth, <>triggerFunc, <nilIndices;

	*new{|genetic, input, ncoef, rate, namespace, isLive=true, decoder, graphics, target=1, addAction='addToHead'|
		^super.newCopyArgs(genetic, input, ncoef, rate, namespace, isLive, decoder, graphics)
			.init(target, addAction)
	}

	init{|target, addAction|
		cospecies = GEP(
			genetic.populationSize,
			genetic.terminals.size,
			3,
			['*', '-', '+', '/'].collect({|opname|
				AbstractFunction.methods.select({|meth| meth.name == opname }).first
			}) ++ ControlSpec.methods.select({|mth| mth.name == 'map' }),
			['w', 'x', 'y', 'z'],
			Array.class.methods.select({|mth| mth.name == 'with' }).first
		);
		comaps = #[freq, lofreq, midfreq, widefreq, phase].collect({|name|
			ControlSpec.specs[name]
		});

		cospecies.chromosomes.do({|chrom|
			chrom.fillConstants(cospecies.terminals.size, { rrand(0.0, 1.0) });
			chrom.addExtraDomain(Array.with(comaps.choose))
		});

		cospecies.mutationRate = genetic.mutationRate;
		cospecies.recombinationRate = genetic.recombinationRate;
		cospecies.transpositionRate = genetic.transpositionRate;
		cospecies.rootTranspositionRate = genetic.rootTranspositionRate;
		cospecies.geneRecombinationRate = genetic.geneRecombinationRate;
		cospecies.geneTranspositionRate = genetic.geneTranspositionRate;

		defStrings = Dictionary();

		defs = genetic.chromosomes.collect({|chrom, i|
			var defname, defstr;
			defname = (namespace ++ genetic.generationCount.asString.padLeft(3, "0")
				++ "_" ++ i.asString.padLeft(3, "0")).asSymbol;
//			defstr = chrom.asUgenExpressionTree.asSynthDefString(defname, Pan2, Normalizer);
			defstr = chrom.asUgenExpressionTree.asFoaSynthDefString(defname, Normalizer,
				UGenExpressionTree.foaControls.keys.choose);
			defStrings[defname.asSymbol] = defstr;
			{
				defstr.interpret.add
			}.try({
				chrom.score = -1;
				nil
			})
		});

		if (defs.includes(nil)) {
			nilIndices = defs.selectIndices(_.isNil);
			defs = defs.select(_.notNil)
		};

		this.convertArgs;

		SynthDef(\triggerGraphics, {|bus, th|
			var input, fft, mfcc, trig;
			input = In.ar(bus);
			fft = FFT(LocalBuf(1024), input);
			mfcc = MFCC.kr(fft, ncoef);
			trig = Onsets.kr(fft, th);
			SendReply.kr(trig, '/triggerGraphics', mfcc)
		}).add;

		if (decoder.isNil) {
			decoder = FoaDecoder()
		};

		synths = ();

		store = ();

		group = Group(target, addAction);

		input.prepare(group);
	}

	convertArgs{
		args = cospecies.chromosomes.collect({|chrom, i|
			var spec;
			spec = chrom.extraDomains.first.first.asString.drop(2);
			chrom.asExpressionTree(false).asFunctionString
				.replace("map", spec ++ ".map").interpret.value(*chrom.constants)
		});
	}

	prepareAnalyzers{

		var targetBuffer, targetBufSynth;

		if (input.isLive) { def = input.defs[0] } { def = input.defs[1] };

		if (input.thruBus.notNil) { def = input.defs[2] };

		inputAnalyzer = UGepAnalyzer(def, ncoef);

		gepAnalyzer = UGepLiveAnalyzer(ncoef, Server.local);

		decoder.start(group, \addAfter);

		statRoutine = Routine({

			Server.default.sync;

			inputAnalyzer.currentDef = def.name.asSymbol;

			inputAnalyzer.run(rate: rate, target: group);

			Server.default.sync;

			input.start;

			inputAnalyzer.synth.set(\in, input.bus);

			if (graphics.notNil) {
				triggerSynth = Synth.after(group, \triggerGraphics, [\bus, decoder.bus, \th, -40.dbamp]);
				triggerFunc = OSCFunc({|msg|
					graphics.sendSOMVector(msg.drop(3))
				}, '/triggerGraphics')
			};

			loop({
				inputAnalyzer.resetStats;
				resetInterval.wait;
//				this.postInputStats;
			})

		}).play;

	}

	compareDef{|index, dur, post=false|
		gepAnalyzer.analyzeSegment(defs[index].name,
			this.getArgsForDef(defs[index].name), dur, rate, inputAnalyzer, post, fitnessFunc);
	}

	evaluateCurrentPopulation{|dur, doneAction|
		Routine({
			defs.do({|sdef, i|
				this.compareDef(i, dur);
				(dur + 1).wait;
			});
			doneAction.(this)
		}).play
	}

	getArgsForDef{|name|
		var index;
		index = defs.indexOf(defs.select({|def| def.name.asSymbol == name.asSymbol }).first);
		^[genetic.terminals, args[index]].lace(genetic.terminals.size * 2)
	}

	meanStats{
		distances = gepAnalyzer.diffStats.collect({|stat, key|
			var means, dev, mean;
			means = [
				stat.mfcc.collect(_.last).collect(_.mean).mean,
				stat.flat.collect(_.last).mean,
				ControlSpec.specs['freq'].unmap(stat.cent.collect(_.last).mean),
				stat.amps.collect(_.last).mean
			];
			dev = [
				stat.mfcc.collect(_.last).collect(_.stdDev).stdDev,
				stat.flat.collect(_.last).stdDev,
				ControlSpec.specs['freq'].unmap(stat.cent.collect(_.last).stdDev),
				stat.amps.collect(_.last).stdDev
			];
			mean = [means.mean, dev.mean].mean;
			if (mean.isNaN) { -1 } { mean }
		})
	}

	findClosest{ ^distances.findKeyForValue(this.excludeZeros.minItem) }

	findFarthest{ ^distances.findKeyForValue(this.excludeZeros.maxItem) }

	excludeZeros{ ^distances.select({|dist| dist > 0 }).values }

	selectWeightedRandom{
		var weights, indices, indA, indB;
		indices = (0..genetic.chromosomes.lastIndex);
		weights = Array.newClear(genetic.chromosomes.size);
		distances.keys(Array).sort.do({|key, i| weights[i] = distances[key] });
		weights = weights.normalizeSum;
		indA = genetic.chromosomes[indices.wchoose(weights)];
		indB = genetic.chromosomes[indices.wchoose(weights)];
		^[indA, indB]
	}

	generateNewDefs{|indA, indB|
		var newA, newB, defnameA, defstrA, defnameB, defstrB;
		newA = GEPChromosome(indA.code.copy, genetic.terminals, genetic.numgenes, genetic.linker);
		newB = GEPChromosome(indB.code.copy, genetic.terminals, genetic.numgenes, genetic.linker);

		this.performGeneticOperations(genetic, newA);
		this.performGeneticOperations(genetic, newB);

		// recombination
		genetic.performRecombination(newA, newB);

		defnameA = (namespace ++ genetic.generationCount.asString.padLeft(3, "0")
			++ "_" ++ defs.size.asString.padLeft(3, "0")).asSymbol;
		defstrA = newA.asUgenExpressionTree.asSynthDefString(defnameA, Pan2, Normalizer);
		defs = defs.add({
			defstrA.interpret.add
		}.try({
			newA.score = -1;
			nil
		})
		);

		defnameB = (namespace ++ genetic.generationCount.asString.padLeft(3, "0")
			++ "_" ++ defs.size.asString.padLeft(3, "0")).asSymbol;
		defstrB = newB.asUgenExpressionTree.asSynthDefString(defnameB, Pan2, Normalizer);
		defs = defs.add({
			defstrB.interpret.add
		}.try({
			newB.score = -1;
			nil
		})
		);

		genetic.add(newA);
		genetic.add(newB);

		newA.setParents(indA, indB);
		newB.setParents(indA, indB);

		this.generateNewArgs(
			cospecies.chromosomes[genetic.chromosomes.indexOf(indA)],
			cospecies.chromosomes[genetic.chromosomes.indexOf(indB)]
		);
	}

	generateNewArgs{|indA, indB|
		var newA, newB, spec;
		newA = GEPChromosome(indA.code.copy, cospecies.terminals, cospecies.numgenes, cospecies.linker);
		newA.extraDomains = [[indA.extraDomains.first.first.copy]];
		newA.constants = indA.constants.copy;
		newB = GEPChromosome(indB.code.copy, cospecies.terminals, cospecies.numgenes, cospecies.linker);
		newB.extraDomains = [[indB.extraDomains.first.first.copy]];
		newB.constants = indB.constants.copy;

		this.performGeneticOperations(cospecies, newA);
		this.performGeneticOperations(cospecies, newB);
		// recombination
		cospecies.performRecombination(newA, newB);

		spec = newA.extraDomains.first.first.asString.drop(2);
		args = args.add(
			newA.asExpressionTree(false).asFunctionString
				.replace("map", spec ++ ".map").interpret.value(*newA.constants)
		);

		spec = newB.extraDomains.first.first.asString.drop(2);
		args = args.add(
			newB.asExpressionTree(false).asFunctionString
				.replace("map", spec ++ ".map").interpret.value(*newB.constants)
		);

		cospecies.add(newA);
		cospecies.add(newB);

		newA.setParents(indA, indB);
		newB.setParents(indA, indB);

	}

	performGeneticOperations{|gep, chrm|
		// mutation
		if (gep.mutationRate > 0.0) {
			gep.mutate(chrm.code);
		};

		// insert sequence transposition
		if (genetic.transpositionRate > 0.0) {
			chrm.code = gep.transposeInsertSequence(chrm.code);
		};

		// root transposition
		if (genetic.rootTranspositionRate > 0.0) {
			chrm.code = gep.transposeRoot(chrm.code);
		};

		// gene transposition
		if (genetic.geneTranspositionRate > 0.0) {
			chrm.code = gep.transposeGene(chrm.code);
		};

	}

	postInputStats{
		Post << "MFCC" << Char.nl;
		Post << inputAnalyzer.stats[inputAnalyzer.currentDef].mfcc.collect(_.mean) << Char.nl;
		Post << inputAnalyzer.stats[inputAnalyzer.currentDef].mfcc.collect(_.stdDev) << Char.nl;
		Post << "Flatness" << Char.nl;
		Post << inputAnalyzer.stats[inputAnalyzer.currentDef].flat.mean << Char.nl;
		Post << inputAnalyzer.stats[inputAnalyzer.currentDef].flat.stdDev << Char.nl;
		Post << "Centroid" << Char.nl;
		Post << inputAnalyzer.stats[inputAnalyzer.currentDef].cent.mean << Char.nl;
		Post << inputAnalyzer.stats[inputAnalyzer.currentDef].cent.stdDev << Char.nl;
		Post << "Amplitude" << Char.nl;
		Post << inputAnalyzer.stats[inputAnalyzer.currentDef].amp.mean << Char.nl;
		Post << inputAnalyzer.stats[inputAnalyzer.currentDef].amp.stdDev << Char.nl;
		Post << "Error" << Char.nl;
		Post << inputAnalyzer.stats[inputAnalyzer.currentDef].err.mean << Char.nl;
	}

//	play{|name|
//		synths[name] = GepSynth(name, this.getArgsForDef(name), group)
//	}

	play{|name, rotx=0, roty=0, rotz=0|
		defStrings[name.asSymbol].postln;
		try {
			this.sendSynthDefString(defStrings[name.asSymbol])
		};
		synths[name] = Synth.head(group, name, [\out, decoder.bus, \amp, 0,
			\rotx, rotx, \roty, roty, \rotz, rotz] ++ this.getArgsForDef(name))
	}

	free{|name|
		synths[name].free;
		synths[name] = nil
	}

	set{|name, value| synths[name].set('amp', value) }

	setRTT{|name, axis='x', value|
		synths[name].set(("rot"++axis).asSymbol, value)
	}

	fade{|name, start=0, end=0, time=1, interval=0.1|
		var value, incr, numSteps;
		numSteps = (time/interval).asInt;
		incr = end - start / numSteps;
		value = start;
		Routine({
			numSteps.do({
				synths[name].set('amp', value);
				value = value + incr;
				interval.wait
			});
			synths[name].set('amp', end);
			Post << "finished fade for " << name << Char.nl;
		}).play
	}

	tag{|name, value=1| store[name] = value  }

	postTags{ store.keysValuesDo({|key, val| Post << key << ": " << val << Char.nl  }) }

	assignCodeWindow{|document|
		var sendarray;
		if (document.isKindOf(Document).not) {
			codewindow = document ? Document("---live gep---")
		}
		{
			codewindow = document
		};
		codewindow.keyDownAction = {|doc, char, mod, uni, key|
			if ((uni == 3) and: { key == 76 })
			{
				sendarray = doc.selectedString.split(Char.nl);
				sendarray[0] = "@ " ++ sendarray[0];
				sendarray.do({|str|
					graphics.sendCodeLine(str)
				})
			}
		}
	}

	sendSynthDefString{|string, limit=72|
		var str = string.replace(" ", "");

		forBy(63, str.lastIndex, limit, {|index|
			var insert = min(str.find(",", offset: index), str.find("(", offset: index));
			str = str.insert(insert+1, Char.nl)
		});

		str.split(Char.nl).do({|line|
			graphics.sendCodeLine(line)
		})

	}

	// need to fix argument handling - wrap symbols in single quotes
	makeNdefWindow{|defname, index, rotx, roty, rotz|
		var contents;
		contents = genetic.chromosomes[index].asUgenExpressionTree
			.asFoaNdefString(defname, Normalizer, rotx, roty, rotz);
		contents = contents ++ Char.nl ++ Char.nl ++ this.getArgsForDef(defname).asString;
		^Document(defname.asString, contents).syntaxColorize
	}

}

GepSynth{

	var <defname, <defargs, <ampargs, <bus, <source, <proc, <patternPlayer;

	*new{|defname, defargs, target, ampargs|
		^super.newCopyArgs(defname, defargs, ampargs).init(target)
	}

	*newproc{|defname, defargs, ampargs|
		^super.newCopyArgs(defname, defargs, ampargs)
	}

	init{|target|
		{
			bus = Bus.audio(Server.default, 2);
			source = Synth.head(target, defname, [\out, bus, \amp, 1.0] ++ defargs);
			Server.default.sync;
			proc = Synth.after(source, \ampctr, [\in, bus, \id, source] ++ ampargs)
		}.fork
	}

	free{
		if (patternPlayer.notNil) {
			this.stopPattern
		};
		bus.free;
		source.free;
		proc.free;
	}

	set{|argname, value| proc.set(argname, value) }

	setWithPattern{|argname, pattern, dur|
		var id;
		if (patternPlayer.notNil) {
			this.stopPattern
		};
		if (argname == 'amp') {
			id = proc.nodeID
		}
		{
			id = source.nodeID
		};
		patternPlayer = Pbind(\type, \set, \id, id, \args, [argname],
			argname, pattern, \dur, dur).play
	}

	stopPattern{
		patternPlayer.stop;
		patternPlayer = nil;
	}

	nodeID{ ^source.nodeID }
}

GepProcSynth : GepSynth {

	var <>procdef, <>procargs;

	*new{|defname, defargs, target, ampargs, def, args|
		^super.newproc(defname, defargs, ampargs).procdef_(def).procargs_(args)
			.initProc(target)
	}

	initProc{|target|
		{
			procdef.add;
			Server.default.sync;
			bus = Bus.audio(Server.default, 2);
			Server.default.sync;
			source = Synth.head(target, defname, [\out, bus, \amp, 1.0] ++ defargs);
			Server.default.sync;
			proc = Synth.after(source, procdef.name, ampargs ++ [\in, bus, \id, source]
				++ procargs
			);
			Server.default.sync;
		}.fork

	}

}

GepPlayer{

	var <>data, <decoder, <graphics, <synths, <defStrings, <group, <foaSynths, foaBus, <>syncDurs = true;
	var <codewindow, <>sendEnabled=false, <>playFunc, <>freeFunc, <routines, <bpm, <bps, <beatdur;

	*new{|data, decoder, graphics|
		^super.newCopyArgs(data, decoder, graphics).init
	}

	init{
		defStrings = ();
		synths = ();
		routines = ();
		foaSynths = ();
		foaBus = ();
		group = Group.before(decoder.synth);
		if (graphics.isNil) {
			graphics = CinderApp()
		};
		this.setBPM(120);
	}

	setBPM{|value|
		bpm = value;
		bps = bpm/60;
		beatdur = bps.reciprocal;
	}

	start{|kinds=#[zoom,focus]|
		{
			kinds.do({|kind|
				var defname = ("foa"++kind.asString).asSymbol;
				SynthDef(defname, {|out, in, amp=0, ar, tr, pr, xr, yr, zr|
					var input, bf, tf;
					input = In.ar(in, 2).dup(2).flat;
					bf = FoaEncode.ar(
						Array.fill(4, { |i|
							IFFT(
								PV_Diffuser(
									FFT( LocalBuf(1024), Limiter.ar(input[i])*amp),
									Dust.kr(20.0)
								)
							)
						}), FoaEncoderMatrix.newAtoB
					);
					tf = FoaTransform.ar(bf, kind,
						LFNoise1.kr(ar).range(-pi/2, pi/2),
						LFNoise1.kr(tr).range(-pi, pi),
						LFNoise1.kr(pr).range(-pi, pi)
					);
					tf = FoaTransform.ar(tf, 'rtt',
						LFNoise1.kr(xr).range(-pi, pi),
						LFNoise1.kr(yr).range(-pi, pi),
						LFNoise1.kr(zr).range(-pi, pi)
					);
					Out.ar(out, tf)
				}).add;
				foaBus[defname] = Bus.audio(Server.default, 2);
				Server.default.sync;
				foaSynths[defname] = Synth.tail(group, defname,
					[\in, foaBus[defname], \out, decoder.bus, \amp, 0]
					++ [#[ar,tr,pr,xr,yr,zr],Array.rand(6, 0.5, 8.0)].lace(12))
			});

			SynthDef(\ampctr, {|in, out, amp, id|
				var input, freeTrig;
				input = In.ar(in, 2);
				freeTrig = CheckBadValues.kr(input, 0, 0);
				Free.kr(freeTrig, id);
				FreeSelf.kr(freeTrig);
				Out.ar(out, input * amp)
			}).add;

			SynthDef(\gep_record, {|in, amp, buf|
				var input;
				input = Mix(In.ar(in, 2)) * amp;
				RecordBuf.ar(input, buf, loop: 0, doneAction: 2);
			}).add;

		}.fork
	}

	setFoa{|kind, value|
		foaSynths[("foa"++kind.asString).asSymbol].set('amp', value)
	}

	cplay{|index, foaKind='zoom'|
		{
			var name, args;
			name = this.compilePanDefString(index);
			this.addDef(name);
			args = [\out, foaBus[("foa"++foaKind).asSymbol]] ++ data[index].args;
			defStrings[name.asSymbol].postln;
			Server.default.sync;
			synths[index] = Synth.head(group, name, args)
		}.fork;
	}

	play{|index, amp=0.0, foaKind='zoom', section=0|
		{
			var name, defargs, ampargs;
			name = data[index].defname;
			defargs = data[index].args.args;
			ampargs = [\out, foaBus[("foa"++foaKind).asSymbol], \amp, amp];
			Server.default.loadSynthDef(name, dir: Paths.gepDefDir);
			Server.default.sync;
			this.compilePanDefString(index);
			defStrings[name.asSymbol].postln;
			if (sendEnabled) {
				this.sendSynthDefString(defStrings[name.asSymbol])
			};
			synths[index] = GepSynth(name, defargs, group, ampargs);
			playFunc.(index, section, synths[index])
		}.fork
	}

	set{|index, value|
		synths[index].set('amp', value)
	}

	fade{|index, start, end, time|
		var interval = 0.1;
		Routine({
			var steps, incr, value = start;
			steps = time / interval;
			incr = (end - start) / steps;
			steps.do({
				value = value + incr;
				synths[index].set('amp', value);
				interval.wait;
			});
		}).play
	}

	setWithPattern{|index, pattern, dur|
		synths[index].setWithPattern('amp', pattern, dur)
	}

	stopPattern{|index| synths[index].stopPattern }

	prepareArgs{|index|
		^[data[index].terminals, data[index].args].lace(data[index].terminals.size * 2)
	}

	compileFoaDefString{|index|
		var defname, defstr, chrom;
		defname = data[index].defname;
		chrom = GEPChromosome(data[index].code, data[index].terminals,
			data[index].header.numgenes, data[index].linker);
		defstr = chrom.asUgenExpressionTree.asFoaSynthDefString(defname, Normalizer,
			UGenExpressionTree.foaControls.keys.choose);
		defStrings[defname.asSymbol] = defstr;
		^defname
	}

	compilePanDefString{|index|
		var defname, defstr, chrom;
		defname = data[index].defname;
		chrom = GEPChromosome(data[index].data.code, data[index].data.terminals,
			data[index].data.header.numgenes, data[index].data.linker);
		defstr = chrom.asUgenExpressionTree.asSynthDefString(defname, Pan2, Normalizer);
		defStrings[defname.asSymbol] = defstr;
		^defname
	}

	addDef{|name|
		{
			defStrings[name.asSymbol].interpret.add
		}.try
	}

	free{|index|
		synths[index].free;
		synths[index] = nil;
		freeFunc.(index)
	}

	stop{
		foaSynths.do(_.free);
		foaSynths = nil;
		foaBus.do(_.free);
		foaBus = nil;
	}

	assignCodeWindow{|document,prompt="@ "|
		if (document.isKindOf(Document).not) {
			codewindow = document ? Document("---live gep---")
		}
		{
			codewindow = document
		};
		if (thisProcess.platform.name == 'linux')
		{

		}
		{
			codewindow.keyDownAction = {|doc, char, mod, uni, key|
				var sendarray;
				if ((uni == 3) and: { key == 76 })
				{
					sendarray = doc.getSelectedLines.split(Char.nl);
					sendarray[0] = prompt ++ sendarray[0];
					sendarray.do({|str|
						graphics.sendCodeLine(str)
					})
				}
			}
		}
	}

	sendSynthDefString{|string, limit=72|
		var str = string.replace(" ", "");

		forBy(63, str.lastIndex, limit, {|index|
			var insert, paren;
			paren = str.find("(", offset: index);
			if (paren.isNil) {
				paren = str.find(")", offset: index)
			};
			insert = min(str.find(",", offset: index), paren);
			str = str.insert(insert+1, Char.nl)
		});

		str.split(Char.nl).do({|line|
			graphics.sendCodeLine(line)
		})

	}

	playSparseRoutines{|dataIndices, sparseName, sparseIndices, div = 4, foaKind, width=1.0|
		var patterns, arr, times, amps, durs, cdur;
		if (SparseMatrix.allPatterns.isNil) { SparseMatrix.makeSparsePatterns(2) };
		patterns = SparseMatrix.sparsePatterns[sparseName][sparseIndices];
		arr = (0 ! patterns.first.size);
		patterns.do({|pat| arr[pat.indexOf(1)] = 1 });
		arr = arr.add(1);
		times = Array();
		cdur = 0;
		arr.do({|val, i|
			if ((i > 0).and(val == 1))
			{
				times = times.add(cdur);
				cdur = 0;
			};
			cdur = cdur + 1;
		});
		durs = times.collect({|time, i|
			[times.keep(i).sum, time*width, time-(time*width), times.drop(i+1).sum]
		}) * (beatdur / div);
		amps = durs.collect({ [0.0, 1.0, 0.0, 0.0] });
		amps.do({|amp, i|
			this.playRoutine(dataIndices[i], Pseq(amp, inf), Pseq(durs[i], inf), foaKind)
		})
	}

	stopSparseRoutines{|dataIndices|
		dataIndices.do({|ind| this.stopRoutine(ind) })
	}

	playRoutine{|index, amps, durs, foaKind|
		if (routines[index].notNil) {
			this.stopRoutine(index);
		};
		{
			var name, defargs, ampargs, pbind, sum, mul;
			name = data[index].defname;
			defargs = data[index].args.args;
			ampargs = [\out, foaBus[("foa"++foaKind).asSymbol]];
			Server.default.loadSynthDef(name, dir: Paths.gepDefDir);
			Server.default.sync;
			this.compilePanDefString(index);
			if (sendEnabled) {
				this.sendSynthDefString(defStrings[name.asSymbol])
			};
			sum = durs.list.sum;
			mul = (beatdur*(sum/beatdur).round(1))/sum;
			durs.list = durs.list * mul;
			routines[index] = Routine({
				var ampstr, durstr;
				ampstr = amps.asStream;
				durstr = durs.asStream;
				loop({
					var synth, dur, amp;
					amp = ampstr.next;
					dur = durstr.next;
					if (amp.isNil.or(dur.isNil)) { this.stopRoutine(index) };
					if (amp > 0) {
						synth = GepSynth(name, defargs, group, ampargs ++ [\amp, amp])
					}
					{
						synth = nil;
					};
					if (synth.isNil.not) {
						SystemClock.sched(dur, { synth.free; nil });
					};
					dur.wait;
				})
			}).play
		}.fork

	}

	stopRoutine{|index|
		routines[index].stop;
		routines[index] = nil;
	}

}

JGepPlayer : GepPlayer {
	var <loader, <defnames, indexstream;
	var <>recordBuf, <>headerFormat, <>sampleFormat, <>recordDir;

	*new{|decoder, graphics, dbname|
		^super.new(decoder: decoder, graphics: graphics).initLoader(dbname)
	}

	initLoader{|dbname|
		loader = JsonLoader(dbname);
	}

	getDefNamesByDate{|date|
		defnames = loader.getIDsByDate(date).collect({|id| id['value'].first });
		data = Array.newClear(defnames.size);
	}

	getDefNamesByDateRange{|from="000000", to="999999"|
		defnames = loader.getIDsByDateRange(from, to).collect({|id| id['value'].first });
		data = Array.newClear(defnames.size);
	}

	getDefNamesByHeader{|headsize, numgenes|
		defnames = loader.getDefNamesByHeader(headsize, numgenes)
			.collect({|def| def['value'] });
		data = Array.newClear(defnames.size);
	}

	startManual{|kinds=#[zoom,focus]|
		{
			kinds.do({|kind|
				var defname = ("foa"++kind.asString).asSymbol;
				SynthDef(defname, {|out, in, amp=0, angle=0, theta=0, phi=0, rox=0, roy=0, roz=0|
					var input, bf, tf, args;
					input = In.ar(in, 2).dup(2).flat;
					bf = FoaEncode.ar(
						Array.fill(4, { |i|
							IFFT(
								PV_Diffuser( FFT( LocalBuf(1024), Limiter.ar(input[i])*amp), Dust.kr(20.0))
							)
						}), FoaEncoderMatrix.newAtoB
					);
					tf = FoaTransform.ar(bf, kind, angle, theta, phi);
					tf = FoaTransform.ar(tf, 'rtt', rox, roy, roz);
					Out.ar(out, tf)
				}).add;
				foaBus[defname] = Bus.audio(Server.default, 2);
				Server.default.sync;
				foaSynths[defname] = Synth.tail(group, defname,
					[\in, foaBus[defname], \out, decoder.bus, \amp, 0])
			});

			SynthDef(\ampctr, {|in, out, amp, id|
				var input, freeTrig;
				input = In.ar(in, 2);
				freeTrig = CheckBadValues.kr(input, 0, 0);
				Free.kr(freeTrig, id);
				FreeSelf.kr(freeTrig);
				Out.ar(out, input * amp)
			}).add;

		}.fork
	}

	loadData{|index|
		if (data.isNil) {
			data = Array.newClear(defnames.size);
		};
		if (data[index].isNil) {
			data[index] = loader.getPlayerDataByDefName(defnames[index])
		};
		Post << "Data for '" << defnames[index] << "' (" << index
			<< ") loaded.." << Char.nl;
	}

	play{|index, amp=0.0, foaKind='zoom', section=0|
		var name, defargs, ampargs;
		if (data.isNil) {
			data = Array.newClear(defnames.size);
		};
		name = defnames[index];
		if (data[index].isNil) {
			data[index] = loader.getPlayerDataByDefName(name)
		};
		{
			defargs = data[index].args;
			ampargs = [\out, foaBus[("foa"++foaKind).asSymbol], \amp, amp];
			Server.default.loadSynthDef(name, dir: Paths.gepDefDir);
			Server.default.sync;
			this.compilePanDefString(index);
			defStrings[name.asSymbol].postln;
			if (sendEnabled) {
				this.sendSynthDefString(defStrings[name.asSymbol])
			};
			synths[index] = GepSynth(name, defargs, group, ampargs);
			playFunc.(index, section, synths[index])
		}.fork
	}

	trigger{|index, amp=1.0, foaKind='zoom', dur=1.0, repeat=1, delta=1.0|
		var name, defargs, ampargs;
		if (data.isNil) {
			data = Array.newClear(defnames.size);
		};
		name = defnames[index];
		if (data[index].isNil) {
			data[index] = loader.getPlayerDataByDefName(name)
		};
		Tdef(("play_gep_" ++ index).asSymbol, {
			defargs = data[index].args;
			ampargs = [\out, foaBus[("foa"++foaKind).asSymbol], \amp, amp];
			Server.default.loadSynthDef(name, dir: Paths.gepDefDir);
			Server.default.sync;
			this.compilePanDefString(index);
			defStrings[name.asSymbol].postln;
			if (sendEnabled) {
				this.sendSynthDefString(defStrings[name.asSymbol])
			};
			repeat.do({
				var synth;
				synth = GepSynth(name, defargs, group, ampargs);
				SystemClock.sched(dur.next, { synth.free; nil });
				delta.next.wait;
			})
		}).play

	}

	procplay{|index, amp=0.0, foaKind='zoom', section=0, procdef, procargs|
		var name, defargs, ampargs;
		if (data.isNil) {
			data = Array.newClear(defnames.size);
		};
		name = defnames[index];
		if (data[index].isNil) {
			data[index] = loader.getPlayerDataByDefName(name)
		};
		{
			defargs = data[index].args;
			ampargs = [\out, foaBus[("foa"++foaKind).asSymbol], \amp, amp];
			Server.default.loadSynthDef(name, dir: Paths.gepDefDir);
			Server.default.sync;
			this.compilePanDefString(index);
			defStrings[name.asSymbol].postln;
			if (sendEnabled) {
				this.sendSynthDefString(defStrings[name.asSymbol])
			};
			synths[index] = GepProcSynth(name, defargs, group, ampargs, procdef, procargs);
			playFunc.(index, section, synths[index])
		}.fork
	}

	compilePanDefString{|index|
		var defname, defstr, chrom;
		defname = data[index].defname;
		chrom = GEPChromosome(data[index].code, data[index].terminals,
			data[index].numgenes, data[index].linker);
		defstr = chrom.asUgenExpressionTree.asSynthDefString(defname, Pan2, Normalizer);
		defStrings[defname.asSymbol] = defstr;
		^defname
	}

	playSparseRoutines{|dataIndices, sparseName, sparseIndices, div = 4, foaKind, durscale=#[1.0], ampscale=#[1.0]|
		var patterns, arr, times, amps, durs, cdur;
		if (SparseMatrix.allPatterns.isNil) { SparseMatrix.makeSparsePatterns(2) };
		patterns = SparseMatrix.sparsePatterns[sparseName][sparseIndices];
		arr = (0 ! patterns.first.size);
		patterns.do({|pat| arr[pat.indexOf(1)] = 1 });
		if (durscale.isKindOf(Number)) { durscale = durscale.bubble; };
		if (ampscale.isKindOf(Number)) { ampscale = ampscale.bubble; };
		arr = arr.add(1);
		times = Array();
		cdur = 0;
		arr.do({|val, i|
			if ((i > 0).and(val == 1))
			{
				times = times.add(cdur);
				cdur = 0;
			};
			cdur = cdur + 1;
		});
		durs = times.collect({|time, i|
			[times.keep(i).sum, time*durscale.wrapAt(i),
				time-(time*durscale.wrapAt(i)), times.drop(i+1).sum]
		}) * (beatdur / div);
		amps = durs.collect({|dur, i| [0.0, 1.0, 0.0, 0.0] * ampscale.wrapAt(i) });
		amps.do({|amp, i|
			this.playRoutine(dataIndices[i], Pseq(amp, inf), Pseq(durs[i], inf), foaKind)
		})
	}

	playRoutine{|index, amps, durs, foaKind|
		var name, defargs, ampargs, pbind, sum, mul;
		if (data.isNil) {
			data = Array.newClear(defnames.size);
		};
		name = defnames[index];
		if (data[index].isNil) {
			data[index] = loader.getPlayerDataByDefName(name)
		};
		if (routines[index].notNil) {
			this.stopRoutine(index);
		};
		{
			defargs = data[index].args;
			ampargs = [\out, foaBus[("foa"++foaKind).asSymbol]];
			Server.default.loadSynthDef(name, dir: Paths.gepDefDir);
			Server.default.sync;
			this.compilePanDefString(index);
			if (sendEnabled) {
				this.sendSynthDefString(defStrings[name.asSymbol])
			};
			sum = durs.list.sum;
			mul = (beatdur*(sum/beatdur).round(1))/sum;
			durs.list = durs.list * mul;
			routines[index] = Routine({
				var ampstr, durstr;
				ampstr = amps.asStream;
				durstr = durs.asStream;
				loop({
					var synth, dur, amp;
					amp = ampstr.next;
					dur = durstr.next;
					if (amp.isNil.or(dur.isNil)) { this.stopRoutine(index) };
					if (amp > 0) {
						synth = GepSynth(name, defargs, group, ampargs ++ [\amp, amp])
					}
					{
						synth = nil;
					};
					if (synth.isNil.not) {
						SystemClock.sched(dur, { synth.free; nil });
					};
					dur.wait;
				})
			}).play
		}.fork

	}

	makeGepDef{|name, func, nargs = 8|
		SynthDef(name, {|out, dur = 0.1, amp = 1.0, rotx = 0.0, roty = 0.0, rotz = 0.0|
			var sig;
			sig = SynthDef.wrap(func, prependArgs: ArrayControl.kr('gepargs', nargs, 0.0) )
				* EnvGen.kr(EnvControl.kr, timeScale: dur, doneAction: 2);
			Out.ar(out, FoaTransform.ar(
				FoaEncode.ar(sig * amp, FoaEncoderMatrix.newDirection), 'rtt', rotx, roty, rotz)
			)
		});
	}

	playGepDef{|defname, out, amp, dur, env, args, target|
		if (target.isNil) { target = decoder };
		^Synth.before(target, defname, [\out, out, \amp, amp, \dur, dur])
			.setn('env', env).setn('gepargs', args)
	}

	prepareForRecord{|headerFormat="aiff", sampleFormat="int24", recordDir|
		this.headerFormat = headerFormat;
		this.sampleFormat = sampleFormat;
		if (recordDir.isNil) {
			recordDir = thisProcess.platform.recordingsDir;
		};
	}

	record{|amp=1.0, dur=5.0|
		var index = indexstream.next;
		Tdef('gep_record').clear;
		if (index.notNil) {
			Tdef('gep_record', {
				var writepath;
				if ((headerFormat.isNil).or(sampleFormat.isNil)) { this.prepareForRecord };
				if (recordBuf.isNil) {
					recordBuf = Buffer.alloc(Server.default, dur * Server.default.sampleRate);
					Server.default.sync;
				};
				this.play(index, 0.0);
				1.0.wait;
				Synth.tail(group, 'gep_record', ['in', foaBus['foazoom'],
					'amp', 1.0, 'buf', recordBuf]
				);
				this.set(index, amp);
				dur.wait;
				this.free(index);
				Server.default.sync;
				writepath = this.recordDir +/+ this.defnames[index] ++ "." ++ this.headerFormat;
				recordBuf.write(writepath, this.headerFormat, this.sampleFormat);
				Post << "Wrote " << writepath << Char.nl;
				Server.default.sync;
				this.record(amp, dur);
			}).play
		}
		{
			"Recording finished".postln;
		}
	}

	recordAll{|amp=1.0, dur=5.0|
		indexstream = Pseq(this.data.selectIndices(_.notNil)).asStream;
		this.record(amp, dur);
	}

	recordOne{|index, amp=1.0, dur=5.0|
		indexstream = Pseq(index.bubble).asStream;
		this.record(amp, dur);
	}

}