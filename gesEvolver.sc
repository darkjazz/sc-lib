EvolverEnvir{

	var <settings;

	*new{|settings|
		^super.newCopyArgs(settings).init
	}

	init{
		settings.keysValuesDo({|key, val|
			currentEnvironment[key] = val
		});
		currentEnvironment['sourceData'] = [];
		currentEnvironment['loader'] = JsonLDLoader(currentEnvironment['dbname']);
	}

	addChromosomeByDefname{|defname|
		currentEnvironment['sourceData'] = currentEnvironment['sourceData'].add(
			currentEnvironment['loader'].getDocumentByDefName(defname)
		);
		if (currentEnvironment['ugens'].isNil)
		{
			currentEnvironment['ugens'] = currentEnvironment['sourceData'].first.methods;
			currentEnvironment['terminals'] = currentEnvironment['sourceData'].first.terminals;
		}
		{
			currentEnvironment['sourceData'].last.methods.do({|method|
				if (currentEnvironment['ugens'].includes(method).not) {
					currentEnvironment['ugens'] = currentEnvironment['ugens'].add(method)
				}
			});
			this.updateAbbreviations
		}
	}

	updateAbbreviations{
		currentEnvironment['abbrev'] = currentEnvironment['ugens'].collect({|ugen| ugen.name.asString.keep(4) });
	}

	initializeRandomPopulation{
		currentEnvironment['terminals'] = settings.numterminals.collect({|i|
			(97 + i).asAscii.asSymbol
		});

		this.updateAbbreviations;

		currentEnvironment['gep'] = UGEP(
			settings.populationSize,
			settings.numgenes,
			settings.headsize,
			currentEnvironment['ugens'],
			currentEnvironment['terminals'],
			settings.linker
		);

		currentEnvironment['gep'].mutationRate = settings['mutationRate'];
		currentEnvironment['gep'].recombinationRate = settings['recombinationRate'];
		currentEnvironment['gep'].transpositionRate = settings['transpositionRate'];
		currentEnvironment['gep'].rootTranspositionRate = settings['rootTranspositionRate'];
		currentEnvironment['gep'].geneRecombinationRate = settings['geneRecombinationRate'];
		currentEnvironment['gep'].geneTranspositionRate = settings['geneTranspositionRate'];

		currentEnvironment['paramgep'] = GEP(
			settings.populationSize,
			currentEnvironment['terminals'].size,
			5,
			['*', '-', '+', '/'].collect({|opname|
				AbstractFunction.methods.select({|meth| meth.name == opname }).first
			}),
			['w', 'x', 'y', 'z'],
			Array.class.methods.select({|mth| mth.name == 'with' }).first
		);

		currentEnvironment['maps'] = #[unipolar, bipolar, freq, lofreq, midfreq, widefreq].collect({|name|
			ControlSpec.specs[name]
		});

		currentEnvironment['paramgep'].chromosomes.do({|chrom|
			chrom.fillConstants(currentEnvironment['paramgep'].terminals.size, { rrand(0.0, 1.0) });
			currentEnvironment['terminals'].size.do({
				chrom.addExtraDomain(currentEnvironment['maps'].choose)
			})
		});

		currentEnvironment['paramgep'].mutationRate = settings['mutationRate'];
		currentEnvironment['paramgep'].recombinationRate = settings['recombinationRate'];
		currentEnvironment['paramgep'].transpositionRate = settings['transpositionRate'];
		currentEnvironment['paramgep'].rootTranspositionRate = settings['rootTranspositionRate'];
		currentEnvironment['paramgep'].geneRecombinationRate = settings['geneRecombinationRate'];
		currentEnvironment['paramgep'].geneTranspositionRate = settings['geneTranspositionRate'];

		currentEnvironment['gep'].dbname = settings['dbname'];

	}

	initializePopulation{

		currentEnvironment['gep'] = UGEP(
			currentEnvironment['sourceData'].size,
			currentEnvironment['sourceData'].first.numgenes,
			currentEnvironment['sourceData'].first.headsize,
			currentEnvironment['ugens'],
			currentEnvironment['terminals'],
			AbstractFunction.methods.select({|meth| meth.name == '*' }).first
		);

		currentEnvironment['sourceData'].do({|data, i|
			currentEnvironment['gep'].chromosomes[i] = GEPChromosome.fromJsonData(data)
		});

		currentEnvironment['gep'].mutationRate = settings['mutationRate'];
		currentEnvironment['gep'].recombinationRate = settings['recombinationRate'];
		currentEnvironment['gep'].transpositionRate = settings['transpositionRate'];
		currentEnvironment['gep'].rootTranspositionRate = settings['rootTranspositionRate'];
		currentEnvironment['gep'].geneRecombinationRate = settings['geneRecombinationRate'];
		currentEnvironment['gep'].geneTranspositionRate = settings['geneTranspositionRate'];

		currentEnvironment['paramgep'] = GEP(
			currentEnvironment['sourceData'].size,
			currentEnvironment['sourceData'].first.terminals.size,
			5,
			['*', '-', '+', '/'].collect({|opname|
				AbstractFunction.methods.select({|meth| meth.name == opname }).first
			}),
			['w', 'x', 'y', 'z'],
			Array.class.methods.select({|mth| mth.name == 'with' }).first
		);

		currentEnvironment['sourceData'].do({|data, i|
			currentEnvironment['paramgep'].chromosomes[i] = GEPChromosome(
				data.params.code,
				currentEnvironment['paramgep'].terminals,
				currentEnvironment['paramgep'].numgenes,
				currentEnvironment['paramgep'].linker
			);
			currentEnvironment['paramgep'].chromosomes[i].constants = data.params.constants;
			currentEnvironment['paramgep'].chromosomes[i].extraDomains = data.params.extraDomains;
		});

		currentEnvironment['maps'] = #[unipolar, bipolar, freq, lofreq, midfreq, widefreq].collect({|name|
			ControlSpec.specs[name]
		});

		currentEnvironment['paramgep'].mutationRate = settings['mutationRate'];
		currentEnvironment['paramgep'].recombinationRate = settings['recombinationRate'];
		currentEnvironment['paramgep'].transpositionRate = settings['transpositionRate'];
		currentEnvironment['paramgep'].rootTranspositionRate = settings['rootTranspositionRate'];
		currentEnvironment['paramgep'].geneRecombinationRate = settings['geneRecombinationRate'];
		currentEnvironment['paramgep'].geneTranspositionRate = settings['geneTranspositionRate'];

		currentEnvironment['gep'].dbname = settings['dbname'];

	}

	extractTargetFeatures{|path|

		Routine({

			currentEnvironment['targetBuffer'] = Buffer.read(Server.default, path);

			Server.default.sync;

			currentEnvironment['targetAnalyzer'] = UGepTimeAnalyzer(
				SynthDef(\targetBuf, {|out, buf|
					Out.ar(out, Mix(PlayBuf.ar(~targetBuffer.numChannels, buf, BufRateScale.kr(buf), doneAction: 2)))
				}).add,
				settings['ncoef']
			);

			Server.default.sync;

			currentEnvironment['targetAnalyzer'].currentDef = \targetBuf;

			Server.default.sync;

			OSCFunc({|msg|
				currentEnvironment['targetAnalyzer'].freeSynth;
				currentEnvironment['targetAnalyzer'].clear;
				"resources freed...".postln;
			}, '/n_end', Server.default.addr).oneShot;


			currentEnvironment['targetAnalyzer'].run(rate: ~rate);

			Synth.before(currentEnvironment['targetAnalyzer'].synth,
				\targetBuf, [\out, currentEnvironment['targetAnalyzer'].bus, \buf, currentEnvironment['targetBuffer']]);

		}).play;

	}

	defineFitnessFunction{
		// fitness function based on target

		currentEnvironment['gep'].addFitnessFunc({

			if (currentEnvironment['defs'].notNil) {
				currentEnvironment['defs'].do({|def| if (def.notNil) { SynthDef.removeAt(def.name) } })
			};

			currentEnvironment['depths'] = Array.newClear(currentEnvironment['gep'].chromosomes.size);

			currentEnvironment['defs'] = currentEnvironment['gep'].chromosomes.collect({|chrom, i|
				var defname, defstr, tree;
				defname = ("gep_gen" ++ chrom.generation.asString.padLeft(3, "0")
					++ "_" ++ i.asString.padLeft(3, "0")).asSymbol;
				tree = chrom.asUgenExpressionTree;
				currentEnvironment['depths'][i] = tree.maxDepth;
				defstr = tree.asSynthDefString(defname, Pan2, Normalizer);
				{
					defstr.interpret.add
				}.try({
					chrom.score = -1;
					nil
				})
			});

			currentEnvironment['minDepth'] = currentEnvironment['depths'].minItem;
			currentEnvironment['maxDepth'] = currentEnvironment['depths'].maxItem;
			currentEnvironment['depthSpec'] = [currentEnvironment['minDepth'], currentEnvironment['maxDepth']].asSpec;
			currentEnvironment['normalizedDepthScores'] = currentEnvironment['depths'].collect({|depth|
				currentEnvironment['depthSpec'].unmap(depth)
			});

			currentEnvironment['params'] = currentEnvironment['paramgep'].chromosomes.collect({|chrom, i|
				var rawargs;
				rawargs = chrom.asExpressionTree(false).asFunctionString.interpret.value(*chrom.constants);
				rawargs.collect({|num, i|
					chrom.extraDomains[i].map(num.wrap(0.0, 1.0)).roundFreq
				})
			});

			Routine({
				var cpu;
				var fragdur = currentEnvironment['targetBuffer'].duration;

				currentEnvironment['gepAnalyzer'] = UGepTimeAnalyzer(
					currentEnvironment['defs'].select(_.notNil),
					currentEnvironment['ncoef'],
					currentEnvironment['targetAnalyzer'].currentStats
				);

				Server.default.sync;

				currentEnvironment['defs'].do({|def, i|
					var synth, score=0, normalized, analyzer, mean=0, stddev=0, scaled;

					if (def.notNil) {

						currentEnvironment['gepAnalyzer'].currentDef = def.name.asSymbol;
						Post << "-------------  START " << def.name << " --------------"
						<< Char.nl;
						currentEnvironment['gepAnalyzer'].run(rate: currentEnvironment['rate']);
						synth = Synth.before(currentEnvironment['gepAnalyzer'].synth, def.name,
							[\out, currentEnvironment['gepAnalyzer'].bus, \amp, 1]
							++ [currentEnvironment['terminals'], currentEnvironment['params'][i]]
							.lace(currentEnvironment['terminals'].size * 2)
						);
						fragdur.wait;
						synth.free;
						synth = nil;
						currentEnvironment['gepAnalyzer'].freeSynth;
						normalized = currentEnvironment['gepAnalyzer'].calculateDistances;
						normalized.mfcc = normalized.mfcc.select({|array|
							array.includes(0/0).not
						});
						normalized.flat = normalized.flat.select({|item| item > 0 });
						normalized.cent = normalized.cent.select({|item| item > 0 });
						normalized.amp = normalized.amp.select({|item| item > 0 });
						scaled = [
							1.0 - (normalized.mfcc.flop.collect(_.mean).mean) * 0.4,
							1.0 - normalized.flat.mean * 0.2,
							1.0 - normalized.cent.mean * 0.2,
							1.0 - normalized.amp.mean * 0.2
						];
						score = scaled.sum;
						if ((currentEnvironment['gepAnalyzer'].currentErrors.mean > 0).or
							(score < 0).or(score.isNaN)) { score = 0 };
						currentEnvironment['gep'].at(i).score = score.round(0.01) * 10;
						currentEnvironment['paramgep'].at(i).score = score.round(0.01) * 10;
						(scaled * 10).postln;
					}
					{
						currentEnvironment['gep'].at(i).score = 0
					};
					currentEnvironment['gep'].at(i).score.postln;
					0.5.wait
				});
				cpu = currentEnvironment['gepAnalyzer'].calculateCPUsage;
				currentEnvironment['defs'].do({|def, i|
					var pscore;
					if (def.notNil) {
						Post << def.name << " - peak CPU: "
						<< currentEnvironment['gepAnalyzer'].cpu[def.name.asSymbol].collect(_.peak).mean
						<< "; CPU score: " << cpu[def.name.asSymbol] << Char.nl;
						pscore = currentEnvironment['gep'].at(i).score;
						// multiply cpu score by 10 as well, makes cpu * 2.0 to account for 10%,
						// multiply depth score by 0.5 to get 5%
						if (currentEnvironment['gep'].at(i).score > 0) {
							currentEnvironment['gep'].at(i).score = [
								currentEnvironment['gep'].at(i).score * 0.85,
								currentEnvironment['normalizedDepthScores'][i] * 0.5,
								cpu[def.name.asSymbol]
							].sum;
						};
						Post << def.name
						<< " new score: " << currentEnvironment['gep'].at(i).score << "; original: " << pscore
						<< "; depth: " << (currentEnvironment['normalizedDepthScores'][i] * 0.5).round(0.001)
						<< "; cpu: " << cpu[def.name.asSymbol].round(0.001)
						<< "; total change: " << (currentEnvironment['gep'][i].score - pscore).round(0.001)
						<< Char.nl;
					}
				});
				currentEnvironment['gepAnalyzer'].clear;
				Post << " ------- analysis finished ------- " << Char.nl;
				[currentEnvironment['gep'].meanScore, currentEnvironment['gep'].maxScore].postln;
				currentEnvironment['gep'].chromosomes.collect(_.score).postln;
			}).play
		})

	}

	playOne{|index|
		var synth, args, def, fragdur;
		fragdur = currentEnvironment['targetBuffer'].duration;
		def = currentEnvironment['defs'][index];
		currentEnvironment['gep'].at(index).asUgenExpressionTree
		.asSynthDefString(def.name.asSymbol, Pan2, Normalizer, false).postln;
		args =  [
			currentEnvironment['terminals'],
			currentEnvironment['params'][index]
		].lace(currentEnvironment['terminals'].size * 2).postln;
		Tdef('playOne', {
			synth = Synth(def.name, [\amp, 0.0, \dur, fragdur+0.5] ++ args);
			0.2.wait;
			synth.set('amp', 0.5);
			(fragdur + rrand(0.5, 1.0)).wait;
			synth.free;
			synth = nil;
		}).play
	}

	play{
		currentEnvironment['player'] = Routine({
			var fragdur = currentEnvironment['targetBuffer'].duration;
			currentEnvironment['defs'].do({|def, i|
				if (currentEnvironment['gep'].at(i).score > currentEnvironment['th']) {
					var synth, args;
					Post << "-------------  START " << def.name << " --------------" << Char.nl;
					currentEnvironment['gep'].at(i).asUgenExpressionTree
					.asSynthDefString(def.name.asSymbol, Pan2, Normalizer, false).postln;
					args =  [
						currentEnvironment['terminals'],
						currentEnvironment['params'][i]
					].lace(currentEnvironment['terminals'].size * 2).postln;
					Post << "SCORE: " << currentEnvironment['gep'].at(i).score << Char.nl;
					synth = Synth(def.name, [\amp, 0.0, \dur, fragdur+0.5] ++ args);
					0.2.wait;
					synth.set('amp', 0.5);
					(fragdur + rrand(0.5, 1.0)).wait;
					synth.free;
					synth = nil;
					Server.default.sync;
					rrand(0.5, 1.0).wait;
				}
			});
			"player finished...".postln;
		}).play

	}

	save{|indices, save = true, fragdur, record = false|
		currentEnvironment['player'] = Routine({
			if (fragdur.isNil) { fragdur = currentEnvironment['targetBuffer'].duration };
			indices.do({|ind, i|
				var synth, args, def, savename, et, co, cospec;
				def = currentEnvironment['defs'][ind];
				Post << "-------------  START " << def.name << " --------------" << Char.nl;
				et = currentEnvironment['gep'].at(ind).asUgenExpressionTree;
				et.asSynthDefString(def.name.asSymbol, Pan2, Normalizer, false).postln;
				args = [
					currentEnvironment['terminals'],
					currentEnvironment['params'][ind]
				].lace(currentEnvironment['terminals'].size * 2).postln;
				Post << "SCORE: " << currentEnvironment['gep'].at(ind).score << Char.nl;
				savename = currentEnvironment['gep'].makeDefName(ind);
				synth = Synth(def.name, [\amp, 0, \dur, fragdur+0.5] ++ args);
				if (record) {
					currentEnvironment['mostRecentSavePath'] = currentEnvironment['recordPath'] ++ savename
					++ ".aiff";
					Server.default.prepareForRecord(
						currentEnvironment['recordPath'] ++ savename ++ ".aiff"
					);
					Server.default.sync;
				};
				if (record) {
					Server.default.record;
					Server.default.sync
				};
				synth.set('amp', 0.5);
				fragdur.wait;
				synth.free;
				synth = nil;
				if (record) { Server.default.stopRecording };
				Server.default.sync;
				0.5.wait;
				if (save) {
					currentEnvironment['gep'].saveJson(ind, Pan2, Normalizer, args,
						currentEnvironment['paramgep'].chromosomes[ind],
						currentEnvironment['gepAnalyzer'].stats[def.name.asSymbol],
						savename
					);
				}
			});
			"Save finished...".postln;
		}).play
	}

}