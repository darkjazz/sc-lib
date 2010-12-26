GA{
	
	var <size, <base, <numDigits, <geneCount, <population;
	
	*new{|size, base = 2, numDigits = 8, geneCount|
		^super.newCopyArgs(size, base, numDigits, geneCount).init
	}
	
	init{
		population = Array.fill(size, {
			Chromosome(geneCount * numDigits, base, numDigits, geneCount)
		})
	}
	
	crossover{|chromo, chooseFunc|
		var crossx, crosspoint;
		crossx = chooseFunc.value(population);
		if ((crossx != chromo).and(crossx.notNil))
		{
			crosspoint = chromo.size.rand;
			population = population.add(
				Chromosome(geneCount * numDigits, base, numDigits, geneCount,
					chromo.copyTo(crosspoint - 1) ++ crossx.copyFrom(crosspoint)
				).score_(1)
			)
		}
	}
		
	do{|func|
		population.do(func)
	}
	
	select{|func|
		^population.select(func)
	}
	
}

Chromosome{
	
	var <size, base, digits, geneCount, <>code, <>score = 0, <active = false, <synth;
	
	*new{|size, base, digits, geneCount, code|
		^super.newCopyArgs(size, base, digits, geneCount).init(code)
	}
	
	init{|argCode|
		if (argCode.isKindOf(Array))
		{
			code = argCode
		}
		{
			code = Array.fill(geneCount, { (base**digits).asInt.rand.asDigits(base, digits) }).flat
		};
		size = size ? code.size
	}
		
	fillRand{|base = 2|
		code = Array.fill(size, {base.rand})
	}
	
	copyTo{|index|
		^code.copyRange(0, index)
	}
	
	copyFrom{|index|
		^code.copyRange(index, code.lastIndex)
	}
	
	mutate{
		code.put(size.rand, base.rand)
	}
	
	activate{|aSynth|
		active = true;
		synth = aSynth;
	}
	
	deactivate{
		active = false;
		synth = nil;
	}
	
					
}
