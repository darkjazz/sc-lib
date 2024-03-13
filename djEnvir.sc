DjEnvir{
	var <spEnvir, <synths, <decoder, <buffers, <group, <names, <timers;
	var tracksDir = "/Users/alo/snd/dj";
	var dbname = "dj3d";
	var <ambiOrder;

	*new{|spEnvir|
		^super.newCopyArgs(spEnvir).init
	}

	init{
		if (currentEnvironment['ambiOrder'].isNil ) {
			ambiOrder = 3
		}
		{
			ambiOrder = currentEnvironment['ambiOrder']
		};
		decoder = currentEnvironment['decoder'];
		synths = ();
		buffers = ();
		timers = ();
		names = (tracksDir +/+ "*.aiff").pathMatch.collect({|path|
			path.basename
		});
		group = Group.before(decoder.synth);
		this.addSynthDefs;
		Post << "djenvir initialized" << Char.nl;
	}

	print{
		names.do(_.postln)
	}

	addSynthDefs{
		SynthDef('pDj', {|out, amp=0, buf, rate=1, pow=1, start=0, rotx=0, roty=0, rotz=0|
			var sig, hoa;
			sig = PlayBuf.ar(2, buf, BufRateScale.kr(buf) * rate, start) * amp;
			hoa = HoaEncodeMatrix.ar(sig**pow,
				HoaMatrixEncoder.newDirections([[0, 0], [180, 0]].degrad, order: ambiOrder)
			);
			hoa = HoaRTT.ar(hoa, rotx, roty, rotz, ambiOrder);
			Out.ar(out, hoa)
		}).add;

		SynthDef('pDjTr', {|out, amp=0, buf, rate=1, start=0, rotx=0.1, roty=0.1, rotz=0.1, rad=4.1|
			var sig, hoa, fft, tq, te, ts, tmp, ang, theta, phi;
			sig = PlayBuf.ar(2, buf, BufRateScale.kr(buf) * rate, start) * amp;
			fft = FFT(LocalBuf(1024), Mix(sig));
			#tq, te, ts, tmp = BeatTrack.kr(fft);
			ang = LFNoise2.kr(pi.reciprocal).range(0, 0.1pi);
			theta = LFNoise2.kr(pi.reciprocal).range(-pi, pi);
			phi = LFNoise2.kr(pi.reciprocal).range(-pi, pi);
			hoa = HoaEncodeMatrix.ar(sig,
				HoaMatrixEncoder.newDirections([[0, 0], [180, 0]].degrad, order: ambiOrder)
			);
			hoa = HoaRTT.ar(hoa,
				Demand.kr(tq, 0, Dwhite(-pi, pi)),
				Demand.kr(tq, 0, Dwhite(-pi, pi)),
				Demand.kr(tq, 0, Dwhite(-pi, pi)),
				ambiOrder
			);
			hoa = HoaFocus.ar(hoa, ang, theta, phi, rad, ambiOrder);
			Out.ar(out, hoa)
		}).add;

		SynthDef('pDjTrLR', {|out, amp=0, buf, rate=1, start=0, rotx=0.1, roty=0.1, rotz=0.1, rad=4.1|
			var sig, hoaL, hoaR, fft, tq, te, ts, tmp, ang, theta, phi;
			sig = PlayBuf.ar(2, buf, BufRateScale.kr(buf) * rate, start) * amp;
			fft = FFT(LocalBuf(1024), Mix(sig));
			#tq, te, ts, tmp = BeatTrack.kr(fft);
			hoaL = HoaEncodeMatrix.ar(sig[0],
				HoaMatrixEncoder.newDirection(-pi, 0, order: ambiOrder)
			);
			hoaR = HoaEncodeMatrix.ar(sig[1],
				HoaMatrixEncoder.newDirection(pi, 0, order: ambiOrder)
			);
			hoaL = HoaRTT.ar(hoaL,
				LFNoise2.kr(rotx).range(-pi, pi),
				LFNoise2.kr(roty).range(-pi, pi),
				LFNoise2.kr(rotz).range(-pi, pi),
				ambiOrder
			);
			hoaR = HoaRTT.ar(hoaR,
				Demand.kr(tq, 0, Dwhite(-pi, pi)),
				Demand.kr(tq, 0, Dwhite(-pi, pi)),
				Demand.kr(tq, 0, Dwhite(-pi, pi)),
				ambiOrder
			);
			hoaR = HoaFocus.ar(hoaR,
				Demand.kr(tq, 0, Dwhite(0, 0.075pi)),
				Demand.kr(tq, 0, Dwhite(-pi, pi)),
				Demand.kr(tq, 0, Dwhite(-pi, pi)),
				rad,
				ambiOrder
			);
			Out.ar(out, hoaL + hoaR)
		}).add;
	}

	query{|name|
		if (timers[name].notNil) {
			Post << name << " --- " << timers[name] << " --- " << Char.nl;
		}
	}

	play{|name, amp, defname='pDj'|
		Tdef(name, {
			var path;
			path = tracksDir +/+ name;
			buffers[name] = Buffer.read(Server.default, path);
			Server.default.sync;
			Post << "loaded " << name << Char.nl;
			synths[name] = Synth.tail(group, defname,
				['out', decoder.bus, 'amp', amp, 'buf', buffers[name]]
			);
			timers[name] = buffers[name].duration;
			buffers[name].duration.asInteger.do({
				1.wait;
				timers[name] = timers[name] - 1;
			});
			1.wait;

		}).play
	}

	free{|name|
		synths[name].free;
		synths[name] = nil;
		Tdef(name).clear;
		timers[name] = nil;
	}
}