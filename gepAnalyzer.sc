UGepAnalyzer{
	
	var defs, ncoef, <stats, <mostRecent, oscfncs, <synth, <>bus, <>currentDef;
	
	*new{|defs, ncoef=13|
		^super.newCopyArgs(defs, ncoef).init
	}
	
	init{
		
		if (defs.isKindOf(SynthDef)) {
			defs = Array.with(defs)
		};
		
		stats = ();
		mostRecent = ();
		defs.do({|def| 
			stats[def.name.asSymbol] = (
				mfcc: Array.fill(ncoef, { RunningStat() }),
				flat: RunningStat(),
				cent: RunningStat(),
				amp: RunningStat(),
				err: RunningStat()
			);
			mostRecent[def.name.asSymbol] = (
				mfcc: Array.fill(ncoef, 0),
				flat: 0,
				cent: 0,
				amp: 0,
				err: 0			
			)
		});
		
		SynthDef(\analyzeGep, {|in, rate|
			var input, fft, mfcc, flat, cent, amp, error, trig;
			trig = Impulse.kr(rate);
			input = In.ar(in);
			fft = FFT(LocalBuf(1024), input);
			mfcc = MFCC.kr(fft, ncoef);
			flat = SpecFlatness.kr(fft);
			cent = SpecCentroid.kr(fft);
			amp = Amplitude.kr(input);
			error = CheckBadValues.kr(input + amp);
			SendReply.kr(trig, '/mfcc', mfcc);
			SendReply.kr(trig, '/flat', flat);
			SendReply.kr(trig, '/cent', cent);
			SendReply.kr(trig, '/amp', amp);
			SendReply.kr(trig, '/err', error);	
		}).add;
				
		bus = Bus.audio;

		this.addOSCFuncs;
 		
	}
	
	addOSCFuncs{
		var saddr = Server.default.addr;

		oscfncs = [
			OSCFunc({|msg|
				//msg.postln;
				stats[currentDef][\mfcc].do({|stat, i|
					stat.push(msg[i+3]);
					mostRecent[currentDef][\mfcc][i] = msg[i+3];
				});
			}, '/mfcc', saddr),
			
			OSCFunc({|msg|
				//msg.postln;
				stats[currentDef][\flat].push(msg[3]);
				mostRecent[currentDef][\flat] = msg[3];
			}, '/flat', saddr),
		
			OSCFunc({|msg|
				//msg.postln;
				stats[currentDef][\cent].push(msg[3]);
				mostRecent[currentDef][\cent] = msg[3];
			}, '/cent', saddr),
			
			OSCFunc({|msg|
				//msg.postln;
				stats[currentDef][\amp].push(msg[3]);
				mostRecent[currentDef][\amp] = msg[3];
			}, '/amp', saddr),
			
			OSCFunc({|msg|
				//msg.postln;
				stats[currentDef][\err].push(msg[3]);
				mostRecent[currentDef][\err] = msg[3];
			}, '/err', saddr)	
		];		
	}
	
	run{|analysisFunc, rate=10, target=1, addAction='addToHead'|
		Server.default.sync;
		synth = Synth(\analyzeGep, [\in, bus, \rate, rate], target, addAction);
		Server.default.sync;				
		analysisFunc.(this);
	}
	
	freeSynth{
		synth.free;
		synth = nil
	}
	
	currentStats{
		^stats[currentDef]	
	}
		
	clear{
		oscfncs.do(_.disable);
		oscfncs = nil;
		bus.free;
		bus = nil;		
	}
	
	asEvents{|key|
		^stats[key].collect({|stat|
			if (stat.isKindOf(Array)) {
				stat.collect({|mfcc| mfcc.asEvent })
			}
			{
				stat.asEvent
			}
		})
	}
	
	resetStats{
		defs.do({|def| 
			stats[def.name.asSymbol].mfcc.do(_.reset);
			stats[def.name.asSymbol].select({|stat, key| key != 'mfcc' }).do(_.reset) 
		});		
	}
	
	currentCoefficients{ ^mostRecent[currentDef].mfcc }
	
}

