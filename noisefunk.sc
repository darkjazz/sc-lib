Noisefunk {

	classvar <>bufferdefpath, <>funcdefpath, <defpath, <skismdefpath;

	var decoderType, action, decoder, <buffers, <deffuncs, <skismDefs, <group, <efxgroup, <nofxbus;
	var <patterndefs, <gepdefs, <gepsynths, <skismSynths, <patternPlayers, <envs, <rDB, <argproto;
	var <bpm, <bps, <beatdur;

	*initClass{
		bufferdefpath = Paths.noisefunkDir +/+ "buf/bufferdefs_00.scd";
		funcdefpath = Paths.noisefunkDir +/+ "def/deffuncs01.scd";
		defpath = Paths.noisefunkDir +/+ "def/deffuncs01.scd";
		skismdefpath = Paths.noisefunkDir +/+ "def/skismdefs.scd";
	}

	*new{|decoderType='stereo', action|
		^super.newCopyArgs(decoderType).init(action)
	}

	init{|action|

		Tdef('initNoiseFunk', {
			decoder = currentEnvironment['decoder'] = FoaDecoder(decoderType: decoderType);
			Server.default.sync;
			this.loadBuffers;
			this.class.defpath.load;
			Server.default.sync;
			this.loadSkismDefs;
			Server.default.sync;
			this.loadPresets;
			SparseMatrix.makeSparsePatterns(1);
			patterndefs = ();
			skismSynths = ();
			this.setBPM(141);
			"sparsematrix performance ready...".postln;
			action.(this);
		}).play

	}

	loadBuffers{
		var pathdict = this.class.bufferdefpath.load;
		buffers = ();
		pathdict.keysValuesDo({|key, path|
			buffers[key] = path.pathMatch.collect({|path|
				Buffer.read(Server.default, path)
			})
		});
		Server.default.sync;
		Post << "noisefunk buffers loaded.." << Char.nl;
	}

	loadDefFuncs{
		deffuncs = this.class.funcdefpath.load;
		deffuncs.collect({|fnc, i|
			this.makeDef(this.class.makeDefName(i, "d"), fnc)
		});
	}

	loadSkismDefs{
		skismDefs = skismdefpath.load;
		skismDefs.do(_.add);
	}

	setBPM{|newbpm|
		bpm = newbpm;
		bps = bpm / 60;
		beatdur = bps.reciprocal;
	}

	prepareAudio{
		Tdef('prepareAudio', {
			group = Group.before(decoder.synth);
			Server.default.sync;
			efxgroup = Group.after(group);
			nofxbus = Server.default.options.numAudioBusChannels-1;
			if (decoder.isRunning.not) {
				decoder.start(efxgroup, \addAfter)
			};
		}).play
	}

	loadPresets{
		envs = (
			sine00: Env.sine,
			sine01: Env([0, 1, 1, 0], [0.2, 0.6, 0.2], \sine),
			sine02: Env([0, 1, 1, 0], [0.3, 0.4, 0.3], \sine),
			perc00: Env.perc,
			perc01: Env.perc(0.1, 0.9, 1, 4),
			perc02: Env.perc(0.005, 1, 1, -16),
			perc03: Env.perc(curve: 0),
			perc04: Env([0.001, 1.0], [1], 4),
			lin00: Env([0, 1, 1, 0], [0.3, 0.4, 0.3]),
			lin01: Env([0, 1, 1, 0], [0, 1, 0]),
			step00: Env([0, 1, 0.3, 0.8, 0], (1/4!4), \step),
			step01: Env([0, 1, 0.5, 1, 0.5, 1, 0], (1/6!6), \step),
			wlch00: Env([0.001, 0.5, 0.4, 1.0, 0.001], [0.2, 0.3, 0.3, 0.2], \welch),
			wlch01: Env([0.001, 1, 0.3, 0.8, 0.001], [0.3, 0.3, 0.1, 0.3], \welch),
			gate00: Env([0, 1, 1, 0], [0, 1, 0], \lin, 2, 1),
			exp00: Env([0.001, 1.0, 1.0, 0.001], [0.1, 0.8, 0.1], \exp),
			exp01: Env([0.001, 1.0, 0.001], [0.05, 0.95], \exp),
			exp02: Env([0.001, 1.0, 1.0, 0.001], [0.2, 0.6, 0.2], \exp),
			exp03: Env([0.001, 1.0, 0.001], [0.9, 0.1], \exp),
			default: Env([1, 1, 0], [1, 0])
		).collect(_.asArray).collect(_.bubble);

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

		argproto['bufproto'] = argproto['argproto'].collect({|args|
			args.collect(_.())
			.putPairs((rate: Array.geom(7, 0.25, 2**(1/5)).choose).asKeyValuePairs)
		});

	}

}