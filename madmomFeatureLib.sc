MadmomFeatureLib{

	var <sets, <chordSet, <intervalSet;

	*new{|path|
		^super.new.load(path)
	}

	load{|path|
		var string, file = File(path, "r");
		string = file.readAllString;
		file.close;
		this.extractLib(string.parseJson);
	}

	extractLib{|json|
		sets = ();
		json.keysValuesDo({|key, val|
			sets[key.asSymbol] = FeatureSet(val)
		})
	}

	trainChordSets{|include, order|
		var obs;
		if (include.isNil) {
			include = sets.keys(Array)
		};
		obs = include.collect({|name|
			sets[name].collectChordEnums
		}).flat;
		chordSet = MarkovSetN.fill(obs.size, Pseq(obs), order);
		^chordSet;
	}

	trainIntervalSets{|include, order|
		var obs;
		if (include.isNil) {
			include = sets.keys(Array)
		};
		obs = include.collect({|name|
			sets[name].collectChordsTimeIntervals.round(0.1);
		}).flat;
		intervalSet = MarkovSetN.fill(obs.size, Pseq(obs), order);
		^intervalSet;
	}

	generateChordSequence{|length, seed, octave=4|
		var chords, chordNames, enum;
		enum = Chord.enumerateBasicTriads;
		if (seed.isNil) { chords = chordSet.dict.keys.choose.asString.interpret }
		{ chords = seed.bubble.flat };
		length.do({
			var next, nextKey = chords[(chords.lastIndex-2..chords.lastIndex)].asSymbol;
			if (chordSet.dict.keys.includes(nextKey).not) {
				nextKey = chordSet.dict.keys.choose
			};
			next = chordSet.next(nextKey);
			chords = chords.add(next)
		});
		chordNames = chords.collect({|index| enum[index] });
		^chordNames.collect({|name| this.collectChordFreqs(name, octave) });
	}

	generateIntervalSequence{|length, seed|
		var intervals;
		if (seed.isNil) { intervals = intervalSet.dict.keys.choose.asString.interpret }
		{ intervals = seed.bubble.flat };
		length.do({
			var next, nextKey = intervals[(intervals.lastIndex-2..intervals.lastIndex)].asSymbol;
			if (intervalSet.dict.keys.includes(nextKey).not) {
				nextKey = intervalSet.dict.keys.choose
			};
			next = intervalSet.next(nextKey);
			intervals = intervals.add(next)
		});
		^intervals
	}

	collectChordFreqs{|chord, octave|
		^Chord.basicTriads[chord.asSymbol].collect({|name|
			(name.toLower ++ " " ++ octave.asString).notemidi.midicps
		}) * this.multiplier(chord)
	}

	multiplier{|chord|
		var mul, note = chord.asString.first.toLower;
		mul = (1 ! 3);
		if ([$f, $g].includesEqual(note)) { mul = [1, 1, 2] };
		if ([$a, $b].includesEqual(note)) { mul = [1, 2, 2] };
		if ([$n].includesEqual(note)) { mul = [ ] };
		^mul
	}

}


FeatureSet{
	classvar <keys;

	var <beats, <chords, <notes, <key, <tempo, <chordIntervals;
	var <chordsHMM, <intervalsHMM;

	*new{|json|
		^super.new.fromJson(json)
	}

	*newFromSet{|beats, chords, notes, key, tempo|
		^super.newCopyArgs(beats, chords, notes, key, tempo)
	}

	fromJson{|json|
		key = json["key"].bubble;
		#beats, chords, notes, tempo = this.class.keys.collect({|key|
			this.extractDict(json[key])
		})
	}

	extractDict{|array|
		^array.collect({|dict|
			var ev = ();
			dict.keysDo({|key|
				ev[key.asSymbol] = dict[key]
			});
			ev
		});
	}

	collectNoteFreqs{
		^notes.collect(_.midicps)
	}

	collectChordEnums{
		^chords.collect({|chord| Chord.triadEnum.indexOf(chord.chord.asSymbol)  })
	}

	collectChordsFreqs{|octave|
		^chords.collect({|chord|
			(
				'start': chord.start,
				'end': chord.end,
				'chord': this.collectChordFreqs(chord.chord, octave)
			)
		})
	}

	collectChordFreqs{|chord, octave|
		^Chord.basicTriads[chord.asSymbol].collect({|name|
			(name.toLower ++ " " ++ octave.asString).notemidi.midicps
		}) * this.multiplier(chord)
	}

	collectChordsTimeIntervals{
		chordIntervals = Array();
		chords.collect(_.start).asFloat.doAdjacentPairs({|a, b|
			chordIntervals = chordIntervals.add(b - a)
		});
		^chordIntervals
	}

	enumerateChordIntervals{|round=0.1|
		this.collectChordsTimeIntervals.round(round);
		^chordIntervals.asSet.asArray.sort;
	}

	collectIntervalObservations{
		var enum = this.enumerateChordIntervals;
		^chordIntervals.collect({|interval| enum.indexOf(interval) });
	}

	toPrimeChord{|name, octave, transpose=0|
		var freqs = this.collectChordFreqs(name, octave).asInt.collect(_.nearestPrime);
		if (transpose != 0) {
			freqs = freqs.collect({|freq| Array.primeSeries(transpose, freq, 1).last })
		}
		^freqs
	}

	multiplier{|chord|
		var mul, note = chord.asString.first.toLower;
		mul = (1 ! 3);
		if ([$f, $g].includesEqual(note)) { mul = [1, 1, 2] };
		if ([$a, $b].includesEqual(note)) { mul = [1, 2, 2] };
		if ([$n].includesEqual(note)) { mul = [ ] };
		^mul
	}

	makeChordsHMM{|numstates, numsymbols, numiter|
		var observations = this.collectChordEnums;
		chordsHMM = HMM(numstates, numsymbols);
		{
			chordsHMM.train(~observations.bubble, numiter)
		}.fork
	}

	generateChords{|length|
		^chordsHMM.generate(length)
	}

	makeIntervalsHMM{|numstates, numsymbols, numiter|
		var observations, intervalEnum, intervals;
		intervals = this.collectChordsTimeIntervals.round(0.1);
		intervalEnum = intervals.asSet.asArray.sort;
		observations =intervals.collect({|interval| intervalEnum.indexOf(interval) });
		intervalsHMM = HMM(numstates, numsymbols);
		{
			intervalsHMM.train
		}.fork
	}

	generateIntervals{|length|
		^intervalsHMM.generate(length);
	}

	aggregate{|set|
		^FeatureSet.newFromSet(
			beats.addAll(set.beats),
			chords.addAll(set.chords),
			notes.addAll(set.notes),
			key.addAll(set.key),
			tempo.addAll(set.tempo)
		)
	}

	*initClass{
		keys = ["beats", "chords", "notes", "tempo"];
	}

}

