GEP{
	var <populationSize, <numgenes, <headsize, <methods, <terminals, <linker, <tailsize, <chromosomes;
	var <>mutationRate = 0.05, <>recombinationRate = 0.3, <>transpositionRate = 0.1, <>rootTranspositionRate = 0.1;
	var <>geneRecombinationRate = 0.1, <>geneTranspositionRate = 0.1, fitnessFuncs;
	var <generationCount = 0;
	
	*new{|populationSize, numgenes, headsize, methods, terminals, linker|
		^super.newCopyArgs(populationSize, numgenes, headsize, methods, terminals, linker).init
	}
		
	init{
		tailsize = headsize * (this.maxNumberOfArgs - 1) + 1;
		this.randInitChromosomes;
		fitnessFuncs = Array();
	}
		
	maxNumberOfArgs{
		var max = 0;
		methods.do({|method|
			if (method.argNames.size > max) {
				max = method.argNames.size
			}
		});
		^max
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
				ORF(indv, terminals, numgenes, linker )
			})
		}
	}
	
	add{|orf| chromosomes = chromosomes.add(orf) }
		
	// one point recombination
	recombineSingle{|codeA, codeB|		
		if (recombinationRate.coin) {
			^this.recombine(codeA, codeB, 1)
		}
		{
			^[codeA, codeB]
		}
	}
	
	// multiple point recombination
	recombineMultiple{|codeA, codeB, numpoints=2|
		if (recombinationRate.coin) {
			^this.recombine(codeA, codeB, numpoints)
		}
		{
			^[codeA, codeB]
		};
	}
	
	// gene recombination
	recombineGene{|codeA, codeB|
		if (geneRecombinationRate.coin) {			
			^this.recombine(codeA, codeB, 1, numgenes)
		}
		{
			^[codeA, codeB]
		};
	}

	recombine{|codeA, codeB, numpoints=2, round=1|
		var points, point, off, newcodeA, newcodeB, list;
		newcodeA = Array.newClear(codeA.size);
		newcodeB = Array.newClear(codeB.size);
		off = 2.rand.booleanValue;
		list = (1..codeA.lastIndex-1).scramble;
		points = Pseq(Array.fill(numpoints, { list.pop }).sort.round(round), 1).asStream;
		point = points.next;
		codeA.do({|dg, i|
			if (off) { 
				newcodeA[i] = codeA[i];
				newcodeB[i] = codeB[i];
			} { 
				newcodeA[i] = codeB[i];
				newcodeB[i] = codeA[i]; 
			};
			if (i == point) { off = off.not; point = points.next }
		});
		^[newcodeA, newcodeB]		
	}
		
	// mutation
	mutate{|code, number=1|
		var index;
		if (mutationRate.coin) {
			number.do({
				index = code.size.rand;
				if (index % this.geneSize == 0) {
					code[index] = methods.select({|mth| mth != code[index] }).choose;
				} 
				{
					if (code[index].isKindOf(Method)) {
						code[index] = [methods.select({|mth| mth != code[index] }).choose, terminals.choose].choose
					}
					{
						code[index] = terminals.select({|trm| trm != code[index] }).choose
					}
				}
			})
		}
	}
	
	// transpose insert sequence
	transposeInsertSequence{|code, seqlo=1, seqhi=3|
		var sourceindex, head, targetindex, seqlen, newcode;
		newcode = code.copy;
		if (transpositionRate.coin) {
			targetindex = rrand(0, code.size-this.geneSize).round(this.geneSize) + 1;
			seqlen = rrand(seqlo,seqhi);
			sourceindex = targetindex + rrand(1, this.geneSize-targetindex-seqlen);
			head = code[(sourceindex..sourceindex+seqlen-1)] ++ 
				code[(targetindex..targetindex+headsize-seqlen-1)];
			if (head.includes(nil)) {
				[targetindex, sourceindex, seqlen].postln;
				code.postln; 
				head.postln;
				"----".postln
			};
			(targetindex..targetindex+headsize-2).do({|index, i|
				newcode[index] = head[i]
			});
		};
		
		^newcode
	}
	
	
	// transpose root sequence
	transposeRoot{|code,seqlo=1,seqhi=3|
		var sourceindex, head, targetindex, seqlen, newcode, root, find;
		newcode = code.copy;
		
		if (rootTranspositionRate.coin) {
		
			targetindex = rrand(0, code.size-this.geneSize).round(this.geneSize);
			seqlen = rrand(seqlo,seqhi);
			sourceindex = targetindex + rrand(1, this.geneSize-seqlen);
			find = code[(targetindex..targetindex+headsize-1)].drop(sourceindex-targetindex)
				.selectIndices({|pos| terminals.includes(pos).not }).first;
			if (find.notNil)
			{
				sourceindex = sourceindex + find;	
				head = code[(sourceindex..sourceindex+seqlen-1)] ++ 
					code[(targetindex..targetindex+headsize-seqlen-1)];
				(targetindex..targetindex+headsize-1).do({|index, i|
					newcode[index] = head[i]
				});
			}
		};
		
		^newcode		
	}
	
	// transpose gene
	transposeGene{|code|
		var newcode, targetindex, head;
		newcode = code.copy;
		if (geneTranspositionRate.coin) {
			targetindex = rrand(1, numgenes-1);
			newcode = newcode.clump(this.geneSize);
			head = newcode.removeAt(targetindex);
			newcode.insert(0, head);
			newcode = newcode.flat
		};
		
		^newcode
	}
	
	performRecombination{|orfA, orfB|
		var codeA, codeB;
		codeA = orfA.code;
		codeB = orfB.code;
		if (recombinationRate > 0.0) {
			if (0.5.coin) {
				#codeA, codeB = this.recombineSingle(codeA, codeB);
			} {
				#codeA, codeB = this.recombineMultiple(codeA, codeB);
			}
		};
		if (geneRecombinationRate > 0.0) {
			#codeA, codeB = this.recombineGene(codeA, codeB)
		};
		orfA.code = codeA;
		orfB.code = codeB;
	}
	
	nextGeneration{
		var weights, newgen, scores;
		scores = chromosomes.collect(_.score);
		if (scores.sum > 0) {
			weights = chromosomes.collect(_.score).normalizeSum;
		}
		{
			"no previous scores detected..".warn;
			weights = (populationSize.reciprocal ! populationSize);
		};
		
		// replication
		newgen = Array.fill(populationSize, {
			var orf, index;
			index = weights.windex;
			orf = ORF(chromosomes[index].code.copy, terminals, numgenes, linker);
			orf.score = chromosomes[index].score;
			if (chromosomes.first.constants.notNil) {
				orf.constants = chromosomes[index].constants.copy;
			};
			if (chromosomes.first.extraDomains.notNil) {
				orf.extraDomains = chromosomes[index].extraDomains.copy;
			};
			orf
		});
		
		// mutation
		if (mutationRate > 0.0) {
			newgen.do({|orf| this.mutate(orf.code) })
		};
		
		// insert sequence transposition
		if (transpositionRate > 0.0) {
			newgen.do({|orf| orf.code = this.transposeInsertSequence(orf.code) })
		};
		
		// root transposition
		if (rootTranspositionRate > 0.0) {
			newgen.do({|orf| orf.code = this.transposeRoot(orf.code) })
		};
		
		// gene transposition
		if (geneTranspositionRate > 0.0) {
			newgen.do({|orf| orf.code = this.transposeGene(orf.code) })
		};
		
		// recombination
		newgen.do({|orfA|
			this.performRecombination(orfA, newgen.choose)
//			var codeA, codeB, orfB = newgen.choose;
//			codeA = orfA.code;
//			codeB = orfB.code;
//			if (recombinationRate > 0.0) {
//				if (0.5.coin) {
//					#codeA, codeB = this.recombineSingle(codeA, codeB);
//				} {
//					#codeA, codeB = this.recombineMultiple(codeA, codeB);
//				}
//			};
//			if (geneRecombinationRate > 0.0) {
//				#codeA, codeB = this.recombineGene(codeA, codeB)
//			};
//			orfA.code = codeA;
//			orfB.code = codeB;
		});
		
		chromosomes = newgen;
		
		generationCount = generationCount + 1;
		
		this.updateScores
		
	}
	
	updateScores{ 
		fitnessFuncs.do(_.(chromosomes)) 
	}
	
	addFitnessFunc{|aFunc|
		fitnessFuncs = fitnessFuncs.add(aFunc)
	}
	
	removeFitnessFunc{|index|
		fitnessFuncs.removeAt(index)
	}
	
	geneSize{ ^(headsize + tailsize) }
	
	at{|index| ^chromosomes[index] }
	
	meanScore{ ^chromosomes.collect({|chr| chr.score }).mean }
	
	maxScore{ ^chromosomes.collect(_.score).maxItem }
	
	maxScoreORFs{ 
		var max = this.maxScore;
		^chromosomes.select({|chr| chr.score == max  }) 
	}
	
	// hack: use with caution
	replacePopulation{|orfArray|
		chromosomes = orfArray
	}
				
}

