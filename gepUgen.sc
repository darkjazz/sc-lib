UGEP : GEP {

	classvar <fileExt = "gepdata";
	classvar <fileNamePrefix = "gep";

	var <>colors, <similarityMatrix, <names, <jsonWriter;
	var <>dbname = "ges_00";

	*new{|populationSize, numgenes, headsize, ugens, terminals, linker, forceArgs|
		^super.new(populationSize, numgenes, headsize, ugens, terminals, linker, forceArgs).init
	}

	*newValid{|populationSize, numgenes, headsize, ugens, terminals, linker, forceArgs, methodRatio=0.5|
		^super.new(populationSize, numgenes, headsize, ugens, terminals, linker, forceArgs, methodRatio).initValid
	}

	*newRandomFromLibrary{|populationSize, numgenes, headsize, linker, excludeUGenList|
		^super.new(populationSize, numgenes, headsize, nil, nil, linker).initFromLibrary(excludeUGenList)
	}

	*newFromLibrary{|numgenes, headsize, linker, excludeUGenList|
		^super.new(1, numgenes, headsize, nil, nil, linker).loadAllFromLibrary(excludeUGenList)
	}

	*newFromList{|list, numgenes, headsize, linker|
		^super.new(list.size, numgenes, headsize, nil, nil, linker).fillFromList(list)
	}

	*newRandomFromDb{|populationSize, numgenes, headsize, linker|
		^super.new(populationSize, numgenes, headsize, nil, nil, linker).fillFromDb
	}

	init{
		this.setTailSize;
		this.randInitChromosomes;
	}

	initValid{
		this.setTailSize;
		this.randInitValidChromosomes;
	}

	randInitChromosomes{|forceInitFunc=true|
		if (terminals.notNil.and(methods.notNil)) {
			chromosomes = Array.fill(populationSize, {
				var indv;
				indv = Array();
				numgenes.do({
					if (forceInitFunc) {
						indv = indv ++ Array.with(methods.choose) ++ Array.fill(headsize-1, {
							[methods, terminals].choose.choose
						})
					}
					{
						indv = indv ++ Array.fill(headsize, {
							[methods, terminals].choose.choose
						})
					};
					indv = indv ++ Array.fill(tailsize, {
						terminals.choose
					})
				});
				GEPChromosome(indv, terminals, numgenes, linker, forceArgs )
			})
		}
	}

	initFromLibrary{|excludeUGenList|
		var selection, data, randInd;
		data = UGepLoader(headsize, numgenes).load;
		selection = data.select({|data|
			(data.header.headsize == headsize).and(data.header.numgenes == numgenes)
		});
		excludeUGenList = excludeUGenList ? [];
		selection = selection.select({|data|
			excludeUGenList.collect({|class| data.code.includes(class) }).asInt.sum == 0
		});
		methods = [];
		terminals = [];
		selection.do({|data|
			data.code.select(_.isKindOf(Class)).do({|meth|
				if (methods.includes(meth).not) {
					methods = methods.add(meth)
				}
			});
			data.code.select(_.isKindOf(Symbol)).do({|term|
				if (terminals.includes(term).not) {
					terminals = terminals.add(term)
				}
			})
		});
		terminals.sort;
		this.setTailSize;
		chromosomes = Array.fill(populationSize, {
			randInd = selection.size.rand;
			GEPChromosome( selection.removeAt(randInd).code, terminals, numgenes, linker )
		});
	}

	loadAllFromLibrary{|excludeUGenList, data|
		var selection, randInd;
		if (data.isNil) {
			data = this.class.loadDataFromDir
		};
		selection = data.select({|data|
			(data.header.headsize == headsize).and(data.header.numgenes == numgenes)
		});
		excludeUGenList = excludeUGenList ? [];
		selection = selection.select({|data|
			excludeUGenList.collect({|class| data.code.includes(class) }).asInteger.sum == 0
		});
		methods = [];
		terminals = [];
		selection.do({|data|
			data.code.select(_.isKindOf(Class)).do({|meth|
				if (methods.includes(meth).not) {
					methods = methods.add(meth)
				}
			});
			data.code.select(_.isKindOf(Symbol)).do({|term|
				if (terminals.includes(term).not) {
					terminals = terminals.add(term)
				}
			})
		});
		terminals.sort;
		this.setTailSize;
		populationSize = selection.size;
		chromosomes = Array.fill(populationSize, {|i|
			GEPChromosome( selection[i].code, terminals, numgenes, linker )
		});
	}

	fillFromList{|list|
		methods = [];
		terminals = [];
		list.do({|code|
			code.select(_.isKindOf(Class)).do({|meth|
				if (methods.includes(meth).not) {
					methods = methods.add(meth)
				}
			});
			code.select(_.isKindOf(Symbol)).do({|term|
				if (terminals.includes(term).not) {
					terminals = terminals.add(term)
				}
			})
		});
		terminals.sort;
		this.setTailSize;
		chromosomes = list.collect({|code|
			GEPChromosome(code, terminals, numgenes, linker)
		});
	}

	fillFromDb{
		var db, docs, loader;
		db = MongoDb();
		docs = db.getDocuments(numgenes: numgenes, headsize: headsize, limit: populationSize * 2);
		docs = docs.scramble;
		Post << "RETRIEVED DOCS FROM MONGO: " << docs.size << Char.nl;
		methods = [];
		terminals = docs.first.terminals;
		chromosomes = populationSize.collect({
			var data = docs.pop;
			data.methods.do({|ugen|
				if (methods.includes(ugen).not) {
					methods = methods.add(ugen)
				}
			});
			GEPChromosome(data.code, data.terminals, numgenes, linker)
		});
		this.setTailSize;
	}

	setTailSize{
		tailsize = headsize * (this.maxNumberOfArgs - 1) + 1;
		genesize = headsize + tailsize;
	}

	randInitValidChromosomes{
		chromosomes = Array();
		this.addChromosome;
	}

	checkChromosome{|chrm|
		var isValid = false;
		try{
			chrm.asUgenExpressionTree.asSynthDefString(\temp).interpret.add;
			isValid = true
		};
		^isValid
	}

	addChromosome{
		var indv, chrm;
		indv = Array();
		numgenes.do({
			indv = indv ++ Array.with(methods.choose) ++ Array.fill(headsize-1, {
				[methods, terminals].wchoose([methodRatio, 1.0-methodRatio].normalizeSum).choose
			});
			indv = indv ++ Array.fill(tailsize, {
				terminals.choose
			})
		});
		chrm = GEPChromosome(indv, terminals, numgenes, linker, forceArgs );
		if (this.checkChromosome(chrm)) {
			chromosomes = chromosomes.add(chrm);
		};
		if (chromosomes.size < populationSize) {
			this.addChromosome
		}

	}

	maxNumberOfArgs{
		var max;
		max = (methods.collect({|ugen|
			var ar;
			ar = ugen.class.methods.select({|mth| mth.name == 'ar' }).first;
			if (ar.isNil) {
				ar = ugen.superclass.class.methods.select({|mth| mth.name == 'ar' }).first
			};
			ar.argNames.size - 1
		}) ? [0]).maxItem;
		^max
	}

	saveData{|index, name, run = 0|
		var filename, archive, header;
		filename = name ? this.makeDefName(index);
		header =  (run: run, generation: generationCount, headsize: headsize, numgenes: numgenes);
		archive = ZArchive.write(Paths.gepArchDir +/+ filename ++ "." ++ this.class.fileExt);
		archive.writeItem(header);
		archive.writeItem(methods);
		archive.writeItem(terminals);
		archive.writeItem(linker);
		archive.writeItem(this.at(index).code);
		archive.writeClose;
	}

	saveJson{|index, panner, limiter, args, pchrom, features, name|
		var data, defname, chrom;
		if (jsonWriter.isNil) { jsonWriter = JsonWriter(dbname) };
		defname = this.makeDefName(index);
		if (name.notNil) { defname = name };
		chrom = this.chromosomes[index];
		data = this.buildDict(defname, chrom, args, pchrom, features);
		this.saveSynthDef(chrom, defname, panner, limiter);
		jsonWriter.writeSynthDef(data);
	}

	buildDict{|defname, chrom, args, pchrom, stats|
		var data, params;
		params = (code: pchrom.code, literals: args, constants: pchrom.constants,
			extraDomains: pchrom.extraDomains);
		data = ();
		data['defname'] = defname.asString;
		data['code'] = chrom.code;
		data['generation'] = generationCount;
		data['headsize'] = headsize;
		data['numgenes'] = numgenes;
		data['methods'] = methods;
		data['terminals'] = terminals;
		data['linker'] = linker;
		data['params'] = params;
		data['stats'] = this.syncStats(stats);
		^data;
	}

	syncStats{|stats|
		var dev, means, values, newstats;
		values = stats.collect(_.last).flop;
		means = values.collect(_.mean);
		newstats = ();
		newstats['amp'] = ('mean': means[22], 'stdDev': values[22].stdDev);
		newstats['cent'] = ('mean': means[21], 'stdDev': values[21].stdDev);
		newstats['flat'] = ('mean': means[20], 'stdDev': values[20].stdDev);
		newstats['err'] = ('mean': 0, 'stdDev': 0);
		newstats['mfcc'] = means[0..19].collect({|mean, i|
			('mean': mean, 'stdDev': values[i].stdDev)
		});
		^newstats
	}

	saveSynthDef{|chrom, defname, panner, limiter|
		var tree, synthDef;
		tree = chrom.asUgenExpressionTree;
		tree.saveSynthDef(defname.asSymbol, panner, limiter);
	}

	makeDefName{|index|
		^(this.class.fileNamePrefix ++ "_gen" ++ generationCount.asString.padLeft(3, "0") ++ "_"
			++ index.asString.padLeft(3, "0") ++ "_" ++ Date.getDate.stamp
		).asSymbol
	}

	*loadData{|path|
		var archive, data;
		data = ();
		archive = ZArchive.read(path);
		data[\header] = archive.readItem;
		data[\methods] = archive.readItem;
		data[\terminals] = archive.readItem;
		data[\linker] = archive.readItem;
		data[\code] = archive.readItem;
		archive.close;
		^data
	}

	*loadDataFromDir{|path|
		path = path ? Paths.gepArchDir;
		^(path+/+"*").pathMatch.collect({|name| this.loadData(name) })
	}

}


