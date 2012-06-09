UGepAnalyzer{
	
	var defs, ncoef, <stats, oscfncs, <synth, <bus, <>currentDef;
	
	*new{|defs, ncoef=13|
		^super.newCopyArgs(defs, ncoef).init
	}
	
	init{

		var saddr = Server.default.addr;
		
		if (defs.isKindOf(SynthDef)) {
			defs = Array.with(defs)
		};
		
		stats = ();
		defs.do({|def| 
			stats[def.name.asSymbol] = (
				mfcc: Array.fill(ncoef, { RunningStat() }),
				flat: RunningStat(),
				cent: RunningStat(),
				amp: RunningStat(),
				err: RunningStat()
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

		oscfncs = [
			OSCFunc({|msg|
				//msg.postln;
				stats[currentDef][\mfcc].do({|stat, i|
					stat.push(msg[i+3])
				})
			}, '/mfcc', saddr),
			
			OSCFunc({|msg|
				//msg.postln;
				stats[currentDef][\flat].push(msg[3])
			}, '/flat', saddr),
		
			OSCFunc({|msg|
				//msg.postln;
				stats[currentDef][\cent].push(msg[3])
			}, '/cent', saddr),
			
			OSCFunc({|msg|
				//msg.postln;
				stats[currentDef][\amp].push(msg[3])
			}, '/amp', saddr),
			
			OSCFunc({|msg|
				//msg.postln;
				stats[currentDef][\err].push(msg[3])
			}, '/err', saddr)	
		];		
 			
	}
	
	run{|analysisFunc, rate=10|
		Server.default.sync;
		synth = Synth(\analyzeGep, [\in, bus, \rate, rate]);
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
	
}