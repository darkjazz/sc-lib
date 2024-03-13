InEnvir {

	var numIns, inRevs, maxShifts, nRecs, bufmul, loadBufs, isHoa;
	var <ins, <inbs, <shifts, <>recBufs, matrix;
	var <nChannels = 1;

	const bufdir = "/Users/alo/snd/inbufs/buf";

	*new{|numIns=1, inRevs=#[1], maxShifts=4, nRecs=21, bufmul=3, loadBufs=false, isHoa=false|
		^super.newCopyArgs(numIns, inRevs, maxShifts, nRecs, bufmul, loadBufs, isHoa)
	}

	initFunc{
		^{
			Tdef('initIns', {
				matrix = currentEnvironment[\matrix];
				if (loadBufs) {
					var sf = SoundFile();
					sf.openRead((bufdir +/+ "*").pathMatch.first);
					nChannels = sf.numChannels;
					sf.close;
				};
				inbs = numIns.collect({ Bus.audio(Server.default) });
				Server.default.sync;
				if (isHoa)
				{
					this.addHoaSynthDefs
				}
				{
					this.addSynthDefs
				};
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
				if (loadBufs) {
					recBufs = Array();
					(bufdir +/+ "*").pathMatch.do({|path|
						var pos, next, sf;
						sf = SoundFile();
						next = true;
						pos = 0;
						sf.openRead(path);
						while ({next}, {
							var mul, start;
							mul = bufmul.next ? 1;
							if (mul <= 0) { mul = 1 };
							start = pos;
							pos = pos + (sf.sampleRate * matrix.beatdur * mul);
							if (pos < sf.numFrames) {
								recBufs = recBufs.add(Buffer.read(Server.default, path, start, pos - start))
							}
							{
								next = false
							}
						});

					})
				}
				{
					recBufs = Array.fill(nRecs, {
						var mul = bufmul.next ? 1;
						if (mul <= 0) { mul = 1 };
						Buffer.alloc(Server.default,
							Server.default.sampleRate * matrix.beatdur * mul, 1)
					})
				};
				Server.default.sync;
				currentEnvironment[\ins] = this;
				Post << numIns << " live inputs activated ..." << Char.nl;
			}).play
		}
	}

	addSynthDefs{
		var encoder;
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
			Out.ar(efx, input * emp);
			Out.ar(out, sig * amp)
		}).add;
		SynthDef('inprec', {|buffer, in|
			var input;
			input = SoundIn.ar(in);
			RecordBuf.ar(input, buffer, loop: 0, doneAction: 2);
		}).add;
		SynthDef('shiftSample', {|out, buffer, amp, rate, winsize, shift, roomsize, revtime, angrate=0.3|
			var sig, bf;
			sig = PlayBuf.ar(2, buffer, rate, loop: 1) * amp;
			sig = GVerb.ar(sig, roomsize, revtime).wrapAt([0, 3]);
			sig = PitchShift.ar(sig, winsize, shift);
			bf = FoaEncode.ar(sig, FoaEncoderMatrix.newDirection);
			bf = FoaRTT.ar(bf,
				LFNoise2.kr(angrate).range(-pi, pi),
				LFNoise2.kr(angrate).range(-pi, pi),
				LFNoise2.kr(angrate).range(-pi, pi));
			Out.ar(out, bf)
		}).add;
		if (nChannels == 1)
		{
			encoder = FoaEncoderMatrix.newDirection;
		}
		{
			encoder = FoaEncoderMatrix.newStereo;
		};
		SynthDef('playSample', {| out, buffer, rate, amp, rx, ry, rz |
			var sig, bf, matrix;
			sig = PlayBuf.ar(nChannels, buffer, rate, doneAction: 2) * amp;
			bf = FoaRTT.ar(
				FoaEncode.ar(sig, encoder),
				rx, ry, rz
			);
			Out.ar(out, bf)
		}).add;
	}

	addHoaSynthDefs{
		SynthDef('inp', {|out, efx, emp, amp, inbc|
			var hoa, sig = SoundIn.ar(inbc);
			Out.ar(efx, sig * emp);
			hoa = HoaEncodeMatrix.ar(sig, HoaMatrixEncoder.newDirection(order: 5));
			hoa = HoaXformMatrix.ar(hoa, HoaMatrixXformer.newRTT(
				LFNoise2.kr(LFNoise2.kr(2.exp.reciprocal).range(pi/2.exp, pi)).range(-pi, pi),
				LFNoise2.kr(LFNoise2.kr(2.exp.reciprocal).range(pi/2.exp, pi)).range(-pi, pi),
				LFNoise2.kr(LFNoise2.kr(2.exp.reciprocal).range(pi/2.exp, pi)).range(-pi, pi),
				5
			));
			Out.ar(out, hoa)
		}).store;
		Server.default.sync;
		SynthDef('inprev', {| out, efx, emp, amp, inbc, revamp=0.05, revtime=3.1 |
			var input, sig, hoa;
			input = SoundIn.ar(inbc);
			sig = GVerb.ar(input, 150, revtime, drylevel: amp, earlyreflevel: revamp,
				taillevel: revamp * 0.5/0.7
			);
			Out.ar(efx, input * emp);
			hoa = HoaEncodeMatrix.ar(sig, HoaMatrixEncoder.newDirections([[45, 0], [135, 0]].degrad, order: 5));
			hoa = HoaXformMatrix.ar(hoa, HoaMatrixXformer.newRTT(
				LFNoise2.kr(LFNoise2.kr(2.exp.reciprocal).range(pi/2.exp, pi)).range(-pi, pi),
				LFNoise2.kr(LFNoise2.kr(2.exp.reciprocal).range(pi/2.exp, pi)).range(-pi, pi),
				LFNoise2.kr(LFNoise2.kr(2.exp.reciprocal).range(pi/2.exp, pi)).range(-pi, pi),
				5
			));
			Out.ar(out, hoa)
		}).store;
		SynthDef('inprec', {|buffer, in|
			var input;
			input = SoundIn.ar(in);
			RecordBuf.ar(input, buffer, loop: 0, doneAction: 2);
		}).add;
		// SynthDef('shiftSample', {|out, buffer, amp, rate, winsize, shift, roomsize, revtime, angrate=0.3|
		// 	var sig, bf;
		// 	sig = PlayBuf.ar(2, buffer, rate, loop: 1) * amp;
		// 	sig = GVerb.ar(sig, roomsize, revtime).wrapAt([0, 3]);
		// 	sig = PitchShift.ar(sig, winsize, shift);
		// 	bf = PanAmbi3O.ar(sig, LFNoise2.kr(pi).range(-pi, pi));
		// 	Out.ar(out, bf)
		// }).add;
		// SynthDef('playSample', {| out, buffer, rate, amp, rx, ry, rz |
		// 	var sig, bf, matrix;
		// 	sig = PlayBuf.ar(nChannels, buffer, rate, doneAction: 2) * amp;
		// 	bf = PanAmbi3O.ar(sig, LFNoise2.kr(pi).range(-pi, pi));
		// 	Out.ar(out, bf)
		// }).add;

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