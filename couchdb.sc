CouchDB {

	const <pwdfile = "/usr/local/etc/pwd.lock";

	var <>netAddr, <>db;
	var cmd = "curl", getcmd = "-X GET", putcmd = "-X PUT", prefix="http://";

	*new{|addr, db| ^super.newCopyArgs(addr, db) }

	*startServer{
		("echo " ++ SpEnvir.pwdfile.load ++ " | sudo -S systemctl start couchdb.service").unixCmd;
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
		Post << "Attempting to write to couchdb: " << guid << Char.nl;
		res = put.unixCmdGetStdOut;
		Post << "CouchDB reponse: " << res << Char.nl;
	}

	get{|view|
		var get;
		get = this.getCmd +/+ db +/+ "_design/application/_view"
			+/+ this.encodeURI(view);
		Post << get << Char.nl;
		^get.unixCmdGetStdOut
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
		this.put("'" + viewstr + "'", "_design/application");
	}

}