FoaDecoder{

	var <isLocal, <>offset, <bus, <synth, <isRunning = false, <decoder;

	*new{|isLocal=true, decoderType='quad', normalize=false, offset = 0|
		^super.newCopyArgs(isLocal, offset).init(decoderType, normalize)
	}

	init{|decoderType, normalize|
		bus = Bus.audio(Server.default, 4);
		if (decoderType=='c&r') {
			this.makeCRDecoder
		}
		{
			this.makeSynthDef(decoderType, normalize);
		}
	}

	makeSynthDef{|decoderType, normalize|

		if (isLocal) {
			{
				case
				{ decoderType == 'uhj' } {
					"Making uhj decoder".inform;
					decoder = FoaDecoderKernel.newUHJ;
					Server.default.sync;
				}
				{ decoderType == 'stereo' } {
					"Making stereo decoder".inform;
					decoder = FoaDecoderMatrix.newStereo;
					Server.default.sync;
				}
				{ decoderType == 'binaural' } {
					"Making binaural decoder".inform;
					decoder = FoaDecoderKernel.newCIPIC;
					Server.default.sync
				}
				{ decoderType == 'quad' } {
					"Making quad decoder".inform;
					decoder = FoaDecoderMatrix.newQuad()
				}
				{ decoderType == 'hex' } {
					"Making hex decoder".inform;
					decoder = FoaDecoderMatrix.newPanto(6)
				}
				{ decoderType == 'octo' } {
					"Making octo decoder".inform;
					decoder = FoaDecoderMatrix.newPanto(8)
				};
				SynthDef(\decoder, {|amp=1|
					if (normalize) {
						Out.ar(offset, FoaDecode.ar(Normalizer.ar(In.ar(bus, 4), 0.95) * amp, decoder))
					}
					{
						Out.ar(offset, FoaDecode.ar(Limiter.ar(In.ar(bus, 4), 0.95) * amp, decoder))
					}
				}).add
			}.fork
		}
		{
			SynthDef(\decoder, {|amp=1|
				if (normalize) {
					Out.ar(offset, Normalizer.ar(In.ar(bus, 4), 0.95) * amp)
				}
				{
					Out.ar(offset, Limiter.ar(In.ar(bus, 4), 0.95) * amp)
				}
			}).add
		}

	}

	makeCRDecoder{
		var coords = this.calculateCRSetup;
		SynthDef(\decoder, {|amp=1, loamp=1|
			var w, x, y, z;
			#w, x, y, z = Limiter.ar(In.ar(bus, 4), 0.95) * amp;
			Out.ar(0, BFDecode1.ar1(w, x, y, z,
				coords['azimuth'],
				coords['elevation'],
				coords['distance'].maxItem,
				coords['distance']
			) ++ ([w, w]*loamp))
			// Out.ar(0, BFDecode1.ar(w, x, y, z,
			// 	coords['azimuth'],
			// 	coords['elevation']
			// ) ++ ([w, w]*loamp))
		}).add
	}

	start{|target=1, addAction='addToHead'|
		if (isRunning.not) {
			synth = Synth(\decoder, target: target, addAction: addAction);
			isRunning = true;
		}
		{
			"Decoder is already running!".inform
		}
	}

	resetRunningFlag{
		isRunning = false;
	}

	free{
		synth.free;
		isRunning = false;
	}

	numChannels{ ^decoder.numChannels }

	type{ ^decoder.kind }

	test{
		{
			if (isRunning.not) { this.start; Server.default.sync };
			SynthDef(\test00, {|out, dur, amp, del, dec, rev, rotX, rotY, rotZ|
				var sig, freqs, low, env, enc;
				env = EnvGen.kr(Env.perc, timeScale: dur, levelScale: amp, doneAction: 2);
				freqs = Array.geom(4, 40, 512**(1/4));
				sig = Mix(VarSaw.ar(freqs, mul: AmpCompA.kr(freqs)))
					* env;
				low = Mix(SinOsc.ar(Array.geom(6, 20, 2**(1/17)) * (1..6), pi, 0.2)) * env;
				sig = sig + CombC.ar(sig, del, del, dec);
				sig = sig + Mix.ar(Dust2.ar(freqs, AmpCompA.kr(freqs) * 0.3));
				sig = FreeVerb1.ar(sig * rev) + sig + low;
				enc = FoaEncode.ar(sig, FoaEncoderMatrix.newDirection);
				Out.ar(out, FoaTransform.ar(enc, 'rtt', rotX, rotY, rotZ))
			}).add;
			Server.default.sync;
			Pdef(\test,
				Pbind(\instrument, \test00, \addAction, \addBefore, \group, synth,
					\out, bus, \delta, 0.5, \amp, 0.3,\rev, 0.05, \rotY, 0, \rotZ, 0,
					\delta, Pseg(Pseq([0.5, 0.125, 0.5]), Pseq([10, 10, 5, 5]), \linear, inf),
					\dur, Pseg(Pseq([0.3, 0.15, 0.05, 0.3]), Pseq([9, 9, 7, 7]), \linear, inf),
					\del, Pseg(Pseq([0.5, 0.25, 0.5]), Pseq([10, 10]), \linear, inf),
					\dec, Pseg(Pseq([2, 4, 2]), Pseq([9, 9]), \linear, inf),
					\rotX, Pseq([
						Pseg(Pseq([0, 2pi, 0]), Pseq([15, 15]), \linear, 1),
						Pseg(Pseq([2pi, 0, 2pi]), Pseq([10, 10]), \linear, 1),
					], inf)
				)
			).play;
		}.fork;
	}

	stopTest{
		Pdef(\test).stop;
		Pdef(\test).clear;
	}

	calculateCRSetup{
		var coordinates, cartesian;
		cartesian = [
			[-1705, 2375, -1510],
			[1705, 2375, -1510],
			[1705, -2375, -1510],
			[-1705, -2375, -1510],
			[-1705, 0, 0],
			[0, 2375, 0],
			[1705, 0, 0],
			[0, -2375, 0],
			[-1705, -2375, 1664],
			[-1705, 2375, 1664],
			[1705, 2375, 1664],
			[1705, -2375, 1664],
			[0,0,1664]
		];

		coordinates = ();
		coordinates['azimuth'] = [];
		coordinates['elevation'] = [];
		coordinates['distance'] = [];

		cartesian.do({|arr|
			var spherical = Cartesian.new(
				arr[0]/1000.0,
				arr[1]/1000.0,
				arr[2]/1000.0
			).asSpherical;
			coordinates['azimuth']  = coordinates['azimuth'].add(spherical.theta);
			coordinates['elevation'] = coordinates['elevation'].add(spherical.phi);
			coordinates['distance'] = coordinates['distance'].add(spherical.rho);
		});

		coordinates['azimuth'] = [
			-0.30180907074951, 0.30180907074951, 0.69819092925049, -0.69819092925049,
			-0.5, 0, 0.5, 1,
			-0.69819092925049, -0.30180907074951, 0.30180907074951, 0.69819092925049
		];

		^coordinates

	}

}

FoaDiffuser {
	*ar{|input, numFrames=1024, rate=20.0|
		var fft, aformat;
		fft = FFT(LocalBuf(numFrames), input);
		aformat = Array.fill(4, {|i|
			IFFT(PV_Diffuser(fft.bubble.flat.wrapAt(i), Dust.ar(rate)))
		});
		^FoaEncode.ar(aformat, FoaEncoderMatrix.newAtoB)
	}
}
