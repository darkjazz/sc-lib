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
	
	var defname, defargs, ampargs, bus, source, proc, patternPlayer;
	
	*new{|defname, defargs, target, ampargs|
		^super.newCopyArgs(defname, defargs, ampargs).init(target)
	}
	
	init{|target|
		{
			bus = Bus.audio(Server.default, 2);
			source = Synth.head(target, defname, [\out, bus] ++ defargs);
			Server.default.sync;
			proc = Synth.after(source, \ampctr, [\in, bus] ++ ampargs)
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

GepPlayer{
	
	var data, decoder, graphics, <synths, <defStrings, <group, foaSynths, foaBus;
	var <codewindow, <>sendEnabled=false, <>playFunc, <>freeFunc;
	
	*new{|data, decoder, graphics|
		^super.newCopyArgs(data, decoder, graphics).init
	}
	
	init{
		defStrings = ();
		synths = ();
		foaSynths = ();
		foaBus = ();
		group = Group.before(decoder.synth);
		if (graphics.isNil) {
			graphics = CinderApp()
		};
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
							IFFT( PV_Diffuser( FFT( LocalBuf(1024), Limiter.ar(input[i])*amp))) 
						}), FoaEncoderMatrix.newAtoB 
					);
					tf = FoaTransform.ar(bf, kind, 
						LFNoise1.kr(ar).range(-pi/4, pi/4),
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
			
			SynthDef(\ampctr, {|in, out, amp|
				Out.ar(out, In.ar(in, 2)*amp)
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
	
	play{|index, amp, foaKind='zoom', section=0|
		{
			var name, defargs, ampargs;
			name = data[index].defname;
			defargs = data[index].args;
			ampargs = [\out, foaBus[("foa"++foaKind).asSymbol], \amp, amp];
			Server.default.loadSynthDef(name, dir: UGenExpressionTree.defDir);
			Server.default.sync;
			this.compilePanDefString(index);
//			defStrings[name.asSymbol].postln;
			if (sendEnabled) {
				this.sendSynthDefString(defStrings[name.asSymbol])
			};
//			synths[index] = Synth.head(group, name, args);
			synths[index] = GepSynth(name, defargs, group, ampargs);
			playFunc.(index, section, synths[index])
		}.fork
	}
		
	set{|index, value|
		synths[index].set('amp', value)
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
		chrom = GEPChromosome(data[index].code, data[index].terminals, 
			data[index].header.numgenes, data[index].linker);
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

	assignCodeWindow{|document,prompt="@ "|
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
				sendarray[0] = prompt ++ sendarray[0];
				sendarray.do({|str|  
					graphics.sendCodeLine(str) 
				})
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
	
		
}