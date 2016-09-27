JsonLDLoader : JsonLoader {
	var <results;

	loadDocumentsByDefNames{|defnamearray, doneAction|
		var current, namestream;
		results = ();
		namestream = Pseq(defnamearray, 1).asStream;
		current = namestream.next;
		results[current] = this.getDocumentByDefName(current.asString);
		SystemClock.sched(0.05, {
			if (results.includesKey(current)) {
				current = namestream.next;
				if (current.notNil) { results[current] = this.getDocumentByDefName(current.asString) };
			};
			if (current.notNil) { 0.05 } { doneAction.(); nil }
		})
	}

	getDocumentByDefName{|defname|
		var result, doc;
		result = db.get("docByDefName?key=\"#\"".replace("#", defname));
		doc = result.subStr((result.find("\"value\":") + 8), result.size - 7)
			.replace(${.asString, $(.asString).replace($}.asString, $).asString).replace("\n", "").replace("\"", "'")
			.interpret;
		^this.unpackData(doc)
	}

	getDocumentsByHeader{|headsize, numgenes|
		var result, rows;
		result = db.get("docByHeader?key=\"#\"".replace("#", headsize.asString ++ numgenes.asString));
		result = result.replace("{", "(").replace("}", ")").replace("\"", "'").replace("\n", "");
		result = result.interpret;
		rows = result['rows'].collect({|row| row['value'] });
		^rows.collect({|doc| this.unpackData(doc) })
	}

	unpackData{|doc|
		var data, operators;
		operators = ['*', '/', '+', '-'];
		data = ();
		data.id = doc['_id'];
		data.defname = doc['ges:defname'];
		data.date = doc['ges:date'];
		data.time = doc['ges:time'];
		data.generation = doc['ges:generation'].asInteger;
		data.headsize = doc['ges:environment']['ges:headsize'].asInteger;
		data.numgenes = doc['ges:environment']['ges:numgenes'].asInteger;
		data.code = doc['ges:genome'].collect({|it|
			if (it.asString.size == 1) { it.asSymbol } { it.asString.interpret }
		});
		data.linker = AbstractFunction.methods.select({|method|
			method.name == doc['ges:environment']['ges:linker']['ges:name']
		}).first;
		data.methods = doc['ges:environment']['ges:methods'].collect(_.asString).collect(_.interpret);
		data.terminals = doc['ges:environment']['ges:terminals'];
		data.stats = doc['ges:features'];
		data.args = doc['ges:parameters']['ges:literals'];
		data.params = ();
		data.params.literals = doc['ges:parameters']['ges:literals'];
		data.params.code = doc['ges:parameters']['ges:genome'].collect({|it|
			if (operators.includes(it)) {
				AbstractFunction.methods.select({|method| method.name.asSymbol == it }).first
			} { it.asSymbol }
		});
		data.params.constants = doc['ges:parameters']['ges:constants'];
		data.params.extraDomains = doc['ges:parameters']['ges:extra_domains'].collect({|ev|
			ControlSpec(ev['ges:minimum_value'], ev['ges:maximum_value'], ev['ges:warp'])
		});
		^data
	}

	getAllValidFeatures{
		var result;
		result = db.get("allValidFeatures");
	}

}