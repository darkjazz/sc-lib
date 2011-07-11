PatchRecognizer{
	
	classvar <savePath = "/Users/alo/mikro/", <extension = ".plib";
	
	var mikro, <weightDict, <currentGuess;
	
	*new{|mikro|
		^super.newCopyArgs(mikro).init
	}
	
	init{
		weightDict = ();
	}
	
	run{
		if (weightDict.size == 0)
		{
			this.load((this.class.savePath ++ "*" ++ this.class.extension).pathMatch.first)
		};
		mikro.analyzer.putEventResponderFunction(\patchRecognizer, {|time, re, ms|
			var weights;
			if (ms[2] == 3) {
				weights = ms[3..mikro.analyzer.numcoef+2];
				currentGuess = this.findBestMatch(weights);
			}
		})
	}
	
	load{|path|
		var file;
		weightDict.clear;
		file = File(path, "rb");
		32.do({
			var key = "";
			Array.fill(6, { file.getChar }).do({|chr| key = key ++ chr });
			file.getFloat;
			weightDict[key.asSymbol] = Array.fill(mikro.analyzer.numcoef, { file.getFloat });
			file.getChar
		});
		file.close;
	}
	
	save{
		var file;
		file = File(this.class.savePath ++ Date.getDate.stamp ++ this.class.extension, "wb");
		weightDict.keysValuesDo({|key, ev|
			file.putString(key.asString);
			file.putFloat(ev.hits);
			ev.weights.do({|val|
				file.putFloat(val)
			});
			file.putChar($\n)
		});
		file.close
	}
	
	train{
		32.do({|i|
			var str = "";
			i.asBinaryDigits(5).do({|dg| str = str ++ dg.asString });
			str = str.insert(2, "_");
			weightDict[str.asSymbol] = (hits: 0, weights: (0 ! mikro.analyzer.numcoef));
		});
		
		mikro.analyzer.putEventResponderFunction(\patchRec, {|time, re, ms|
			var weights, key;
			if (ms[2] == 3) {
				weights = ms[3..~nCoef+2];
				key = mikro.currentPatch;
				weightDict[key].weights = weightDict[key].weights * weightDict[key].hits 
					+ weights / (weightDict[key].hits + 1);
				weightDict[key].hits = weightDict[key].hits + 1
			}
		});
	}
	
	findBestMatch{|inputVector|
		var lowest=999, match;
		weightDict.keysValuesDo({|key, weights|
			var diff = (weights - inputVector).squared.sum.sqrt;
			if (diff < lowest) { match = key; lowest = diff };
		});
		^match		
	}
	
}