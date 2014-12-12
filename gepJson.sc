GepDataJsonConverter{
	
	var <db, <loader, <lib;
	
	*new{|db, loader| 
		^super.newCopyArgs(db, loader).init 
	}
	
	init{
		if (loader.notNil) { lib = loader.load };
	}
	
	convert{
		Routine({
			lib.do({|data|
				db.put(this.wrapSingleQuotes(this.build(data)));
				0.5.wait;
			})
		}).play;
	}
	
	build{|data|
		var collect, json, mean, std, mfcc, args;
		json = "";
		
		// header
		json = json ++ this.wrap('defname', data.defname);
		json = json ++ this.wrap('date', data.defname.drop(15).keep(6));
		json = json ++ this.wrap('time', data.defname.drop(15).drop(7));
		json = json ++ this.wrap('generation', data.data.header.generation);
		json = json ++ this.wrap('headsize', data.data.header.headsize);
		json = json ++ this.wrap('numgenes', data.data.header.numgenes);
		
		// data
		collect = "";
		data.data.code.do({|codon|
			collect = collect ++ this.wrapQuotes(
				if (codon.isKindOf(Class)) { codon.name } { codon }
			);
			collect = collect ++ ", ";
		});
		json = json ++ this.wrapLiteral('code', 
			this.wrapList(this.terminate(collect)));

		json = json ++ this.wrap('linker', data.data.linker.name.asString);

		collect = "";
		data.data.methods.collect(_.name).do({|ugen|
			collect = collect ++ this.wrapQuotes(ugen.asString) ++ ", ";
		});
		json = json ++ this.wrapLiteral('methods', 
			this.wrapList(this.terminate(collect)));
			
		collect = "";
		data.data.terminals.do({|term|
			collect = collect ++ this.wrapQuotes(term.asString) ++ ", ";
		});
		json = json ++ this.wrapLiteral('terminals', 
			this.wrapList(this.terminate(collect)));
		
		// stats
		collect = "";
		#[cent, amp, flat, err].do({|stat|
			var str;
			str = this.wrapLiteral('mean', data.stats[stat].mean); 
			str = str ++ this.wrapLiteral('stdDev', data.stats[stat].stdDev, true);
			collect = collect ++ this.wrapLiteral(stat, this.wrapJson(str));
		});
		mean = "";
		data.stats.mfcc.collect(_.mean).do({|mfc|
			mean = mean ++ mfc ++ ", ";
		});
		mean = this.wrapList(this.terminate(mean));
		std = "";
		data.stats.mfcc.collect(_.stdDev).do({|mfc|
			std = std ++ mfc ++ ", ";
		});
		std = this.wrapList(this.terminate(std));
		
		mfcc = this.wrapLiteral('mean', mean) 
			++ this.wrapLiteral('stdDev', std, true); 
		
		collect = collect ++ this.wrapLiteral('mfcc', this.wrapJson(mfcc), true);
		json = json ++ this.wrapLiteral('stats', this.wrapJson(collect));
		
		// args
		
		args = "";
		collect = "";
		data.args.args.do({|item| 
			collect = collect ++ if (item.isKindOf(Symbol)) 
			{ this.wrapQuotes(item) }
			{ item } ++ ", "
		});
		
		args = args ++ this.wrapLiteral('literals', 
			this.wrapList(this.terminate(collect)));

		collect = "";
		data.args.code.do({|codon|
			collect = collect ++ this.wrapQuotes(
				if (codon.isKindOf(Symbol)) { codon } { codon.name }
			);
			collect = collect ++ ", ";
		});
		args = args ++ this.wrapLiteral('code', 
			this.wrapList(this.terminate(collect)));

		collect = "";
		data.args.constants.do({|cons|
			collect = collect ++ cons.asString;
			collect = collect ++ ", ";
		});
		args = args ++ this.wrapLiteral('constants', 
			this.wrapList(this.terminate(collect)));
			
		collect = "";
		data.args.extraDomains.flat.do({|spec|
			collect = collect ++ this.wrapList(
				spec.minval.asString ++ ", " ++ 
				spec.maxval.asString ++ ", " ++ 
				"\"" ++ spec.warp.asSpecifier.asString ++ "\""
			) ++ ", "
		});
		args = args ++ this.wrapLiteral('extraDomains', 
			this.wrapList(this.terminate(collect)));
				
		json = json ++ this.wrapLiteral('args', 
			this.wrapJson(this.terminate(args)), true);
		
		^this.wrapJson(json)
	}
	
	wrap{|key, value, last=false|
		var str;
		str = ("\"" ++ key.asString ++ "\": \"" ++ value.asString ++ "\"");
		if (last.not) { str = str ++ ", " };
		^str
	}
	
	wrapLiteral{|key, value, last=false|
		var str = "\"" ++ key.asString ++ "\": " ++ value.asString;
		if (last.not) { str = str ++ ", " };
		^str
	}
	
	wrapJson{|json|
		^("{ " ++ json ++ " }")
	}
	
	wrapList{|list|
		^("[ " ++ list ++ " ]")
	}
	
	wrapQuotes{|value|
		^("\"" ++ value ++ "\"")
	}
	
	wrapSingleQuotes{|value|
		^("'" ++ value ++ "'")
	}
	
	terminate{|str|
		^(str.keep(str.size - 2))
	}
}