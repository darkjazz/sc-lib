GESBufferLoader{
	var <paths, <buffers, <loops;
	const <gesBufferPath = "/Users/kurivari/snd/evolver/";
	const numChannels = 1;

	*new{|paths|
		^super.newCopyArgs(paths).preload;
	}

	loadByDate{|start="000000", end="999999"|
		paths = (GESBufferLoader.gesBufferPath ++ "*").pathMatch.select({|path|
			var date, arr = path.basename.split($_);
			if (arr.size > 2) {
				date = arr[3].asInteger
			}
			{
				date = -1
			};
			(date >= start).and(date <= end)
		});
		this.preload;
	}

	loadFromFile{|path|
		var pathdict = path.load;
		paths = pathdict.values.collect({|fn| GESBufferLoader.gesBufferPath +/+ fn });
		this.preload;
	}

	loadLoops{|path, loopath|
		var pathdict;
		pathdict = path.load;
		loops = ();
		loopath.load.do({|arr|
			loops[arr[0]] = ('start': arr[1], 'nframes': arr[2] - arr[1], 'bpm': arr[3])
		});
		if (pathdict.isKindOf(Event)) {
			paths = pathdict.values.collect({|fn| GESBufferLoader.gesBufferPath +/+ fn })
		}
		{
			paths = pathdict.collect({|fn| GESBufferLoader.gesBufferPath +/+ (fn ++ ".aiff") })
		};
		buffers = Array();
		paths.do({|path, i|
			var loop;
			loop = loops[path.basename.asSymbol];
			buffers = buffers.add(Buffer.readChannel(Server.default, path, loop.start, loop.nframes, [0]))
		})

	}

	search{|bpm, pct=0.05|
		var results, inds;
		results = loops.select({|loop|
			var bpms = [0.25, 0.5, 1, 2.0, 4.0] * loop.bpm;
			bpms.any({|b| b.inRange(bpm-(bpm*pct), bpm+(bpm*pct)) });
		});

		inds = results.keys(Array).collect(_.asString).collect({|key|
			buffers.selectIndices({|buf|
				buf.path.basename == key
			})
		}).flat;

		^results.keys(Array).collect({|key, i|
			var matchbpm, bpms = loops[key].bpm * [0.25, 0.5, 1, 2.0, 4.0];
			matchbpm = bpms.select({|b| b.inRange(bpm-(bpm*pct), bpm+(bpm*pct)) }).first;
			('index': inds[i], 'bpm': matchbpm, 'rate': bpm / matchbpm)
		})
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

	loopsAtBuf{|buf|
		^loops[buf.path.basename.asSymbol]
	}

	choose{
		^buffers.choose
	}

	preload{
		buffers = Array.newClear(paths.size);
		paths.do({|path, i|
			buffers[i] = Buffer.readChannel(Server.default, path, channels: [0])
		})
	}

}