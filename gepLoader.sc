UGepLoader{

	classvar <>synthdefdir="synthdefs/";
	classvar <>metadir = "metadata/", <>datadir="data/";

	var headsize, numgenes, <data;

	*new{|headsize, numgenes|
		^super.newCopyArgs(headsize, numgenes)
	}

	*load{|headsize, numgenes|
		^this.class.new(headsize, numgenes).load
	}

	load{
		var meta;
		meta = UGenExpressionTree.loadMetadataFromDir.select({|data|
			data.args.isKindOf(Event)
		});

		data = meta.select({|meta|
			var data, include, path;
			path = Paths.gepArchDir +/+ meta.defname.asString ++ "." ++ UGEP.fileExt;
			if (File.exists(path)) {
				data = UGEP.loadData(path);
				include = ((headsize.isNil).xor(data.header.headsize == headsize))
				.and((numgenes.isNil).xor(data.header.numgenes == numgenes));
				if (include) {
					meta.data = data;
				};
			}
			{
				include = false
			};
			include
		});

		this.syncStats;

		Post << "UGep metadata loaded." << Char.nl;

		^data

	}

	loadNames{|names|
		data = names.collect({|defname|
			var path, meta, data;
			path = Paths.gepArchDir +/+ defname.asString ++ "." ++ UGEP.fileExt;
			if (File.exists(path)) { data = UGEP.loadData(path) };
			meta = UGenExpressionTree.loadMetadata(defname);
			meta.data = data;
			meta.defname = defname.asSymbol;
			meta
		});

		this.syncStats;

		Post << "UGep metadata loaded." << Char.nl;

		^data

	}

	loadFromFile{|path|
		var file, defarray;
		file = File.open(path, "r");
		defarray = file.readAllString.split(Char.nl).reject(_.isEmpty).collect(_.asSymbol);
		file.close;
		file = nil;
		^this.loadNames(defarray)
	}

	/*
	simple date search format: yymmdd
	*/
	loadByDate{|from, to|
		var paths, dates, indices;
		paths = (Paths.gepArchDir ++ "*").pathMatch;
		dates = paths.collect({|path| path.basename.split($.).first.drop(15).keep(6) });
		indices = dates.selectIndices({|date|
			var select = false;
			if (from.notNil.and(to.isNil))
			{
				select = (date.asInt >= from.asInt)
			};
			if (from.isNil.and(to.notNil))
			{
				select = (date.asInt <= to.asInt)
			};
			if (from.notNil.and(to.notNil))
			{
				select = (date.asInt >= from.asInt).and(date.asInt <= to.asInt)
			};
			select
		});

		paths = paths[indices];

		data = paths.collect({|path|
			var gesdata, meta, defname;
			defname = path.basename.split($.).first;
			if (File.exists(path)) { data = UGEP.loadData(path) };
			meta = UGenExpressionTree.loadMetadata(defname);
			meta.data = data;
			meta.defname = defname.asSymbol;
			meta
		});

		this.syncStats;

		if (headsize.notNil) {
			data = data.select({|item| item.data.header.headsize == headsize });
		};

		if (numgenes.notNil) {
			data = data.select({|item| item.data.header.numgenes == numgenes });
		};

		Post << "UGep metadata loaded." << Char.nl;

		^data

	}

	syncStats{

		var sync = data.select({|item|
			item.stats.size > 5
		});

		sync.do({|item, i|
			var dev, means, values, newstats;
			values = item.stats.collect(_.last).flop;
			means = values.collect(_.mean);
			newstats = ();
			newstats['amp'] = ('mean': means[22], 'stdDev': values[22].stdDev);
			newstats['cent'] = ('mean': means[21], 'stdDev': values[21].stdDev);
			newstats['flat'] = ('mean': means[20], 'stdDev': values[20].stdDev);
			newstats['err'] = ('mean': 0, 'stdDev': 0);
			newstats['mfcc'] = means[0..19].collect({|mean, i| ('mean': mean, 'stdDev': values[i].stdDev) });
			item.stats = newstats
		})

	}

}

