MikroBatchAnalyzer{

	var <paths, libpath, ncoef, bus, buffer, analyzer, synth, pathstream;

	var <decodepath = "/Users/alo/snd/decode/";

	*new{|paths, libpath, ncoef=20|
		^super.newCopyArgs(paths, libpath, ncoef).init
	}

	init{

		MikroData.savePath = libpath ? MikroData.savePath;

		bus = Bus.audio(Server.default);

		SynthDef(\input, {|out, buf|
			var sig;
			sig = PlayBuf.ar(1, buf, doneAction: 2);
			Out.ar(out, sig)
		}).add;

		SynthDef(\input2, {|out, buf|
			var sig;
			sig = Mix(PlayBuf.ar(2, buf, doneAction: 2));
			Out.ar(out, sig)
		}).add;

		pathstream = Pseq(paths).asStream;
	}

	process{
		var origPath, tempPath, split;
		origPath = pathstream.next;
		Post << "Processing " << origPath << Char.nl;
		if (origPath.notNil)
		{
			split = origPath.basename.split($.);
			if (split.last == "mp3")
			{
				this.convertMP3(origPath)						}
			{
				this.convertM4A(origPath)
			};
		}
		{
			Post << "Batch process completed." << Char.nl;
		}
	}

	analyze{|path|
		Routine({
			buffer = Buffer.read(Server.default, path);
			Server.default.sync;
			analyzer = MikroAnalyzer(buffer.duration, ncoef, false, false);
			Server.default.sync;
			synth = Synth(\input2, [\out, bus, \buf, buffer]);
			Server.default.sync;
			analyzer.start(bus, synth, \addAfter, -12.dbamp, 0.06, 30);
			OSCFunc({|msg|
			 	analyzer.free;
			 	MikroData.saveEvents(analyzer);
			 	Post << "Finished analyzing " << path << Char.nl;
			 	buffer.free;
			 	analyzer = nil;
			 	("rm" + path).unixCmd;
			 	this.process;
			}, '/n_end', Server.default.addr).oneShot;
		}).play
	}

	convertMP3{|path|
		var tmpPath;
		tmpPath = decodepath ++ (path.basename.split($.)[0]) ++ ".wav";
		if ((MP3.lamepath + "--decode" + "\"" ++ path ++ "\"" + tmpPath).systemCmd == 0)
		{
			// this.analyze(tmpPath);
			Post << tmpPath << Char.nl;
		}
		{
			("MP3: unable to read file:" + path).warn;
			("rm" + tmpPath).unixCmd;
		};
	}

	convertM4A{|path|
		var converter, inform, tmpPath;
		tmpPath = decodepath ++ (path.basename.split($.)[0]) ++ ".aif";
		converter = AFConvert(path, tmpPath);
		converter.convert({
			this.analyze(tmpPath);
		})
	}

}