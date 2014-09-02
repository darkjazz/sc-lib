UGepLoader{

	classvar <>synthdefdir="synthdefs/";
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
			path = Paths.gepArchDir +/+ meta.defname.asString ++ "." ++ UGEP.fileExt;
			if (File.exists(path)) {
				data = UGEP.loadData(path);
				include = ((headsize.isNil).xor(data.header.headsize == headsize))
				.and((numgenes.isNil).xor(data.header.numgenes == numgenes));
				if (include) {
					meta.data = data;
				};
			}
			{
				include = false
			};
			include
		});

		this.syncStats;

		Post << "UGep metadata loaded." << Char.nl;

		^data

	}

	loadNames{|names|
		data = names.collect({|defname|
			var path, meta, data;
			path = Paths.gepArchDir +/+ defname.asString ++ "." ++ UGEP.fileExt;
			if (File.exists(path)) { data = UGEP.loadData(path) };
			meta = UGenExpressionTree.loadMetadata(defname);
			meta.data = data;
			meta.defname = defname.asSymbol;
			meta
		});

		this.syncStats;

		Post << "UGep metadata loaded." << Char.nl;

		^data

	}

	loadFromFile{|path|
		var file, defarray;
		file = File.open(path, "r");
		defarray = file.readAllString.split(Char.nl).reject(_.isEmpty).collect(_.asSymbol);
		file.close;
		file = nil;
		^this.loadNames(defarray)
	}

	syncStats{

		var sync = data.select({|item|
			item.stats.size > 5
		});

		sync.do({|item, i|
			var dev, means, values, newstats;
			values = item.stats.collect(_.last).flop;
			means = values.collect(_.mean);
			newstats = ();
			newstats['amp'] = ('mean': means[22], 'stdDev': values[22].stdDev);
			newstats['cent'] = ('mean': means[21], 'stdDev': values[21].stdDev);
			newstats['flat'] = ('mean': means[20], 'stdDev': values[20].stdDev);
			newstats['err'] = ('mean': 0, 'stdDev': 0);
			newstats['mfcc'] = means[0..19].collect({|mean, i| ('mean': mean, 'stdDev': values[i].stdDev) });
			item.stats = newstats
		})

	}

}