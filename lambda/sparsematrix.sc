SparseMatrix{
	
	classvar <>allPatterns, <>patterns12, <>patterns16, <>sparseObjects, <>sparsePatterns;
	
	var <decoder, <graphics, <quant, <envs, <buffers, <group, <efxgroup, <nofxbus, <bpm, <bps, <beatdur;
	var <rDB, <efx, <efxamps, <patterndefs, <argproto, <deffuncs, codewindow, <listener, <mfccresp;
	var <skismDefs, skismSynths, grainEnvs;
	
	var <>defpath = "/Users/alo/Development/lambda/supercollider/sparsematrix/sparsedefs.scd";
	var <>skismDefPath = "/Users/alo/Development/skism/sc/defs.scd";

	
	*new{|decoder, graphics, quant=2|
		^super.newCopyArgs(decoder, graphics, quant).init
	}
	
	init{
		{
		decoder = decoder ? FoaDecoder();
		graphics = graphics ? CinderApp();
		
		this.loadBuffers;
		
		Server.default.sync;
		
		grainEnvs = (
			perc: Env.perc,
			rev: Env([0.001, 1.0, 0.001], [0.99, 0.01], [4, 0]),
			tri: Env.triangle,
			sine: Env.sine
		).collect({|env| Buffer.sendCollection(Server.default, env.discretize(1024))});
		
		envs = (
			sine00: Env.sine,
			sine01: Env([0, 1, 1, 0], [0.2, 0.6, 0.2], \sine),
			sine02: Env([0, 1, 1, 0], [0.3, 0.4, 0.3], \sine),
			perc00: Env.perc,
			perc01: Env.perc(0.1, 0.9, 1, 4),
			perc02: Env.perc(0.005, 1, 1, -16),
			perc03: Env.perc(curve: 0),
			lin00: Env([0, 1, 1, 0], [0.3, 0.4, 0.3]),
			lin01: Env([0, 1, 1, 0], [0, 1, 0]),
			step00: Env([0, 1, 0.3, 0.8, 0], (1/4!4), \step),
			step01: Env([0, 1, 0.5, 1, 0.5, 1, 0], (1/6!6), \step),
			wlch00: Env([0.001, 0.5, 0.4, 1.0, 0.001], [0.2, 0.3, 0.3, 0.2], \welch),
			wlch01: Env([0.001, 1, 0.3, 0.8, 0.001], [0.3, 0.3, 0.1, 0.3], \welch),
			gate00: Env([0, 1, 1, 0], [0, 1, 0], \lin, 2, 1),
			default: Env([1, 1, 0], [1, 0])
		).collect(_.asArray).collect(_.bubble);
		
		this.loadDefFuncs;
		
		defpath.load;
				
		Server.default.sync;

		skismDefs = skismDefPath.load;
		
		skismDefs.do(_.add);
		
		Server.default.sync;
		
		group = Group();
		efxgroup = Group.after(group);
		nofxbus = Server.default.options.numAudioBusChannels-1;
		
		if (this.class.allPatterns.isNil) {
			this.class.allPatterns = DjembeLib.convertAll(quant);
			this.class.patterns12 = this.class.allPatterns.select({|pat| (pat.first.size / quant) % 6 == 0 });
			this.class.patterns16 = this.class.allPatterns.select({|pat, name| this.class.patterns12.keys.includes(name).not });
			this.class.sparseObjects = this.class.allPatterns.collect(SparsePattern(_)).collect(_.makeSparse);
			this.class.sparsePatterns = this.class.sparseObjects.collect(_.patterns);
			this.class.patterns16.collect(_.size).postln;
			this.class.patterns16.size.postln;
			"-----".postln;
			this.class.patterns12.collect(_.size).postln;
			this.class.patterns12.size.postln;
		};
		
		rDB = (
			r00: { Pseq([
					Pseq([pi/6, pi/6.neg, pi/4.neg, pi/4], 4),
					Pseq([0, 1, 1, 0, 0.5, -0.5, -0.5, 0.5], 2),
					Pseq([pi/8, 5pi/8, pi/8.neg, 5pi/8.neg], 4),
					Pn(0, 8)
				], inf) },
			r01: { Prand((0,0.25..2)*pi-pi, inf) },
			r02: { Pxrand((0,0.2..1.8)*pi-pi, inf) },
			r03: { Pbrown(pi.neg, pi, pi/24, inf) },
			r04: { Pstutter(Pxrand((1..3), inf), Pseq((0,pi/6..2pi).mirror2-pi, inf)) },
			r05: { Prand((0,0.3..2)*pi-pi, inf) },
			r06: { Pwhite(pi.neg, pi, inf) },
			r07: { Pstutter(Pseq([2, 3, 2], inf), Pxrand( (0,0.3..2)*pi-pi, inf )) },
			r08: { Pbrown(pi.neg, pi, pi/6, inf) },
			r09: { Pwrand( (0.5.neg,0.4.neg..0.5)*pi, (0.5,0.4..0).mirror.normalizeSum, inf ) },
			r10: { Pwrand( (0.5,0.4..0.5.neg)*pi, (0.5,0.4..0).mirror.normalizeSum, inf ) },
			r11: { Pwrand( (0.5,0.6..1.5)*pi, (0.5,0.4..0).mirror.normalizeSum, inf ) },
			r12: { Pbrown(0, 0.5pi, 0.5pi/6, inf) },
			r13: { Pbrown(0, 0.5pi.neg, 0.5pi/6, inf) },
			r14: { Pbrown(pi, 0.5pi, 0.5pi/8, inf) },
			r15: { Pbrown(pi.neg, 0.5pi.neg, 0.5pi/8, inf) },
			default: 0
		);	
		

		efx = (
			rev00: (room: Pn(200), rtime: Pbrown(4.0, 10.0, 0.5, inf), damp: Pn(0.5), bw: Pn(0.5), spr: Pn(20), 
				dry: Pn(0), early: Pn(0.5), tail: Pn(1.0)
			),
			rev01: (room: Pn(50), rtime: Pn(15), damp: Pn(0.5), bw: Pn(0.5), spr: Pn(10), dry: Pn(0), 
				early: Pn(1.0), tail: Pn(0.5), wsz: Pn(0.05), pch: Prand([0.5, 0.25, 0.125], inf), 
				pds: Pwrand([0.0, 0.05, 0.1, 0.2], [0.4, 0.3, 0.2, 0.1], inf), 
				tds: Pwrand([0.0, 0.05, 0.1, 0.2], [0.3, 0.3, 0.2, 0.2], inf)
			),
			del00: (del: Pfunc({ bps / 4 }), dec: Pfunc({ beatdur }), rmp: Pn(0.5), rt: Pwhite(3.0, 10.0, inf)),
			del01: (del: Pxrand([0.04, 0.02, 0.05, 0.08, 0.1], inf), grw: Pn(1.618), 
				rmp: Pseq([0.05, 0.07, 0.09, 0.07], inf), rt: Prand([10, 5], inf)
			)
		).keysValuesDo({|name, ev|
			ev[\delta] = Pfunc({ beatdur });
			ev[\in] = Bus.audio;
			ev[\addAction] = \addToHead;
			ev[\group] = efxgroup;
			ev[\amp] = Pfunc({ efxamps[name] });
			ev[\out] = decoder.bus;
		});
		
		efxamps = efx.collect({ 0.0 });	
		
		Pdef(\efx, Ppar(
			efx.collect({|efx, name| Pmono(name, *efx.asKeyValuePairs) }).values
		));
		
		argproto = ();
		
		argproto['argproto'] = (
			p00: (efx: nofxbus, emp: 0, rotx: rDB.r00, roty: rDB.r00, rotz: rDB.r00, env: envs.perc01),
			p01: (efx: nofxbus, emp: 0, rotx: rDB.r06, roty: rDB.r05, rotz: rDB.r04, env: envs.perc01),
			p02: (efx: nofxbus, emp: 0, rotx: rDB.r01, roty: rDB.r01, rotz: rDB.r01, env: envs.perc00),
			p03: (efx: nofxbus, emp: 0, rotx: rDB.r02, roty: rDB.r02, rotz: rDB.r02, env: envs.step00 ),
			p04: (efx: nofxbus, emp: 0, rotx: rDB.r03, roty: rDB.r03, rotz: rDB.r03, env: envs.perc00 ),
			p05: (efx: efx.rev00.in, emp: 0.2, rotx: rDB.r04, roty: rDB.r04, rotz: rDB.r04, env: envs.perc02 ),
			p06: (efx: efx.rev00.in, emp: 0.2, rotx: rDB.r04, roty: rDB.r04, rotz: rDB.r04, env: envs.perc02 ),
			p07: (efx: nofxbus, emp: 0.2, rotx: rDB.r06, roty: rDB.r06, rotz: rDB.r06, env: envs.sine00 ),
			p08: (efx: nofxbus, emp: 0, rotx: rDB.r05, roty: rDB.r05, rotz: rDB.r05, env: envs.perc00 ),
			p09: (efx: efx.rev00.in, emp: 0.1, rotx: rDB.r04, roty: rDB.r04, rotz: rDB.r04, env: envs.perc02 ),
			p10: (efx: efx.rev00.in, emp: 0.1, rotx: rDB.r04, roty: rDB.r04, rotz: rDB.r04, env: envs.perc02 ),
			p11: (efx: efx.rev00.in, emp: 0.1, rotx: rDB.r04, roty: rDB.r04, rotz: rDB.r04, env: envs.perc02 ),
			p13: (efx: efx.del00.in, emp: 0.3, rotx: rDB.r06, roty: rDB.r06, rotz: rDB.r06, env: envs.perc00 ),
			p14: (efx: efx.del01.in, emp: 0.2, rotx: rDB.r02, roty: rDB.r06, rotz: rDB.r06, env: envs.perc00 ),
			p15: (efx: efx.del00.in, emp: 0.2, rotx: 0, roty: rDB.r06, rotz: rDB.r06, env: envs.perc00 ),
			p16: (efx: efx.rev01.in, emp: 0.1, rotx: rDB.r09, roty: rDB.r05, rotz: rDB.r03, env: envs.perc00 ),
			p17: (efx: nofxbus, emp: 0, rotx: rDB.r07, roty: rDB.r07, rotz: rDB.r07, env: envs.perc00 ),
			p18: (efx: nofxbus, emp: 0, rotx: rDB.r06, roty: rDB.r06, rotz: rDB.r06, env: envs.perc00 ),
			p19: (efx: nofxbus, emp: 0, rotx: rDB.r03, roty: rDB.r03, rotz: rDB.r03, env: envs.perc00 ),
			p20: (efx: nofxbus, emp: 0, rotx: rDB.r09, roty: rDB.r09, rotz: rDB.r09, env: envs.perc00  ),
			p22: (efx: efx.del01.in, emp: 0.2, rotx: rDB.r10, roty: rDB.r10, rotz: rDB.r10, env: envs.perc00),
			p24: (efx: nofxbus, emp: 0, rotx: rDB.r12, roty: rDB.r11, rotz: rDB.r12, env: envs.perc00 ),
			p25: (efx: nofxbus, emp: 0, rotx: rDB.r13, roty: rDB.r12, rotz: rDB.r11, env: envs.perc00 ),
			p26: (efx: nofxbus, emp: 0, rotx: rDB.r14, roty: rDB.r14, rotz: rDB.r14, env: envs.perc00 ),
			p27: (efx: nofxbus, emp: 0, rotx: rDB.r15, roty: rDB.r15, rotz: rDB.r10, env: envs.perc00 ),
			p28: (efx: efx.rev00.in, emp: 0.2, rotx: rDB.r06, roty: rDB.r06, rotz: rDB.r06, env: envs.perc00  ),
			p29: (efx: efx.del01.in, emp: 0.2, rotx: rDB.r06, roty: rDB.r06, rotz: rDB.r06, env: envs.perc00  ),
			p30: (efx: efx.rev01.in, emp: 0.2, rotx: rDB.r06, roty: rDB.r06, rotz: rDB.r06, env: envs.perc00  ),
			p31: (efx: nofxbus, emp: 0, rotx: rDB.r06, roty: rDB.r07, rotz: rDB.r07, env: envs.perc00 ),
			default: ( efx: nofxbus, emp: 0, rotx: rDB.r06, roty: rDB.r06, rotz: rDB.r06, env: envs[\default] )
		);
		
		argproto['fragproto'] = argproto['argproto'].collect({|args|
			args.collect(_.()).putPairs(
				(
					rate: (0.125 * Array.geom(14, 1, 2**(1/7))).choose, warp: Pxrand((2..6).reciprocal, inf), 
					dur: Pfunc({ beatdur }) * Prand([1, 2, 4], inf), wisz: Pxrand((0.05, 0.075..0.2), inf), genv: -1, 
					ffhi: 1, wrnd: Prand([0.01, 0.0], inf), dens: Pstutter(Pwhite(1, 4, inf), Pseq([4, 6, 8, 10], inf)), 
					intr: 4, fwid: Pseq([0.75, 0.5], inf), fflo: Pseg(Pseq([0.2, 0.5, 0.2], 1), Pseq([2, 4, 2, 3], 1), \linear, inf),
					ffrq: Pseq([2, 4, 6], inf) * Pfunc({ beatdur.reciprocal * 4 })
				).asKeyValuePairs
			)
		});
		
		argproto['fragproto01'] = argproto['argproto'].collect({|args|
			args.collect(_.()).putPairs(
				(
					grate: 10, gdlo: 0.01, gdhi: 0.2, loop: 1, dur: Pfunc({|ev| ev.buf.duration * 2 }),
					rate: 0.125, ffhi: 1, fwid: Pseq([0.25, 0.5], inf), 
					fflo: Pseg(Pseq([0.0, 0.3, 0.0], 1), Pseq([2, 4, 2, 3], 1), \linear, inf),
					ffrq: Pseq([1, 2, 4, 8], inf) * Pfunc({ beatdur.reciprocal * 4 }), fwid: Pseq([0.25, 0.5, 0.5], inf),
					rmz: rrand(100, 200), rev: rrand(6.0, 12.0), ear: rrand(0.1, 0.4), tai: rrand(0.4, 0.7),
					done: 2
				).asKeyValuePairs
			)
		});
		
		patterndefs = ();	
		
		this.setBPM(120);
		
		skismSynths = ();
		
		"sparsematrix performance ready...".postln;
		
		}.fork
				
	}
	
	loadDefFuncs{
		deffuncs = [
			{ Mix(SinOsc.ar(this.roundFreq([40, 51, 63]), 0.5pi)) },
			{ Impulse.ar(1, 10, 10).clip(-0.9, 0.9) },
			{ PinkNoise.ar.clip(-0.9, 0.9) },
			{ Mix(LFSaw.ar(this.roundFreq([20, 31]) + LFSaw.ar([1, 8]).range(20, 40))).clip(-0.9, 0.9) },
			{ RLPF.ar(BrownNoise.ar(10).softclip, this.roundFreq(320), 0.5, 1) },
			{ VarSaw.ar(IRand(*this.roundFreq([60, 80])), 0.25, 0.01, 20).clip(-0.5, 0.5) },
			{ LFPulse.ar(this.roundFreq(20) + LFPulse.ar(this.roundFreq(10))).distort },
			{ Dust2.ar(this.roundFreq(1000), 2, SinOsc.ar(Rand(8000, 16000).round(2**(1/5)))) },
			
			{ LFGauss.ar(1/44, XLine.kr(0.1, 0.01, 0.2)) },
			{ LFNoise0.ar(this.roundFreq(1000) + LFNoise0.ar(2500, 10).range(*this.roundFreq([50, 200])), 200).tanh * 0.8 },
			{ Mix(SinOsc.ar(this.roundFreq([20, 25, 30, 35]), 0.5pi)) },
			{ Mix(SinOsc.ar(SinOsc.ar(this.roundFreq([1000, 100])).range(*this.roundFreq([20, 200])), 0.5pi)) },
			{ Mix(SinOsc.ar(SinOsc.ar(this.roundFreq([51, 50])).range(*this.roundFreq([20, 80])), 0.5pi)) },
			{ Impulse.ar(1, 100, 10).clip(-0.9, 0.9) + Dust2.ar(this.roundFreq(10000), 2).tanh },
			{ LFSaw.ar(this.roundFreq(32), 0.5, LFNoise0.ar(10000).range(10, 100)).distort },
			{ Blip.ar(this.roundFreq(10), 100, 10).clip(-0.9, 0.9) },
			
			{ LFTri.ar(this.roundFreq(32), 0, LFNoise0.ar(this.roundFreq(200)).range(*this.roundFreq([40, 80]))).clip(-0.9, 0.9).distort },
			{ SinOsc.ar(this.roundFreq(2**13*1.5)) },
			{ Crackle.ar(1.6, 35).softclip },
			{ Logistic.ar(VarSaw.kr(pi**2).range(3.57, 3.8), 2**14) },
			{ Osc.ar(LocalBuf.newFrom((32.fib.mirror2.normalizeSum - 0.1) * [-1, 1].lace(128)), IRand(*this.roundFreq([16, 32])), 0, 5).softclip },
			{ Pluck.ar(SinOsc.ar(LFNoise0.ar(999).range(*this.roundFreq([40, 80])), 0, 10), Impulse.kr(2), 0.1, 0.1, 4).tanh },
			{ LFSaw.ar(LFNoise0.ar(999).range(*this.roundFreq([40, 80])), 0, 10).softclip },
			{ Decimator.ar(Impulse.ar(64, 10, 10).softclip, 48000, 24, 2) }, 
			
			{ SineShaper.ar(SinOsc.ar(this.roundFreq(10), 0, 10), 0.9) },
			{ SineShaper.ar(SinOsc.ar(this.roundFreq(20), 0, 10), 0.8) },
			{ SineShaper.ar(SinOsc.ar(this.roundFreq(20), 0, IRand(*this.roundFreq([200, 300]))), 0.5) },
			{ SineShaper.ar(SinOsc.ar(this.roundFreq(8), 0, IRand(*this.roundFreq([1000, 1500]))), 0.7) },
			{ CrossoverDistortion.ar(SinOsc.ar(LFNoise2.ar(1000).range(*this.roundFreq([180, 200])).round(10), 0, 2).softclip, 0.4, 0.2) },
			{ Disintegrator.ar(SinOsc.ar(LFSaw.ar(20).range(*this.roundFreq([50, 200])).round(10), 0, 2).clip, 0.5, 0.5) },
			{ Gendy1.ar(2, 2, 1, 1, this.roundFreq(40), this.roundFreq(80)) },
			{ Gendy1.ar(6, 6, 0.01, 0.01, this.roundFreq(40), this.roundFreq(160), 1, 1, 24, 24) },
			
			{ LFSaw.ar(LFSaw.ar(this.roundFreq(16)).range(*this.roundFreq([pi**pi, pi**pi*2])), LFSaw.ar(15).range(0, 2), LFPulse.ar(16).range(0.5, 1.0)) },
			{ StkPluck.ar(this.roundFreq(pi**pi), 1.0, 10).clip(-0.9, 0.9) },
			{ StkSaxofony.ar(this.roundFreq(pi**pi*2), 20, 40, XLine.kr(30, 10, 0.2), 10, 16, 10, 64, 1, 64).clip(-0.9, 0.9) },
			{ Oregonator.ar(Impulse.kr(16), 4, 0.5).clip(-0.9, 0.9) },
			{ Brusselator.ar(0, 0.5, 2.0).tanh },
			{ SpruceBudworm.ar(0,0.1,25.45,1.5,0.5,5.0, initx:0.7, inity: 0.4).tanh },
			{ Mix(MdaPiano.ar(this.roundFreq(16000),1,127,1,1,1,0,1,1,0.5,0.1,0.5,mul:20).softclip) },
			{ (Perlin3.ar(LFSaw.kr(220), SinOsc.ar(440), LFTri.ar(500))*10).distort },
			
			{ CA0.ar(11025, 32, 18, 0) },
			{ CA0.ar(11025, 64, 22, 0) },
			{ CA0x.ar(11025, 32, 26, 0) },
			{ CA0x.ar(11025, 64, 30, 0) },
			{ CA1.ar(4410, 32, 45, 0) },
			{ CA1.ar(4410, 64, 73, 0) },
			{ CA1x.ar(4410, 32, 89, 0) },
			{ CA1x.ar(4410, 64, 105, 0) }
			
		];
		deffuncs.collect({|fnc, i|
			this.makeDef(this.class.makeDefName(i, "d"), fnc)
		});		
	}
	
	loadBuffers{
		buffers = ();
		buffers.frags = "/Users/alo/sounds/funkt/frag*".pathMatch.collect({|path| Buffer.read(Server.default, path) });
		buffers.bits = "/Users/alo/sounds/funkt/bit*".pathMatch.collect({|path| Buffer.read(Server.default, path) });
		buffers.cycles = "/Users/alo/sounds/funkt/cycle*".pathMatch.collect({|path| Buffer.read(Server.default, path) });
		Server.default.sync;
	}
		
	makeDef{|name, func|
		SynthDef(name, {|out, efx, dur = 0.1, amp = 1.0, emp = 0.0, rotx = 0.0, roty = 0.0, rotz = 0.0| 
			var sig;
			sig = SynthDef.wrap(func) 
				* EnvGen.kr(EnvControl.kr, timeScale: dur, doneAction: 2);
			Out.ar(efx, sig * emp);
			Out.ar(out, FoaTransform.ar(
				FoaEncode.ar(sig * amp, FoaEncoderMatrix.newDirection), 'rtt', rotx, roty, rotz)
			)
		}).add;
	}
	
	testDef{|index|
		Pdef(\testDef, Pbind(\instrument, this.class.makeDefName(index, "d"), \group, group, \addAction, \addToHead, 
			\delta, 1, \amp, 0.7, \out, decoder.bus, \dur, 0.3, \pat, Pseq([1,0], inf), \efx, 511, \emp, 0, 
			\env, envs.perc00
			)
		);
		Pdef(\testDef).play;
	}
	
	stopDefTest{ 
		Pdef(\testDef).stop; 
		Pdef(\testDef).clear 
	}
	
	*makeDefName{|i, chr="p"| ^(chr++i.asString.padLeft(2, "0")).asSymbol }
	
	setBPM{|newbpm|
		bpm = newbpm;
		bps = bpm / 60;
		beatdur = bps.reciprocal;
	}
	
	makePattern{|name, seq, rpt|  
		rpt = rpt ? [1];
		^Pdefn(name, 
			Pseq(seq.collect({|arr, i| Pseq(arr.replace(0, \rest).replace(1, \note), rpt.wrapAt(i)) }), inf)
		).count(seq.size * seq.first.size)
	}
	
	addPatternSynthDef{|name, size=32, groupsize=4, div=8, sourcenames, subpatterns=0, prefix, protoname|
		patterndefs[name] = SparseSynthPattern(name, size, groupsize, div, sourcenames, subpatterns, prefix, this, protoname ? 'argproto')
	}

	addPatternBufferDef{|name, size=32, groupsize=4, div=8, sourcenames, subpatterns=0, prefix, protoname, buffers, defname|
		patterndefs[name] = SparseBufferPattern(name, size, groupsize, div, sourcenames, subpatterns, prefix, this, protoname ? 'argproto', buffers, defname)
	}
	
	addPatternCycleDef{|name, size, buffers, defname, prefix|
		patterndefs[name] = SparseCyclePattern(name, size, buffers, defname, prefix, this)
	}
	
	defsAt{|name| ^patterndefs[name]  }
	
	assignCodeWindow{|document|
		var sendarray;
		if (document.isKindOf(Document).not) {
			codewindow = document ? Document("---sparsematrix---")
		}
		{
			codewindow = document
		};
		codewindow.keyDownAction = {|doc, char, mod, uni, key|
			if ((uni == 3) and: { key == 76 }) 
			{
				sendarray = doc.selectedString.split(Char.nl);
				sendarray[0] = "@ " ++ sendarray[0];
				sendarray.do({|str|  
					graphics.sendCodeLine(str) 
				})
			}
		}
	}
	
	runGraphicsApp{ graphics.open }
	
	startDecoder{
		{
			decoder.start(efxgroup, \addAfter);
			Server.default.sync;
			listener = Synth.before(decoder.synth, \mfcc, [\in, decoder.bus, \th, -6.dbamp]);
			mfccresp = OSCFunc({|ms|
				graphics.sendSOMVector(ms[3..11]);
			}, '/mfcc', Server.default.addr ).add;
		}.fork
	}
	
	quit{
		decoder.free;
		mfccresp.disable;
		listener.free;
	}
		
	roundFreq{|freq, octavediv=24, ref=440|
		^(2**(round(log2(freq/ref)*octavediv)/octavediv)*ref)
	}
	
	activateSkismSynth{|name|
		var def, args, ctrbus;
		Post << "activating " << name << Char.nl;
		def = skismDefs.select({|sdf| sdf.name == name.asString }).first;
		args = ();
		def.metadata.specs.keysValuesDo({|argname, argvalue|
			args[argname] = argvalue.default
		});
		args[\out] = decoder.bus;
		args[\in] = decoder.bus;
		args[\amp] = 0.0;
		if (def.metadata.includesKey(\grainEnvBuf)) {
			args[\grainEnvBuf] = grainEnvs[def.metadata.grainEnvBuf]
		};			
		skismSynths[name] = Synth.tail(efxgroup, def.name, args.asKeyValuePairs);
		if (def.metadata.includesKey(\envbufnums)) {
			skismSynths[name].setn(\envbufnums, grainEnvs.collect(_.bufnum))
		};
		if (def.metadata.includesKey(\buffers)) {
			skismSynths[name].setn(\bufnums, def.metadata.buffers)
		}
		
	}
	
	setSkismAmp{|name, val| skismSynths[name].set(\amp, val) }
	
	deactivateSkismSynth{|name|
		skismSynths[name].free;
		skismSynths[name] = nil;
	}
			
}

