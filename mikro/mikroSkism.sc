MikroSkism{
	
	var <mikro, <recognizer, <synthDefs, sections, sectionDurations, <synths, <amps, grainEnvs;
	var graphics, eventStats, currentIndex=0, <>cleanup;
	
	*new{|mikro, recognizer, synthdefs, sections, sectionDurations|
		^super.newCopyArgs(mikro, recognizer, synthdefs, sections, sectionDurations).init
	}
	
	init{		
		grainEnvs = (
			perc: Env.perc,
			rev: Env([0.001, 1.0, 0.001], [0.99, 0.01], [4, 0]),
			tri: Env.triangle,
			sine: Env.sine
		).collect({|env| Buffer.sendCollection(Server.default, env.discretize(1024))});

		eventStats = (
			timeFrame: 10,
			duration: (avg: 0, dev: 0 ),
			peakamp: (avg: 0, dev: 0 ),
			eventcount: 0
		);
				
		if (synthDefs.notNil) {
			synthDefs.do(_.add)
		};
		
		if (sectionDurations.isKindOf(Number)) {
			sectionDurations = sectionDurations.bubble
		};
						
	}
		
	run{
		var patch;
		synths = ();
		Routine({
			synthDefs.do({|def|
				var readbufs;
				if (def.metadata.includesKey(\readbufs)) {
					readbufs = def.metadata.readbufs.();
					Server.default.sync;
					def.metadata.buffers = readbufs.collect(_.bufnum);
				}
			});
			
			mikro.analyzer.putEventResponderFunction(\skism, {|ti, re, ms|
				amps = recognizer.patchStringAsDigits(recognizer.mostCommon.asString);
				sections[currentIndex].do({|assoc, i|
					if (assoc.notNil) {
						synths[assoc.key].set(\amp, amps[i]*assoc.value.amp);
						mikro.graphics.setPattern(assoc.value.pattern, amps[i], rrand(0.5, assoc.value.amp), 2.rand, 2.rand)
					}
				});
			});	
			
			this.activateStats;
							
			sections.do({|sectarray, i|
				Post << "changing section to: " << i << Char.nl;
				sectarray.select(_.notNil).collect(_.key).do({|name|
					this.activateSynth(name)
				});
				sectionDurations.wrapAt(i).wait;
				currentIndex = currentIndex + 1;
				sectarray.select(_.notNil).do({|assoc|
					this.deactivateSynth(assoc.key);
					mikro.graphics.setPattern(assoc.value.pattern, 0, 0.0, 0, 0)
				})
			});

			Post << "elapsed time is approximately: " << mikro.analyzer.elapsedTime << Char.nl;
			
			Post << "i'm finished, cleanup commencing in 4 seconds..." << Char.nl;
			
			Post << "goodbye!" << Char.nl;
				
			4.wait;
			
			cleanup.();				
			
		}).play
	}
	
	activateStats{
		mikro.analyzer.offAction = {
			var lastEvent, includeEvents, dur, amp;
			lastEvent = mikro.analyzer.events.last;
			includeEvents = mikro.analyzer.events.select({|ev|
				ev.start > (lastEvent.start - eventStats.timeFrame) 
			});
			if (includeEvents.size > 0) {
				dur = includeEvents.collect(_.duration);
				eventStats.duration.avg = dur.mean;
				eventStats.duration.dev = dur.stdDev;
				amp = includeEvents.collect(_.peakAmp);
				eventStats.peakamp.avg = amp.mean;
				eventStats.peakamp.dev = amp.stdDev;
				eventStats.eventcount = includeEvents.size;
			};
//			Post << "average amplitude: " << eventStats.peakamp.avg << Char.nl;
//			Post << "amplitude deviation: " << eventStats.peakamp.dev << Char.nl;
//			Post << "average duration: " << eventStats.duration.avg << Char.nl;
//			Post << "duration deviation: " << eventStats.duration.dev << Char.nl;
//			Post << "event count: " << eventStats.eventcount << Char.nl;
			
			if (0.1.coin) {
				if (0.7.coin) {
					mikro.graphics.changeSetting(\add, max(eventStats.peakamp.avg, 0.005));
					Post << "changed add to " << max(eventStats.peakamp.avg, 0.005) << Char.nl;
				}
				{
					mikro.graphics.changeSetting(\add, min(1.0 - eventStats.peakamp.avg, 0.995));
					Post << "changed add to " << min(1.0 - eventStats.peakamp.avg, 0.995) << Char.nl;
				}
			};
			if (0.05.coin) {
				if (0.7.coin) {
					mikro.graphics.changeSetting(\symmetry, (1..4).wchoose((4..1).normalizeSum))
				}
				{
					mikro.graphics.changeSetting(\interp, [3.rand, (1..4).choose])
				}
			}
			
		}		
	}
	
	activateSynth{|name|
		var def, args, ctrbus;
		Post << "activating " << name << Char.nl;
		def = synthDefs.select({|sdf| sdf.name == name.asString }).first;
		args = ();
		def.metadata.specs.keysValuesDo({|argname, argvalue|
			args[argname] = argvalue.default
		});
		args[\out] = mikro.decoder.bus;
		args[\in] = mikro.input.bus;
		args[\amp] = 0.0;
		if (def.metadata.includesKey(\grainEnvBuf)) {
			args[\grainEnvBuf] = grainEnvs[def.metadata.grainEnvBuf]
		};		
		if (synths.isNil) { synths = () };
		synths[name] = Synth.tail(mikro.group, def.name, args.asKeyValuePairs);
		if (def.metadata.includesKey(\envbufnums)) {
			synths[name].setn(\envbufnums, grainEnvs.collect(_.bufnum))
		};
		if (def.metadata.includesKey(\buffers)) {
			synths[name].setn(\bufnums, def.metadata.buffers)
		}
		
	}
	
	deactivateSynth{|name|
		synths[name].free;
		synths[name] = nil;
	}
				
	free{
		synths.do(_.free);
	}
	
}

