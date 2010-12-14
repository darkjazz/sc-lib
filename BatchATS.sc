FileToAts{
	var paths, options, pathstream;
	
	*new{|paths, options|
		^super.newCopyArgs(paths, options).init
	}
	
	init{
		if (paths.isKindOf(String)) { paths = [paths] };
		pathstream = Pseq(paths, 1).asStream;
	}
	
	process{
		var atscmd, path;
		path = pathstream.next;
		if (path.notNil)
		{
			atscmd = BufferToAts.atsapath + path + 
				(path.dirname ++ "/" ++ path.basename.split($.)[0] ++ ".ats");
			atscmd.unixCmdThen({
				("'" + atscmd + "' executed...proceeding to next file").postln;
				this.process;
			});
		}
		{
			"Batch process completed..".postln
		}
	}
	
}

StereoToMono{
	var paths, server, channel, pathstream;
	
	*new{|paths, server, channel = 0|
		^super.newCopyArgs(paths, server, channel).init
	}
	
	init{
		if (paths.isKindOf(String)) { paths = Array.with(paths) };
		pathstream = Pseq(paths, 1).asStream;		
	}
	
	process{
		var path, buffer;
		path = pathstream.next;
		path.postln;
		if (path.notNil)
		{
			fork({
				buffer = Buffer.readChannel(server, path, channels:[channel]);
				server.sync;
				buffer.write(path.dirname ++ "/mono" ++ path.basename.split($.)[0] ++ channel ++ ".aif");
				server.sync;
				this.process
			})
		}
		{
			"Batch process completed..".postln
		}
	}
}