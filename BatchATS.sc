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
			atscmd.unixCmd({
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
		if (path.notNil)
		{
			Post << "Processing " << path << "..." << Char.nl;
			
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

FixLeakDC{
	var paths, server, replace, pathstream;
	
	*new{|paths, server, replace=false|
		^super.newCopyArgs(paths, server, replace).init
	}
	
	init{
		if (paths.isKindOf(String)) { paths = Array.with(paths) };
		pathstream = Pseq(paths, 1).asStream;		
		server = server ? Server.default;
	}
	
	process{
		var path, buffer, copy, copypath;
		
		path = pathstream.next;
		
		if (path.notNil) {
			Post << "Processing " << path << "..." << Char.nl;
			
			fork({
				buffer = Buffer.read(server, path);
				server.sync;
				copy = Buffer.alloc(server, buffer.numFrames, buffer.numChannels);
				server.sync;
				SynthDef(\leakdc, {
					RecordBuf.ar(
						LeakDC.ar(PlayBuf.ar(buffer.numChannels, buffer, doneAction: 2)),
						copy, 
						loop:0 
					);
				}).play;
				(buffer.duration+0.1).wait;
				copypath = path.dirname ++ "/" ++ path.basename.split($.).first ++ 
					"_dc." ++ path.basename.split($.).last;
				copy.write(copypath);
				server.sync;
				buffer.free;
				copy.free;
				if (replace) {
					("mv" + path + "~/.Trash/ && mv" + copypath + path).unixCmd;
				};
				server.sync;
				this.process;
			})
		}
		{
			"Batch process completed..".postln		
		}
	}
}