SparseMatrixPattern{
	
	var <name, <size, <groupsize, div, prefix, matrix, <patterns, <args, <groups, <ctrls;
	var <previousStates, maxStateSize = 4;
	
	*new{|name, size, groupsize, div, prefix, matrix|
		^super.newCopyArgs(name, size, groupsize, div, prefix, matrix).init
	}
	
	init{
		previousStates = Array.newClear(maxStateSize);
	}
	
	setControls{|onFunc, ampFunc, durFunc, names|
		var coll;
		
		this.saveCurrentState;
		
		if (names.notNil) {
			coll = names.collect({|name| ctrls[name] });
		}
		{
			coll = ctrls
		};
		
		coll.do({|ctr|
			ctr.active = onFunc.();
			ctr.amp = ampFunc.();
			ctr.dur = durFunc.();
		})
	}
	
	setActives{|ampFunc, durFunc|
		this.saveCurrentState;
		
		ctrls.select({|ctr| ctr.active.booleanValue }).do({|ctr|
			ctr.amp = ampFunc.();
			ctr.dur = durFunc.();
		})
	}
	
	setGroups{|indices, onFunc, ampFunc, durFunc|
		
		this.saveCurrentState;
		
		groups[indices].flat.do({|name|
			ctrls[name].active = onFunc.();
			ctrls[name].amp = ampFunc.();
			ctrls[name].dur = durFunc.();
		})
	}
	
	saveCurrentState{
		previousStates.pop;
		previousStates.insert(0, ctrls.collect({|ctr| ctr.collect(_.())  }));
	}
	
	recall{|index|
		
		this.saveCurrentState;
		
		ctrls = previousStates[(index + 1).clip(1, maxStateSize-1)].collect({|ctr| ctr.collect(_.())  })		
	}
	
}

