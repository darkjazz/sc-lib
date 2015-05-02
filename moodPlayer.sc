MoodPlayer{
	
	const <resultURI = "http://isophonics.net/moodconductor/result";
	const <endpointURI = "http://127.0.0.1:3030/dataset/query";
	const <sparqlPath = "/Users/alo/FAST/dev/mood/sparql";
	const <queryService = "/usr/local/fuseki/s-query";
	const <audioPath = "/Volumes/G-DRIVE/ilmaudio";
	
	var <>density, <>mindur, <>bpm, <synthData, <kdtree;
	var meanCoords, <>env, playingSynths;
		
	*new{|density=2, mindur=2, bpm=60|
		^super.newCopyArgs(density, mindur, bpm).init
	}
	
	init{
		synthData = ();
		playingSynths = Array();
		this.loadActData;
		this.makeKDTree;
		this.addSynthDef;
	}
	
	*getQuery{
		var file, query;
		file = File.open(MoodPlayer.sparqlPath, "r");
		query = file.readAllString;
		file.close; file = nil;
		^query.urlEncode
	}
	
	loadActData{
		var response = (MoodPlayer.queryService ++ " --service=" ++ 
			MoodPlayer.endpointURI ++ " --query=" ++ MoodPlayer.sparqlPath 
			+/+ "qry_act.rq" ++ " --output=csv --post").unixCmdGetStdOut;
		response.split(Char.nl).drop(1).do({|line|
			var id, va, ar, fn;
			#id, va, ar, fn = line.split(Char.comma);
			if (fn.notNil) { fn = fn.keep(fn.size-1) };
			if (id.notNil.and(va.notNil).and(ar.notNil).and(fn.notNil))
			{
				synthData[fn.asSymbol] = MoodSynth(fn.asSymbol, id.asSymbol, va.asFloat, ar.asFloat); 
			}
		})
	}
	
	makeKDTree{
		var treeargs = Array(); 
		synthData.values.asArray.do({|sy|
			treeargs = treeargs.add(
				Array.with(
					this.convert(sy.valence), 
					this.convert(sy.arousal),
					sy.filename
				)
			)
		});
		kdtree = KDTree(treeargs, lastIsLabel: true);
	}
	
	findNearestFile{|input|
		^kdtree.nearest(input)[0].label.asString
	}
	
	getUserData{
		^("curl" + resultURI).unixCmdGetStdOut;
	}
	
	convert{|value|
		^value.linlin(-1.0, 1.0, 0.0, 1.0)
	}
	
	getMetadata{|filename|
		var md, id, response;
		id = "ilm:" ++ synthData[filename.asSymbol].id.asString.split($/).last;
		response = (MoodPlayer.queryService ++ " --service=" ++ 
			MoodPlayer.endpointURI ++ " " ++ this.loadMetadataQuery(id)
			++ " --output=csv --post").unixCmdGetStdOut;
		md = response.split(Char.nl).drop(1).first.split(Char.comma);
		synthData[filename.asSymbol].title = md[0]; 
		synthData[filename.asSymbol].artist = md[1]; 
		synthData[filename.asSymbol].album = md[2];
		synthData[filename.asSymbol].year = md[3]; 
		synthData[filename.asSymbol].duration = md[4];
	}
	
	loadMetadataQuery{|id|
		var file, str;
		file = File.open(MoodPlayer.sparqlPath +/+ "qry_metadata.rq", "r");
		str = file.readAllString;
		file.close; file = nil;
		^"'" ++ str.replace("%id%", id) ++ "'"
	}
	
	calculateCoordinates{
		var latest, data, counts;
		latest = this.getUserData;
		data = latest.replace("(", "[").replace(")", "]").interpret;
		if (data.size > 0)
		{
			counts = data.collect(_.at(2));
			^(data.collect({|coords| coords.keep(2) }) * counts).sum / counts.sum;
		}
		{
			^nil;
		}
	}
	
	addSynthDef{
		SynthDef('moodplay', {|out, buf, amp, gate=1|
			var in;
			in = DiskIn.ar(2, buf) 
				* EnvGen.kr(EnvControl.kr, gate, levelScale: amp, doneAction: 2);
			Out.ar(0, in)
		}).add
	}
	
	beatdur{ ^this.bps.reciprocal }
	
	bps{ ^(bpm/60) }
	
	play{
		Tdef('moodPlayer', {
			loop({
				var filename, coords = this.calculateCoordinates;
				if (coords.notNil)
				{
					Post << coords << Char.nl
				}
				{
					Post << "waiting for input.." << Char.nl
				};
				if (coords.notNil) {
					filename = this.findNearestFile(coords).asSymbol;
					Post << "Closest filename: " << filename << Char.nl;
					if (synthData[filename].title.isNil) {
						this.getMetadata(filename);
						Post << synthData[filename.asSymbol].getTrackMetadata << Char.nl;
					};
					if (synthData[filename].isPlaying.not)
					{
						Post << "ACT-Coordinates: valence " << synthData[filename].valence << 
							" | arousal " << synthData[filename].arousal << Char.nl;
						Post << "Starting " << filename << Char.nl;
						this.xfadeSynth(filename);
					}
				}
				{
					if (playingSynths.size >= density) {
						playingSynths.keep(playingSynths.size-1).do({|name|
							this.fadeOutSynth(name)
						})
					}
				};
				(this.beatdur * 4).wait
			})	
		}).play
	}
	
	xfadeSynth{|filename|
		var info, buffer, synth;
		Tdef('startxfade', {
			env = Env([0.001, 1.0, 1.0, 0.001], ((this.beatdur * 4) ! 3), [-3, 0, 3], 2, 1);
			if (synthData[filename].numFrames.isNil)
			{
				info = SoundFile.openRead(MoodPlayer.audioPath +/+ filename.asString);
				synthData[filename].numFrames = info.numFrames;
				synthData[filename].lastPosition = 30*info.sampleRate;
				synthData[filename].sampleRate = info.sampleRate;
				info.close; info = nil;
			};
			synthData[filename].isPlaying = true;
			synthData[filename].startTime = SystemClock.seconds;
			buffer = Buffer.cueSoundFile(Server.default, MoodPlayer.audioPath +/+ filename.asString, 
				synthData[filename].lastPosition);
			Server.default.sync;
			synthData[filename].synth = Synth('moodplay', [\out, 0, \buf, buffer, \amp, 0.5])
				.setn('env', env);
			Server.default.sync;
			playingSynths = playingSynths.add(filename);
			if (playingSynths.size >= density) {
				playingSynths.keep(playingSynths.size-1).do({|name|
					this.fadeOutSynth(name)
				})
			}
		}).play
	}
	
	fadeOutSynth{|filename|
		Post << "Fading out " << filename << Char.nl;
		synthData[filename].synth.set(\gate, 0);
		SystemClock.sched(this.beatdur * 4, {
			playingSynths.remove(filename);
			synthData[filename].isPlaying = false;
			synthData[filename].setLastPosition; 
			nil
		});
	}
	
	stop{ Tdef('moodPlayer').stop }
	
	clear{ Tdef('moodPlayer').clear }
	
}

MoodSynth{
	
	var <filename, <id, <valence, <arousal, <>isPlaying;
	var <>numFrames, <>sampleRate, <>lastPosition, <>startTime;
	var <>title, <>artist, <>album, <>year, <>duration;
	var <>synth;
	
	*new{|filename, id, valence, arousal|
		^super.newCopyArgs(filename, id, valence, arousal, false)
	}
	
	getTrackMetadata{
		^(
			title: title, 
			artist: artist,
			album: album,
			year: year,
			duration: duration
		)
	}
	
	setLastPosition{
		lastPosition = (SystemClock.seconds - startTime) * sampleRate;
	}
	
}

