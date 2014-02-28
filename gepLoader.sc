UGepLoader{
	
	classvar <>path = "/Users/alo/Data/gep/", <>synthdefdir="synthdefs/";
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
			path = UGEP.archDir +/+ meta.defname.asString ++ "." ++ UGEP.fileExt;
			if (File.exists(path)) { 
				data = UGEP.loadData(path) 
			};
			include = ((headsize.isNil).xor(data.header.headsize == headsize))
				.and((numgenes.isNil).xor(data.header.numgenes == numgenes));
			if (include) {  
				meta.data = data;
			};
			include
		});
		
		^data

	}
	
}