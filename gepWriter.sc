JsonWriter{
	var db;
	
	*new{|dbname, useLocal=true|
		^super.new.init(dbname, useLocal)
	}
	
	init{|dbname, useLocal|
		if (useLocal)
		{
			db = CouchDB(NetAddr(JsonLoader.localIP, JsonLoader.localPort), dbname);
		}
		{
			db = CouchDB(NetAddr(JsonLoader.remoteIP, JsonLoader.remotePort), dbname);
		};
	}
	
	saveSynthDef{|data|
		db.put(JsonUtils.wrapSingleQuotes(this.build(data)))
	}
	
	build{|data|
		var collect, json, mean, std, mfcc, args;
		json = "";
		
		// header
		json = json ++ JsonUtils.wrap('defname', data.defname);
		json = json ++ JsonUtils.wrap('date', data.defname.drop(15).keep(6));
		json = json ++ JsonUtils.wrap('time', data.defname.drop(15).drop(7));
		json = json ++ JsonUtils.wrap('generation', data.generation);
		json = json ++ JsonUtils.wrap('headsize', data.headsize);
		json = json ++ JsonUtils.wrap('numgenes', data.numgenes);
		
		// data
		collect = "";
		data.code.do({|codon|
			collect = collect ++ JsonUtils.wrapQuotes(
				if (codon.isKindOf(Class)) { codon.name } { codon }
			);
			collect = collect ++ ", ";
		});
		json = json ++ JsonUtils.wrapLiteral('code', 
			JsonUtils.wrapList(JsonUtils.terminate(collect)));

		json = json ++ JsonUtils.wrap('linker', data.linker.name.asString);

		collect = "";
		data.methods.collect(_.name).do({|ugen|
			collect = collect ++ JsonUtils.wrapQuotes(ugen.asString) ++ ", ";
		});
		json = json ++ JsonUtils.wrapLiteral('methods', 
			JsonUtils.wrapList(JsonUtils.terminate(collect)));
			
		collect = "";
		data.terminals.do({|term|
			collect = collect ++ JsonUtils.wrapQuotes(term.asString) ++ ", ";
		});
		json = json ++ JsonUtils.wrapLiteral('terminals', 
			JsonUtils.wrapList(JsonUtils.terminate(collect)));
		
		// stats
		collect = "";
		#[cent, amp, flat, err].do({|stat|
			var str;
			str = JsonUtils.wrapLiteral('mean', data.stats[stat].mean); 
			str = str ++ JsonUtils.wrapLiteral('stdDev', 
				data.stats[stat].stdDev, true);
			collect = collect ++ JsonUtils.wrapLiteral(stat, JsonUtils.wrapJson(str));
		});
		mean = "";
		data.stats.mfcc.collect(_.mean).do({|mfc|
			mean = mean ++ mfc ++ ", ";
		});
		mean = JsonUtils.wrapList(JsonUtils.terminate(mean));
		std = "";
		data.stats.mfcc.collect(_.stdDev).do({|mfc|
			std = std ++ mfc ++ ", ";
		});
		std = JsonUtils.wrapList(JsonUtils.terminate(std));
		
		mfcc = JsonUtils.wrapLiteral('mean', mean) 
			++ JsonUtils.wrapLiteral('stdDev', std, true); 
		
		collect = collect ++ 
			JsonUtils.wrapLiteral('mfcc', JsonUtils.wrapJson(mfcc), true);
		json = json ++ JsonUtils.wrapLiteral('stats', JsonUtils.wrapJson(collect));
		
		// args
		
		args = "";
		collect = "";
		data.params.literals.do({|item| 
			collect = collect ++ if (item.isKindOf(Symbol)) 
			{ JsonUtils.wrapQuotes(item) }
			{ 
				if (item.isNaN.or(item.abs == inf)) { 0 } { item } 
			} ++ ", "
		});
		
		args = args ++ JsonUtils.wrapLiteral('literals', 
			JsonUtils.wrapList(JsonUtils.terminate(collect)));

		collect = "";
		data.params.code.do({|codon|
			collect = collect ++ JsonUtils.wrapQuotes(
				if (codon.isKindOf(Symbol)) { codon } { codon.name }
			);
			collect = collect ++ ", ";
		});
		args = args ++ JsonUtils.wrapLiteral('code', 
			JsonUtils.wrapList(JsonUtils.terminate(collect)));

		collect = "";
		data.params.constants.do({|cons|
			collect = collect ++ cons.asString;
			collect = collect ++ ", ";
		});
		args = args ++ JsonUtils.wrapLiteral('constants', 
			JsonUtils.wrapList(JsonUtils.terminate(collect)));
			
		collect = "";
		data.params.extraDomains.flat.do({|spec|
			collect = collect ++ JsonUtils.wrapList(
				spec.minval.asString ++ ", " ++ 
				spec.maxval.asString ++ ", " ++ 
				"\"" ++ spec.warp.asSpecifier.asString ++ "\""
			) ++ ", "
		});
		args = args ++ JsonUtils.wrapLiteral('extraDomains', 
			JsonUtils.wrapList(JsonUtils.terminate(collect)));
				
		json = json ++ JsonUtils.wrapLiteral('args', 
			JsonUtils.wrapJson(JsonUtils.terminate(args)), true);
		
		^JsonUtils.wrapJson(json)
	}
	
}

JsonUtils{

	*wrap{|key, value, last=false|
		var str;
		str = ("\"" ++ key.asString ++ "\": \"" ++ value.asString ++ "\"");
		if (last.not) { str = str ++ ", " };
		^str
	}
	
	*wrapLiteral{|key, value, last=false|
		var str = "\"" ++ key.asString ++ "\": " ++ value.asString;
		if (last.not) { str = str ++ ", " };
		^str
	}
	
	
	*wrapJson{|json|
		^("{ " ++ json ++ " }")
	}
	
	*wrapList{|list|
		^("[ " ++ list ++ " ]")
	}
	
	*wrapQuotes{|value|
		^("\"" ++ value ++ "\"")
	}
	
	*wrapSingleQuotes{|value|
		^("'" ++ value ++ "'")
	}
	
	*terminate{|str|
		^(str.keep(str.size - 2))
	}
	
}