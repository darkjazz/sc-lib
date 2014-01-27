UGepTimeAnalyzer{
	
	var defs, ncoef, target, <stats, <mostRecent, oscfnc, <synth, <>bus, <>currentDef;
	var errfnc, <errors, <cpu;
	
	*new{|defs, ncoef=13, target|
		^super.newCopyArgs(defs, ncoef, target).init
	}
	
	init{
		
		if (defs.isKindOf(SynthDef)) {
			defs = Array.with(defs)
		};
		
		stats = ();
		errors = ();
		cpu = ();
		mostRecent = ();
		defs.do({|def| 
			stats[def.name.asSymbol] = Array();
			errors[def.name.asSymbol] = Array();
			cpu[def.name.asSymbol] = Array();
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
			SendReply.kr(trig, '/stats', mfcc ++ flat ++ cent ++ amp);
			SendReply.kr(trig, '/err', error);	
		}).add;
				
		bus = Bus.audio;

		this.addOSCFuncs;
 		
	}
	
	addOSCFuncs{

		oscfnc = OSCFunc({|msg, time|
			stats[currentDef] = stats[currentDef].add([time, msg.drop(3)]);
			cpu[currentDef] = cpu[currentDef].add(
				(peak: Server.default.peakCPU, avg: Server.default.avgCPU)
			)
		}, '/stats', Server.default.addr);
		
		errfnc = OSCFunc({|msg|
			errors[currentDef] = errors[currentDef].add(msg[3])
		}, '/err', Server.default.addr)
			
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
	
	currentErrors{
		^errors[currentDef]			
	}
		
	clear{
		oscfnc.disable;
		oscfnc = nil;
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
	
	calculateDistances{
		var distances, targetSize, max, normalized, currentStats;
		currentStats = stats[currentDef];
		targetSize = target.size;
		distances = (
			'mfcc': Array.newClear(targetSize),
			'flat': Array.newClear(targetSize),
			'cent': Array.newClear(targetSize),
			'amp': Array.newClear(targetSize)
		);
		
		target.do({|targetEvent, i|
			distances.mfcc[i] = (targetEvent[1].keep(20) - currentStats[i][1].keep(20)).abs;
			distances.flat[i] = (targetEvent[1][20] - currentStats[i][1][20]).abs;
			distances.cent[i] = (targetEvent[1][21] - currentStats[i][1][21]).abs;
			distances.amp[i] = (targetEvent[1][22] - currentStats[i][1][22]).abs;
		});
		
		max = this.calculateMaximums(distances);
		
		normalized = ();
		
		normalized.mfcc = distances.mfcc.collect({|mfcc, i| [0, max.mfcc[i]].asSpec.unmap(mfcc) });
		normalized.flat = [0, max.flat].asSpec.unmap(distances.flat);
		normalized.cent = [0, max.cent].asSpec.unmap(distances.cent);
		normalized.amp = [0, max.amp].asSpec.unmap(distances.amp);
		
		^normalized
		
	}
	
	calculateMaximums{|distances|
		var maximums;
		maximums = (
			'mfcc': Array.newClear(ncoef),
			'flat': 0,
			'cent': 0,
			'amp': 0
		);
		
		maximums.mfcc = distances.mfcc.collect(_.maxItem);
		maximums.flat = distances.flat.maxItem;
		maximums.cent = distances.cent.maxItem;
		maximums.amp = distances.amp.maxItem;
		
		^maximums
	}
	
	calculateCPUsage{
		var min, max, peakavg, cpuspec;
		peakavg = cpu.collect({|ev| ev.collect({|it| [it.peak, it.avg].mean }).mean });
		max = peakavg.values.maxItem;
		min = peakavg.values.minItem;
		cpuspec = [min, max].asSpec;
		^cpu.collect({|cpu, key| 1.0 - cpuspec.unmap( peakavg[key] ) })
	}

}