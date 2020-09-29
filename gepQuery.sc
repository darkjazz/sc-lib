GESQuery {

	classvar <>db_name = "ges-synth-dict";
	classvar <>designdoc = "views";
	classvar <>view = "names_by_tag";

	var <>db;

	*new{
		^super.new.init()
	}

	init{
		this.db = CouchDB(NetAddr(CouchDB.ip, CouchDB.port), this.class.db_name, this.class.designdoc);
	}

	search{|term|
		var all_names, return;
		all_names = term.split(Char.space).collect({|word|
			var result = this.db.getParsed(this.class.view, word);
			if (result["rows"].size == 1) {
				return = result["rows"].first["value"]
			}
		}).select(_.notNil);
		all_names.postln;
		all_names.do({|names| return = return.asSet & names.asSet });
		^return.asArray
	}

}