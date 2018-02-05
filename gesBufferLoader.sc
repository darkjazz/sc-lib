GESBufferLoader{
	var <paths, <buffers;
	const <gesBufferPath = "~/snd/evolver/";
	const numChannels = 1;

	*new{|paths|
		^super.newCopyArgs(paths).preload;
	}

	loadByDate{|start="000000", end="999999"|
		paths = (GESBufferLoader.gesBufferPath ++ "*").pathMatch.select({|path|
			var date, arr = path.basename.split($_);
			if (arr.size > 2) {
				date = arr[3].asInt
			}
			{
				date = -1
			};
			(date >= start).and(date <= end)
		});
		this.preload;
	}

	size{ ^buffers.size }

	at{|index|
		if (buffers[index].isNil) {
			if (GESBufferLoader.numChannels == 1) {
				buffers[index] = Buffer.readChannel(Server.default, paths[index], channels: [0])
			}
			{
				buffers[index] = Buffer.read(Server.default, paths[index])
			}
		}
		^buffers[index]
	}

	preload{
		buffers = Array.newClear(paths.size);
		paths.do({|path, i|
			buffers[i] = Buffer.readChannel(Server.default, path, channels: [0])
		})
	}

}