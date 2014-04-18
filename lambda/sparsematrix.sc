SparseMatrix{
	
	classvar <>allPatterns, <>patterns12, <>patterns16, <>sparseObjects, <>sparsePatterns;
	
	var <decoder, <graphics, <quant, <ncoef, <envs, <buffers, <group, <efxgroup, <nofxbus, <bpm, <bps, <beatdur;
	var <rDB, <efx, <efxamps, <patterndefs, <argproto, <deffuncs, codewindow, <listener, <mfccresp;
	var <skismDefs, skismSynths, grainEnvs, <gepdefs, <gepsynths, <>onsetFunc;
	var <patternPlayers;

	var <>defpath, <>skismDefPath, <>bufferPath;

	*new{|decoder, graphics, quant=2, ncoef=8|
		^super.newCopyArgs(decoder, graphics, quant, ncoef).init
	}
	
	init{
		defpath = Paths.matrixdefs;
		skismDefPath = Paths.skismdefs;
		bufferPath = Paths.matrixbufs;
		{
		if (decoder.isNil) {
			decoder = FoaDecoder()
		};
		if (graphics.isNil) {
			graphics = CinderApp()
		};
		
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
		
		if (decoder.isRunning) {
			group = Group.before(decoder.synth)
		}
		{
			group = Group();
		};
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
			efx0: ( def: \rev00, args: (rtime: Pbrown(2.0, 3.0, 0.1,inf), hf: Pn(1.0))),
			efx1: ( def: \rev00, args: (rtime: Pseq([Pshuf([0.2,0.4,0.6,0.8]), Pshuf([1.0,2.0,3.0,4.0])],inf), 
				hf: Pbrown(0.0, 1.0, 0.1,inf))
			),
//			efx2: ( def: \rev01, args: (room: Pseq([30, 40, 40, 40],inf), 
//				rtime: Prand([1.0, 1.5, 2.0, 2.5, 3.0],inf), damp: Pn(0.5), bw: Pn(0.5), spr: Pn(20), 
//				dry: Pn(0.0), early: Pseq([0.2, 0.3, 0.6, 1.0],inf), tail: Pseq([1.0, 0.6, 0.3, 0.2],inf)) 
//			),
			efx3: ( def: \rev02, args: (room: Prand((0.4,0.5..0.8),inf), damp: Pwhite(0.0, 1.0,inf), 
				wsz: Prand([0.02,0.06,0.1,0.2],inf), pch:Prand(Array.geom(24,1.0,2**(1/24)).reverse.keep(10),inf), 
				pds: Pwrand([0,Pwhite(0.1, 0.5, 1)],[0.7, 0.3],inf), 
				tds: Pwrand([0,Pwhite(0.1, 0.5, 1)],[0.7, 0.3],inf) )
			),
			efx4: (def: \del00, args: (del: Pfunc({ beatdur / 2 }), dec: Pfunc({ beatdur * 4 }), 
				pch: Pwrand(Array.geom(24, 0.5, 2**(3/24))[(0..23).select(_.isPrime)], 
					Array.geom(9, 1.0, 2**(1/9)).normalizeSum, inf ), 
				wsz: Pwhite(0.05, 0.2, inf),   
				pds: Pwrand([0,Pwhite(0.1, 0.5, 1)],[0.8, 0.2],inf), 
				tds: Pwrand([0,Pwhite(0.1, 0.5, 1)],[0.8, 0.2],inf) )
			),
			efx5: (def: \del01, args: (del: Pfunc({ beatdur / 2 }), 
				rmp: Prand(Array.geom(8, 0.05, 1.2), inf), 
				rt: Pwhite(0.5, 3.0, inf) )
			)
		).keysValuesDo({|name, ev|
			ev[\args][\delta] = Pfunc({ beatdur });
			ev[\args][\in] = Bus.audio;
			ev[\args][\addAction] = \addToHead;
			ev[\args][\group] = efxgroup;
			ev[\args][\amp] = Pfunc({ efxamps[name] });
			ev[\args][\out] = decoder.bus;
		});
		
		efxamps = efx.collect({ 0.0 });	
		
		Pdef(\efx, Ppar(
			efx.collect({|efx| Pmono(efx[\def], *efx[\args].asKeyValuePairs) }).values
		));
		
		argproto = ();
				
		argproto['argproto'] = (
			p00: (efx: nofxbus, rotx: rDB.r00, roty: rDB.r00, rotz: rDB.r00, env: envs.perc01 ),
			p01: (efx: nofxbus, rotx: rDB.r06, roty: rDB.r05, rotz: rDB.r04, env: envs.perc01 ),
			p02: (efx: nofxbus, rotx: rDB.r01, roty: rDB.r01, rotz: rDB.r01, env: envs.perc00 ),
			p03: (efx: nofxbus, rotx: rDB.r02, roty: rDB.r02, rotz: rDB.r02, env: envs.step00 ),
			p04: (efx: nofxbus, rotx: rDB.r03, roty: rDB.r03, rotz: rDB.r03, env: envs.perc00 ),
			p05: (efx: nofxbus, rotx: rDB.r04, roty: rDB.r04, rotz: rDB.r04, env: envs.perc02 ),
			p06: (efx: nofxbus, rotx: rDB.r04, roty: rDB.r04, rotz: rDB.r04, env: envs.perc02 ),
			p07: (efx: nofxbus, rotx: rDB.r06, roty: rDB.r06, rotz: rDB.r06, env: envs.sine00 ),
			p08: (efx: nofxbus, rotx: rDB.r05, roty: rDB.r05, rotz: rDB.r05, env: envs.perc00 ),
			p09: (efx: nofxbus, rotx: rDB.r04, roty: rDB.r04, rotz: rDB.r04, env: envs.perc02 ),
			p10: (efx: nofxbus, rotx: rDB.r04, roty: rDB.r04, rotz: rDB.r04, env: envs.perc02 ),
			p11: (efx: nofxbus, rotx: rDB.r04, roty: rDB.r04, rotz: rDB.r04, env: envs.perc02 ),
			p13: (efx: nofxbus, rotx: rDB.r06, roty: rDB.r06, rotz: rDB.r06, env: envs.perc00 ),
			p14: (efx: nofxbus, rotx: rDB.r02, roty: rDB.r06, rotz: rDB.r06, env: envs.perc00 ),
			p15: (efx: nofxbus, rotx: rDB.r15, roty: rDB.r06, rotz: rDB.r06, env: envs.perc00 ),
			p16: (efx: nofxbus, rotx: rDB.r09, roty: rDB.r05, rotz: rDB.r03, env: envs.perc00 ),
			p17: (efx: nofxbus, rotx: rDB.r07, roty: rDB.r07, rotz: rDB.r07, env: envs.perc00 ),
			p18: (efx: nofxbus, rotx: rDB.r06, roty: rDB.r06, rotz: rDB.r06, env: envs.perc00 ),
			p19: (efx: nofxbus, rotx: rDB.r03, roty: rDB.r03, rotz: rDB.r03, env: envs.perc00 ),
			p20: (efx: nofxbus, rotx: rDB.r09, roty: rDB.r09, rotz: rDB.r09, env: envs.perc00 ),
			p22: (efx: nofxbus, rotx: rDB.r10, roty: rDB.r10, rotz: rDB.r10, env: envs.perc00 ),
			p24: (efx: nofxbus, rotx: rDB.r12, roty: rDB.r11, rotz: rDB.r12, env: envs.perc00 ),
			p25: (efx: nofxbus, rotx: rDB.r13, roty: rDB.r12, rotz: rDB.r11, env: envs.perc00 ),
			p26: (efx: nofxbus, rotx: rDB.r14, roty: rDB.r14, rotz: rDB.r14, env: envs.perc00 ),
			p27: (efx: nofxbus, rotx: rDB.r15, roty: rDB.r15, rotz: rDB.r10, env: envs.perc00 ),
			p28: (efx: nofxbus, rotx: rDB.r06, roty: rDB.r06, rotz: rDB.r06, env: envs.perc00 ),
			p29: (efx: nofxbus, rotx: rDB.r06, roty: rDB.r06, rotz: rDB.r06, env: envs.perc00 ),
			p30: (efx: nofxbus, rotx: rDB.r06, roty: rDB.r06, rotz: rDB.r06, env: envs.perc00 ),
			p31: (efx: nofxbus, rotx: rDB.r06, roty: rDB.r07, rotz: rDB.r07, env: envs.perc00 ),
			default: ( efx: nofxbus, rotx: rDB.r06, roty: rDB.r06, rotz: rDB.r06, env: envs[\default] )
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

		argproto['fragproto00'] = argproto['argproto'].collect({|args|
			args.collect(_.()).putPairs((rate: 1).asKeyValuePairs)
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
		
		gepdefs = ();
		
		gepsynths = ();
		
		this.setBPM(120);
		
		skismSynths = ();
		
		patternPlayers = ();
		
		"sparsematrix performance ready...".postln;
		
		}.fork
				
	}
	
	loadDefFuncs{
		deffuncs = [			
			{|freq=40| Mix(SinOsc.ar([freq, freq+11, freq+23], 0.5pi)) },
			{|freq=1| Mix(DelayC.ar(Impulse.ar(1, 10, 10),0.05,[0, 0.05])).clip(-0.9, 0.9) },
			{|freq=8000| PinkNoise.ar(10.0).clip(-0.9, 0.9) * SinOsc.ar(freq) },
			{|freq=20| Mix(LFSaw.ar([freq, freq + 11] + LFSaw.ar([1, 8]).range(freq, freq*2))).clip(-0.9, 0.9) },
			{|freq=320| RLPF.ar(BrownNoise.ar(10).softclip, freq, 0.5, 1) },
			{|freq=60| VarSaw.ar(IRand(*[freq, freq+20]), 0.25, 0.01, 20).clip(-0.5, 0.5) },
			{|freq=20| LFPulse.ar(freq + LFPulse.ar(freq/2)).distort },
			{|freq=1000| Dust2.ar(freq, 2, SinOsc.ar(Rand(freq*8, freq*16).round(2**(1/5)))) },
			
			{|freq=44| LFGauss.ar(1/freq, XLine.kr(0.1, 0.01, 0.2)) },
			{|freq=1000| LFNoise0.ar(freq + LFNoise0.ar(freq*2, 10).range(*[freq/20, freq/5]), freq/5).tanh * 0.8 },
			{|freq=20| Mix(SinOsc.ar([freq,freq+5,freq+10,freq+15], 0.5pi)) },
			{|freq=1000| Mix(SinOsc.ar(SinOsc.ar([freq, freq/10]).range(*[freq*2, freq*20]), 0.5pi)) },
			{|freq=50| Mix(SinOsc.ar(SinOsc.ar([freq, freq+1]).range(*[freq/2, freq*2]), 0.5pi)) },
			{|freq=10000| Impulse.ar(1, 100, 10).clip(-0.9, 0.9) + Dust2.ar(freq, 2).tanh },
			{|freq=32| LFSaw.ar(freq, 0.5, LFNoise0.ar(10000).range(10, 100)).distort },
			{|freq=10| Blip.ar(freq, 100, 10).clip(-0.9, 0.9) },
			
			{|freq=32| LFTri.ar(freq, 0, LFNoise0.ar(freq*8).range(freq, freq*2)).clip(-0.9, 0.9).distort },
			{|freq=12288| SinOsc.ar(freq) },
			{|freq=16| Crackle.ar(1.6, 32).softclip },
			{|freq=16384| Logistic.ar(VarSaw.kr(pi**2).range(3.57, 3.8), 2**14) },
			{|freq=200| PMOsc.ar(SinOsc.kr(16).range(freq, freq*2), SinOsc.kr(12).range(freq*8,freq*2)) },
			{|freq=32| Pluck.ar(SinOsc.ar(LFNoise0.ar(999).range(freq, freq * 2), 0, 10), Impulse.kr(2), 0.1, 0.1, 4).tanh },
			{|freq=32| LFSaw.ar(LFNoise0.ar(freq**2).range(freq, freq*2), 0, 10).softclip },
			{|freq=64| Decimator.ar(Impulse.ar(freq, 10, 10).softclip, 48000, 24, 2) }, 
			
			{|freq=10| SineShaper.ar(SinOsc.ar(freq, 0, 10), 0.9) },
			{|freq=20| SineShaper.ar(SinOsc.ar(freq, 0, 10), 0.8) },
			{|freq=20| SineShaper.ar(SinOsc.ar(freq, 0, IRand(freq*10, freq*20)), 0.5) },
			{|freq=8| SineShaper.ar(SinOsc.ar(freq, 0, IRand(freq**4, freq**4*2)), 0.7) },
			{|freq=180| CrossoverDistortion.ar(SinOsc.ar(LFNoise2.ar(1000).range(freq, freq+20).round(10), 0, 2).softclip, 0.4, 0.2) },
			{|freq=50| Disintegrator.ar(SinOsc.ar(LFSaw.ar(20).range(freq, freq*4).round(10), 0, 2).clip, 0.5, 0.5) },
			{|freq=40| Gendy1.ar(2, 2, 1, 1, freq, freq*2) },
			{|freq=16| Gendy1.ar(6, 6, 0.01, 0.01, freq, freq*4, 1, 1, 24, 24) },
			
			{|freq=16| LFSaw.ar(LFSaw.ar(freq).range(*this.roundFreq([pi**pi, pi**pi*2])), LFSaw.ar(15).range(0, 2), LFPulse.ar(16).range(0.5, 1.0)) },
			{|freq=32| StkPluck.ar(freq, 1.0, 10).clip(-0.9, 0.9) },
			{|freq=64| StkSaxofony.ar(freq, 20, 40, XLine.kr(30, 10, 0.2), 10, 16, 10, 64, 1, 64).clip(-0.9, 0.9) },
			{|freq=16| Oregonator.ar(Impulse.kr(freq), 4, 0.5).clip(-0.9, 0.9) },
			{|freq=16| Brusselator.ar(0, 0.5, 2.0).tanh },
			{|freq=16| SpruceBudworm.ar(0,0.1,25.45,1.5,0.5,5.0, initx:0.7, inity: 0.4).tanh },
			{|freq=16000| Mix(MdaPiano.ar(freq,1,127,1,1,1,0,1,1,0.5,0.1,0.5,mul:20).softclip) },
			{|freq=440| (Perlin3.ar(LFSaw.kr(freq/2), SinOsc.ar(freq), LFTri.ar(freq+100))*10).distort },
			
			{|freq=16| CA0.ar(5512, 32, 18, 0) },
			{|freq=16| CA0.ar(11025, 64, 22, 0) },
			{|freq=16| CA0x.ar(11025, 32, 26, 0) },
			{|freq=16| (CA0x.ar(11025, 64, 30, 0)*10).clip(-0.9, 0.9) },
			{|freq=16| CA1.ar(44100/8, 32, 30, 0, 10).softclip },
			{|freq=16| CA1.ar(4410, 64, 73, 0) },
			{|freq=16| CA1x.ar(2205, 32, 105, 0) },
			{|freq=16| CA1x.ar(4410, 64, 105, 0) },
			
			{|freq=440| Logist0.ar((freq * Rand(1, 5)).round(2**(1/12)), 1.8) },
			{|freq=120| CML0.ar(Select.kr(IRand(0, 4), Scale.jiao.ratios * freq), 1.2, 0.05, 1.0) },
			{|freq=880| GCM0.ar(Select.kr(IRand(0, 4), Scale.jiao.ratios * freq), 1.5, 0.01) },
			{|freq=256| HCM0.ar(Select.kr(IRand(0, 4), Scale.jiao.ratios * freq), 1.1, 0.3) },
			{|freq=16| Nagumo.ar(0.01, 0.01, LFPulse.ar(10).range(0, 1)) },
			{|freq=2048| FIS.ar(LFSaw.ar(4).range(1,4),LFNoise0.ar(10).abs,SinOsc.ar(freq).range(1,10).round(1)) },
			{|freq=16| CombN.ar(CA1.ar(800,20,SinOsc.kr(30, 0.5pi).range(30, 60).round(1)),0.2,0.125,0.25) },
			{|freq=800| Mix(GVerb.ar(LPF.ar(Impulse.ar(1),freq,20),5)) },
			
			{|freq=50| Logist0.ar(freq, LFNoise2.kr(4).range(1, 2), 0.01) },
			{|freq=880| CML0.ar(freq, 1.99, 0.01, 0.1) },
			{|freq=440| GCM3.ar(Select.kr(IRand(0, 4), Scale.jiao.ratios * freq), 1.7, 0.1) },
			{|freq=1760| HCM3.ar(Select.kr(IRand(0, 4), Scale.jiao.ratios * freq), 1.99, 0.8) },
			{|freq=16| Nagumo.ar(0.01, 0.001, LFPulse.ar(110).range(0, 1)) },
			{|freq=16| FIS.ar(LFSaw.ar(1).range(1,4),Crackle.ar(1.99).abs,LFSaw.ar(64).range(1,10).round(1)) },
			{|freq=16| Mix(DelayN.ar(CombN.ar(CA1.ar(440,200,165),0.2,0.01,0.2),0.05,(0.01,0.02..0.04))) },
			{|freq=500| Mix(GVerb.ar(HPF.ar(Impulse.ar(1),freq,20),5)) }
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
		SynthDef(name, {|out, efx, dur = 0.1, amp = 1.0, freq = 32.0, emp = 0.0, rotx = 0.0, roty = 0.0, rotz = 0.0| 
			var sig;
			sig = SynthDef.wrap(func, prependArgs: [freq] ) 
				* EnvGen.kr(EnvControl.kr, timeScale: dur, doneAction: 2);
			Out.ar(efx, sig * emp);
			Out.ar(out, FoaTransform.ar(
				FoaEncode.ar(sig * amp, FoaEncoderMatrix.newDirection), 'rtt', rotx, roty, rotz)
			)
		}).add;
	}
	
	makeGepDef{|name, func, nargs = 8|
		SynthDef(name, {|out, efx, dur = 0.1, amp = 1.0, emp = 0.0, rotx = 0.0, roty = 0.0, rotz = 0.0| 
			var sig;
			sig = tanh(LeakDC.ar(SynthDef.wrap(func, prependArgs: ArrayControl.kr('gepargs', nargs, 0.0) ) ) ) * 0.9
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
	
	addPatternGepDef{|name, data, groupsize=4, div=8, sourcenames, subpatterns=0, prefix, protoname|
		patterndefs[name] = SparseGepPattern(name, data, groupsize, div, sourcenames, subpatterns, prefix, this, protoname ? 'argproto')
	}
	
	makeAmpPattern{|sourcename|
		var arr, on = false;
		this.class.sparsePatterns[sourcename].sum.do({|num|
			if (num == 1) { on = on.not } ;
			arr = arr.add(on.asInt)	
		})
		^arr
	}
	
	addGepPatternDef{|name, sourcename, gepname, div=4, rotx=1, roty=1, rotz=1|
		var player, ampPattern;
		player = UGepPlayer(gepname);
		ampPattern = this.makeAmpPattern(sourcename);
		gepdefs[name] = Pdef(name, player.asPmono(group, 'addToHead', Pseq(ampPattern, inf), Pfunc({ this.beatdur / div }), 
			decoder.bus, rotx, roty, rotz	
		));
	}
	
	playGepSynth{|name, gepname, amp=0, rotx=1, roty=1, rotz=1|
		{
			gepsynths[name] = UGepPlayer(gepname);
			Server.default.sync;
			gepsynths[name].play(group, 'addToHead', decoder.bus, amp, rotx, roty, rotz);
		}.fork
	}
	
	setWithPattern{|name, pattern, dur|
		if (patternPlayers[name].notNil) {
			this.stopPattern(name)
		};
		patternPlayers[name] = Pbind(\type, \set, \id, gepsynths[name].synth.nodeID, 
			\args, #[amp], \amp, pattern, \dur, dur).play;
	}
	
	stopPattern{|name|
		patternPlayers[name].stop;
		patternPlayers[name] = nil;
	}
	
	fadeGepSynth{|name, start=0, end=0, time=1, interval=0.1|
		gepsynths[name].fade(start, end, time, interval)
	}
	
	freeGepSynth{|name|
		if (patternPlayers[name].notNil) {
			this.stopPattern(name)
		};
		gepsynths[name].free;
		gepsynths[name] = nil;
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
	
	prepareAudio{
		{
			if (decoder.isRunning.not) {
				decoder.start(efxgroup, \addAfter)
			};
			Server.default.sync;
			listener = Synth.before(decoder.synth, \mfcc, [\in, decoder.bus, \th, -6.dbamp]);
			mfccresp = OSCFunc({|ms|
				graphics.sendSOMVector(ms[3..(ncoef+2)]);
				onsetFunc.(ms[3..(ncoef+2)])
			}, '/mfcc', Server.default.addr ).add;
		}.fork
	}
		
	quit{|quitDecoder=true|
		if (quitDecoder) { decoder.free };
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
		args[\bps] = bps;
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

	makeEfxProto{
		8.do({|x|
			var proto, pname;
			proto = ();
			pname = "r0"++x.asString;
			(0..63).do({|i|
				var name = SparseMatrix.makeDefName(i);
				proto[name] = (
					efx: Pdefn((pname++"e"++i.asString).asSymbol, nofxbus), 
					rotx: rDB.choose.(), roty: rDB.choose.(), rotz: rDB.choose.(), 
					env: envs[['perc00','perc01','perc02','perc03'].choose]
				)
			});
			argproto[pname.asSymbol] = proto;
		});
	}
	
	preparePatternDefs{|data|
		this.addPatternCycleDef('c00', 4, this.buffers.cycles[[1, 2, 7, 14]], 'frag05', "c0");

		this.addPatternSynthDef('r00', 
			sourcenames: ['kpanilogo', 'yole', 'diansa', 'sorsornet'], prefix: "r0", protoname: 'r00');
		
		this.addPatternSynthDef('r01', div: 4, 
			sourcenames: ['diansa', 'liberte', 'macrou'], prefix: "r1", protoname: 'r01');
		
		this.addPatternSynthDef('r02', 
			sourcenames: ['raboday'], subpatterns: 3, prefix: "r2", protoname: 'r02');
		
		this.addPatternSynthDef('r03', 48, 8, 8, ['kpanilogo', 'yole'], 2, "r3", protoname: 'r03');

		this.addPatternSynthDef('r04', 64, 8, 8, ['tiriba', 'foret'], 2, "r4", protoname: 'r04');

		this.addPatternSynthDef('r05', 64, 8, 8, ['basikolo', 'djakandi'], 2, "r5", protoname: 'r05');

		this.addPatternSynthDef('r06', 64, 8, 8, ['doudoumba', 'mandiani'], 3, "r6", protoname: 'r06');

		this.addPatternSynthDef('r07', 64, 8, 4, ['yole'], 3, "r7", protoname: 'r07');
		
		if (data.notNil) 
		{
			this.addPatternGepDef('g00', data.keep(64), 8, 8, ['basikolo', 'diansa'], 3, "g00", 'r00' );
			this.addPatternGepDef('g01', data[(64..127)], 8, 8, ['kokou', 'macrou'], 3, "g01", 'r01' );
			this.addPatternGepDef('g02', data[(128..191)], 8, 8, ['mandiani', 'djakandi'], 3, "g02", 'r02' );
			this.addPatternGepDef('g03', data.drop(192), 8, 8, ['yole', 'tiriba'], 3, "g03", 'r03' );
		};

		this.addPatternBufferDef('b00',
			size: 32, groupsize: 4, div: 1,
			sourcenames: ['kokou', 'soli', 'macrou'],
			prefix: "b0", protoname: 'fragproto00',
			buffers: this.buffers.frags, defname: 'frag04'
		);
		
		this.addPatternBufferDef('b01',
			size: 32, groupsize: 4, div: 4,
			sourcenames: ['mandiani', 'kakilambe', 'basikolo'],
			prefix: "b1", protoname: 'fragproto01',
			buffers: ~matrix.buffers.frags.drop(28).reverse, defname: 'frag04'
		);
		
		this.addPatternBufferDef('b02', 
			size: 64, groupsize: 8,
			sourcenames: ['kpanilogo', 'yole', 'diansa', 'kokou', 'kakilambe', 'soli', 'mandiani'], 
			prefix: "b2", protoname: 'argproto',
			buffers: this.buffers.bits, defname: 'bit01'
		);
		
		this.addPatternBufferDef('b03', 
			size: 32, groupsize: 4, div: 4,
			sourcenames: ['sokou', 'cassa'], 
			subpatterns: 1, protoname: 'argproto',
			prefix: "b3", buffers: this.buffers.bits.keep(32), defname: 'bit00'
		);
		
		this.addPatternBufferDef('b04', 
			size: 32, groupsize: 4, div: 4,
			sourcenames: ['rumba', 'liberte'], 
			subpatterns: 1,
			prefix: "b4", buffers: this.buffers.bits.drop(32), defname: 'bit00'
		);
		
	}
	
	patternKeys{ ^patterndefs.keys(Array) }
	
	collectPatternKeys{|names|
		if (names.isNil) {
			names = this.patternKeys;
		};
		^names.collect({|name| Pdef(name.asSymbol) }) 
	}
	
}

SparseMatrixPattern{
		
	var <name, <size, <groupsize, div, prefix, matrix, sourcenames, subpatterns, protoname, <patterns, <args, <groups, <ctrls;
	var <previousStates, maxStateSize = 4;
	
	*new{|name, size, groupsize, div, prefix, matrix, sourcenames, subpatterns, protoname|
		^super.newCopyArgs(name, size, groupsize, div, prefix, matrix, sourcenames, subpatterns, protoname).init
	}
	
	init{
		previousStates = Array.newClear(maxStateSize);
	}
	
	setControls{|onFunc, ampFunc, durFunc, empFunc, names|
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
			ctr.emp = empFunc.();
		})
	}
	
	setActives{|ampFunc, durFunc, empFunc|
		this.saveCurrentState;
		
		ctrls.select({|ctr| ctr.active.booleanValue }).do({|ctr|
			ctr.amp = ampFunc.();
			ctr.dur = durFunc.();
			ctr.emp = empFunc.();
		})
	}
	
	setGroups{|indices, onFunc, ampFunc, durFunc, empFunc|
		
		this.saveCurrentState;
		
		groups[indices].flat.do({|name|
			ctrls[name].active = onFunc.();
			ctrls[name].amp = ampFunc.();
			ctrls[name].dur = durFunc.();
			ctrls[name].emp = empFunc.();
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
	
	assignEfx{|assignments|
		assignments.keysValuesDo({|efx, indices|
			indices.do({|ind| 
				Pdefn((name++"e"++ind.asString).asSymbol, matrix.efx[efx].args.in)
			})
		})
	}
	
	savePreset{|name|
		var key;
		if (name.isNil)
		{
			key = (Date.localtime.stamp).asSymbol;
		}
		{
			key = name.asSymbol;
		};
		if (Archive.global[\matrix].isNil)
		{
			Archive.global[\matrix] = ()
		};

		if (Archive.global[\matrix][\presets].isNil)
		{
			Archive.global[\matrix][\presets] = ()
		};
		
		if (Archive.global[\presets][\presets][name].isNil)
		{
			Archive.global[\presets][\presets][name] = ()
		};
		
		Archive.global[\matrix][\presets][name].put(key, ctrls)
		
	}
	
	loadPreset{|key|
		ctrls = Archive.global[\matrix][\presets][name][key]
	}
	
	mergePatterns{
		var merged = Array();
		sourcenames.flat.do({|name|
			var sub;
			merged = merged ++ SparseMatrix.sparsePatterns[name];
			if (subpatterns > 0) {
				sub = SparseMatrix.sparseObjects[name].makeSubPatterns(subpatterns).subpatterns;
				sub.do({|subpat|
					merged = merged ++ subpat
				})
			}
		});
		^merged
	}
	
	makePatterns{|merged|
		patterns = ();
		groups = Array();
		
		merged.keep(size).do({|seq, i|
			var key;
			key = SparseMatrix.makeDefName(i, prefix);
			patterns[key] = seq;
			groups = groups.add(key);
		});	
		
		groups = groups.clump(groupsize);
	
	}
	
	makeControls{
		ctrls = patterns.collect({  (active: 0, amp: 0, emp: 0, dur: rrand(0.01, 0.1)) });
	}
}

SparseSynthPattern : SparseMatrixPattern{
	
	classvar <>scale;
		
	*new{|name, size, groupsize, div, sourcenames, subpatterns, prefix, matrix, protoname|
		^super.new(name, size, groupsize, div, prefix, matrix, sourcenames, subpatterns, protoname).makePdef
	}
	
	makePdef{
		var instr, argproto;
		var combined;
		if (this.class.scale.isNil) { scale = Scale.jiao };
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
		
		ctrls = patterns.collect({  (active: 0, amp: 0, emp: 0, dur: rrand(0.01, 0.1)) });
		
		argproto = ();
		
		matrix.argproto[protoname].keys(Array).sort.do({|key|
			argproto[key.asString.replace("p", prefix).asSymbol] = matrix.argproto[protoname][key]
		});
		
		args = patterns.collect({|pat, key| argproto[key] ? argproto[\default]; });

		instr = ().putPairs(args.size.collect({|i| [SparseMatrix.makeDefName(i, prefix), SparseMatrix.makeDefName(i, "d")]  }).flat);
		
		Pdef(name, Ppar(
			args.collect({|args, key|  
				var defindex, freq;
				defindex = instr[key].asString.drop(1).asInteger;
				freq = matrix.deffuncs[defindex].def.makeEnvirFromArgs[\freq];
				freq = this.class.scale.performNearestInScale(freq.cpsmidi).midicps;
				Pbind(\instrument, instr[key], \group, matrix.group, \addAction, \addToHead, \delta, Pfunc({ matrix.beatdur / div }), 
					\amp, Pfunc({ ctrls[key].amp }), \emp, Pfunc({ ctrls[key].emp }), \out, matrix.decoder.bus, \freq, freq,
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
		^super.new(name, size, groupsize, div, prefix, matrix, sourcenames, subpatterns, protoname).makePdef(buffers, defname)
	}
	
	makePdef{|bufs, defname|
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
		
		ctrls = patterns.collect({  (active: 0, amp: 0, emp: 0, dur: rrand(0.01, 0.1)) });
		
		argproto = ();
		
		matrix.argproto[protoname].keysValuesDo({|key, val|
			argproto[key.asString.replace("p", prefix).asSymbol] = val
		});
		
		args = patterns.collect({|pat, key| argproto[key] ? argproto[\default]; });
		
		Pdef(name, Ppar(
			args.collect({|args, key|  
				Pbind(\instrument, defname, \group, matrix.group, \addAction, \addToHead, 
					\delta, Pfunc({ matrix.beatdur / div }), \emp, Pfunc({ ctrls[key].emp }),
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
				ctrls[key] = ( active: 0, amp: 0, emp: 0, buf: buf );
				Pbind(
					\instrument, defname, \group, matrix.group, \addAction, \addToHead,
					\buf, Pfunc({ ctrls[key].buf }), \amp, Pfunc({ ctrls[key].amp }),
					\emp, Pfunc({ ctrls[key].emp }),
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

SparseGepPattern : SparseMatrixPattern {
	
	var gepdata;
	
	*new{|name, gepdata, groupsize, div, sourcenames, subpatterns, prefix, matrix, protoname|
		^super.new(name, gepdata.size, groupsize, div, prefix, matrix, sourcenames, subpatterns,  protoname ).makePdef(gepdata)
	}
		
	makePdef{|data|
		var instr, argproto, gepargs;
		var combined;
		gepdata = data;
		combined = this.mergePatterns;
		
		this.makePatterns(combined);
		
		this.makeControls;
		
		argproto = ();
		
		matrix.argproto[protoname].keys(Array).sort.do({|key|
			argproto[key.asString.replace("p", prefix).asSymbol] = matrix.argproto[protoname][key]
		});
		
		args = patterns.collect({|pat, key| argproto[key] ? argproto[\default]; });
		
		instr = ();
		gepargs = ();
		
		Routine({
		
			gepdata.do({|gepitem, i|
				var key = SparseMatrix.makeDefName(i, prefix);
				instr[key] = gepitem.defname;
				gepargs[key] = gepitem.args.bubble;
				this.addGepSynthDef(gepitem);
			});
			
			Pdef(name, Ppar(
				args.collect({|args, key|  
					var defindex, freq;
					defindex = instr[key].asString.drop(1).asInteger;
					Pbind(
						\instrument, instr[key], \group, matrix.group, \addAction, \addToHead, 
						\delta, Pfunc({ matrix.beatdur / div }), 
						\amp, Pfunc({ ctrls[key].amp }), \emp, Pfunc({ ctrls[key].emp }), \out, matrix.decoder.bus,
						\dur, Pfunc({ ctrls[key].dur }), \pat, matrix.makePattern(key, patterns[key].bubble),
						\type, Pfunc({|ev| if (ctrls[key].active.booleanValue) { ev.pat } { \rest } }), 
						\gepargs, gepargs[key], *args.asKeyValuePairs
					)
				}).values
			)).quant(64);	
				
		}).play
	}
	
	addGepSynthDef{|dataitem|
		var fnc, chrom, str;
		chrom = GEPChromosome(dataitem.code, dataitem.terminals, dataitem.header.numgenes, dataitem.linker);
		str = chrom.asUgenExpressionTree.asFunctionString;
		fnc = str.interpret;
		{
			matrix.makeGepDef(name, fnc, dataitem.terminals.size);
		}.try({
			Post << "ERROR: " << str << Char.nl;
		})
		
	}
}