// Open Reading Frame
ORF{
	var <>code, <terminals, <numGenes, <linker, <>score=0;
	var <tree, <>extraDomains, <>constants, parents;
	
	*new{|code, terminals, ngenes, linker|
		^super.newCopyArgs(code, terminals, ngenes, linker)
	}
	
	asExpressionTree{|includeObjects=true|
		tree = ExpressionTree(this, includeObjects);
		^tree
	}
	
	asUgenExpressionTree{|includeObjects=true|
		tree = UGenExpressionTree(this, includeObjects);
		^tree
	}
	
	asSymbols{
		^code.collect({|it| if (it.class == Symbol) { it } { it.name }  })
	}

	isExceptionOp{|name|
		^(['-', '+', '*', '/', '**'].includes(name))
	}
		
	isBooleanOp{|name|
		^(['or', 'not', 'and', 'xor', 'and', 'nand'].includes(name))
	}
	
	fillConstants{|size, func|
		constants = Array.fill(size, func)
	}
	
	addExtraDomain{|domain|
		if (extraDomains.isNil) {
			extraDomains = Array();
		};
		
		extraDomains = extraDomains.add(domain)
	}
	
	setParents{|indA, indB|
		parents = Array.newClear(2);
		parents[0] = indA;
		parents[1] = indB;
	}
	
	progenyOf{ ^parents }
}