SkismLambda : MikroSkism{
	
	run{
		var patch;
		synths = ();
		Routine({
			synthDefs.do({|def|
				var readbufs;
				if (def.metadata.includesKey(\readbufs)) {
					readbufs = def.metadata.readbufs.();
					Server.default.sync;
					def.metadata.buffers = readbufs.collect(_.bufnum);
				}
			});
			
			mikro.analyzer.putEventResponderFunction(\skism, {|ti, re, ms|
				amps = recognizer.patchStringAsDigits(recognizer.mostCommon.asString);
				sections[currentIndex].do({|assoc, i|
					if (assoc.notNil) {
						synths[assoc.key].set(\amp, amps[i]*assoc.value.amp);
						mikro.graphics.setPattern(assoc.value.pattern, amps[i], rrand(0.5, assoc.value.amp), 2.rand, 2.rand,
							1.0.rand, 1.0.rand, 1.0.rand
						)
					}
				});
			});	
			
			this.activateStats;
							
			sections.do({|sectarray, i|
				Post << "changing section to: " << i << Char.nl;
				sectarray.select(_.notNil).collect(_.key).do({|name|
					this.activateSynth(name)
				});
				sectionDurations.wrapAt(i).wait;
				currentIndex = currentIndex + 1;
				sectarray.select(_.notNil).do({|assoc|
					this.deactivateSynth(assoc.key);
					mikro.graphics.setPattern(assoc.value.pattern, 0, 0.0, 0, 0, 1.0, 1.0, 1.0)
				})
			});

			Post << "elapsed time is approximately: " << mikro.analyzer.elapsedTime << Char.nl;
			
			Post << "i'm finished, cleanup commencing in 4 seconds..." << Char.nl;
			
			Post << "goodbye!" << Char.nl;
				
			4.wait;
			
			cleanup.();				
			
		}).play
	}
	
	activateStats{
		mikro.analyzer.offAction = {
			var lastEvent, includeEvents, dur, amp;
			lastEvent = mikro.analyzer.events.last;
			includeEvents = mikro.analyzer.events.select({|ev|
				ev.start > (lastEvent.start - eventStats.timeFrame) 
			});
			if (includeEvents.size > 0) {
				dur = includeEvents.collect(_.duration);
				eventStats.duration.avg = dur.mean;
				eventStats.duration.dev = dur.stdDev;
				amp = includeEvents.collect(_.peakAmp);
				eventStats.peakamp.avg = amp.mean;
				eventStats.peakamp.dev = amp.stdDev;
				eventStats.eventcount = includeEvents.size;
			};
			
			if (0.1.coin) {
				if (0.7.coin) {
					mikro.graphics.setAdd(max(eventStats.peakamp.avg, 0.005));
					Post << "changed add to " << max(eventStats.peakamp.avg, 0.005) << Char.nl;
				}
				{
					mikro.graphics.setAdd(min(1.0 - eventStats.peakamp.avg, 0.995));
					Post << "changed add to " << min(1.0 - eventStats.peakamp.avg, 0.995) << Char.nl;
				}
			};
			if (0.05.coin) {
				if (0.7.coin) {
					mikro.graphics.setSymmetry((1..11).wchoose((11..1).normalizeSum))
				}
				{
					mikro.graphics.setInterpolation(3.rand, (1..4).choose)
				}
			}
			
		}		
	}
	

}