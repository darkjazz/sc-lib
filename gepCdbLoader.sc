CdbLoader{
		
	classvar <defaultAddr;
	classvar <defaultName = "ges_00";
	classvar <views;
	
	var <dbname, <addr, <db;
		
	*new{|dbname, addr|
		^super.newCopyArgs(dbname, addr).init
	}
	
	init{
		if (addr.isNil) {
			addr = this.class.defaultAddr
		};
		
		if (dbname.isNil) {
			dbname = this.class.defaultName
		};
		
		db = CouchDB(addr, dbname)
	}
	
	// expects: headsize, numgenes
	// returns: defname, args.literals, code, terminals, numgenes, headsize, linker
	getDataByHeader{|headsize, numgenes|
		
	}
	
	// expects: headsize, numgenes, datefrom, dateto
	// returns: defname, args.literals, code, terminals, numgenes, headsize, linker
	getDataByHeaderAndDate{|headsize, numgenes, from, to|
		
	}
	
	*initClass{
		defaultName = NetAddr("127.0.0.1", 5984);
		views = ();
	}
	
}