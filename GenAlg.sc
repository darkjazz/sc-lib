GA{
	
	var <size, <base, <numDigits, <geneCount, <population, <>mutationRate = 0.01;
	
	*new{|size, base = 2, numDigits = 8, geneCount|
		^super.newCopyArgs(size, base, numDigits, geneCount).init
	}
	
	init{
		population = Array.fill(size, {
			Chromosome(geneCount * numDigits, base, numDigits, geneCount)
		})
	}
	
	mutate{|code|
		code.do({|dig, i|
			var select;
			if (mutationRate.coin) 
			{ 
				select = (0..base-1);
				select.remove(code[i]);
				code[i] = select.choose; 
			}
		})
	}
	
	crossover{|chrA, chrB|
		var points, point, off, code;
		code = Array.newClear(chrA.code.size);
		off = [true, false].choose;
		points = Pseq(Array.rand(rrand(1, chrA.code.size / 4), 0, chrA.code.lastIndex), 1);
		point = points.next;
		chrA.code.do({|dg, i|
			if (off) { code[i] = chrA.code[i] } { code[i] = chrB.code[i] };
			if (i == point) { off = off.not; point = points.next }
		});
		this.mutate(code);
		^code
	}
		
	do{|func|
		population.do(func)
	}
	
	select{|func|
		^population.select(func)
	}
	
	resetScores{
		this.do(_.score_(0))
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
	
	copyRange{|start, end|
		^code.copyRange(start, end)
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
