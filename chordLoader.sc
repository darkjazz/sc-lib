ChordLoader{

	var path, <chords;

	*new{|path|
		^super.newCopyArgs(path)
	}

	load{
		var txt, file, lines, notes, currentStart, origin;
		if (File.exists(path))
		{
			file = File.open(path, "r");
		}
		{
			^"File not found!".error
		};
		txt = file.readAllString;
		file.close; file = nil;

		lines = txt.split(Char.nl);

		chords = Array();
		currentStart = lines.first[1];
		lines.do({|line|
			var fields;
			fields = line.split(Char.comma);
			if (fields.first.isEmpty.not)
			{
				if (notes.notNil.and(notes.size > 0))
				{
					chords = chords.add(Chord(notes, origin));
				};
				notes = Array();
				origin = fields.first;
			};

			if (currentStart != fields[1])
			{
				if (notes.notNil.and(notes.size > 0))
				{
					chords = chords.add(Chord(notes, origin))
				};
				notes = Array();
				currentStart = fields[1]
			};

			if (fields.size == 4)
			{
				notes = notes.add(ChordNote(*fields.drop(1).rotate(1)))
			}
		})
	}

}

Chord{
	classvar <basicTriads, <triadEnum;
	var <notes, <origin;

	*new{|notes, origin|
		^super.newCopyArgs(notes, origin).removeDups.sort
	}

	removeDups{
		var result;
		result = Array();
		notes.do({ arg item;
			if (result.collect(_.midinote).includes(item.midinote).not)
			{ result = result.add(item) }
		});
		notes = result
	}

	sort{
		notes.sort({|a, b| a.midinote < b.midinote })
	}

	getKey{
		var key = "";
		if (notes.size > 0)
		{
			notes.collect(_.midinote).do({|note|
				key = key ++ note ++ "-"
			});
			key = key.keep(key.lastIndex).asSymbol
			^key
		}
		{
			^nil
		}
	}

	getFreqs{
		^notes.collect(_.midinote).collect(_.midicps)
	}

	getDur{
		^notes.first.duration
	}

	getStartTime{
		^notes.first.start
	}

	*enumerateBasicTriads{
		^Chord.basicTriads.keys(Array).sort
	}

	*initClass{
		basicTriads = (
			'C:maj': ["C", "E", "G"],
			'C#:maj': ["C#", "E#", "G#"],
			'D:maj': ["D", "F#", "A"],
			'D#:maj': ["Eb", "G", "Bb"],
			'Eb:maj': ["Eb", "G", "Bb"],
			'E:maj': ["E", "G#", "B"],
			'F:maj': ["F", "A", "C"],
			'F#:maj': ["F#", "A#", "C#"],
			'G:maj': ["G", "B", "D"],
			'G#:maj': ["G#", "C", "D#"],
			'Ab:maj': ["Ab", "C", "Eb"],
			'A:maj': ["A", "C#", "E"],
			'A#:maj': ["Bb", "D", "F"],
			'Bb:maj': ["Bb", "D", "F"],
			'B:maj': ["B", "D#", "F#"],
			'C:min': ["C", "Eb", "G"],
			'C#:min': ["C#", "E", "G#"],
			'D:min': ["D", "F", "A"],
			'D#:min': ["D#", "E#", "F#"],
			'Eb:min': ["Eb", "Gb", "Bb"],
			'E:min': ["E", "G", "B"],
			'F:min': ["F", "Ab", "C"],
			'F#:min': ["F#", "A", "C#"],
			'G:min': ["G", "Bb", "D"],
			'Ab:min': ["Ab", "B", "Eb"],
			'A:min': ["A", "C", "E"],
			'A#:min': ["A#", "B#", "C#"],
			'Bb:min': ["Bb", "Db", "F"],
			'B:min': ["B", "D", "F#"],
			'N': []
		);
		triadEnum = Chord.enumerateBasicTriads;
	}
}

ChordNote{

	var <midinote, <start, <duration;

	*new{|note, start, dur|
		^super.newCopyArgs(note.asInteger, start.asFloat, dur.asFloat)
	}

	asFreq{ ^midinote.midicps }

	asName{ ^midinote.midinote }

}