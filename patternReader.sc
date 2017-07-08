PatternReader{

	classvar <dbname = "rhythm_patterns";
	classvar <viewdir = "views";
	classvar <>view = "unraveled_by_name";

	var dbname, db, allPatterns;

	*new{|dbname|
		^super.newCopyArgs(dbname).init
	}

	init{
		if (dbname.isNil) { dbname = this.class.dbname };
		db = CouchDB(NetAddr(CouchDB.ip, CouchDB.port), dbname, this.class.viewdir);
	}

	load{|patternName|
		var response, pattern;
		if (patternName.isNil) {
			^this.loadAll
		}
		{
			response = db.getParsed(this.class.view, patternName);
			pattern = (response["rows"][0]["key"]: response["rows"][0]["value"].collect({|pat|
				pat.collect(_.asInt)
			}));
			^pattern
		}
	}

	loadAll{
		var response;
		response = db.getParsed(this.class.view);
		allPatterns = ();
		response["rows"].do({|row|
			allPatterns[row["key"].asSymbol] = row["value"].collect({|pat|
				pat.collect(_.asInt)
			});
		})
		^allPatterns
	}

}