SparseSynthPattern : SparseMatrixPattern{
	
	*new{|name, size, groupsize, div, sourcenames, subpatterns, prefix, matrix, protoname|
		^super.new(name, size, groupsize, div, prefix, matrix).makePdef(sourcenames, subpatterns, protoname)
	}
	
	makePdef{|sourcenames, subpatterns, protoname|
		var instr, argproto;
		var combined = Array();
		sourcenames.bubble.flat.do({|name|
			var sub;
			combined = combined ++ SparseMatrix.sparsePatterns[name];
			if (subpatterns > 0) {
				sub = SparseMatrix.sparseObjects[name].makeSubPatterns(subpatterns).subpatterns;
				sub.do({|subpat|
					combined = combined ++ subpat
				})
			}
		});
		patterns = ();
		groups = Array();
		
		combined.keep(size).do({|seq, i|
			var key;
			key = SparseMatrix.makeDefName(i, prefix);
			patterns[key] = seq;
			groups = groups.add(key);
		});
		
		groups = groups.clump(groupsize);
		
		ctrls = patterns.collect({  (active: 0, amp: 0, dur: rrand(0.01, 0.1)) });
		
		argproto = ();
		
		matrix.argproto[protoname].keysValuesDo({|key, val|
			argproto[key.asString.replace("p", prefix).asSymbol] = val
		});
		
		args = patterns.collect({|pat, key| argproto[key] ? argproto[\default]; });

		instr = ().putPairs(args.size.collect({|i| [SparseMatrix.makeDefName(i, prefix), SparseMatrix.makeDefName(i, "d")]  }).flat);
		
		Pdef(name, Ppar(
			args.collect({|args, key|  
				Pbind(\instrument, instr[key], \group, matrix.group, \addAction, \addToHead, \delta, Pfunc({ matrix.beatdur / div }), 
					\amp, Pfunc({ ctrls[key].amp }), \out, matrix.decoder.bus,
					\dur, Pfunc({ ctrls[key].dur }), \pat, matrix.makePattern(key, patterns[key].bubble),
					\type, Pfunc({|ev| if (ctrls[key].active.booleanValue) { ev.pat } { \rest } }),
					*args.asKeyValuePairs
				)
			}).values
		)).quant(64);		
		
	}
	
	changePattern{|sourcenames, subpatterns=0, size=32, groupsize=4|
		var combined = Array();
		sourcenames.bubble.flat.do({|name|
			var sub;
			combined = combined ++ SparseMatrix.sparsePatterns[name];
			if (subpatterns > 0) {
				sub = SparseMatrix.sparseObjects[name].makeSubPatterns(subpatterns).subpatterns;
				sub.do({|subpat|
					combined = combined ++ subpat
				})
			}
		});
		patterns = ();
		groups = Array();
		
		combined.keep(size).do({|seq, i|
			var key;
			key = SparseMatrix.makeDefName(i, prefix);
			patterns[key] = seq;
			groups = groups.add(key);
		});
		
		groups = groups.clump(groupsize);
		
		patterns.keysValuesDo({|key, pat|
			matrix.makePattern(key, pat.bubble)
		})
		
	}
			
}

