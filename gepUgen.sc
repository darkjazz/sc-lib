UGEP : GEP {
	
	classvar <archDir = "/Users/alo/Data/gep/data", <fileExt = "gepdata", <>ranges;
	classvar <fileNamePrefix = "gep";
				
	*new{|populationSize, numgenes, headsize, ugens, terminals, linker|
		^super.new(populationSize, numgenes, headsize, ugens, terminals, linker).init
	}
	
	*newValid{|populationSize, numgenes, headsize, ugens, terminals, linker|
		^super.new(populationSize, numgenes, headsize, ugens, terminals, linker).initValid
	}
	
	init{
		tailsize = headsize * (this.maxNumberOfArgs - 1) + 1;
		this.randInitChromosomes;
	}
	
	initValid{
		tailsize = headsize * (this.maxNumberOfArgs - 1) + 1;
		this.randInitValidChromosomes
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
				[methods, terminals].choose.choose
			});
			indv = indv ++ Array.fill(tailsize, {
				terminals.choose
			})
		});
		chrm = ORF(indv, terminals, numgenes, linker );
		if (this.checkChromosome(chrm)) {
			chromosomes = chromosomes.add(chrm);
		};
		if (chromosomes.size < populationSize) {
			this.addChromosome
		}
		
	}
	
	maxNumberOfArgs{
		var max = 0;
		max = methods.collect({|ugen|
			var ar;
			ar = ugen.class.methods.select({|mth| mth.name == 'ar' }).first;
			if (ar.isNil) {
				ar = ugen.superclass.class.methods.select({|mth| mth.name == 'ar' }).first
			};
			ar.argNames.size - 1
		}).maxItem;
		^max
	}
	
	saveData{|index, name, run = 0|
		var filename, archive, header;
		filename = name ? this.makeDefName(index);
		header =  (run: run, generation: generationCount, headsize: headsize, numgenes: numgenes);
		archive = ZArchive.write(this.class.archDir +/+ filename ++ "." ++ this.class.fileExt);
		archive.writeItem(header);
		archive.writeItem(methods);
		archive.writeItem(terminals);
		archive.writeItem(linker);
		archive.writeItem(this.at(index).code);
		archive.writeClose;
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
	
}

UGenExpressionTree : ExpressionTree {
	
	classvar <defDir = "/Users/alo/Data/gep/synthdefs/", <metaDir = "/Users/alo/Data/gep/metadata/";
	
	unpack{
		var code;
		code = orf.code.clump((orf.code.size/orf.numGenes).asInt);
		root = GepNode(\root, Array.fill(orf.numGenes, {|i|
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
		var nodes;
		nodes = event.values.pop.collect({|ind| 
			if (gene[ind].isKindOf(Class)) {
				this.appendArgs(array.select({|sev| sev.keys.pop == ind }).first, array ) 
			} {
				GepNode(gene[ind])
			}
		}); 
		^GepNode(gene[event.keys.pop], nodes)		
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
	
	asSynthDefString{|defname, panner, limiter|
		var string;
		string = "SynthDef('" ++ defname ++ "', " 
			++ "{" ++ this.appendSynthDefTerminals ++ " Out.ar(out, ";
		if (panner.notNil) {
			string = string ++ panner.asString ++ ".ar("
		};
		if (limiter.notNil) {
			string = string ++ limiter.asString ++ ".ar("
		};
		string = string ++ this.asFunctionString(false); 
		if (panner.notNil) {
			string = string ++ ")"
		};
		if (limiter.notNil) {
			string = string ++ ")"
		};
		string = string ++ ") })";
		^string
	}
	
	appendSynthDefTerminals{
		var str = "|out=0,";
		orf.terminals.do({|sym|
			str = str ++ sym.asString ++ ","
		});
		^(str.keep(str.size-1) ++ "| ")		
	}

	appendString{|node|
		var str = "";
		if (node.isFunction) {
			
			str = str + node.value.asString ++ ".ar(";
							
			node.nodes.do({|subnode, i|
				str = str ++ this.appendString(subnode);
				if (i < node.nodes.lastIndex) { 
					if (orf.isExceptionOp(node.value.name)) {
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
		def.writeDefFile(this.class.defDir);
		Post << "Wrote SynthDef " << name << " to " << this.class.defDir << Char.nl; 
		arch = ZArchive.write(this.class.metaDir ++ name ++ ".gepmeta");
		meta = (args: args, stats: stats);
		arch.writeItem(meta);
		arch.writeClose;
		arch = nil;
		Post << "Wrote metadata for " << name << " to " << this.class.metaDir << Char.nl; 
	}
		
	*loadMetadata{|defname|
		var meta, arch;
		arch = ZArchive.read(this.metaDir ++ defname.asString ++ ".gepmeta");
		meta = arch.readItem;
		arch.close;
		arch = nil;
		^meta
	}
	
	*loadMetadataFromDir{|path|
		path = path ? this.metaDir;
		^(path++"*").pathMatch.collect(_.basename).collect(_.split($.)).collect(_.first).collect({|name|
			var data = this.loadMetadata(name);
			data.defname = name;
			data
		})
	}
	
}

UGepNode : GepNode{
	var <value, <>nodes, <>range;
	
	*new{|value, nodes, range|
		^super.new(value, nodes).range_(range)
	}

}
