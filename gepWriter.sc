JsonWriter{

	const <contextURI = "http://geen.tehis.net/ontology/";
	classvar <>contextID = "dea880ff0fcfcde78086408cce005333";
	classvar <viewsPath = "/Users/alo/dev/gep/json/views_02.js";

	var <db, <envirID;

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

	putViewsFromFile{
		db.putViewsFromDoc(this.class.viewsPath)
	}

	writeContext{
		var ev, res;
		res = db.put(JsonUtils.wrapSingleQuotes(this.buildContext));
		res = res.replace("{", "(").replace("}", ")").replace($".asString, "'");
		ev = res.interpret;
		if (ev.includesKey('id')) {
			JsonWriter.contextID = ev['id'];
		}
	}

	writeEnvir{|data|
		var ev, res;
		res = db.put(JsonUtils.wrapSingleQuotes(this.buildEnvir(data)));
		res = res.replace("{", "(").replace("}", ")").replace($".asString, "'");
		ev = res.interpret;
		if (ev.includesKey('id')) {
			envirID = ev['id'];
		}
	}

	writeSynthDef{|data, guid|
		if (JsonWriter.contextID.isNil) { this.writeContext };
		//if (envirID.isNil) { this.writeEnvir(data) };
		db.put(JsonUtils.wrapSingleQuotes(this.build(data)), guid)
	}

	addRDFContext{
		^JsonUtils.wrapLiteral('@context', JsonUtils.wrapJson(
				JsonUtils.wrap('ges', JsonWriter.contextURI, true);
			)
		);
	}

	buildContext{
		var json, sysinfo;
		json = this.addRDFContext;
		json = json ++ JsonUtils.wrap('@type', "ges:Context");
		sysinfo = JsonUtils.wrap('@type', "ges:System");
		if (thisProcess.platform.name == 'linux')
		{
			sysinfo = sysinfo ++ JsonUtils.wrap('ges:type', "ges:sysLinux");
			sysinfo = sysinfo ++ JsonUtils.wrap('ges:name', "Arch Linux", true);
		}
		{
			sysinfo = sysinfo ++ JsonUtils.wrap('ges:type', "ges:sysMac");
			sysinfo = sysinfo ++ JsonUtils.wrap('ges:name', "Mac OS X");
			sysinfo = sysinfo ++ JsonUtils.wrap('ges:version', "10.10.4", true);
		};
		json = json ++ JsonUtils.wrapLiteral('ges:system', JsonUtils.wrapJson(sysinfo), true);
		^JsonUtils.wrapJson(json);
	}

	buildEnvir{|data|
		var collect, json;
		json = this.addRDFContext;
		json = json ++ JsonUtils.wrap('@type', "ges:Environment");
		json = json ++ JsonUtils.wrapLiteral('ges:headsize', data.headsize);
		json = json ++ JsonUtils.wrapLiteral('ges:numgenes', data.numgenes);

		json = json ++ JsonUtils.wrapLiteral('ges:linker', JsonUtils.wrapJson(
			JsonUtils.wrap('@type', "ges:Function") ++
			JsonUtils.wrap('ges:name', data.linker.name.asString) ++
			JsonUtils.wrap('ges:class', data.linker.ownerClass.name.asString, true)
		));

		json = json ++ JsonUtils.wrap('ges:context', JsonWriter.contextID);

		collect = "";
		data.methods.collect(_.name).do({|ugen|
			collect = collect ++ JsonUtils.wrapQuotes(ugen.asString) ++ ", ";
		});
		json = json ++ JsonUtils.wrapLiteral('ges:methods',
			JsonUtils.wrapList(JsonUtils.terminate(collect)));
		"Methods collected".postln;
		collect = "";
		data.terminals.do({|term|
			collect = collect ++ JsonUtils.wrapQuotes(term.asString) ++ ", ";
		});
		json = json ++ JsonUtils.wrapLiteral('ges:terminals',
			JsonUtils.wrapList(JsonUtils.terminate(collect)), true);

		^JsonUtils.wrapJson(json)

	}

	build{|data|
		var collect, json, mean, std, mfcc, args, map;
		json = this.addRDFContext;

		// header
		data.defname = data.defname.asString;
		json = json ++ JsonUtils.wrap('@type', "ges:Synth");
		json = json ++ JsonUtils.wrapLiteral('ges:environment', this.buildEnvir(data));
		json = json ++ JsonUtils.wrap('ges:defname', data.defname);
		json = json ++ JsonUtils.wrap('ges:date', data.defname.drop(15).keep(6));
		json = json ++ JsonUtils.wrap('ges:time', data.defname.drop(15).drop(7));
		json = json ++ JsonUtils.wrap('ges:generation', data.generation);

		// data
		collect = "";
		data.code.do({|codon|
			collect = collect ++ JsonUtils.wrapQuotes(
				if (codon.isKindOf(Class)) { codon.name } { codon }
			);
			collect = collect ++ ", ";
		});
		json = json ++ JsonUtils.wrapLiteral('ges:genome',
			JsonUtils.wrapList(JsonUtils.terminate(collect)));

		// features
		map = (cent: 'ges:centroid', flat: 'ges:flatness', amp: 'ges:amplitude', err:'ges:error');
		collect = "";
		#[cent, amp, flat, err].do({|stat|
			var str;
			str = JsonUtils.wrapLiteral('ges:mean', data.stats[stat].mean);
			str = str ++ JsonUtils.wrapLiteral('ges:std_dev',
				data.stats[stat].stdDev, true);
			collect = collect ++ JsonUtils.wrapLiteral(map[stat], JsonUtils.wrapJson(str));
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

		mfcc = JsonUtils.wrapLiteral('ges:mean', mean)
			++ JsonUtils.wrapLiteral('ges:std_dev', std, true);

		collect = collect ++
			JsonUtils.wrapLiteral('ges:mfcc', JsonUtils.wrapJson(mfcc), true);
		json = json ++ JsonUtils.wrapLiteral('ges:features', JsonUtils.wrapJson(collect));

		// params
		args = "";
		collect = "";
		data.params.literals.do({|item|
			collect = collect ++ if (item.isKindOf(Symbol))
			{ JsonUtils.wrapQuotes(item) }
			{
				if (item.isNaN.or(item.abs == inf)) { 0 } { item }
			} ++ ", "
		});

		args = args ++ JsonUtils.wrap('@type', "ges:Parameters");
		args = args ++ JsonUtils.wrapLiteral('ges:literals',
			JsonUtils.wrapList(JsonUtils.terminate(collect)));

		collect = "";
		data.params.code.do({|codon|
			collect = collect ++ JsonUtils.wrapQuotes(
				if (codon.isKindOf(Symbol)) { codon } { codon.name }
			);
			collect = collect ++ ", ";
		});
		args = args ++ JsonUtils.wrapLiteral('ges:genome',
			JsonUtils.wrapList(JsonUtils.terminate(collect)));

		collect = "";
		data.params.constants.do({|cons|
			collect = collect ++ cons.asString;
			collect = collect ++ ", ";
		});
		args = args ++ JsonUtils.wrapLiteral('ges:constants',
			JsonUtils.wrapList(JsonUtils.terminate(collect)));

		collect = "";
		data.params.extraDomains.flat.do({|spec|
			collect = collect ++ JsonUtils.wrapJson(
				JsonUtils.wrap('@type', "ges:ControlSpec") ++
				JsonUtils.wrapLiteral('ges:minimum_value', spec.minval) ++
				JsonUtils.wrapLiteral('ges:maximum_value', spec.maxval) ++
				JsonUtils.wrap('ges:warp', spec.warp.asSpecifier.asString, true)
			) ++ ", "
		});
		args = args ++ JsonUtils.wrapLiteral('ges:extra_domains',
			JsonUtils.wrapList(JsonUtils.terminate(collect)));

		json = json ++ JsonUtils.wrapLiteral('ges:parameters',
			JsonUtils.wrapJson(JsonUtils.terminate(args)), true);

		json.postcs;

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