PatchRecognizer{
	
	classvar <savePath = "/Users/alo/Data/mikro/", <extension = ".plib";
	
	var mikro, <weightDict, <currentGuess, <mostCommon, lastValues, <length = 20;
	
	var responderFunctions;
	
	*new{|mikro|
		^super.newCopyArgs(mikro).init
	}
	
	init{
		weightDict = ();
		lastValues = (0 ! length);
		responderFunctions = ();
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
				this.findMostCommon;
				responderFunctions.do(_.value(this))
			}
		})
	}
	
	stop{
		mikro.analyzer.removeEventResponderFunction(\patchRecognizer);
	}
	
	addResponderFunction{|key, func|
		responderFunctions.put(key, func);
	}
	
	removeResponderFunction{|key|
		responderFunctions[key] = nil;
	}
	
	clearResponderFunctions{
		responderFunctions.clear;
	}
	
	findMostCommon{
		var counts, maxItem, str;
		counts = ();
		lastValues = lastValues.shift(1);
		lastValues[0] = this.patchStringToInt(currentGuess.asString);
		lastValues.do({|val, i|
			if (counts.includesKey(val)) {
				counts[val] = counts[val] + 1
			}
			{
				counts[val] = 1
			};
		});
		
		maxItem = counts.findKeyForValue(counts.values.maxItem);
		str = "";
		maxItem.asBinaryDigits(5).do({|dg| str = str ++ dg.asString });
		str = str.insert(2, "_");
		mostCommon = str.asSymbol;
	}
	
	patchStringAsDigits{|str|
		var tempstr, arr = Array.newClear(5);
		str = str ? currentGuess.asString;
		tempstr = str.copy;
		tempstr.removeAt(2);
		tempstr.do({|chr, i|
			arr[i] = chr.asString.asInt;
		});
		^arr		
	}
	
	patchStringToInt{|str|
		^this.patchStringAsDigits(str).convertDigits(2);
	}
	
	load{|path|
		var file, ncoef;
		weightDict.clear;
		file = File(path, "rb");
		ncoef = file.getFloat.asInt;
		if (ncoef != mikro.analyzer.numcoef) {
			"Number of coefficients in the specified file does not match the current analysis. Load failed.".error;
		}
		{
			32.do({
				var key = "";
				Array.fill(6, { file.getChar }).do({|chr| key = key ++ chr });
				file.getFloat;
				weightDict[key.asSymbol] = Array.fill(mikro.analyzer.numcoef, { file.getFloat });
				file.getChar
			});
		};
		file.close;
	}
	
	save{
		var file;
		file = File(this.class.savePath ++ Date.getDate.stamp ++ this.class.extension, "wb");
		file.putFloat(mikro.analyzer.numcoef.asFloat);
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
				key = mikro.input.currentPatch;
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