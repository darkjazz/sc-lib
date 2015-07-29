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
	
	var <notes, <origin;
	
	*new{|notes, origin|
		^super.newCopyArgs(notes, origin).removeDups.sort
	}
	
	removeDups{
ÊÊÊ ÊÊÊ var result;
ÊÊÊ ÊÊÊ result = Array();
ÊÊÊ ÊÊÊ notes.do({ arg item;
ÊÊÊ ÊÊÊ ÊÊÊ if (result.collect(_.midinote).includes(item.midinote).not) 
			{ result = result.add(item) }
ÊÊÊ ÊÊÊ });
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
		
}

ChordNote{
	
	var <midinote, <start, <duration;
	
	*new{|note, start, dur|
		^super.newCopyArgs(note.asInt, start.asFloat, dur.asFloat)
	}
	
	asFreq{ ^midinote.midicps }
	
	asName{ ^midinote.midinote }
	
}