// Expression Tree: translates the ORF into a tree structure

ExpressionTree{
	var orf, includeObjects, gene;
	var <root;
	
	*new{|orf, includeObjects=true|
		^super.newCopyArgs(orf, includeObjects).unpack
	}
	
	unpack{
		var code;
		code = orf.code.clump((orf.code.size/orf.numGenes).asInt);
		root = GepNode(\root, Array.fill(orf.numGenes, {|i|
			var argindex = 1, array;
			gene = code.at(i);
			array = gene.collect({|codon, i|
				var indices;
				if (codon.isKindOf(Method)) {
					var event = (), argNames;
					argNames = codon.argNames.select({|name| name != \this });
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
			if (gene[ind].isKindOf(Method)) {
				this.appendArgs(array.select({|sev| sev.keys.pop == ind }).first, array ) 
			} {
				GepNode(gene[ind])
			}
		}); 
		^GepNode(gene[event.keys.pop], nodes)		
	}
		
	asFunctionString{|includeBrackets=true|
		var string = "";
		if (includeBrackets) {
			string = string ++ "{" ++ this.appendTerminals;
		};
		
		if (orf.isExceptionOp(orf.linker.name).not)
		{
			if (orf.isBooleanOp(orf.linker.name)) {
				string = string ++ orf.linker.name.asString
			}
			{
				string = string ++ orf.linker.ownerClass.asString.drop(5) ++ "." ++ orf.linker.name.asString
			}
		};

		string = string ++ "( ";
		
		root.nodes.do({|node, i|
			string = string ++ this.appendString(node);
			if (i < root.nodes.lastIndex) {
				if (orf.isExceptionOp(orf.linker.name)) {
					string = string ++ ")" ++ orf.linker.name.asString ++ "(";
				}
				{
					string = string ++ ", ";
				}
			}
		});
		string = string ++ " )";
		if (includeBrackets) {
			string = string ++ "}"
		};
		^string
	}
	
	appendString{|node|
		var str = "";
		if (node.isFunction) {
			if (includeObjects) {
				str = str + node.value.ownerClass.asString.drop(5) ++ "." 
			};
			
			if (orf.isExceptionOp(node.value.name).not) {
				str = str ++ node.value.name.asString 
			};
			
			str = str ++ "( ";
				
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
	
	appendTerminals{
		var str = "|";
		orf.terminals.do({|sym|
			str = str ++ sym.asString ++ ","
		});
		^(str.keep(str.size-1) ++ "| ")
	}
	
}

GepNode{
	var <value, <>nodes;
	
	*new{|value, nodes|
		^super.newCopyArgs(value, nodes)
	}
		
	isTerminal{ ^nodes.isNil }
	
	isFunction{ ^nodes.notNil }
}