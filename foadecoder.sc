FoaDecoder{
		
	var <isLocal, <bus, <synth, <isRunning = false, <decoder;
	
	*new{|isLocal=true, decoderType='quad', normalize=false|
		^super.newCopyArgs(isLocal).init(decoderType, normalize)
	}
	
	init{|decoderType, normalize|
		bus = Bus.audio(Server.default, 4);
		this.makeSynthDef(decoderType, normalize);	
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
					"Making uhj decoder".inform;
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
						Out.ar(0, FoaDecode.ar(Normalizer.ar(In.ar(bus, 4), 0.95) * amp, decoder))
					}
					{
						Out.ar(0, FoaDecode.ar(Limiter.ar(In.ar(bus, 4), 0.95) * amp, decoder))
					}
				}).add
			}.fork
		}
		{
			SynthDef(\decoder, {|amp=1|
				if (normalize) {
					Out.ar(0, Normalizer.ar(In.ar(bus, 4), 0.95) * amp)
				}
				{
					Out.ar(0, Limiter.ar(In.ar(bus, 4), 0.95) * amp)
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
				sig = Reverb.ar(sig * rev) + sig + low;
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
		if (input.asArray.size > 1) { input = Mix(input) };
		fft = FFT(LocalBuf(numFrames), input);
		aformat = Array.fill(4, { IFFT(PV_Diffuser(fft, Dust.ar(rate))) });
		^FoaEncode.ar(aformat, FoaEncoderMatrix.newAtoB)
	}
}