SparseBufferPattern : SparseMatrixPattern{
	var <buffers;
	
	*new{|name, size, groupsize, div, sourcenames, subpatterns, prefix, matrix, protoname, buffers, defname|
		^super.new(name, size, groupsize, div, prefix, matrix).makePdef(sourcenames, subpatterns, buffers, defname, protoname)
	}
	
	makePdef{|sourcenames, subpatterns, bufs, defname, protoname|
		var argproto, combined = Array();
		sourcenames.bubble.flat.do({|name|
			var sub;
			combined = combined ++ SparseMatrix.sparsePatterns[name];
			if (subpatterns > 0) {
				sub = SparseMatrix.sparseObjects[name].makeSubPatterns(subpatterns).subpatterns;
				sub.do({|subpat|
					combined = combined ++ subpat
				})
			}
		});
		patterns = ();
		groups = Array();
		buffers = ();
		
		combined.keep(size).do({|seq, i|
			var key;
			key = SparseMatrix.makeDefName(i, prefix);
			patterns[key] = seq;
			groups = groups.add(key);
			buffers[key] = bufs[i];
		});
		
		groups = groups.clump(groupsize);
		
		ctrls = patterns.collect({  (active: 0, amp: 0, dur: rrand(0.01, 0.1)) });
		
		argproto = ();
		
		matrix.argproto[protoname].keysValuesDo({|key, val|
			argproto[key.asString.replace("p", prefix).asSymbol] = val
		});
		
		args = patterns.collect({|pat, key| argproto[key] ? argproto[\default]; });
		
		Pdef(name, Ppar(
			args.collect({|args, key|  
				Pbind(\instrument, defname, \group, matrix.group, \addAction, \addToHead, 
					\delta, Pfunc({ matrix.beatdur / div }), 
					\amp, Pfunc({ ctrls[key].amp }), \out, matrix.decoder.bus, \buf, buffers[key],
					\dur, Pfunc({ ctrls[key].dur }), \pat, matrix.makePattern(key, patterns[key].bubble),
					\type, Pfunc({|ev| if (ctrls[key].active.booleanValue) { ev.pat } { \rest } }),
					*args.asKeyValuePairs
				)
			}).values
		));		
		
	}
	
	changePattern{|sourcenames, subpatterns=0, size=32, groupsize=4|
		var combined = Array();
		sourcenames.bubble.flat.do({|name|
			var sub;
			combined = combined ++ SparseMatrix.sparsePatterns[name];
			if (subpatterns > 0) {
				sub = SparseMatrix.sparseObjects[name].makeSubPatterns(subpatterns).subpatterns;
				sub.do({|subpat|
					combined = combined ++ subpat
				})
			}
		});
		patterns = ();
		groups = Array();
		
		combined.keep(size).do({|seq, i|
			var key;
			key = SparseMatrix.makeDefName(i, prefix);
			patterns[key] = seq;
			groups = groups.add(key);
		});
		
		groups = groups.clump(groupsize);
		
		patterns.keysValuesDo({|key, pat|
			matrix.makePattern(key, pat.bubble)
		})
		
	}
		
}