UGenExpressionTree : ExpressionTree {

	classvar <foaControls;

	var drawdict, ugens, colors, names;

	decode{
		var code;
		code = chrom.code.clump((chrom.code.size/chrom.numGenes).asInteger);
		root = UGepNode(\root, Array.fill(chrom.numGenes, {|i|
			var argindex = 1, array;
			gene = code.at(i);
			array = gene.collect({|codon, i|
				var indices;
				if (codon.isKindOf(Class)) {
					var event = (), argNames;
					argNames = this.getArgNames(codon.class).select({|name| name != \this });
					indices = (argindex..argindex+argNames.size-1);
					argindex = argindex + indices.size;
					event[i] = indices;
				}
			}).select(_.notNil);
			this.appendArgs(array.first, array)

		}));
	}

	appendArgs{|event, array|
		var nodes, class;
		depth = depth + 1;
		if (depth > maxDepth) { maxDepth = depth };
		nodes = event.values.pop.collect({|ind|
			if (gene[ind].isKindOf(Class)) {
				this.appendArgs(array.select({|sev|
					sev.keys.pop == ind
				}).first, array )
			} {
				UGepNode(gene[ind])
			}
		});
		class = gene[event.keys.pop];
		depth = depth - 1;
		^UGepNode(class, nodes, this.getArgNames(class.class).select({|name| name != \this }))
	}

	getArgNames{|class|
		var ar;
		if (class.methods.notNil) {
			ar = class.methods.select({|meth| meth.name == 'ar' }).first.argNames
		}
		{
			ar = this.getArgNames(class.superclass)
		}
		^ar
	}

	asSynthDefString{|defname, panner, limiter, addEnv=false|
		var string;
		string = "SynthDef('" ++ defname ++ "', "
			++ "{" ++ this.appendSynthDefTerminals ++ " Out.ar(out,";
		if (panner.notNil) {
			string = string ++ panner.asString ++ ".ar("
		};
		if (limiter.notNil) {
			string = string ++ limiter.asString ++ ".ar("
		};
		string = string ++ "LeakDC.ar(";
		string = string ++ this.asFunctionString(false);
		string = string ++ ")";
		if (limiter.notNil) {
			string = string ++ ")"
		};
		if (panner.notNil) {
			string = string ++ ",0,amp)"
		};
		if (addEnv)
		{
			string = string ++ " * EnvGen.kr(EnvControl.kr,timeScale:dur,doneAction:2)"
		};
		string = string ++ ") })";
		^string
	}

	asSynthDefWrapString{|limiter|
		var string;
		string = "{" ++ this.appendSynthDefWrapTerminals;
		if (limiter.notNil) {
			string = string ++ limiter.asString ++ ".ar("
		};
		string = string ++ "LeakDC.ar(";
		string = string ++ this.asFunctionString(false);
		string = string ++ ")";
		if (limiter.notNil) {
			string = string ++ ")"
		};
		string = string ++ "}";
		^string
	}

	asFoaSynthDefString{|defname, limiter, rotX, rotY, rotZ|
		var string;
		string = "SynthDef('" ++ defname ++ "', "
			++ "{" ++ this.appendFoaSynthDefTerminals ++ " Out.ar(out, ";
		string = string ++ this.openFoaString;
		if (limiter.notNil) {
			string = string ++ limiter.asString ++ ".ar("
		};
		string = string ++ this.asFunctionString(false);
		if (limiter.notNil) {
			string = string ++ ")"
		};
		if (rotX.isKindOf(Symbol).and(this.class.foaControls.keys.includes(rotX) )) {
			string = string ++ this.closeFoaString(*this.class.foaControls[rotX])
		}
		{
			if (rotX.isKindOf(Symbol)) { rotX = 0 };
			string = string ++ this.closeFoaString(rotX, rotY ? 0, rotZ ? 0);
		};
		string = string ++ ") })";
		^string

	}

	asFoaNdefString{|defname, limiter, rotX, rotY, rotZ|
		^this.asFoaSynthDefString(defname, limiter, rotX, rotY, rotZ).replace("SynthDef", "Ndef")
	}

	openFoaString{
		^"FoaTransform.ar( FoaEncode.ar( Array.fill(4, { IFFT( PV_Diffuser( FFT( LocalBuf(1024), Limiter.ar("
	}

	closeFoaString{|rotX, rotY, rotZ|
		^(")*amp))) }), FoaEncoderMatrix.newAtoB ), 'rtt', " ++ rotX ++ ", " ++ rotY ++ ", " ++ rotZ ++ ")")
	}

	appendSynthDefTerminals{
		var str = "|out=0,amp=0,dur=1,";
		chrom.terminals.do({|sym|
			str = str ++ sym.asString ++ ","
		});
		^(str.keep(str.size-1) ++ "| ")
	}

	appendFoaSynthDefTerminals{
		var str = "|out=0,amp=0,rotx=0,roty=0,rotz=0,";
		chrom.terminals.do({|sym|
			str = str ++ sym.asString ++ ","
		});
		^(str.keep(str.size-1) ++ "| ")
	}

	appendSynthDefWrapTerminals{
		var str = "|";
		chrom.terminals.do({|sym|
			str = str ++ sym.asString ++ ","
		});
		^(str.keep(str.size-1) ++ "| ")
	}

	appendString{|node|
		var str = "";

		if (node.isFunction) {

			str = str + node.value.asString ++ ".ar(";

			node.nodes.do({|subnode, i|
				var farg;
				farg = chrom.forceArgs.select({|aval, aname| node.argNames[i] == aname });
				if (farg.size == 1) {
					str = str ++ farg.first.values.first
				}
				{
					str = str ++ this.appendString(subnode);
				};
				if (i < node.nodes.lastIndex) {
					if (chrom.isExceptionOp(node.value.name)) {
						str = str + node.value.name.asString ++ " "
					}
					{
						str = str ++ ", "
					}
				}
			});

			str = str ++ " )";
		}
		{
			str = str ++ node.value.asString
		};
		^str
	}

	saveAsSynthDef{|name, panner, limiter, args, stats|
		var def, arch, meta;
		def = this.asSynthDefString(name, panner, limiter).interpret;
		def.writeDefFile(Paths.gepDefDir);
		Post << "Wrote SynthDef " << name << " to " << Paths.gepDefDir << Char.nl;
		arch = ZArchive.write(Paths.gepMetaDir ++ name ++ ".gepmeta");
		meta = (args: args, stats: stats);
		arch.writeItem(meta);
		arch.writeClose;
		arch = nil;
		Post << "Wrote metadata for " << name << " to " << Paths.gepMetaDir << Char.nl;
	}

	saveSynthDef{|name, panner, limiter|
		var def;
		def = this.asSynthDefString(name, panner, limiter).interpret;
		def.writeDefFile(Paths.gepDefDir);
		Post << "Wrote SynthDef " << name << " to " << Paths.gepDefDir << Char.nl;
	}

	*loadMetadata{|defname, path|
		var meta, arch;
		path = path ? Paths.gepMetaDir;
		arch = ZArchive.read(path ++ defname.asString ++ ".gepmeta");
		meta = arch.readItem;
		arch.close;
		arch = nil;
		^meta
	}

	*loadMetadataFromDir{|path|
		path = path ? Paths.gepMetaDir;
		^(path++"*").pathMatch.collect(_.basename).collect(_.split($.)).collect(_.first).collect({|name|
			var data = this.loadMetadata(name, path);
			data.defname = name;
			data
		})
	}

	*initClass{
		foaControls = Dictionary();
		foaControls['noise0'] = [
			"LFNoise0.kr(rotx).range(-pi, pi)",
			"LFNoise0.kr(roty).range(-pi, pi)",
			"LFNoise0.kr(rotz).range(-pi, pi)"
		];
		foaControls['noise2'] = [
			"LFNoise2.kr(rotx).range(-pi, pi)",
			"LFNoise2.kr(roty).range(-pi, pi)",
			"LFNoise2.kr(rotz).range(-pi, pi)"
		];
		foaControls['saw'] = [
			"LFSaw.kr(rotx).range(-pi, pi)",
			"LFSaw.kr(roty).range(-pi, pi)",
			"LFSaw.kr(rotz).range(-pi, pi)"
		];
		foaControls['sine'] = [
			"SinOsc.kr(rotx).range(-pi, pi)",
			"SinOsc.kr(roty).range(-pi, pi)",
			"SinOsc.kr(rotz).range(-pi, pi)"
		];
		foaControls['mix1'] = [
			"SinOsc.kr(rotx).range(-pi, pi)",
			"LFSaw.kr(roty).range(-pi, pi)",
			"LFNoise0.kr(rotz).range(-pi, pi)"
		];
		foaControls['mix2'] = [
			"LFNoise1.kr(rotx).range(-pi, pi)",
			"LFTri.kr(roty).range(-pi, pi)",
			"SinOsc.kr(rotz).range(-pi, pi)"
		];
		foaControls['mix3'] = [
			"LFSaw.kr(rotx).range(-pi, pi)",
			"LFNoise1.kr(roty).range(-pi, pi)",
			"LFPulse.kr(rotz).range(-pi, pi)"
		];
		foaControls['mix4'] = [
			"LFNoise0.kr(rotx).range(-pi, pi)",
			"SinOsc.kr(roty).range(-pi, pi)",
			"LFSaw.kr(rotz).range(-pi, pi)"
		];
	}

}

