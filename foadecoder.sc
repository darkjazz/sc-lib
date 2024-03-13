FoaDecoder{

	var <isLocal, <>offset, <bus, <synth, <isRunning = false, <decoder, <type = 'foa';
	var <hoaChannels, <kind;

	*new{|isLocal=true, decoderType='quad', normalize=false, offset = 0|
		^super.newCopyArgs(isLocal, offset).init(decoderType, normalize)
	}

	init{|decoderType, normalize|
		if ((decoderType=='3o').or(decoderType=='4o').or(decoderType=='5o')) {
			kind = decoderType;
			this.makeHoabDecoder(decoderType, normalize)
		}
		{
			bus = Bus.audio(Server.default, 4);
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

	makeHoabDecoder{|decoderType='3o', normalize|
		"Making hoa decoder".inform;
		case
		{ decoderType == '3o' } {
			hoaChannels = 16;
		}
		{ decoderType == '4o' } {
			hoaChannels = 25;
		}
		{ decoderType == '5o' } {
			hoaChannels = 36;
		};
		bus = Bus.audio(Server.default, hoaChannels);
		if (isLocal) {
			Post << "making local " << decoderType << " HOA decoder" << Char.nl;
			SynthDef(\decoder, {|amp=1|
				var dels = [ 4.93, 4.93, 4.9, 4.83, 4.7, 4.75, 4.96, 5.04, 6.55, 5.95, 6.44, 6, 5, 5, 5, 5, 3, 1 ] / 343;
				if (normalize)
				{
					Out.ar(offset, Normalizer.ar(
						DelayL.ar(
							DecodeAmbi3O.ar(
								In.ar(bus, hoaChannels),
								'emta'),
						dels, dels),
					0.97) * amp)
				}
				{
					Out.ar(offset, Limiter.ar(DecodeAmbi3O.ar(In.ar(bus, hoaChannels), 'emta'), 0.97) * amp)
				}
			}).add
		}
		{
			SynthDef(\decoder, {|amp=1|
				if (normalize) {
					Out.ar(offset, Normalizer.ar(In.ar(bus, hoaChannels), 0.97) * amp)
				}
				{
					Out.ar(offset, Limiter.ar(In.ar(bus, hoaChannels), 0.97) * amp)
				}
			}).add

		}

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

	numChannels{
		if ((decoder.class == FoaDecoderKernel).or(decoder.class == FoaDecoderMatrix))
		{
			^decoder.numChannels
		}
		{
			if (isLocal) {
				^17
			}
			{
				^hoaChannels
			}
		}
	}

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

HoaSpDecoder{
	var <order, <decoderType, <numChannels, <numBusChannels, <bus, <decoder;
	var <synth, <isRunning = false, <directions, <delays, <gains, <distances;
	var <type = 'hoa';
	*new{|order=5, decoderType|
		^super.newCopyArgs(order, decoderType).init
	}

	init{
		{
			numBusChannels = (
				3: 16,
				4: 25,
				5: 36
			)[order];
			bus = Bus.audio(Server.default, numBusChannels);
			if (decoderType == 'binaural') {
				numChannels = 2;
				HOABinaural.loadbinauralIRs(Server.default);
				HOABinaural.loadHeadphoneCorrections(Server.default);
				Server.default.sync;
				SynthDef(\decoder, {|amp=1|
					var hoa;
					hoa = HoaNFCtrl.ar(In.ar(bus, numBusChannels), AtkHoa.refRadius, 3.25, order);
					Out.ar(0, HOABinaural.ar(order, hoa * amp))
				}).add
			}
			{
				numChannels = 23;
				directions = [
					[22.5, -22.5, -67.5, -112.5, -157.5, 157.5, 112.5, 67.5, 0.0, -90.0, 180.0, 90.0, 23.0, -23.0, -157.0, 157.0, 0.0, 0.0, -60.0, -120.0, 180.0, 120.0, 60.0],
					[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 30.0, 30.0, 30.0, 30.0, 53.0, 53.0, 53.0, 53.0, 90.0, -11.0, -15.0, -15.0, -11.0, -15.0, -15.0]
				].flop.degrad;
				delays = [0.5, 0.354167, 7.1875, 7.520833, 0.833333, 0.833333, 6.75, 6.583333, 0.0, 8.125, 1.708333, 7.666667, 9.4375, 9.5625, 9.770833, 9.6875, 12.645833, 3.541667, 9.291667, 9.625, 5.104167, 9.25, 9.083333] * 0.001;
				distances =  (6.41 / 343) - delays * (6.41 / 343);
				gains = [-3.838411, -3.161154, -5.262777, -3.552501, -4.144597, -3.231475, -3.58058, -2.144974, -3.237421, -5.819212, -5.507934, -7.193305, -6.04261, -5.971529, -8.458384, -8.827554, -14.312817, 0.0, -6.787363, -5.077706, -4.872296, -6.427014, -4.582825].dbamp;
				SynthDef(\decoder, {|amp=1|
					var hoa, output, sub, bf;
					bf = In.ar(bus, numBusChannels);
					hoa = HoaDecodeMatrix.ar(
						bf,
						HoaMatrixDecoder.newModeMatch(directions, order: order)
					);
					hoa = DegreeDist.ar(hoa, distances) * gains;
					sub = Mix.ar(bf) * 0.001;
					Out.ar(0, hoa[(0..16)] );
					Out.ar(17, bf[0]*0.1);
					Out.ar(18, hoa[(17..22)])
				}).add
			}
		}.fork
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

	free{
		synth.free;
		isRunning = false;
	}

}