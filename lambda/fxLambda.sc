FxLambda{

	var decoder, graphics, nano, args, loops, loopstreams, fxdef, efxsynth, mapSynths, fxsynths, fxReady=false;
	var group, freqs, nano, accActive, accControl, oscfncs, rotation, efxbus, gepnames, <gepdefs, gepsynths;
	var <>accMin, <>accMax, <>bpm=0;

	*new{|decoder, graphics, nano|
		^super.newCopyArgs(decoder, graphics, nano).init
	}

	init{
		var loadstruct;

		if (decoder.isNil) {
			decoder = FoaDecoder();
		};

		if (graphics.isNil) {
			graphics = CinderApp();
		};

		loadstruct = (
			detloop: (1..16),
			fbloop: (0..7),
			monooop: [3, 4, 5, 6, 15, 22, 33, 36],
			sume: [27, 28, 3, 4, 7, 8, 12, 13],
			nime: [14, 15, 16, 17, 5, 6, 7, 9],
			tehis: [15, 19, 21, 23, 25, 28, 40, 42],
			fxloop: [0, 11, 22, 23, 24, 25, 26, 27]
		);

		loops = loadstruct.collect({|inds, key|
			("/Users/alo/snd/fx_loops/" ++ key.asString ++ "*").pathMatch[inds].collect({|path|
				Buffer.read(Server.default, path)
			})
		});

		loops['detloop2'] = loops['detloop'].drop(8);
		loops['detloop'] = loops['detloop'].keep(8);

		loopstreams = 4.collect({
			(
				stream: Pseq([
					'detloop', 'detloop2', 'fbloop', 'monooop',
					'sume', 'nime', 'tehis', 'fxloop'], inf
				).asStream
			)
		});

		loopstreams.do({|ev|
			ev['current'] = ev['stream'].next
		});

		fxdef = SynthDef('zone01wrp', {|out, efx, amp, aamp, eamp, dur, buf, str, end, wrp, frq, rate, wsz, dns, rnd, xang, yang, zang|
			var ptr, sig, src, bf, filter, fft, af, del;
			filter = [ [BLowShelf, 100, 0.65], [BPeakEQ, 600, 2], [BPeakEQ, 2000, 2], [BHiShelf, 7500, 1] ];
			del = ArrayControl.kr('del', 4, 1);
			ptr = LFSaw.kr(wrp).range(str, end);
			src = LeakDC.ar(Warp1.ar(1, buf, ptr, frq*rate, wsz, -1, dns, rnd), 0.995, aamp).pow(0.5);
			Out.ar(efx, src * eamp);
			src = src * amp;
			sig = Mix.fill(4, {|i|
				DelayC.ar(filter[i][0].ar(src, filter[i][1], filter[i][2]), delaytime: del[i] )
			});
			bf = FoaDiffuser.ar(sig, 1024, 20.0);
//			fft = FFT(LocalBuf(1024), sig);
//			af = Array.fill(4, {
//				IFFT(PV_Diffuser(fft, Dust.kr(Rand(10.0, 20.0))))
//			});
//			bf = FoaEncode.ar(af, FoaEncoderMatrix.newAtoB);
			Out.ar(out, FoaTransform.ar(bf, 'rtt', xang, yang, zang))
		}, metadata: (
			maps: (
				aamp: Array.fill(8, { ControlSpec(0.1, 5.0, 'lin') }),
				str: Array.fill(8, { ControlSpec(0.0, 0.5) }),
				end: Array.fill(8, { ControlSpec(0.5, 1.0) }),
				eamp: Array.fill(8, { ControlSpec(0.5, 0.1) }),
				rate: Array.fill(8, { ControlSpec((35/36)**2, (35/36)**2, 'lin', 36/35) }),
				xang: Array.fill(8, {|i| ControlSpec((0 + (pi/7*i)).wrap(0, 2pi) - pi, (pi+(pi/7*i)).wrap(0, 2pi) - pi) }),
				yang: Array.fill(8, {|i| ControlSpec((0 + (pi/7*i)).wrap(0, 2pi) - pi, (pi+(pi/7*i)).wrap(0, 2pi) - pi) }),
				zang: Array.fill(8, {|i| ControlSpec((0 + (pi/7*i)).wrap(0, 2pi) - pi, (pi+(pi/7*i)).wrap(0, 2pi) - pi) })
			),
			specs: (
				aamp: ControlSpec(0.1, 5.0, \lin),
				str: ControlSpec(0.0, 0.5),
				end: ControlSpec(0.5, 1.0),
				eamp: ControlSpec(8.0, 1.0),
				frq: ControlSpec((35/36)**2, (35/36)**2, 'lin', 36/35),
				xang: ControlSpec(-pi, pi),
				yang: ControlSpec(-pi, pi),
				zang: ControlSpec(-pi, pi)
			)
		)).add;

		SynthDef('verb', {|out, in, rtime, damp=0.5, inbw=0.5, spr=20, dry=0.0, early, tail, amp, fbamp = 0.01|
			var sig, input, actr, fb;
			input = In.ar(in);
			fb = LocalIn.ar;
			sig = GVerb.ar(input + fb, 800, rtime, damp, inbw, spr, dry, early, tail, 800);
			LocalOut.ar(Mix(sig) * fbamp);
			Out.ar(out, FoaEncode.ar(sig, FoaEncoderMatrix.newStereo) * amp);
		}).add;

		this.initGepDefs;

		this.mapNANO;

	}

	initGepDefs{

		SynthDef(\procgen, {|out, in, amp|
			var input, sig, fft, bf, rot, til, tum;
			input = Limiter.ar(Mix(In.ar(in, 2)).tanh, -1.0.dbamp, 0.1) * amp;
			bf = FoaDiffuser.ar(input, 1024, 20.0);
			rot = LFNoise2.kr(bf[0].explin(0.001, 1.0, 0.5, 20.0)).range(-pi, pi);
			til = LFNoise2.kr(bf[1].explin(0.001, 1.0, 0.5, 20.0)).range(-pi, pi);
			tum = LFNoise2.kr(bf[2].explin(0.001, 1.0, 0.5, 20.0)).range(-pi, pi);
			bf = FoaTransform.ar(bf, 'rtt', rot, til, tum );
			Out.ar(out, bf)
		}).add;

		gepdefs = Array.newClear(4);

		gepnames = [
			'gep_gen000_023_120524_202530',
			'gep_gen000_040_120526_001019',
			'gep_gen000_048_120617_122928',
			'gep_gen000_074_120524_193125',
			'gep_gen001_009_120523_232519',
			'gep_gen001_029_120525_224745',
			'gep_gen001_035_120525_232458',
			'gep_gen001_050_120615_144459',
			'gep_gen001_062_120523_232604',
			'gep_gen001_073_120523_232614',
			'gep_gen002_038_120525_123932',
			'gep_gen002_059_120621_001420',
			'gep_gen003_051_120525_230443',
			'gep_gen003_084_120618_093408',
			'gep_gen005_031_120525_235055',
			'gep_gen005_092_120524_003017',
			'gep_gen000_035_120525_121508',
			'gep_gen001_083_120524_142451',
			'gep_gen001_088_120621_105144',
			'gep_gen002_043_120617_124311',
			'gep_gen003_022_120619_122458',
			'gep_gen003_039_120619_122503',
			'gep_gen003_051_120525_230443',
			'gep_gen003_084_120618_093408',
			'gep_gen004_016_120619_163415',
			'gep_gen004_026_120619_124255',
			'gep_gen005_095_120621_134612',
			'gep_gen006_019_120523_223408'
		].collect(_.asString);

		args = ();

		gepnames.do({|name|
			Server.default.loadSynthDef(name, dir: Paths.gepDefDir );
			args[name] = UGenExpressionTree.loadMetadata(name.asSymbol).args
		});

		gepnames.clump(gepnames.size/4).do({|arr, i|
			var def;
			def = ();
			arr.do({|name|
				def[name] = args[name]
			});
			gepdefs[i] = (
				stream: Pseq(arr, inf).asStream,
				def: def
			)
		});

		gepdefs.do({|ev|
			ev['current'] = ev['stream'].next
		});

	}

	mapSynths{|values|
		var states = values.clump(8);
		if (fxsynths.notNil) {
			fxsynths.do({|ev, i|
				var args, stream = Pseq(states.wrapAt(i), inf).asStream;
				args = fxdef.metadata.specs.collect({|spec| spec.map(stream.next) });
//				args = fxdef.metadata.maps.collect({|array| array.wrapAt(i).map(stream.next) });
				ev['synth'].set(*args.asKeyValuePairs)
			})
		}
	}

	prepare{
		Routine({
			graphics.open;
			2.wait;
			decoder.start;
			Server.default.sync;
			group = Group.before(decoder.synth);
			efxbus = Bus.audio;
			Server.default.sync;
			efxsynth = Synth.tail(group, 'verb', [\out, decoder.bus, \in, efxbus, \rtime, 3.0,
				\early, 0.8, \tail, 1.5, \amp, 0.0
			]);
			freqs = Array.geom(8, 0.125, 55/34);
			fxsynths = 8.collect({|i|
				(
				active: false,
				synth: Synth.newPaused(\zone01wrp, [\out, decoder.bus, \amp, 0, \efx, efxbus, \gate, 1.0,
					\aamp, 0.5, \eamp, 0.0, \dur, 1.0, \buf, loops['detloop'][i], \str, 0, \end, 1.0,
					\wrp, 0.001, \frq, 1.0, \rate, freqs@i, \wsz, 0.1, \dns, rrand(2, 10), \rnd, 0.01,
					\doneAction, 2] ).setn(\del, [0.0, 0.001, 0.002, 0.003], group)
				)
			});
			gepsynths = 4.collect({
				var bus = Bus.audio(Server.default, 2);
				Server.default.sync;
				(
				active: false,
				bus: bus,
				proc: Synth('procgen', [\out, decoder.bus, \in, bus, \amp, 0.0])
				)
			});
			0.25.wait;
			graphics.initWorld(20, 20, 20, 8);
			0.25.wait;
			graphics.initContinuous;
			0.25.wait;
			graphics.setAdd(0.05);
			graphics.resetWorld(9, 9, 9, 4, 4, 4);
			0.25.wait;
			this.mapiPhone;
			2.wait;
			graphics.queryStates(QueryStates.sides2D(graphics.world.sizeX, 4), {|msg|
				this.mapSynths(msg.drop(1))
			});
			"f(x) initialized".postln;
		}).play
	}

	cleanup{
		graphics.stopQuery;
		graphics.stopRotation;
		fxsynths.do({|ev| ev['synth'].free;  });
		fxsynths = nil;
		gepsynths.do({|ev|
			ev['proc'].free;
			if (ev['synth'].notNil) {
				ev['synth'].free;
				ev['synth'] = nil;
			};
			ev['bus'].free;
			ev['bus'] = nil;
		});
		gepsynths = nil;
		decoder.free;
		oscfncs.do(_.disable);
		group.free;
		loops.do({|lib|
			lib.do(_.free);
		});
		loops = nil;
		SystemClock.sched(3, {
			nano.knobs[0].do({|ctr| ctr.action = {}  });
			nano.sliders[0].do({|ctr| ctr.action = {}  });
			nano.buttons[0].do({|ctr| ctr.action = {}  });
			"Cleanup finished".postln;
			nil
		})
	}

	performSynthAction{|index|
		var args;
		if (gepsynths[index]['active']) {
			if ( gepsynths[index]['synth'].isNil ) {
				args = gepdefs[index]['def'][gepdefs[index]['current']];
				gepsynths[index]['synth'] = Synth.before(gepsynths[index]['proc'], gepdefs[index]['current'],
					[\out, gepsynths[index]['bus']] ++ this.scaleArgsToBeat(args)
				)
			}
		}
		{
			if (gepsynths[index]['synth'].notNil) {
				gepsynths[index]['synth'].free;
				gepsynths[index]['synth'] = nil
			}
		}
	}

	scaleArgsToBeat{|args|
		if (bpm > 0) {
			^Event.newFrom(args).collect({|value| value.roundFreq(1, this.calcBeatDur)  }).asKeyValuePairs;
		} {
			^args
		}
	}

	calcBeatDur{ ^(bpm/60).reciprocal }

	mapNANO{

		var addspec, interspec, ampspec, decspec;
		addspec = ControlSpec(0.001, 0.999, \cos);
		interspec = ControlSpec(1, 24, 'lin', 1);
		ampspec = FaderWarp();
		decspec = ControlSpec(1.0, 2.0);

		nano.knobs[0][0].action = {|knob|
			var map;
			map = nano.buttons[0][0].value.asInt;
			if (knob.value > 0.1) {
				graphics.setPattern(0, 1, knob.value, map, map, 0.6, 0.8, 1.0);
			}
			{
				graphics.setPattern(0, 0, knob.value, map, map, 0.6, 0.8, 1.0);
			}
		};
		nano.knobs[0][1].action = {|knob|
			var map;
			map = nano.buttons[0][1].value.asInt;
			if (knob.value > 0.1) {
				graphics.setPattern(1, 1, knob.value, map, map, 0.9, 0.1, 0.4);
			}
			{
				graphics.setPattern(1, 0, knob.value, map, map, 0.9, 0.1, 0.4);
			}
		};
		nano.knobs[0][2].action = {|knob|
			var map;
			map = nano.buttons[0][2].value.asInt;
			if (knob.value > 0.1) {
				graphics.setPattern(2, 1, knob.value, map, map, 0.8, 0.8, 0.85);
			}
			{
				graphics.setPattern(2, 0, knob.value, map, map, 0.8, 0.8, 0.85);
			}
		};
		nano.knobs[0][3].action = {|knob|
			var map;
			map = nano.buttons[0][3].value.asInt;
			if (knob.value > 0.1) {
				graphics.setPattern(3, 1, knob.value, map, map, 0.6, 0.8, 0.8);
			}
			{
				graphics.setPattern(3, 0, knob.value, map, map, 0.6, 0.8, 0.8);
			}
		};
		nano.knobs[0][4].action = {|knob|
			var map;
			map = nano.buttons[0][4].value.asInt;
			if (knob.value > 0.1) {
				graphics.setPattern(4, 1, knob.value, map, map, 1.0, 1.0, 0.3);
			}
			{
				graphics.setPattern(4, 0, knob.value, map, map, 1.0, 1.0, 0.3);
			}
		};

		nano.knobs[0][5].action = {|knob|
			graphics.setAdd( addspec.map(knob.value));
		};
		nano.knobs[0][6].action = {|knob|
			graphics.setInterpolation(1, interspec.map(knob.value).asInt);
		};
		nano.knobs[0][7].action = {|knob|
			graphics.setBackground(knob.value, knob.value, knob.value + 0.05);
		};
		nano.knobs[0][8].action = {|knob|
			decoder.synth.set(\amp, decspec.map(knob.value))
		};

		nano.sliders[0][0].action = {|slider|
			if (fxsynths.notNil) {
				fxsynths[[0, 1]].do({|ev|
					ev.active = slider.value > 0.1;
					ev['synth'].run(ev.active);
					ev['synth'].set('amp', ampspec.map(slider.value) )
				})
			}
		};

		nano.sliders[0][1].action = {|slider|
			if (fxsynths.notNil) {
				fxsynths[[2, 3]].do({|ev|
					ev.active = slider.value > 0.1;
					ev['synth'].run(ev.active);
					ev['synth'].set('amp', ampspec.map(slider.value) )
				})
			}
		};

		nano.sliders[0][2].action = {|slider|
			if (fxsynths.notNil) {
				fxsynths[[4, 5]].do({|ev|
					ev.active = slider.value > 0.1;
					ev['synth'].run(ev.active);
					ev['synth'].set('amp', ampspec.map(slider.value) )
				})
			}
		};

		nano.sliders[0][3].action = {|slider|
			if (fxsynths.notNil) {
				fxsynths[[6, 7]].do({|ev|
					ev.active = slider.value > 0.1;
					ev['synth'].run(ev.active);
					ev['synth'].set('amp', ampspec.map(slider.value) )
				})
			}
		};

		nano.sliders[0][4].action = {|slider|
			efxsynth.set(\amp, ampspec.map(slider.value))
		};

		nano.sliders[0][5].action = {|slider|
			if (gepsynths.notNil) {
				gepsynths[0]['proc'].set('amp', ampspec.map(slider.value) );
				gepsynths[0]['active'] = slider.value > 0.1;
				this.performSynthAction(0);
			}
		};

		nano.sliders[0][6].action = {|slider|
			if (gepsynths.notNil) {
				gepsynths[1]['proc'].set('amp', ampspec.map(slider.value) );
				gepsynths[1]['active'] = slider.value > 0.1;
				this.performSynthAction(1);
			}
		};

		nano.sliders[0][7].action = {|slider|
			if (gepsynths.notNil) {
				gepsynths[2]['proc'].set('amp', ampspec.map(slider.value) );
				gepsynths[2]['active'] = slider.value > 0.1;
				this.performSynthAction(2);
			}
		};

		nano.sliders[0][8].action = {|slider|
			if (gepsynths.notNil) {
				gepsynths[3]['proc'].set('amp', ampspec.map(slider.value) );
				gepsynths[3]['active'] = slider.value > 0.1;
				this.performSynthAction(3);
			}
		};

		nano.buttons[0][0].action = {|btn|
			var map, alpha;
			map = btn.value.asInt;
			alpha = nano.knobs[0][0].value;
			if (alpha > 0.1) {
				graphics.setPattern(0, 1, alpha, map, map, 0.6, 0.8, 1.0);
			}
			{
				graphics.setPattern(0, 0, alpha, map, map, 0.6, 0.8, 1.0);
			}

		};

		nano.buttons[0][1].action = {|btn|
			var map, alpha;
			map = btn.value.asInt;
			alpha = nano.knobs[0][1].value;
			if (alpha > 0.1) {
				graphics.setPattern(1, 1, alpha, map, map, 0.9, 0.1, 0.4);
			}
			{
				graphics.setPattern(1, 0, alpha, map, map, 0.9, 0.1, 0.4);
			}

		};

		nano.buttons[0][2].action = {|btn|
			var map, alpha;
			map = btn.value.asInt;
			alpha = nano.knobs[0][2].value;
			if (alpha > 0.1) {
				graphics.setPattern(2, 1, alpha, map, map, 0.8, 0.8, 0.85);
			}
			{
				graphics.setPattern(2, 0, alpha, map, map, 0.8, 0.8, 0.85);
			}

		};

		nano.buttons[0][3].action = {|btn|
			var map, alpha;
			map = btn.value.asInt;
			alpha = nano.knobs[0][3].value;
			if (alpha > 0.1) {
				graphics.setPattern(3, 1, alpha, map, map, 0.6, 0.8, 0.8);
			}
			{
				graphics.setPattern(3, 0, alpha, map, map, 0.6, 0.8, 0.8);
			}

		};

		nano.buttons[0][4].action = {|btn|
			var map, alpha;
			map = btn.value.asInt;
			alpha = nano.knobs[0][4].value;
			if (alpha > 0.1) {
				graphics.setPattern(4, 1, alpha, map, map, 1.0, 1.0, 0.3);
			}
			{
				graphics.setPattern(4, 0, alpha, map, map, 1.0, 1.0, 0.3);
			}

		};

		nano.buttons[0][5].action = {|btn|
			if (btn.value == 0) {
				gepdefs[0]['current'] = gepdefs[0]['stream'].next;
				if (gepsynths[0]['active'] and: { gepsynths[0]['synth'].notNil }) {
					gepsynths[0]['synth'] = Synth.replace(gepsynths[0]['synth'], gepdefs[0]['current'],
						[\out, gepsynths[0]['bus']] ++ gepdefs[0]['def'][gepdefs[0]['current']]
					)
				}
			}
		};

		nano.buttons[0][6].action = {|btn|
			if (btn.value == 0) {
				gepdefs[1]['current'] = gepdefs[1]['stream'].next;
				if (gepsynths[1]['active'] and: { gepsynths[1]['synth'].notNil }) {
					gepsynths[1]['synth'] = Synth.replace(gepsynths[1]['synth'], gepdefs[1]['current'],
						[\out, gepsynths[1]['bus']] ++ gepdefs[1]['def'][gepdefs[1]['current']]
					)
				}
			}
		};

		nano.buttons[0][7].action = {|btn|
			if (btn.value == 0) {
				gepdefs[2]['current'] = gepdefs[2]['stream'].next;
				if (gepsynths[2]['active'] and: { gepsynths[2]['synth'].notNil }) {
					gepsynths[2]['synth'] = Synth.replace(gepsynths[2]['synth'], gepdefs[2]['current'],
						[\out, gepsynths[2]['bus']] ++ gepdefs[2]['def'][gepdefs[2]['current']]
					)
				}
			}
		};

		nano.buttons[0][8].action = {|btn|
			if (btn.value == 0) {
				gepdefs[3]['current'] = gepdefs[3]['stream'].next;
				if (gepsynths[3]['active'] and: { gepsynths[3]['synth'].notNil }) {
					gepsynths[3]['synth'] = Synth.replace(gepsynths[3]['synth'], gepdefs[3]['current'],
						[\out, gepsynths[3]['bus']] ++ gepdefs[3]['def'][gepdefs[3]['current']]
					)
				}
			}
		};


		nano.buttons[0][9].action = {|btn|
			if (btn.value == 0) {
				if (fxsynths.notNil) {
					loopstreams[0]['current'] = loopstreams[0]['stream'].next;
					Post << "synths 0 & 1 set to buffers " << loopstreams[0]['current'] << Char.nl;
					fxsynths[0]['synth'].set(\buf, loops[loopstreams[0]['current']][0]);
					fxsynths[1]['synth'].set(\buf, loops[loopstreams[0]['current']][1]);
				}
			}
		};

		nano.buttons[0][10].action = {|btn|
			if (btn.value == 0) {
				if (fxsynths.notNil) {
					loopstreams[1]['current'] = loopstreams[1]['stream'].next;
					Post << "synths 2 & 3 set to buffers " << loopstreams[1]['current'] << Char.nl;
					fxsynths[2]['synth'].set(\buf, loops[loopstreams[1]['current']][2]);
					fxsynths[3]['synth'].set(\buf, loops[loopstreams[1]['current']][3]);
				}
			}
		};

		nano.buttons[0][11].action = {|btn|
			if (btn.value == 0) {
				if (fxsynths.notNil) {
					loopstreams[2]['current'] = loopstreams[2]['stream'].next;
					Post << "synths 4 & 5 set to buffers " << loopstreams[2]['current'] << Char.nl;
					fxsynths[4]['synth'].set(\buf, loops[loopstreams[2]['current']][4]);
					fxsynths[5]['synth'].set(\buf, loops[loopstreams[2]['current']][5]);
				}
			}
		};

		nano.buttons[0][12].action = {|btn|
			if (btn.value == 0) {
				if (fxsynths.notNil) {
					loopstreams[3]['current'] = loopstreams[3]['stream'].next;
					Post << "synths 6 & 7 set to buffers " << loopstreams[3]['current'] << Char.nl;
					fxsynths[6]['synth'].set(\buf, loops[loopstreams[3]['current']][6]);
					fxsynths[7]['synth'].set(\buf, loops[loopstreams[3]['current']][7]);
				}
			}
		};

		nano.buttons[0][19].action = {|btn|
			if (btn.value == 1 and: { fxReady.not }) {
				this.prepare();
			}
		};

		nano.buttons[0][22].action = {|btn|
			if (btn.value == 0) {
				this.cleanup
			}
		}


	}

	mapiPhone{
		accActive = false;
		accControl = (vals: [0.0, 0.0, 0.0], rates:[0.0, 0.0, 0.0],
			vec: [0.0, 0.0, 0.0], loc: [40.0, 0.0, 0.0],
			min: -20.0, max: 20.0
		);

		accMin = 20.0;
		accMax = 200.0;

		oscfncs = (
			acc: OSCFunc({|msg|
				if (accActive) {
					accControl['rates'] = msg[1..3] - accControl['vals'];
					accControl['vals'] = msg[1..3];
					accControl['vec'] = accControl['vec'] + accControl['rates'];
					accControl['loc'] = (accControl['loc'] + accControl['vec'])
						.clip(accControl['min'], accControl['max']);
					graphics.setViewpoint(accControl['loc'][0], accControl['loc'][1], accControl['loc'][2],
						0.0, 0.0, 0.0)
				};
			}, '/accxyz'),

			push1: OSCFunc({
				accControl['min'] = (accControl['min'] + 5.0).clip(accMin.neg, accMax.neg);
				accControl['max'] = (accControl['max'] - 5.0).clip(accMin, accMax);
			}, '/fxone/push1'),
			push2: OSCFunc({
				accControl['min'] = (accControl['min'] - 5.0).clip(accMin.neg, accMax.neg);
				accControl['max'] = (accControl['max'] + 5.0).clip(accMin, accMax);
			}, '/fxone/push2'),
			toggle1: OSCFunc({|msg|
				accActive = msg[1].booleanValue;
				if (accActive.not) {
					rotation = Rotation(rrand(40.0, 80.0), rrand(0.001, 0.01), rrand(40.0, 80.0),
						rrand(60.0, 120.0), 2pi.rand, rrand(0.001, 0.01), 2pi.rand, rrand(0.001, 0.01));
					graphics.setCameraRotation(rotation, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
					graphics.rotateCamera;
				}
				{
					graphics.stopRotation;
				}
				}, '/fxone/toggle1'),
			fader: OSCFunc({|msg|
				var value = msg[1].linlin(0.0, 1.0, accMin, accMax);
				accControl['min'] = value.neg;
				accControl['max'] = value;
			}, '/fxone/fader')
		);
	}


}