UGepNode : GepNode{

	var <>argNames;

	*new{|value, nodes, argNames|
		^super.new(value, nodes).init(argNames)
	}

	init{|names| argNames = names }

}

/*
UGepNode : GepNode{
	var <value, <>nodes, <>range;

	*new{|value, nodes, range|
		^super.new(value, nodes).range_(range)
	}

}
*/

UGepPlayer{

	var <defname, <defstr, headsize, numgenes, code, linker, methods, terminals, args, stats;
	var argcode, chrom, et, isValid, <synth, <>amp = 0;

	*new{|defname|
		^super.newCopyArgs(defname).init
	}

	init{
		var path, data, meta;
		path = Paths.gepArchDir +/+ defname.asString ++ "." ++ UGEP.fileExt;
		if (File.exists(path)) {
			data = UGEP.loadData(path);
			meta = UGenExpressionTree.loadMetadata(defname);
			headsize = data.header.headsize;
			numgenes = data.header.numgenes;
			code = data.code;
			linker = data.linker;
			methods = data.methods;
			terminals = data.terminals;
			args = meta.args;
			stats = meta.stats;
			isValid = true;
			this.makeDefString;
		}
		{
			("Data file " ++ path ++ " not found").warn
		}
	}

	makeDefString{
		chrom = GEPChromosome(code, terminals, numgenes, linker);
		et = chrom.asUgenExpressionTree;
		defstr = et.asFoaSynthDefString(defname, Normalizer,
			UGenExpressionTree.foaControls.keys.choose
		);
		{
			defstr.interpret.add
		}.try({ isValid = false });
	}

	getArgs{
		if (args.isKindOf(Event)) {
			^args.args
		}
		{
			^args
		}
	}

	play{|target=1, addAction='addToHead', out=0, amp=0, rotx=0, roty=0, rotz=0|
		if (isValid) {
			synth = Synth(defname, [\out, out, \amp, amp, \rotx, rotx, \roty, roty, \rotz, rotz]
				++ this.getArgs, target, addAction)
		}
		{
			"SynthDef not valid".error
		}
	}

	fade{|start=0, end=0, time=1, interval=0.1|
		var value, incr, numSteps;
		numSteps = (time/interval).asInteger;
		incr = end - start / numSteps;
		value = start;
		Routine({
			numSteps.do({
				synth.set('amp', value);
				value = value + incr;
				interval.wait
			});
			synth.set('amp', end);
			Post << "finished fade for " << defname << Char.nl;
		}).play
	}

	set{|name, value| synth.set(name, value) }

	free{ synth.free }

	asPmono{|target=1, addAction='addToHead', ampPattern=1, deltaPattern=1, out=0, rotx=1, roty=1, rotz=1|
		^Pmono(
			defname, \group, target, \addAction, addAction,
			\out, out, \rotx, rotx, \roty, roty, \rotz, rotz,
			\ampvalue, ampPattern, \delta, deltaPattern,
			\amp, Pfunc({|event| amp * event.ampvalue })
			*this.getArgs
		)
	}

}