UGepLiveAnalyzer {
	
	var <ncoef, <server, <segmentStats, <diffStats, <bus, <group, oscfncs;
	
	*new{|ncoef, server|
		^super.newCopyArgs(ncoef, server).init
	}
	
	init{
				
		segmentStats = ();
		
		diffStats = ();
		
		server = server ? Server.local;
				
		SynthDef(\analyzeLiveGep, {|in, rate, dur|
			var input, fft, mfcc, flat, cent, amp, error, trig;
			trig = Impulse.kr(rate);
			input = In.ar(in);
			fft = FFT(LocalBuf(1024), input);
			mfcc = MFCC.kr(fft, ncoef);
			flat = SpecFlatness.kr(fft);
			cent = SpecCentroid.kr(fft);
			amp = Amplitude.kr(input);
			error = CheckBadValues.kr(input + amp);
			Line.kr(dur: dur, doneAction: 13);
			SendReply.kr(trig, '/symfcc', mfcc);
			SendReply.kr(trig, '/syflat', flat);
			SendReply.kr(trig, '/sycent', cent);
			SendReply.kr(trig, '/syamp', amp);
			SendReply.kr(trig, '/syerr', error);	
		}).send(server);	
		
		bus = Bus.audio(server);
		
		group = Group(server);			
		
	}
	
	analyzeSegment{|def, args, dur, rate, inputAnalyzer, postValues=false, doneAction|
		
		var saddr, compStats;
		
		saddr = server.addr;
		
		def = def.asSymbol;
						
		compStats = inputAnalyzer.mostRecent[inputAnalyzer.currentDef];
		
		oscfncs = [
			OSCFunc({|msg, time|
				if (postValues) {msg.postln};
				if (segmentStats[def].isNil) {
					segmentStats[def] = SegmentStat(time);
					diffStats[def] = SegmentStat(time);
				};
				segmentStats[def].addMFCC(time, msg[3..ncoef+2]);
				diffStats[def].addMFCC(time, this.diff(msg[3..ncoef+2], compStats.mfcc ));
			}, '/symfcc', saddr),
			
			OSCFunc({|msg, time|
				if (postValues) {msg.postln};
				if (segmentStats[def].isNil) {
					segmentStats[def] = SegmentStat(time);
					diffStats[def] = SegmentStat(time);
				};
				segmentStats[def].addFlatness(time, msg[3]);
				diffStats[def].addFlatness(time, this.diff( msg[3], compStats.flat ));
			}, '/syflat', saddr),
		
			OSCFunc({|msg, time|
				if (postValues) {msg.postln};
				if (segmentStats[def].isNil) {
					segmentStats[def] = SegmentStat(time);
					diffStats[def] = SegmentStat(time);
				};
				segmentStats[def].addCentroid(time, msg[3]);
				diffStats[def].addCentroid(time, this.diff( msg[3], compStats.cent ));
			}, '/sycent', saddr),
			
			OSCFunc({|msg, time|
				if (postValues) {msg.postln};
				if (segmentStats[def].isNil) {
					segmentStats[def] = SegmentStat(time);
					diffStats[def] = SegmentStat(time);
				};
				segmentStats[def].addAmp(time, msg[3]);
				diffStats[def].addAmp(time, this.diff( msg[3], compStats.amp ));
			}, '/syamp', saddr),
			
			OSCFunc({|msg, time|
				if (postValues) {msg.postln};
				if (segmentStats[def].isNil) {
					segmentStats[def] = SegmentStat(time);
					diffStats[def] = SegmentStat(time);
				};
				segmentStats[def].addError(time, msg[3]);
			}, '/syerr', saddr),
			
			OSCFunc({|msg|
				Post << "analysis complete for " << def << Char.nl;
				oscfncs.do(_.disable);
				oscfncs = nil;
				doneAction.(this);
			}, '/n_end', Server.local.addr).oneShot;
				
		];	

		Synth.tail(group, \analyzeLiveGep, [\in, bus, \rate, rate, \dur, dur]);

		Synth.head(group, def, [\out, bus] ++ args);
		
	}
	
	diff{|featureA, featureB| ^abs(featureA - featureB) }	
}