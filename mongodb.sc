MongoDb{

	var collection, dbname, dataSource;
	var <curlCmd;
	var <url = "https://data.mongodb-api.com/app/data-zolcv/endpoint/data/v1";
	var <contentHeader = "--header 'Content-Type: application/json' ";
	var <accessControlHeader = "--header 'Access-Control-Request-Headers: *' ";
	var <apiKeyHeader = "--header 'api-key: 629b75295ca69874e5e18f3d' ";
	var <dataTemplate = "--data-raw '%'";
	var <queries;

	*new{|collection="ges_ld_00", dbname="ges", dataSource="gep"|
		^super.newCopyArgs(collection, dbname, dataSource).init
	}

	init{
		curlCmd = "curl --location --request POST 'https://data.mongodb-api.com/app/data-zolcv/endpoint/data/v1/action/find' " ++
		"--header 'Content-Type: application/json' " ++
		"--header 'Access-Control-Request-Headers: *' " ++
		"--header 'api-key: 629b75295ca69874e5e18f3d' ";
	}

	actionUrl{|action|
		^(this.url +/+ "action" +/+ action)
	}

	getQuery{|params, projection, limit|
		var query = ("collection": collection, "database": dbname, "dataSource": dataSource);
		query["filter"] = params;
		if (projection.notNil) {
			query["projection"] = projection
		};
		if (limit.notNil) {
			query["limit"] = limit.asInteger
		}
		^query;
	}

	getDefnameQuery{|defname|
		var query = this.getQuery(("ges:defname": defname));
		^(this.makeCommand("findOne") ++ this.dataTemplate.format(query.asJSON))
	}

	getDefnamesQuery{|start, end, headsize, numgenes, numterminals|
		var query, projection, params;
		params = ();
		if (start.notNil.and(end.notNil)) {
			params["ges:date"] = ("$gte": start, "$lte": end);
		};
		if (headsize.notNil) {
			params["ges:environment.ges:headsize"] = headsize
		};
		if (numgenes.notNil) {
			params["ges:environment.ges:numgenes"] = numgenes
		};
		if (numterminals.notNil) {
			params["ges:environment.ges:terminals"] = ("$size": numterminals)
		};
		projection = ("ges:defname": 1);
		query = this.getQuery(params, projection);
		^(this.makeCommand("find") ++ this.dataTemplate.format(query.asJSON))
	}

	getDateQuery{|start, end, limit|
		var query = this.getQuery(
			("ges:date": ("$gte": start, "$lte": end)),
			limit: limit
		);

		^(this.makeCommand("find") ++ this.dataTemplate.format(query.asJSON))
	}

	makeCommand{|action|
		var cmd = this.curlCmd.format(this.actionUrl(action));
		cmd = cmd ++ this.contentHeader ++ this.accessControlHeader;
		cmd = cmd ++ this.apiKeyHeader;
		^cmd
	}

	getDocumentByDefname{|defname|
		^this.run(this.getDefnameQuery(defname))
	}

	getDefnames{|start, end, headsize, numgenes, numterminals|
		var query = this.getDefnamesQuery(start, end, headsize, numgenes, numterminals);
		^this.run(query)
	}

	getDocumentsByDate{|start="000000", end="999999", limit=10|
		var response, dict;
		var query = this.getDateQuery(start, end, limit);
		response = query.unixCmdActions;
		dict = response.replace(Char.nl.asString).parseJSON;
		^dict["documents"]
	}

	run{|query|
		var response, dict;
		query.postln;
		response = query.unixCmdGetStdOut;
		dict = response.replace(Char.nl.asString).parseJSON;
		^dict["documents"]
	}

	getDocuments{|start, end, numgenes, headsize, limit|
		var re, query, cmd, docs;
		query = ("collection": "ges_ld_00", "database": "gep", "dataSource": "gep");
		if (start.notNil) {
			query["filter"] = ("ges:date": ("$gte": start, "$lte": end));
		}
		{
			query["filter"] = ("ges:environment.ges:headsize": headsize, "ges:environment.ges:numgenes": numgenes)
		};
		query["limit"] = limit;
		cmd = curlCmd ++ "--data-raw '%'".format(query.asJSON);
		re = cmd.unixCmdGetStdOut;
		docs = re.replace(Char.nl.asString).parseJSON["documents"];
		^docs.collect({|doc| this.unpack(doc) })
	}

	unpack{|doc|
		var data, operators;
		operators = ['*', '/', '+', '-'];
		data = ();
		data.id = doc["_id"];
		data.defname = doc["ges:defname"];
		data.date = doc["ges:date"];
		data.time = doc["ges:time"];
		data.generation = doc["ges:generation"].asInteger;
		data.headsize = doc["ges:environment"]["ges:headsize"].asInteger;
		data.numgenes = doc["ges:environment"]["ges:numgenes"].asInteger;
		data.code = doc["ges:genome"].collect({|it|
			if (it.asString.size == 1) { it.asSymbol } { it.asString.interpret }
		});
		data.linker = AbstractFunction.methods.select({|method|
			method.name == doc["ges:environment"]["ges:linker"]["ges:name"].asSymbol
		}).first;
		data.methods = doc["ges:environment"]["ges:methods"].collect(_.asString).collect(_.interpret);
		data.terminals = doc["ges:environment"]["ges:terminals"].collect(_.asSymbol);
		data.stats = doc["ges:features"];
		data.args = doc["ges:parameters"]["ges:literals"].collect({|it|
			if (it.size == 1) { it.asSymbol } { it.asFloat }
		});
		data.params = ();
		data.params.literals = doc["ges:parameters"]["ges:literals"].collect({|it|
			if (it.size == 1) { it.asSymbol } { it.asFloat }
		});
		data.params.code = doc["ges:parameters"]["ges:genome"].collect({|it|
			if (operators.includes(it.asSymbol)) {
				AbstractFunction.methods.select({|method| method.name.asSymbol == it.asSymbol }).first
			} { it.asSymbol }
		});
		data.params.constants = doc["ges:parameters"]["ges:constants"].collect(_.asFloat);
		data.params.extraDomains = doc["ges:parameters"]["ges:extra_domains"].collect({|ev|
			ControlSpec(ev["ges:minimum_value"].asInteger, ev["ges:maximum_value"].asInteger, ev["ges:warp"].asSymbol)
		});
		^data
	}

}
