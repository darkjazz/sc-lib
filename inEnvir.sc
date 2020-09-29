InEnvir {

	var numIns, inRevs, maxShifts, nRecs, bufmul;
	var <ins, <inbs, <shifts, <>recBufs, matrix;

	*new{|numIns=1, inRevs=#[1], maxShifts=4, nRecs=21, bufmul=3|
		^super.newCopyArgs(numIns, inRevs, maxShifts, nRecs, bufmul)
	}

	initFunc{
		^{
			Tdef('initIns', {
				matrix = currentEnvironment[\matrix];
				SynthDef('inp', {|out, efx, emp, amp, inbc|
					var bf, sig = SoundIn.ar(inbc);
					// bf = FoaDiffuser.ar(sig);
					Out.ar(efx, sig * emp);
					Out.ar(out, sig.dup * amp)
				}).add;
				SynthDef('inprev', {| out, efx, emp, amp, inbc, revamp=0.05, revtime=3.1 |
					var input, sig, bf;
					input = SoundIn.ar(inbc);
					sig = GVerb.ar(input, 150, revtime, drylevel: amp, earlyreflevel: revamp,
						taillevel: revamp * 0.5/0.7
					);
					// sig = sig.dup.flat;
					// bf = FoaDiffuser.ar(sig);
					Out.ar(efx, sig[0] * emp);
					Out.ar(out, sig * amp)
				}).add;
				SynthDef('inprec', {|buffer, in|
					var input;
					input = SoundIn.ar(in);
					RecordBuf.ar(input, buffer, loop: 0, doneAction: 2);
				}).add;
				SynthDef('shiftSample', {|out, buffer, amp, rate, winsize, shift, roomsize, revtime, angrate=0.3|
					var sig, bf;
					sig = PlayBuf.ar(1, buffer, rate, loop: 1) * amp;
					sig = GVerb.ar(sig, roomsize, revtime);
					sig = PitchShift.ar(sig, winsize, shift);
					bf = FoaEncode.ar(sig, FoaEncoderMatrix.newDirection);
					bf = FoaRTT.ar(bf,
						LFNoise2.kr(angrate).range(-pi, pi),
						LFNoise2.kr(angrate).range(-pi, pi),
						LFNoise2.kr(angrate).range(-pi, pi));
					Out.ar(out, bf)
				}).add;
				SynthDef('playSample', {| out, buffer, rate, amp, rx, ry, rz |
					var sig, bf;
					sig = PlayBuf.ar(1, buffer, rate, doneAction: 2) * amp;
					bf = FoaRTT.ar(
						FoaEncode.ar(sig, FoaEncoderMatrix.newDirection),
						rx, ry, rz
					);
					Out.ar(out, bf)
				}).add;
				inbs = numIns.collect({ Bus.audio(Server.default) });
				Server.default.sync;
				ins = numIns.collect({|i|
					if (inRevs[i].booleanValue) {
						Synth.head(matrix.group, 'inprev',
							['out', matrix.decoder.bus,
								'efx', inbs[i], 'emp', 1.0, 'amp', 0.0, 'inbc', i]
						)
					}
					{
						Synth.head(matrix.group, 'inp',
							['out', matrix.decoder.bus,
								'efx', inbs[i], 'emp', 1.0, 'amp', 0.0, 'inbc', i]
						)
					}
				});
				shifts = Array.newClear(maxShifts);
				if (bufmul.isKindOf(Pattern)) { bufmul = bufmul.asStream };
				recBufs = Array.fill(nRecs, {
					var mul = bufmul.next ? 1;
					if (mul <= 0) { mul = 1 };
					Buffer.alloc(Server.default,
						Server.default.sampleRate * matrix.beatdur * mul, 1)
				});
				Server.default.sync;
				currentEnvironment[\ins] = this;
				Post << numIns << " live inputs activated ..." << Char.nl;
			}).play
		}
	}

	startEfx{|key, index|
		matrix.activateSkismSynth(key, inbs[index]);
	}

	setEfx{|key, args|
		matrix.skismSynths[key].set(*args);
	}

	stopEfx{|key|
		matrix.deactivateSkismSynth(key);
	}

	startRecording{
		Tdef('rec', {
			loop({
				var mul = bufmul.next ? 1;
				if (mul <= 0) { mul = 1 };
				Synth.tail(matrix.efxgroup, 'inprec',
					['buffer', recBufs.choose, 'in', numIns.rand]);
				mul.wait;
			})
		}).play
	}

	stopRecording{
		Tdef('rec').clear
	}

	startShift{|amp=1.0, rate=1.0, shift=1.0|
		var index = shifts.indexOf(nil);
		if (index.notNil) {
			shifts[index] = Synth.tail(matrix.group, \shiftSample,
				[\amp, amp, \buffer, recBufs.choose, \rate, rate, \winsize, 0.1, \shift, shift,
					\roomsize, 100, \revtime, 4]);
			Post << "Started shift at index " << index << Char.nl;
		} {
			"No more available shift slots!".warn;
		}
	}

	setShift{|index, args|
		shifts[index].set(*args);
	}

	stopShift{|index|
		shifts[index].free;
		shifts[index] = nil;
		Post << "Stopped shift at index " << index << Char.nl;
	}

}