PatternReader{

	classvar <dbname = "rhythm_patterns";
	classvar <viewdir = "views";
	classvar <>view = "unraveled_by_name";
	classvar <>dnbview = "dnb_by_name";
	classvar <>dubview = "dubstep_patterns";

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
				pat.collect(_.asInteger)
			}));
			^pattern
		}
	}

	loadDnB{
		var response;
		response = db.getParsed(this.class.dnbview);
		allPatterns = ();
		response["rows"].do({|row|
			allPatterns[row["key"].asSymbol] = row["value"].collect({|pat|
				pat.collect(_.asFloat)
			});
		})
		^allPatterns
	}

	loadDub{
		var response;
		response = db.getParsed(this.class.dubview);
		allPatterns = ();
		response["rows"].do({|row|
			allPatterns[row["key"].asSymbol] = row["value"].collect({|pat|
				pat.collect(_.asFloat)
			});
		})
		^allPatterns
	}

	loadAll{
		var response;
		response = db.getParsed(this.class.view);
		allPatterns = ();
		response["rows"].do({|row|
			allPatterns[row["key"].asSymbol] = row["value"].collect({|pat|
				pat.collect(_.asInteger)
			});
		})
		^allPatterns
	}

}