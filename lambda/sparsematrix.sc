SparseMatrix{

	classvar <>allPatterns, <>patterns12, <>patterns16, <>sparseObjects, <>sparsePatterns;
	classvar <>funcdefpath, <>patternDb, <>patternsDub, <>patternsDnB;

	var <decoder, <>graphics, <quant, <ncoef, <envs, <buffers, <>group, <>efxgroup, <nofxbus, <bpm, <bps, <beatdur;
	var <rDB, <efx, <efxamps, <patterndefs, <argproto, <deffuncs, codewindow, <listener, <mfccresp;
	var <skismDefs, <skismSynths, grainEnvs, <gepdefs, <gepsynths, <>onsetFunc;
	var <patternPlayers, <ampctrls;

	var <>defpath, <>skismDefPath, <>bufferPath, isonhold;

	var <>eventLibName = "lib003", eventData, <freqSet, <durSet, <ampSet;

	*initClass{
		funcdefpath = Paths.devdir +/+ "lambda/supercollider/sparsematrix/deffuncs01.scd";
		patternDb = "rhythm_patterns";
	}

	*new{|decoder, graphics, quant=2, ncoef=8, action|
		^super.newCopyArgs(decoder, graphics, quant, ncoef).init(action)
	}

	*makeSparsePatterns{|quant|
		var dubs, dnb, loader = PatternReader(Noisefunk.pattern_db);
		SparseMatrix.allPatterns = loader.loadAll;
		// SparseMatrix.allPatterns = DjembeLib.convertAll(quant);
		SparseMatrix.patterns12 = SparseMatrix.allPatterns.select({|pat|
			(pat.first.size / quant) % 6 == 0
		});
		SparseMatrix.patterns16 = SparseMatrix.allPatterns.select({|pat, name|
			SparseMatrix.patterns12.keys.includes(name).not
		});
		// add a time step to have prime number steps
		SparseMatrix.patterns12 = SparseMatrix.patterns12.collect({|pat, key|
			pat.collect({|instr| instr  })
		});
		SparseMatrix.patterns16 = SparseMatrix.patterns16.collect({|pat, key|
			pat.collect({|instr| instr  })
		});
		SparseMatrix.sparseObjects = SparseMatrix.allPatterns.collect(
			SparsePattern(_)).collect(_.makeSparse
		);
		SparseMatrix.sparsePatterns = SparseMatrix.sparseObjects.collect(_.patterns);
		SparseMatrix.patterns16.collect(_.size).postln;
		SparseMatrix.patterns16.size.postln;
		"-----".postln;
		SparseMatrix.patterns12.collect(_.size).postln;
		SparseMatrix.patterns12.size.postln;

		dubs = loader.loadDub;
		SparseMatrix.patternsDub = dubs.collect({|pat|
			pat.collect({|arr|  arr.collect({|num| if (num > 0) { 1 } { 0 }  }) });

		});

		SparseMatrix.patternsDub.keysValuesDo({|key, val|
			SparseMatrix.sparseObjects[key] = SparsePattern(val);
			SparseMatrix.sparseObjects[key].patterns = val;
			SparseMatrix.sparsePatterns[key] = val;
		});

		dnb = loader.loadDnB;
		SparseMatrix.patternsDnB = dnb.collect({|pat|
			pat.collect({|arr|  arr.collect({|num| if (num > 0) { 1 } { 0 }  }) });

		});

		SparseMatrix.patternsDnB.keysValuesDo({|key, val|
			SparseMatrix.sparseObjects[key] = SparsePattern(val);
			SparseMatrix.sparseObjects[key].patterns = val;
			SparseMatrix.sparsePatterns[key] = val;
		});

		SparseSynthPattern.makePrimeFreqs;
	}

	init{|action|
		defpath = Paths.matrixdefs;
		skismDefPath = Paths.skismdefs;
		bufferPath = Paths.matrixbufs;
		isonhold = false;
		{
			if (this.class.allPatterns.isNil) {
				this.class.makeSparsePatterns(quant)
			};

			1.wait;

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

			this.loadDefFuncs;

			defpath.load;
			this.loadSkismDefs;

			Server.default.sync;

			if (decoder.isRunning) {
				group = Group.before(decoder.synth)
			}
			{
				group = Group();
			};
			efxgroup = Group.after(group);
			nofxbus = Server.default.options.numAudioBusChannels-1;

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
				efx1: ( def: \rev01, args: (room: 80, rtime: 1.5, damp: 0.5, bw: 0.5,
					spr: 15, dry: 0, early: 1.0, tail: 0.7)
				),
				efx2: ( def: \rev02, args: (room: 20, rtime: 6,
					wsz: Prand([0.02,0.06,0.1,0.2],inf),
					pch:Prand(Array.geom(24,1.0,2**(1/24)).reverse.keep(10),inf),
					pds: Pwrand([0,Pwhite(0.1, 0.5, 1)],[0.7, 0.3],inf),
					tds: Pwrand([0,Pwhite(0.1, 0.5, 1)],[0.7, 0.3],inf) )
				),
				efx3: (def: \del00, args: (del: Pfunc({ beatdur / 2 }), dec: Pfunc({ beatdur * 4 }),
					pch: Pwrand(Array.geom(24, 0.5, 2**(3/24))[(0..23).select(_.isPrime)],
						Array.geom(9, 1.0, 2**(1/9)).normalizeSum, inf ),
					room: 8, rtime: 4, ramp: 0.2 )
				),
				efx4: (def: \del01, args: (del: Pfunc({ beatdur / 2 }), dec: Pfunc({ beatdur * 4 }) )),
				efx5: ( def: \del02, args: ( rtime: 10, del: Pfunc({ beatdur }), dec: Pfunc({ beatdur }) ))
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

			argproto['bufproto'] = argproto['argproto'].collect({|args|
				args.collect(_.())
				.putPairs((rate: Prand([-1, 1], inf)).asKeyValuePairs)
			});

			patterndefs = ();

			gepdefs = ();

			gepsynths = ();

			this.setBPM(120);

			skismSynths = ();

			patternPlayers = ();

			ampctrls = ();

			"sparsematrix performance ready...".postln;

			action.(this);

		}.fork

	}

	loadSkismDefs{|loadPath|
		if (loadPath.isNil) {
			skismDefs = skismDefPath.load;
		}
		{
			skismDefs = loadPath.load;
		};
		skismDefs.do(_.add);
	}

	loadDefFuncs{
		deffuncs = this.class.funcdefpath.load;
		deffuncs.collect({|fnc, i|
			this.makeDef(this.class.makeDefName(i, "d"), fnc)
		});
	}

	loadBuffers{
		buffers = ();

		buffers.frags = (Paths.matrixbufs +/+ "frag*").pathMatch
			.collect({|path| Buffer.read(Server.default, path) });
		buffers.bits = (Paths.matrixbufs +/+ "bit*").pathMatch
			.collect({|path| Buffer.read(Server.default, path) });
		buffers.cycles = (Paths.matrixbufs +/+ "cycle*").pathMatch
		    .collect({|path| Buffer.read(Server.default, path) });
		buffers.evo = (Paths.soundDir +/+ "evolver/dub/beats/*").pathMatch
		    .collect({|path| Buffer.read(Server.default, path) });
		buffers.dnb = (Paths.soundDir +/+ "dnb_samples/*").pathMatch
			.collect({|path| Buffer.read(Server.default, path) });
		buffers.lctr = (Paths.soundDir +/+ "lctrnc_loops/*").pathMatch
		    .collect({|path| Buffer.read(Server.default, path) });
		// buffers.evo = (Paths.matrixbufs +/+ "ges/*").pathMatch
		// .collect({|path| Buffer.read(Server.default, path) });
		// buffers.msk = (Paths.matrixbufs +/+ "msk*").pathMatch
		// .collect({|path| Buffer.read(Server.default, path) });
		// buffers.boc = (Paths.soundDir +/+ "bocca/samples/*").pathMatch
		// .collect({|path| Buffer.read(Server.default, path) });
		Server.default.sync;
		Post << "matrix buffers loaded.." << Char.nl;
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
			sig = SynthDef.wrap(func, prependArgs: ArrayControl.kr('gepargs', nargs, 0.0) )
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

	addPatternSynthDef{|name, indices, groupsize=4, div=8, sourcenames, subpatterns=0, prefix, protoname, append=false|
		patterndefs[name] = SparseSynthPattern(name, indices, groupsize, div, sourcenames, subpatterns,
			prefix, this, protoname ? 'argproto', append)
	}

	addPatternBufferDef{|name, indices, groupsize=4, div=8, sourcenames, subpatterns=0, prefix, protoname, buffers, defname, append=false|
		patterndefs[name] = SparseBufferPattern(name, indices, groupsize, div, sourcenames, subpatterns,
			prefix, this, protoname ? 'argproto', buffers, defname, append)
	}

	addPatternDubDef{|name, indices, groupsize=4, div=8, sourcenames, subpatterns=0, prefix, protoname, buffers, defname, append=false|
		patterndefs[name] = SparseDubPattern(name, indices, groupsize, div, sourcenames, subpatterns,
			prefix, this, protoname ? 'argproto', buffers, defname, append)
	}

	addPatternCycleDef{|name, size, buffers, defname, prefix|
		patterndefs[name] = SparseCyclePattern(name, size, buffers, defname, prefix, this)
	}

	addPatternGepDef{|name, groupsize=4, div=8, sourcenames, subpatterns=0, prefix, protoname, append, defnames, loader|
		patterndefs[name] = SparseJGepPattern(name, groupsize, div, sourcenames, subpatterns, prefix, this,
			protoname ? 'argproto', append, defnames, loader)
	}

	addPatternMelodyDef{|name, indices, groupsize=4, div=8, sourcenames, subpatterns=0, prefix, protoname, append, defname, melody|
		patterndefs[name] = SparseMelodyPattern(name, indices, groupsize, div, sourcenames, subpatterns, prefix, this, protoname ? 'argproto', append, defname, melody);
	}

	addPatternChordDef{|name, groupsize, div, prefix, protoname, defname, freqs, inds, amp, dur|
		patterndefs[name] = SparseChordPattern(name, groupsize, div, prefix, this, protoname, defname, freqs, inds, amp, dur)
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

	assignCodeWindow{|document,prompt="@ "|
		var sendarray;
		if (document.isKindOf(Document).not) {
			codewindow = document ? Document("---sparsematrix---")
		}
		{
			codewindow = document
		};
		codewindow.keyDownAction = {|doc, char, mod, uni, key|
			if ((uni == 13) and: { key == 36 })
			{
				sendarray = doc.selectedString.split(Char.nl);
				sendarray[0] = prompt ++ sendarray[0];
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

	activateSkismSynth{|name, bus|
		var def, args, ctrbus;
		Post << "activating " << name << Char.nl;
		def = skismDefs.select({|sdf| sdf.name.asString == name.asString }).first;
		args = ();
		def.metadata.specs.keysValuesDo({|argname, argvalue|
			args[argname] = argvalue.default
		});
		args[\out] = decoder.bus;
		args[\in] = bus ? decoder.bus;
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

/*	activateBoccaSynth{|name, bus|
		var def, args, ctrbus;
		Post << "activating " << name << Char.nl;
		def = skismDefs.select({|sdf| sdf.name.asString == name.asString }).first;
		args = ();
		def.metadata.specs.keysValuesDo({|argname, argvalue|
			args[argname] = argvalue.default
		});
		args[\out] = decoder.bus;
		args[\in] = bus;
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

	}*/

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
					efx: nofxbus, //Pdefn((pname++"e"++i.asString).asSymbol, nofxbus),
					rotx: rDB.choose.(), roty: rDB.choose.(), rotz: rDB.choose.(),
					env: envs[['perc00','perc01','perc02','perc03'].choose]
				)
			});
			argproto[pname.asSymbol] = proto;
		});
		6.do({|x|
			var proto, pname;
			proto = ();
			pname = "g0"++x.asString;
			(0..63).do({|i|
				var name = SparseMatrix.makeDefName(i);
				proto[name] = (
					efx: nofxbus,
					rotx: rDB.choose.(), roty: rDB.choose.(), rotz: rDB.choose.(),
					env: envs[['exp00', 'exp01', 'exp02', 'exp03'].choose]
				)
			});
			argproto[pname.asSymbol] = proto;
		});

	}

	preparePatternDefs{
		this.makeEfxProto;

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
			buffers: this.buffers.frags.drop(28).reverse, defname: 'frag04'
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

	loadPatternDefs{|path|
		this.makeEfxProto;
		(Paths.devdir +/+ "lambda/supercollider/sparsematrix" +/+ path).load.(this)
	}

	prepareGepDefs{|defnames, loader|
		this.addPatternGepDef('g00', 8, 8, ['mandiani', 'diansa'], 3, "g00", 'g00', false,
			defnames.keep(64), loader );
		this.addPatternGepDef('g01', 8, 8, ['kokou', 'macrou'], 3, "g01", 'g01', false,
			defnames[(64..127)], loader );

		this.addPatternGepDef('g02', 8, 4, ['kokou', 'diansa', 'macrou', 'yole'], 1, "g02",
			'g02', true, defnames[(128..159)], loader );
		this.addPatternGepDef('g03', 8, 4, ['cassa', 'raboday', 'kpanilogo', 'rumba'], 1, "g03",
			'g03', true,
		defnames[(160..191)], loader );

		// this.addPatternGepDef('g04', 8, 8, ['koukou', 'cassa', 'doudoumba', 'kakilambe'], 1,
		// "g04", 'g04', true, defnames[(192..223)], loader );
		// this.addPatternGepDef('g05', 8, 4, ['sorsornet', 'soli', 'rumba', 'foret'], 1,
		// "g05", 'g05', true, defnames[(224..255)], loader );

	}

	patternKeys{ ^patterndefs.keys(Array) }

	collectPatternKeys{|names|
		if (names.isNil) {
			names = this.patternKeys;
		};
		^names.collect({|name| Pdef(name.asSymbol) })
	}

	loadEventAnalysis{

		var doneFunc;

		doneFunc = {
			var min, max;
			eventData.datalib.collect({arg set;
				set.events.collect(_.frqs).collect(_.at(1))
			}).values.asArray.flat.slide(freqSet.order+1, 1).clump(freqSet.order+1).do({arg freqs;
				freqSet.read(freqs.keep(freqSet.order).collect(_.roundFreq).round(0.01),
					freqs.last.roundFreq.round(0.01))
			});

			min = beatdur/8;
			max = eventData.datalib.collect({arg set;
				set.events.collect(_.duration)
			}).values.asArray.flat.maxItem;

			eventData.datalib.collect({arg set;
				set.events.collect(_.duration)
			}).values.asArray.flat.linlin(min, max, min, beatdur*8).round(min).doAdjacentPairs({arg durA, durB;
				durSet.read(durA, durB)
			});

			eventData.datalib.collect({arg set;
				set.events.collect(_.peakAmp)
			}).values.asArray.flat.slide(ampSet.order+1, 1).clump(ampSet.order+1).do({arg amps;
				ampSet.read(amps.keep(ampSet.order).round(0.01), amps.last.round(0.01))
			});

		};

		if (eventData.isNil)
		{
			MikroData.loadPath = Paths.eventLibDir +/+ eventLibName +/+ "";
			eventData = MikroData().loadPathMatch(doneAction: doneFunc);
			freqSet = MarkovSetN(order: 2);
			durSet = MarkovSetN(order: 1);
			ampSet = MarkovSetN(order: 2);
		}
	}

	makeFreqSeq{|size=64|
		var freqs, key = freqSet.dict.keys(Array).choose;

		freqs = Array.fill(size, {
			var arr, frq = freqSet.next(key);
			arr = key.asString.interpret;
			arr.removeAt(0);
			arr = arr.add(frq);
			key = arr.asSymbol;
			frq
		});

		freqs.postln;

		^freqs
	}

	makeDurSeq{|size=64|
		var durs, key = durSet.dict.keys(Array).choose;

		durs = Array.fill(size, {
			key = durSet.next(key);
		});

		durs.postln;

		^durs

	}

	makeAmpSeq{|size=64|
		var amps, key = ampSet.dict.keys(Array).choose;

		amps = Array.fill(size, {
			var arr, amp = ampSet.next(key);
			arr = key.asString.interpret;
			arr.removeAt(0);
			arr = arr.add(amp);
			key = arr.asSymbol;
			amp
		});

		amps.postln;

		^amps
	}

	makeMelody{|name, defname, size, efxname, noteweight=1.0|

		if (ampctrls.includesKey(name).not)
		{
			ampctrls[name] = 0.0
		};

		Pdef(name,
			Pbind(\instrument, defname, \out, decoder.bus, \efx, efx[efxname].args.in, \group, group, \addAction, \addToHead,
				\amp, Pseq((this.makeAmpSeq(size) ** 0.5) * 2, inf) * Pfunc({ ampctrls[name] }), \emp, Pkey(\amp),
				\dur, Pseq(this.makeDurSeq(size) * 2, inf),
				\freq, Pseq(this.makeFreqSeq(size).clump(8).collect({|freqs| Pseq(freqs, 4) }), inf),
				\freqs, (1..4).bubble, \bw, (1 ! 4).bubble, \amps, (Array.geom(4, 1.0, 0.9)).bubble,
				\rotx, rDB[rDB.keys(Array).choose].(), \roty, rDB[rDB.keys(Array).choose].(),
				\rotz, rDB[rDB.keys(Array).choose].(), \type, Pwrand([\note, \rest], [noteweight, 1.0-noteweight], inf),
				\envindex, Pstutter(Pseq([2, 4], inf), Pseq(['perc00', 'perc04', 'sine00', 'lin00'], inf)),
				\env, Pfunc({|ev| envs.at(ev['envindex']) })
			)
		)
	}

	set{|params, names, pct=0.1|
		names.do({|name|
			this.defsAt(name).saveCurrentState;
			params.do({|param|
				this.defsAt(name).ctrls.select({|ctr| ctr.active.booleanValue }).do({|ctr|
					ctr[param] = ctr[param] * pct;
				})
			})
		})
	}

	kill{|names|
		names.do({|name|
			this.defsAt(name).setControls(0, nil, nil, nil, names);
		})
	}

	undo{|names|
		names.do({|name|
			this.defsAt(name).recall(0);
		})
	}


}

SparseMatrixPattern{

	classvar <>twinPrimes, <>usePrimes = false, <>useTwinPrimes = false;

	var <name, <indices, <groupsize, div, prefix, matrix, sourcenames, subpatterns, protoname, appendSubPatterns;
	var <patterns, <args, <groups, <ctrls;
	var <previousStates, maxStateSize = 4, <size;

	*new{|name, indices, groupsize, div, prefix, matrix, sourcenames, subpatterns, protoname, append=false|
		^super.newCopyArgs(name, indices, groupsize, div, prefix, matrix, sourcenames,
			subpatterns, protoname, append).init
	}

	init{
		previousStates = Array.newClear(maxStateSize);
		size = indices.size;
	}

	*makePrimeFreqs{|offset=13|
		var number, allPrimes;
		number = offset;
		allPrimes = Array();
		while ({ number < 20000 }, {
			allPrimes = allPrimes.add(number);
			number = (number + 1).asInt.nextPrime;
		});

		twinPrimes = Array();

		allPrimes.doAdjacentPairs({|x, y|
			if (y - x == 2) {
				twinPrimes = twinPrimes.addAll([x, y]);
			}
		});

	}

	findNearestTwinPrime{|freq|
		var diff, index;
		diff = (freq - this.class.twinPrimes).abs;
		index = diff.indexOf(diff.minItem);
		^this.class.twinPrimes[index];
	}

	setControl{|name, active, amp, dur, emp|
		ctrls[name].active = active;
		ctrls[name].amp = amp;
		ctrls[name].dur = dur;
		ctrls[name].emp = emp;
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
			if (ampFunc.notNil) { ctr.amp = ampFunc.() };
			if (durFunc.notNil) { ctr.dur = durFunc.() };
			if (empFunc.notNil) { ctr.emp = empFunc.() };
		})
	}

	setAmpDev{|pct, pat|
		if (pat.isNil) {
			ctrls.do({|ctr| ctr.ampdev = pct })
		}
		{
			ctrls[pat].ampdev = pct
		}
	}

	setDurDev{|pct, pat|
		if (pat.isNil) {
			ctrls.do({|ctr| ctr.durdev = pct })
		}
		{
			ctrls[pat].durdev = pct
		}
	}

	activate{|onFunc|
		this.saveCurrentState;

		ctrls.do({|ctr|
			ctr.active = onFunc.();
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

	setEfx{|index, efx|
		if (efx.isKindOf(Array).not) { efx = efx.bubble };
		if (efx.size == 0) { matrix.nofxbus.bubble };
		Pdefn((SparseMatrix.makeDefName(index, prefix)++"efx").asSymbol,
			efx.collect({|efxkey| matrix.efx[efxkey].args.in})
		)
	}

	assignEfx{|assignments|
		assignments.keysValuesDo({|efx, indices|
			indices.do({|ind| this.setEfx(ind, efx) })
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
		if (appendSubPatterns)
		{
			merged = this.appendSubPatterns
		}
		{
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
		}
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

	appendSubPatterns{
		var appendedPatterns = Array();
		sourcenames.do({|sourcename|
			SparseMatrix.sparseObjects[sourcename].makeSubPatterns(subpatterns);
			SparseMatrix.sparseObjects[sourcename].appendSubPatterns;
			appendedPatterns = appendedPatterns.addAll(SparseMatrix.sparseObjects[sourcename].appendedPatterns.copy);
		});
		^appendedPatterns;
	}

	makeControls{
		ctrls = patterns.collect({  (active: 0, amp: 0, emp: 0, dur: rrand(0.01, 0.1), ampdev: 0, durdev: 0) });
	}

	calculateAmp{|key|
		^ctrls[key].amp + (ctrls[key].amp * ctrls[key].ampdev.rand * [-1, 1].choose)
	}

	calculateDur{|key|
		^ctrls[key].dur + (ctrls[key].dur * ctrls[key].durdev.rand * [-1, 1].choose)
	}
}

SparseSynthPattern : SparseMatrixPattern{

	classvar <>scale;

	*new{|name, indices, groupsize, div, sourcenames, subpatterns, prefix, matrix, protoname, append=false|
		^super.new(name, indices, groupsize, div, prefix, matrix, sourcenames, subpatterns, protoname, append).makePdef
	}

	makePdef{
		var instr, argproto;
		var combined;
		if (this.class.scale.isNil) { scale = Scale.chromatic24 };
		patterns = ();
		groups = Array();

		combined = this.mergePatterns;

		this.makePatterns(combined);

		this.makeControls;

		argproto = ();

		matrix.argproto[protoname].keys(Array).sort.do({|key|
			argproto[key.asString.replace("p", prefix).asSymbol] = matrix.argproto[protoname][key];
		});

		args = patterns.collect({|pat, key| argproto[key] ? argproto[\default]; });

		args.keysValuesDo({|patkey, argev|
			argev['efx'] = Pdefn((patkey ++ "efx").asSymbol, argev['efx']);
		});

		instr = ().putPairs(args.size.collect({|i| [SparseMatrix.makeDefName(i, prefix),
			SparseMatrix.makeDefName(indices[i], "d")]  }).flat);

		Pdef(name, Ppar(
			args.collect({|args, key|
				var defindex, freq;
				defindex = instr[key].asString.drop(1).asInteger;
				freq = matrix.deffuncs[defindex].def.makeEnvirFromArgs[\freq];
				if (this.class.usePrimes) { freq = freq.asInt.nextPrime; }
				{
					if (this.class.useTwinPrimes) { freq = this.findNearestTwinPrime(freq); }
					{
						freq = this.class.scale.performNearestInScale(freq.cpsmidi).midicps;
					}
				};
				Pbind(\instrument, instr[key], \group, matrix.group, \addAction, \addToHead,
					\delta, Pfunc({ matrix.beatdur / div }),
					\amp, Pfunc({ this.calculateAmp(key) }), \emp, Pfunc({ ctrls[key].emp }), \out, matrix.decoder.bus,
					\freq, freq, \dur, Pfunc({ this.calculateDur(key) }), \pat, matrix.makePattern(key, patterns[key].bubble),
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

SparseMelodyPattern : SparseMatrixPattern{

	*new{|name, indices, groupsize, div, sourcenames, subpatterns, prefix, matrix, protoname, append=false, defname, melody|
		^super.new(name, indices, groupsize, div, prefix, matrix, sourcenames, subpatterns, protoname, append).makePdef(defname, melody)
	}

	makePdef{|defname, melody|
		var argproto, combined;
		patterns = ();
		groups = Array();

		combined = this.mergePatterns;

		this.makePatterns(combined);

		this.makeControls;

		groups = groups.clump(groupsize);

		argproto = ();

		matrix.argproto[protoname].keysValuesDo({|key, val|
			argproto[key.asString.replace("p", prefix).asSymbol] = val
		});

		args = patterns.collect({|pat, key| argproto[key] ? argproto[\default]; });

		args.keysValuesDo({|patkey, argev|
			argev['efx'] = Pdefn((patkey ++ "efx").asSymbol, argev['efx']);
		});

		Pdef(name, Ppar(
			args.collect({|args, key|
				Pbind(\instrument, defname, \group, matrix.group, \addAction, \addToHead,
					\delta, Pfunc({ matrix.beatdur / div }), \emp, Pfunc({ ctrls[key].emp }),
					\amp, Pfunc({ ctrls[key].amp }), \out, matrix.decoder.bus, \freq, melody,
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

SparseChordPattern : SparseMatrixPattern {
	*new{|name, groupsize, div, prefix, matrix, protoname, defname, freqs, inds, amp, dur|
		^super.new(name, nil, groupsize, div, prefix, matrix, nil, nil, protoname, nil).makePdef(defname, freqs, inds, amp, dur)
	}

	makePdef{|defname, freqs, inds, amp, dur|

		ctrls = (active: 0, amp: 0.0, dur: 0.01, emp: 0.0);

		Pdef(name, Pbind(
			'instrument', defname, 'efx', Pdefn((name ++ "efx").asSymbol, matrix.nofxbus),
			'az', 0, 'el', 0, 'env', matrix.envs.perc01, 'freqs', freqs, 'inds', inds,
			'type', Pfunc({|ev| if (ctrls.active.booleanValue) { \note } { \rest } }),
			'dur', dur * Pfunc({ ctrls.dur }), 'amp', amp * Pfunc({ ctrls.amp }),
			'group', matrix.group, 'addAction', \addToHead,
			'delta', Pfunc({ matrix.beatdur / div }), 'emp', Pfunc({ ctrls.emp }),
			'out', matrix.decoder.bus
		))
	}

}

SparseBufferPattern : SparseMatrixPattern{
	var <buffers;

	*new{|name, indices, groupsize, div, sourcenames, subpatterns, prefix, matrix, protoname, buffers, defname,
			append=false|
		^super.new(name, indices, groupsize, div, prefix, matrix, sourcenames, subpatterns, protoname, append)
			.makePdef(buffers, defname)
	}

	makePdef{|bufs, defname|
		var argproto, combined;
		patterns = ();
		groups = Array();
		buffers = ();

		combined = this.mergePatterns;

		this.makePatterns(combined);

		patterns.keys(Array).sort.do({|key, i|
			buffers[key] = bufs[i]
		});

		this.makeControls;

		groups = groups.clump(groupsize);

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

SparseDubPattern : SparseMatrixPattern{
	var <buffers;

	*new{|name, indices, groupsize, div, sourcenames, subpatterns, prefix, matrix, protoname, buffers, defname,
			append=false|
		^super.new(name, indices, groupsize, div, prefix, matrix, sourcenames, subpatterns, protoname, append)
			.makePdef(buffers, defname)
	}

	makePdef{|bufs, defname|
		var argproto, combined;
		patterns = ();
		groups = Array();
		buffers = ();

		combined = this.mergePatterns;

		this.makePatterns(combined);

		patterns.keys(Array).sort.do({|key, i|
			buffers[key] = bufs[i]
		});

		this.makeControls;

		groups = groups.clump(groupsize);

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
				gepargs[key] = gepitem.args.args.select(_.isKindOf(Number)).bubble;
				this.addGepSynthDef(gepitem.data, gepitem.defname);
				Server.default.sync;
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

			Post << "Gepdef " << name << " initialized.." << Char.nl;

		}).play
	}

	addGepSynthDef{|data, defname|
		var fnc, chrom, str;
		chrom = GEPChromosome(data.code, data.terminals, data.header.numgenes, data.linker);
		str = chrom.asUgenExpressionTree.asSynthDefWrapString(Normalizer);
		fnc = str.interpret;
		{
			matrix.makeGepDef(defname, fnc, data.terminals.size);
		}.try({
			Post << "ERROR: " << str << Char.nl;
		})

	}
}

SparseJGepPattern : SparseMatrixPattern {

	var <gepdata, defnames, loader, player;

	*new{|name, groupsize, div, sourcenames, subpatterns, prefix, matrix, protoname, append, defnames, loader|
		^super.new(name, (0..defnames.size), groupsize, div, prefix, matrix, sourcenames, subpatterns, protoname, append)
			.makePdef(defnames, loader)
	}

	makePdef{|names, jloader|
		var instr, argproto, gepargs;
		var combined;
		defnames = names;
		gepdata = Array.newClear(defnames.size);
		loader = jloader;
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

			var t = SystemClock.seconds;

			defnames.do({|dname, i|
				var grgs, key = SparseMatrix.makeDefName(i, prefix);
				instr[key] = dname;
				gepdata[i] = loader.getPlayerDataByDefName(dname);
				grgs = gepdata[i].args.select(_.isKindOf(Number));
				if (usePrimes) { grgs = grgs.collect({|frq| frq.asInt.nextPrime }) }
				{
					if (useTwinPrimes) {
						grgs = grgs.collect({|frq| this.findNearestTwinPrime(frq) });
					}
				};
				gepargs[key] = grgs.bubble;
				this.addGepSynthDef(dname, gepdata[i]);
				Server.default.sync;
			});

			Post << "Gep patterns init time elapsed: " << (SystemClock.seconds - t) << Char.nl;

			Pdef(name, Ppar(
				args.collect({|args, key|
					var defindex, freq;
					defindex = instr[key].asString.drop(1).asInteger;
					Pbind(
						\instrument, instr[key], \group, matrix.group, \addAction, \addToHead,
						\delta, Pfunc({ matrix.beatdur / div }),
						\amp, Pfunc({ ctrls[key].amp }), \emp, Pfunc({ ctrls[key].emp }),
						\out, matrix.decoder.bus,
						\dur, Pfunc({ ctrls[key].dur }), \pat, matrix.makePattern(key, patterns[key].bubble),
						\type, Pfunc({|ev| if (ctrls[key].active.booleanValue) { ev.pat } { \rest } }),
						\gepargs, gepargs[key], *args.asKeyValuePairs
					)
				}).values
			)).quant(64);

			Post << "Gepdef " << name << " initialized.." << Char.nl;

		}).play
	}

	addGepSynthDef{|defname, gepitem|
		var fnc, chrom, str;
		chrom = GEPChromosome(gepitem.code, gepitem.terminals, gepitem.numgenes, gepitem.linker);
		str = chrom.asUgenExpressionTree.asSynthDefWrapString(Normalizer);
		fnc = str.interpret;
		{
			matrix.makeGepDef(gepitem.defname, fnc, gepitem.terminals.size);
		}.try({
			Post << "ERROR: " << str << Char.nl;
		})

	}

}