SparseCyclePattern : SparseMatrixPattern{
	
	var buffers, defname;
	
	*new{|name, size, buffers, defname, prefix, matrix|
		^super.new(name, size, prefix: prefix, matrix: matrix).makePdef(buffers, defname)
	}
	
	makePdef{|aBuffers, aDefname|
		buffers = aBuffers;
		defname = aDefname;
		ctrls = ();
		Pdef(name, Ppar(
			buffers.collect({|buf, i|
				var key = SparseMatrix.makeDefName(i, prefix);
				ctrls[key] = ( active: 0, amp: 0, buf: buf );
				Pbind(
					\instrument, defname, \group, matrix.group, \addAction, \addToHead,
					\buf, Pfunc({ ctrls[key].buf }), \amp, Pfunc({ ctrls[key].amp }),
					\dur, Pfunc({|ev| ev.buf.duration.round(matrix.beatdur) }),
					\out, matrix.decoder.bus, \env, matrix.envs.sine01,
					\rate, Pfunc({|ev| ev.buf.duration / ev.buf.duration.round(matrix.beatdur) }),
					\delta, Pfunc({|ev| ev.dur / 1.5 }), \rtm, Pbrown(3.0, 12.0, 2.0, inf), 
					\early, Pbrown(0.3, 0.6, 0.01, inf), \tail, Pbrown(0.4, 0.8, 0.01, inf), 
					\rmp, Pbrown(0.05, 0.2, 0.01, inf), \rotx, this.makeRotationPattern, 
					\roty, this.makeRotationPattern, \rotz, this.makeRotationPattern,
					\type, Pfunc({ if (ctrls[key].active.booleanValue) { \note } { \rest } })
				)
			})
		))
	}
	
	makeRotationPattern{
		^Pwrand(
			['r06', 'r01', 'r02', 'r08'].collect({|key| var pat = matrix.rDB[key].(); 
				if (pat.isKindOf(ListPattern)) { pat.repeats_(1) } { pat.length_(1) } }), 
			Array.rand(4, 0.0, 1.0).normalizeSum, 
			inf
		)
	}
	
}
