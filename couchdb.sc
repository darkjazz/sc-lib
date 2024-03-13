CouchDB {

	classvar <ip = "127.0.0.1";
	classvar <port = 5984;

	const <pwdfile = "/usr/local/etc/pwd.lock";

	var <>netAddr, <>db, <>viewdir;
	var cmd = "curl", getcmd = "-X GET", putcmd = "-X PUT", prefix="http://";

	*new{|addr, db, viewdir="application"| ^super.newCopyArgs(addr, db, viewdir) }

	*startServer{
		("echo " ++ CouchDB.pwdfile.load ++ " | sudo -S systemctl start couchdb.service").unixCmd;
	}

	addrString{ ^(prefix ++ netAddr.hostname ++ ":" ++ netAddr.port.asString) }

	request{ ^(cmd + this.addrString) }

	getCmd{ ^(cmd + getcmd + this.addrString) }

	putCmd{ ^(cmd + putcmd + this.addrString) }

	info{ ^this.request.unixCmdGetStdOut }

	allDbs{ ^(this.getCmd +/+ "_all_dbs").unixCmdGetStdOut }

	requestGuid{
		var res, guid;
		res = (this.request +/+ "_uuids").unixCmdGetStdOut;
		guid = res.split($")[3];
//		Post << "Retrieved GUID: " << guid << Char.nl;
		^guid
	}

	put{|json, guid|
		var put, res;
		if (guid.isNil) { guid = this.requestGuid };
		put = this.putCmd +/+ db +/+ guid + " -d " + json;
		Post << "Attempting to write to CouchDB database " << db << ": "<< guid << Char.nl;
		res = put.unixCmdGetStdOut;
		Post << "CouchDB reponse: " << res << Char.nl;
		^res
	}

	get{|view|
		var get;
		get = this.getCmd +/+ db +/+ "_design" +/+ viewdir +/+ "_view"
			+/+ this.encodeURI(view);
		Post << get << Char.nl;
		^get.unixCmdGetStdOut
	}

	getParsed{|view, key="", endKey=""|
		var query = view;
		if (endKey.isEmpty.not.and(key.isEmpty.not)) {
			query = query ++ "?startKey=\"%\"&endKey=\"%\"".format(key, endKey);
		};
		if (key.isEmpty.not.and(endKey.isEmpty)) {
			query = query ++ "?key=\"%\"".format(key);
		};
		^this.get(query).parseJSON

	}

	view{|name, designdoc, key, startKey, endKey|
		var query;
		this.viewdir = designdoc;
		if ((endKey.notNil).and(startKey.notNil)) {
			query = "?startKey=\"%\"&endKey=\"%\"".format(startKey, endKey);
		};
		if ((endKey.isNil).and(startKey.notNil)) {
			query = "?startKey=\"%\"".format(startKey);
		};
		if ((endKey.notNil).and(startKey.isNil)) {
			query = "?endKey=\"%\"".format(endKey);
		};
		if (key.notNil) {
			query = "?key=\"%\"".format(key);
		};
		^this.get(name ++ query).parseJSON
	}

	encodeURI{|request|
		var dict, ret;
		dict = ("\"": "%22");
		dict.keys(Array).do({|term|
			request = request.replace(term, dict[term])
		});
		^request
	}

	putViewsFromDoc{|path|
		var viewstr, file;
		file = File(path, "r");
		viewstr = file.readAllString;
		file.close;
		viewstr = viewstr.replace("\n", "").replace("\t", "");
		this.put("'" + viewstr + "'", "_design/" ++ viewdir);
	}

}