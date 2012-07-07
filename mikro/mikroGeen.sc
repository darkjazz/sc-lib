MikroGeen{
	
	var analyzer, metadata, datasize = 20;
	
	*new{|analyzer, defdir|
		^super.newCopyArgs(analyzer).init(defdir)
	}
	
	init{|defdir|
		Server.default.loadDirectory(defdir ? UGenExpressionTree.defDir);
		metadata = UGenExpressionTree.loadMetadataFromDir.select({|data|
			data.stats.mfcc.size == datasize && data.stats.mfcc.collect(_.mean).sum.isNaN.not
		});

	}	
}