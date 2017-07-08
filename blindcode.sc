Bc{

	classvar <>announce_buf_path = "/Users/alo/snd/blindcoding_announce.aiff";

	*new{
		^super.new.init
	}

	init{
		var action, settings = ( ncoef: 20, rate: 20, headsize: 14, numgenes: 4, quant: 2, screenX: 1024, screenY: 768, mode: 1, decoderType: 'stereo', bpm: 141, channels: 2, foa: #[zoom,push], dbname: "ges_ld_00", patdefs: "patternDefsAppendLnx.scd", initPdefs: ['r00', 'r01', 'r02', 'b03', 'b04', 'b05', 'b06'], worldDim: 21, ip: "127.0.0.1");

		action = {
			var buf;
			Tdef('initblind', {
				currentEnvironment['gesbufs'] = GESBufferLoader().loadByDate("161224");
				0.5.wait;
				currentEnvironment['gesbufs'].preload;
				Server.default.sync;
				Sd('b2', {|out=512, efx=512, amp=0, emp=0, buf=1024, dur=0|
					var sig;
					sig = Normalizer.ar(
						PlayBuf.ar(2, buf, BufRateScale.ir(buf) * 1.neg, 1, BufFrames.kr(buf)),
						amp) * EnvGen.kr(\env.kr(Env.newClear(8).asArray), timeScale: dur,
						doneAction: 2);
					Out.ar(efx, sig * emp);
					Out.ar(out, sig * amp)
				}).add;
				Server.default.sync;
				currentEnvironment['bf'] = currentEnvironment['matrix'].buffers['evo'];
				currentEnvironment['mx'] = currentEnvironment['matrix'];

				Pb('a', 'instrument', 'b2',  'out', currentEnvironment['decoder'].bus, 'efx', 512);
				Pb('a', 'bufind', Pw(45, 50, 11));
				Pb('a', 'buf', Pf({|ev| ev['bufind'].postln; currentEnvironment['bf'][ev['bufind']] }));
				Pb('a', 'env', Pr(currentEnvironment['mx'].envs.asArray, inf));
				Pb('a', 'amp', Pw(0.7, 1.0), 'dur', Pw(0.3, 0.8));
				Pb('a', 'delta', Pr(1/[1,2,4,6] * currentEnvironment['mx'].beatdur, inf));
				buf = Buffer.read(Server.default, Bc.announce_buf_path);
				Server.default.sync;
				SynthDef('announce', {
					Out.ar(0, PlayBuf.ar(2, buf, doneAction: 2));
				}).play;
			}).play

		};

		SpEnvir(settings, action);

	}

}

Wrp{

	classvar <>wrps;

	*new{|key, index|
		if (Wrp.wrps.isNil) { Wrp.wrps = () };
		Wrp.wrps[key] = Synth.tail(currentEnvironment['mx'].group, 'wrprevm', ['out', currentEnvironment['decoder'].bus, 'rate', 1.0, 'start', 0, 'wrp', 0.1, 'freq', 1.0, 'wsize', 0.1, 'dens', 6, 'randr', 0.03, 'room', 100, 'rtime', 6.0, 'ramp', 0.0, 'damp', 0, 'iamp', 0.0, 'buf', currentEnvironment['gesbufs'][index], 'amp', 1.0, 'wamp', 0.0, 'rx', 0, 'ry', 0, 'rz', 0]);
	}

	*free{|key|
		Wrp.wrps[key].free;
		Wrp.wrps[key] = nil;
	}

	*set{|key ... args|
		Wrp.wrps[key].set(*args)
	}

}

Dj{

	classvar <>djs;

	*initClass{ djs = () }

	*on{|key, active|
		if (Dj.djs.includesKey(key)) {
			Dj.djs[key]['onFunc'] = { [0, 1].wchoose([1-active, active]) }
		}
		{
			Dj.djs[key] = ('onFunc': { [0, 1].wchoose([1-active, active]) } )
		};
		currentEnvironment['mx'].defsAt(key).ctrls.do({|ctr|
			ctr.active = Dj.djs[key]['onFunc'].()
		})
	}

	*dur{|key, lo, hi|
		if (Dj.djs.includesKey(key)) {
			Dj.djs[key]['durFunc'] = { exprand(lo, hi) }
		}
		{
			Dj.djs[key] = ('durFunc': { exprand(lo, hi) } )
		};
		currentEnvironment['mx'].defsAt(key).ctrls.do({|ctr|
			ctr.dur = Dj.djs[key]['durFunc'].()
		})
	}

	*amp{|key, lo, hi|
		if (Dj.djs.includesKey(key)) {
			Dj.djs[key]['ampFunc'] = { exprand(lo, hi) }
		}
		{
			Dj.djs[key] = ('ampFunc': { exprand(lo, hi) } )
		};
		currentEnvironment['mx'].defsAt(key).ctrls.do({|ctr|
			ctr.amp = Dj.djs[key]['ampFunc'].()
		})
	}

	*re{|key, num|
		currentEnvironment['mx'].defsAt(key).recall(num)
	}

	*off{|key|
		currentEnvironment['mx'].defsAt(key).ctrls.select({|ctr| ctr.active.booleanValue }).do({|ctr|
			ctr.active = 0
		})
	}

}

Pb : Pbindef{ }

Pd : Pdef{ }

Pp : Ppar{ }

Pw : Pwhite { }

Ps : Pseq{ }

Pr : Prand{ }

Pf : Pfunc { }

Sd : SynthDef{ }

Nd : Ndef { }

