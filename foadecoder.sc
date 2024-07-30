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
	var <order, <decoderType, <path, <numChannels, <numBusChannels, <bus, <decoder;
	var <synth, <isRunning = false, <directions, <delays, <gains, <distances;
	var <type = 'hoa';
	*new{|order=5, decoderType, path|
		^super.newCopyArgs(order, decoderType, path).init
	}

	init{
		numBusChannels = (
			3: 16,
			4: 25,
			5: 36
		)[order];
		bus = Bus.audio(Server.default, numBusChannels);
		if (decoderType == 'binaural') {
			this.initBinaural
		}
		{
			this.initFromFile
		}
	}

	initBinaural{
		{
			numChannels = 2;
			HOABinaural.loadbinauralIRs(Server.default);
			HOABinaural.loadHeadphoneCorrections(Server.default);
			Server.default.sync;
			SynthDef(\decoder, {|amp=1|
				var hoa;
				hoa = HoaNFCtrl.ar(In.ar(bus, numBusChannels), AtkHoa.refRadius, 3.25, order);
				Out.ar(0, HOABinaural.ar(order, hoa * amp))
			}).add
		}.fork
	}

	initFromFile{
		{
			var config, speakers;
			config = path.parseJSONFile;
			speakers = config["LoudspeakerLayout"]["Loudspeakers"]
			.select({|speaker|
				speaker["IsImaginary"] == "false"
			});
			numChannels = speakers.size;
			directions = speakers.collect({|speaker|
				Array.with(speaker["Azimuth"].asFloat, speaker["Elevation"].asFloat)
			}).degrad;
			distances = speakers.collect({|speaker|
				speaker["Radius"].asFloat
			});
			gains = speakers.collect({|speaker|
				speaker["Gain"].asFloat
			});
			Server.default.sync;
			SynthDef(\decoder, {|amp=1, subamp=0.01|
				var hoa, output, sub, bf;
				bf = In.ar(bus, numBusChannels);
				hoa = HoaDecodeMatrix.ar(
					bf,
					HoaMatrixDecoder.newModeMatch(directions, order: order)
				);
				hoa = DegreeDist.ar(hoa, distances) * gains;
				Out.ar(0, hoa);
			}).add;
			Post << "Hoa decoder order: " << order << Char.nl;
			Post << "Decoder config loaded from " << path << Char.nl;
			Post << "Directions: " << directions.size << "  " << directions << Char.nl;
			Post << "Distances: " << distances.size << "  " << distances << Char.nl;
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

	runChannelCheck{|channels, dur=1.0, amp=0.5|
		Tdef(\channelcheck, {
			var chstr = Pseq(channels, inf).asStream;
			loop({
				this.channelCheck(chstr.next, dur, amp);
				dur.wait;
			})
		}).play
	}

	channelCheck{|channel, dur=1, amp=0.5|
		SynthDef(\check, {
			var sig;
			sig = Dust.ar(pi**pi*pi) * amp;
			Out.ar(channel, sig * EnvGen.kr(Env.perc, timeScale: 0.3, doneAction: 2))
		}).play
	}

	free{
		synth.free;
		isRunning = false;
	}

}