UGepRun{
	
	var size, numgenes, ncoef, rate, headsize, methods, linker, targetBufPath, forceArgs, methodRatio;
	var <gep, terminals, maps, <targetAnalyzer, <defs, depths, minDepth, maxDepth, depthSpec;
	var normalizedDepthScores, <gepAnalyzer, targetBuffer, params;
	var <paramgep, paramheadsize, paramnumgenes, paramterminals, paramethods;
	var <history;
	
	*new{|size, numgenes, ncoef, rate, headsize, methods, linker, targetBufPath, forceArgs, methodRatio=0.5|
		^super.newCopyArgs(size, numgenes, ncoef, rate, headsize, methods, 
			linker, targetBufPath, forceArgs, methodRatio).init
	}
	
	init{
		var mthds;
		mthds = methods.collect({|ugen| 
			var ar;
			ar = ugen.class.methods.select({|mth| mth.name == 'ar' }).first;
			if (ar.isNil) {
				ar = ugen.superclass.class.methods.select({|mth| mth.name == 'ar' }).first
			};
			ar
		});
		
		terminals = (mthds.collect(_.argNames).collect(_.size).maxItem + headsize).collect({|i|
			(97 + i).asAscii.asSymbol
		});
		
		gep = UGEP.newValid(size, numgenes, headsize, methods, terminals, linker, forceArgs, methodRatio);
		
		gep.mutationRate = 0.3;
		gep.recombinationRate = 0.5;
		gep.transpositionRate = 0.2;
		gep.rootTranspositionRate = 0.3;
		gep.geneRecombinationRate = 0.25;
		gep.geneTranspositionRate = 0.0;
		
		paramheadsize = 5;
		paramnumgenes = terminals.size;
		paramterminals = ['w', 'x', 'y', 'z'];
		
		paramethods = ['*', '-', '+', '/'].collect({|opname|
			AbstractFunction.methods.select({|meth| meth.name == opname }).first
		}) ++ ControlSpec.methods.select({|mth| mth.name == 'map' });
		
		paramgep = GEP(size, paramnumgenes, paramheadsize, paramethods, paramterminals, 
			Array.class.methods.select({|mth| mth.name == 'with' }).first
		);
		
		maps = #[freq, lofreq, midfreq, widefreq, phase].collect({|name|
			ControlSpec.specs[name]
		});
		
		paramgep.chromosomes.do({|chrom| 
			chrom.fillConstants(paramterminals.size, { rrand(0.0, 1.0) }); 
			chrom.addExtraDomain(Array.with(maps.choose))
		});
		
		paramgep.mutationRate = 0.25;
		paramgep.recombinationRate = 1.0;
		paramgep.transpositionRate = 0.25;
		paramgep.rootTranspositionRate = 0.1;
		paramgep.geneRecombinationRate = 0.1;
		paramgep.geneTranspositionRate = 0.1;
		
		history = Array();
		
	}
	
	createTargetStats{
		Routine({
			targetBuffer = Buffer.read(Server.default, targetBufPath);
			Server.default.sync;
			targetAnalyzer = UGepTimeAnalyzer(
				SynthDef(\targetBuf, {|out, buf|
					Out.ar(out, Mix(
						PlayBuf.ar(targetBuffer.numChannels, buf, BufRateScale.kr(buf), 
						doneAction: 2))
					)
				}).add, 
				ncoef
			);
			Server.default.sync;
			targetAnalyzer.currentDef = \targetBuf;
			Server.default.sync;
			OSCFunc({|msg|
				targetAnalyzer.freeSynth;
		 		targetAnalyzer.clear;
		 		"resources freed...".postln;
			}, '/n_end', Server.default.addr).oneShot;
			targetAnalyzer.run(rate: rate);
			Synth.before(targetAnalyzer.synth, \targetBuf, [\out, targetAnalyzer.bus, 
				\buf, targetBuffer]
			);
		}).play;
		
	}
	
	defineFitnessFunction{		
		gep.addFitnessFunc({
			
			if (defs.notNil) {
				defs.do({|def| if (def.notNil) { SynthDef.removeAt(def.name) } })
			};
			
			depths = Array.newClear(gep.chromosomes.size);
				
			defs = gep.chromosomes.collect({|chrom, i|
				var defname, defstr, tree;
				defname = ("gep_gen" ++ gep.generationCount.asString.padLeft(3, "0") 
					++ "_" ++ i.asString.padLeft(3, "0")).asSymbol;
				tree = chrom.asUgenExpressionTree;
				depths[i] = tree.maxDepth;
				defstr = tree.asSynthDefString(defname, Pan2, Normalizer);
				{
					defstr.interpret.add
				}.try({
					chrom.score = -1;
					nil	
				})
			});
			
			minDepth = depths.minItem;
			maxDepth = depths.maxItem;
			depthSpec = [minDepth, maxDepth].asSpec;
			normalizedDepthScores = depths.collect({|depth|  
				depthSpec.unmap(depth)
			});
		
			params = paramgep.chromosomes.collect({|chrom, i|
				var spec;
				spec = chrom.extraDomains.first.first.asString.drop(2);
				chrom.asExpressionTree(false).asFunctionString
					.replace("map", spec ++ ".map").interpret.value(*chrom.constants)
			});
					
			Routine({
				var cpu;
				var fragdur=targetBuffer.duration;
				
				gepAnalyzer = UGepTimeAnalyzer(defs.select(_.notNil), ncoef, 
					targetAnalyzer.currentStats);
				
				Server.default.sync;
				
				defs.do({|def, i|	
					var synth, score=0, normalized, analyzer, mean=0, stddev=0, scaled;
					
					if (def.notNil) {
					
						gepAnalyzer.currentDef = def.name.asSymbol;
						Post << "-------------  START " << def.name << " --------------" << Char.nl;
						gepAnalyzer.run(rate: rate);
						synth = Synth.before(gepAnalyzer.synth, def.name, 
							[\out, gepAnalyzer.bus, \amp, 1] 
							++ [terminals, params[i]].lace(terminals.size * 2));
						fragdur.wait;
						synth.free;
						synth = nil;
						gepAnalyzer.freeSynth;	
						normalized = gepAnalyzer.calculateDistances;
						normalized.mfcc = normalized.mfcc.select({|array| array.includes(0/0).not });
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
						if ((gepAnalyzer.currentErrors.mean > 0).or
							(score < 0).or(score.isNaN)) { score = 0 };
						gep.at(i).score = score.round(0.01) * 10;
						paramgep.at(i).score = score.round(0.01) * 10;
						(scaled * 10).postln;
					}
					{
						gep.at(i).score = 0
					};
					gep.at(i).score.postln;
					0.5.wait
				});
				cpu = gepAnalyzer.calculateCPUsage;
				defs.do({|def, i|
					var pscore;
					if (def.notNil) {
						Post << def.name << " - peak CPU: " 
							<< gepAnalyzer.cpu[def.name.asSymbol].collect(_.peak).mean
							<< "; CPU score: " << cpu[def.name.asSymbol] << Char.nl;
						pscore = gep[i].score;
						// multiply cpu score by 10 as well, makes cpu * 2.0 to account for 10%, 
						// multiply depth score by 0.5 to get 5%
						if (gep[i].score > 0) {
							gep[i].score = [
								gep[i].score * 0.85, 
								normalizedDepthScores[i] * 0.5, 
								cpu[def.name.asSymbol]
							].sum;
						};
						Post << def.name 
						<< " new score: " << gep[i].score << "; original: " << pscore 
						<< "; depth: " << (normalizedDepthScores[i] * 0.5).round(0.001)
						<< "; cpu: " << cpu[def.name.asSymbol].round(0.001)
						<< "; total change: " << (gep[i].score - pscore).round(0.001) << Char.nl;
					}
				});
				gepAnalyzer.clear;
				Post << " ------- analysis finished ------- " << Char.nl;
				[gep.meanScore, gep.maxScore].postln;
				gep.chromosomes.collect(_.score).postln;
				history = history.add(('mean': gep.meanScore, 'max': gep.maxScore))
			}).play
		})
				
	}
	
	assignScores{ gep.updateScores }
	
	next{
		paramgep.nextGeneration;
		gep.nextGeneration;			
	}
	
	selectScores{|threshold=5.0| ^gep.chromosomes.collect(_.score).select({|sc| sc > threshold }) }
	
	play{|threshold=5.0, dur|

		Routine({
			var fragdur;
			if (dur.isNil) {fragdur = targetBuffer.duration} { fragdur = dur };
			defs.do({|def, i|
				if (gep.at(i).score > threshold) {
					var synth, args;
					Post << "-------------  START " << def.name << " --------------" << Char.nl;
					gep.at(i).asUgenExpressionTree
						.asSynthDefString(def.name.asSymbol, Pan2, Normalizer, true).postln;
					args =  [terminals, params[i]].lace(terminals.size * 2).postln;
					Post << "SCORE: " << gep.at(i).score << Char.nl;
					synth = Synth(def.name, [\amp, 0.7, \dur, fragdur] ++ args)
						.setn(\env, Env([0,1,1,0],[0.1, 0.8, 0.1]));
					fragdur.wait;
					synth.free;
					synth = nil;				
					Server.default.sync;
					0.5.wait;
				}
			})
		}).play
		
	}
	
	save{|indices, dur|
		
		Routine({
			var fragdur = 5;
			if (dur.isNil) {fragdur = targetBuffer.duration} { fragdur = dur };
			indices.do({|ind, i|
				var synth, args, def, savename, et, co, cospec;
				def = defs[ind];
				Post << "-------------  START " << def.name << " --------------" << Char.nl;
				et = gep.at(ind).asUgenExpressionTree;
				et.asSynthDefString(def.name.asSymbol, Pan2, Normalizer, true).postln;
				args =  [terminals, params[ind]].lace(terminals.size * 2).postln;
				Post << "SCORE: " << gep.at(ind).score << Char.nl;
				synth = Synth(def.name, [\amp, 1, \dur, fragdur] ++ args)
					.setn(\env, Env([0,1,1,0],[0.1, 0.8, 0.1]));
				fragdur.wait;
				synth.free;
				synth = nil;				
				Server.default.sync;
				0.5.wait;
				co = paramgep.chromosomes[ind];
				savename = gep.makeDefName(ind);
				cospec = ('code': co.code, 'args': args, 
					'constants': co.constants, 'extraDomains': co.extraDomains, 
					'defname': savename);
				et.saveAsSynthDef(savename, Pan2, Normalizer, cospec, 
					gepAnalyzer.stats[def.name.asSymbol]);
				gep.saveData(ind, savename);
			})
		}).play	
	}
	
}