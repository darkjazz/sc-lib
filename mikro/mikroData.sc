MikroData{
	
	classvar <>savePath = "/Users/alo/Data/mikro/lib000/"; 
	classvar <>loadPath = "/Users/alo/Data/mikro/lib000/";
	
	var <datalib;
		
	loadPathMatch{|pathmatch|
		pathmatch = pathmatch ? (this.class.loadPath ++ "*");
		fork{
			Post << "loading data from " << pathmatch << " ..." << Char.nl;
			pathmatch.pathMatch.do({|path|
				var name = path.basename.split($.).first.asSymbol;
				if (datalib.isNil) { datalib = () };
				datalib[name] = this.class.loadEvents(path: path);
				0.1.wait;
			});
			Post << Char.nl;
			Post << "Loading data lib complete..." << Char.nl;
		}
	}
	
	compareEvents{|evA, evB|
		var diff = Array();
		diff = diff.add( evB.start - (evA.start + evA.duration) );
		diff = diff.add( abs(evB.duration - evA.duration) );
		diff = diff.add( abs(evA.mfcs.collect(_.at(1)).mean - evB.mfcs.collect(_.at(1)).mean).mean );
		diff = diff.add( abs(evA.flts.collect(_.at(1)) - evB.flts.collect(_.at(1))).mean );
		diff = diff.add( abs(evA.amps.collect(_.at(1)) - evB.amps.collect(_.at(1))).mean );
		diff = diff.add( abs(evA.frqs.collect(_.at(1)) - evB.frqs.collect(_.at(1))).mean / 10000.0);
		^diff.sum		
	}
	
	fillEventDiff{|libname|
		datalib[libname].diff = Array();
		datalib[libname].events.doAdjacentPairs({|evA, evB|
			datalib[libname].diff = datalib[libname].diff.add( this.compareEvents(evA, evB) )
		});
	}
	
	*saveEvents{|analyzer, path|
		var file;
		path = path ? (this.savePath ++ Date.getDate.stamp ++ ".events");
		file = File(path, "wb");
		file.putFloat(analyzer.events.size.asFloat);
		file.putFloat(analyzer.maxdur);
		file.putFloat(analyzer.numcoef);
		file.putChar($\n);
		analyzer.events.do({|event|
			file.putFloat(event.start);
			file.putFloat(event.duration);
			file.putFloat(event.amps.size.asFloat);
			file.putChar($\n);
			event.amps.do({|amp|
				file.putFloat(amp[0]);
				file.putFloat(amp[1]);
				file.putChar($\n);
			});
			event.mfcs.do({|mfc|
				file.putFloat(mfc[0]);
				mfc[1].do({|coef| file.putFloat(coef) });
				file.putChar($\n);
			});
			event.flts.do({|flt|
				file.putFloat(flt[0]);
				file.putFloat(flt[1]);
				file.putChar($\n);
			});
			event.frqs.do({|frq|
				file.putFloat(frq[0]);
				file.putFloat(frq[1]);
				file.putChar($\n);
			})
		});
		file.close;
		("events saved to file " ++ path).inform;
			
	}
	
	*loadEvents{|analyzer, path|
		var file;
		analyzer = analyzer ? ();
		file = File(path, "rb");
		analyzer.events = Array.newClear(file.getFloat.asInt);
		analyzer.maxdur = file.getFloat;
		analyzer.numcoef = file.getFloat;
		file.getChar;
		analyzer.events.size.do({|i|
			var event, size, amps, mfcs, flts, frqs;
			event = MikroEvent(file.getFloat).duration_(file.getFloat);
			size = file.getFloat.asInt;
			file.getChar;
			amps = Array.newClear(size);
			size.do({|j|
				amps[j] = [file.getFloat, file.getFloat];
				file.getChar;
			});
			event.amps = amps;
			mfcs = Array.newClear(size);
			size.do({|j|
				mfcs[j] = [file.getFloat, Array.fill(analyzer.numcoef, { file.getFloat })];
				file.getChar;
			});
			event.mfcs = mfcs;
			flts = Array.newClear(size);
			size.do({|j|
				flts[j] = [file.getFloat, file.getFloat];
				file.getChar;
			});
			event.flts = flts;
			frqs = Array.newClear(size);
			size.do({|j|
				frqs[j] = [file.getFloat, file.getFloat];
				file.getChar;
			});
			event.frqs = frqs;
			analyzer.events[i] = event
		});
		file.close;
		("events loaded from file " ++ path).inform;
		^analyzer;
	}
	
}