JsonLoader{

	classvar <localIP = "127.0.0.1", <localPort = 5984;
	classvar <remoteIP = "", remotePort = 0;
	classvar <viewsPath = "/Users/alo/SuperCollider/gep/json/views_00.js";
	classvar <>viewDir = "application";

	var <db;

	*new{|dbname, useLocal=true|
		^super.new.init(dbname, useLocal)
	}

	init{|dbname, useLocal|
		db = CouchDB(NetAddr(this.class.localIP, this.class.localPort), dbname, JsonLoader.viewDir)
	}

	putViewsFromFile{
		db.putViewsFromDoc(this.class.viewsPath)
	}

	getIDsByDate{|date|
		var str, ids, defkey;
		str = db.get("headerByDate?key=\"#\"".replace("#", date));
		str = str.subStr((str.find("\"rows\":")+10), str.size-6) ++ "  ";
		ids = str.split(Char.nl).collect({|id|
			id.replace("{", "(").replace("}", ")").replace("\"", "'")
				.keep(id.size-2).interpret
		});
		^ids
	}

	getIDsByDateRange{|from="000000", to="999999"|
		var str, ids, defkey;
		str = db.get("headerByDate?'startkey=\"#\"&endkey=\"$\"'"
			.replace("#", from).replace("$", to)
		);
		str = str.subStr((str.find("\"rows\":")+10), str.size-6) ++ "  ";
		ids = str.split(Char.nl).collect({|id|
			id.replace("{", "(").replace("}", ")").replace("\"", "'")
				.keep(id.size-2).interpret
		});
		^ids
	}

	getPlayerDataByDefName{|defname|
		var rsp, array, data;
		rsp = db.get("playerDataByDefName?key=\"#\"".replace("#", defname));
		array = rsp.subStr((rsp.find("\"value\":")+8), rsp.size - 7)
			.replace("\"", "'").replace("\n", "").interpret;
		data = ();
		data.defname = defname;
		data.args = array[0];
		data.headsize = array[1].asInteger;
		data.numgenes = array[2].asInteger;
		data.code = array[3].collect({|it|
			if (it.asString.size == 1) { it.asSymbol } { it.asString.interpret }
		});
		data.terminals = array[4];
		data.linker = AbstractFunction.methods.select({|method|
			method.name == array[5]
		}).first;
		^data
	}

	getDefNamesByHeader{|headsize, numgenes|
		var str, ids, defkey;
		if (headsize.isNil.and(numgenes).isNil) {
			str = db.get("defnamesByHeader")
		}
		{
			str = db.get("defnamesByHeader?key=\"#\"".replace("#", headsize.asString ++ numgenes.asString));
		};
		str = str.subStr((str.find("\"rows\":")+10), str.size-6) ++ "  ";
		ids = str.split(Char.nl).collect({|id|
			id.replace("{", "(").replace("}", ")").replace("\"", "'")
				.keep(id.size-2).interpret
		});
		^ids
	}

	getDocumentByDefName{|defname|
		var result, doc, data, argchrom, operators;
		operators = ['*', '/', '+', '-'];
		result = db.get("docByDefName?key=\"#\"".replace("#", defname));
		doc = result.subStr((result.find("\"value\":") + 8), result.size - 7)
			.replace("{", "(").replace("}", ")").replace("\n", "").replace("\"", "'")
			.interpret;
		data = ();
		data.id = doc['_id'];
		data.defname = doc.defname;
		data.date = doc.date;
		data.time = doc.time;
		data.generation = doc.generation;
		data.headsize = doc.headsize;
		data.numgenes = doc.numgenes;
		data.code = doc.code.collect({|it|
			if (it.asString.size == 1) { it.asSymbol } { it.asString.interpret }
		});
		data.linker = AbstractFunction.methods.select({|method|
			method.name == doc.linker
		}).first;
		data.methods = doc.methods.collect(_.asString).collect(_.interpret);
		data.terminals = doc.terminals;
		data.stats = doc.stats;
		data.args = doc.args.literals;
		data.params = ();
		data.params.literals = doc.args.literals;
		data.params.code = doc.args.code.collect({|it|
			if (operators.includes(it)) {
				AbstractFunction.methods.select({|method| method.name.asSymbol == it }).first
			} { it.asSymbol }
		});
		data.params.constants = doc.args.constants;
		data.params.extraDomains = doc.args.extraDomains.collect({|domain|
			ControlSpec(*domain)
		});
		^data
	}

}