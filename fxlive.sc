FxLive{
	
	classvar <visuals, <actions;

	var <opts;
	var <vaddr, responder, sfuncs, server, speakers, maxlen;
	var cmd, avg, dev, nw, ne, sw, se, mnw, mne, msw, mse;
	var bfbus, inbus, efxbus, <ambigrp, <atsgrp, <srcgrp, <decoder, sf;
	var currentAts, machine, accavg = 0, statehist, checkcount, defname;
	var <activesynths, totaldur = 0, seconds, eventcount = 0, <paramaps;
	var <invalues, mainloop, cursor = 0, <visualdict, reverb, <>currentBuffer = 0;
	var <buffers, agui, <input, recordNode, recordBuf, <>bufferDur = 1.0;
	var ampresponder, ampcount = 0, ampsum = 0, amptrack, live = true;
	
	*initClass{
	
		visuals = (
			kanji: 0,
			ringz: 1,
			wobble: 2,
			grid: 3,
			horizons: 4,
			blinds: 5,
			axial: 6, 
			radial: 7,
			elastic: 8,
			mesh: 9
		);
		
		actions = (
			active: 0,
			color: 1,
			colormap: 2,
			alphamap: 3,
			colorlo: 4,
			colorhi: 5,
			alphalo: 6,
			alphahi: 7,
			sizelo: 8,
			sizehi: 9,
			fade: 10
		);
		
	}
	
	*new{
		^super.new.init;
	}
	
	init{
		opts = FxOptions();
		statehist = (1..6);
		checkcount = 0;
		seconds = Clock.seconds;
		vaddr = NetAddr(opts.fxip, opts.fxport);
		activesynths = Event();
		paramaps = Event();
		invalues = Event();
		visualdict = Event();
		visualdict.globals = (
			clear: 0.2,
			zoom: -12.0,
			fade: 0.0,
			add: 0.01
		);		
		visuals.keysDo({|key|
			var temp;
			temp = Event();
			actions.keysDo({|akey| temp.put(akey, 0.0) });
			visualdict.put(key, temp)
		});
		visualdict.world = (
			seed: 0,
			habitat: 0,
			radius: 1,
			left: 5,
			top: 5,
			width: 10,
			height: 10,
			weights: (1.0 ! 8)
		);
		sfuncs = (
			\sine: {|freqs, amps| SinOsc.ar(freqs, 0, amps) },
			\varsaw: {|freqs, amps| VarSaw.ar(freqs, 0, amps, amps) },
			\dust: {|freqs, amps| Resonz.ar(Dust.ar(freqs), freqs, 1 / freqs, amps) },
			\pulse: {|freqs, amps| Pulse.ar(freqs, amps, amps) }
		);
		speakers = IbiSpeakers.quad;
		sf = SoundFile.openRead(opts.filePath);
		this.initServer;
	}
	
	initResponder{
		responder = OSCresponderNode(nil, opts.fxcmd, {|ti, re, ms|
			#cmd, avg, dev, nw, ne, sw, se, mnw, mne, msw, mse = ms;
			invalues.avgstate = avg;
			invalues.stddev = dev;
			invalues.avgnw = nw;
			invalues.avgne = ne;
			invalues.avgsw = sw;
			invalues.avgse = se;
			invalues.avgmnw = mnw;
			invalues.avgmne = mne;
			invalues.avgmsw = msw;
			invalues.avgmse = mse;
			this.mapSynths;
//			this.checkStateHistory(avg);

		}).add;
		
		ampresponder = OSCresponderNode(server.addr, '/tr', {|ti, re, ms|
			ampcount = ampcount + 1;
			if (ampcount == 10)
			{
				ampcount = 0;
				ampsum = ampsum / 10;
				this.sendMsg("add", [0.008, 0.5, \lin].asSpec.map(ampsum));
				//[0.008, 0.5, \lin].asSpec.map(ampsum).postln;
			}
			{
				ampsum = ampsum + abs(ms[3]);
			}
		}).add
	}
	
	initServer{
		server = Server.internal;
		Server.default = server;
		maxlen = opts.defaultSampleRate * opts.maxlen;
		server.options.memSize = 32769.nextPowerOfTwo;
		server.options.numOutputBusChannels = 4;
		server.options.numWireBufs = 128;
		if (server.serverRunning)
		{
//			Routine.run {
//				server.quit;
//				server.wait(\done);
//				0.5.wait;
//				server.waitForBoot({ this.startMainEventLoop })
//			}
		}	
		{
			server.waitForBoot({ this.startMainEventLoop })
		}	
	}
	
	startMainEventLoop{
		this.sendSynthDefs;
		this.initResponder;
		
//		SystemClock.sched(1, {
//			this.sendGlobalFadeIn;
//			this.startAudio;
//			nil		
//		});
//		("open" + opts.fxapp).unixCmdThen({
//			this.sendGlobalFadeIn;
//			this.startAudio;
//		}, 0.05);
	}
	
	gui{
		agui = FxLiveGui(this)
	}
	
	startVisual{
		("open" + opts.fxapp).unixCmd;
	}
	
	startAudio{
//		var paths;
//		paths = (opts.atsdir ++ "*").pathMatch;
		var lbuf;
		ambigrp = Group();
		atsgrp = Group.before(ambigrp);
		srcgrp = Group.before(srcgrp);
		bfbus = Bus.audio(server, 4);
		inbus = Bus.audio(server);	
		efxbus = Bus.audio(server);
		mainloop = Routine({
			decoder = Synth.tail(ambigrp, \fxdecoder, [\in, bfbus]);
			reverb = Synth.head(ambigrp, \fxdustspace, [\out, bfbus, \in, efxbus, 
				\dust, 20, \pow, 1.4, \amp, 1.0]);
			
			if (live) {			
				input = Synth.head(srcgrp, \fxlivein, [\out, bfbus, \efx, efxbus, \amp, 0.0, 
					\efxamp, 0.0])
			}
			{
				lbuf = Buffer.cueSoundFile(server, opts.filePath, 0, 1);
				server.sync;
				input = Synth.head(srcgrp, \fxdiskin, [\out, bfbus, \efx, efxbus, \buffer, lbuf,
				 	\rate, 1.0, \amp, 0.0, \efxamp, 0.0])
			}
//			inf.do({|i|
//				//this.playProcessedSegment(paths.wrapAt(i));
//				this.playLiveSegment;
//				rrand(opts.defaultSegDur * 0.7, opts.defaultSegDur * 0.85).round(0.25).wait;
//			});
		}).play
	}
	
	playLiveSegment{	
		var name, buf;
		name = ("fxd" ++ Date.getDate.secStamp).asSymbol;
		buf = Buffer.read(server, opts.filePath, cursor, 30 * sf.sampleRate);
		server.sync;
		activesynths.put(name, 
			Synth.head(srcgrp, \fxplayer, [\out, bfbus, \efx, efxbus, \buffer, buf, \rate, 1.0, 
				\dur, opts.defaultSegDur, \amp, 0.5, \efxamp, 0.5, \azm, mnw, 
				\rad, mne, \elv, 0])
				.setn(\env, Env([0.0, 1.0, 1.0, 0.0], [0.3, 0.4, 0.3], \lin).asArray)
		);
		paramaps.put(name, Event());
		paramaps[name].put(\azm, 
			FxParam(\azm, \avgmnw, ControlSpec(-pi, pi, \lin), false, this)
		);
//		paramaps[name].put(\rad, 
//			FxParam(\rad, \avgmne, ControlSpec(0.0, 2.0, \lin), true, this)
//		);
		SystemClock.sched(opts.defaultSegDur, {
			this.removeSynth(name);
			buf.free;
			nil
		});
		cursor = cursor + (opts.defaultSegDur * opts.defaultSampleRate);
		if (cursor > (sf.numFrames - (opts.defaultSampleRate * 60))) { cursor = 0 };
	
	}
	
	sendGlobalFadeIn{
		var cl, zm, ad;
		Routine({
			var times, step, fade;
			2.wait;
			//set background
			vaddr.sendMsg(opts.sendcmd ++ "/clear", visualdict.globals[\clear]);
			//set zoom
			vaddr.sendMsg(opts.sendcmd ++ "/zoom", visualdict.globals[\zoom]);
			//set add
			vaddr.sendMsg(opts.sendcmd ++ "/add", visualdict.globals[\add]);
			//fade in patch
			this.fadeInPatch(\elastic, opts.fadeTime);
			//fade in global
			times = opts.fadeTime * opts.msgRate;
			fade = 0.0;
			step = times.reciprocal;
			times.do({
				vaddr.sendMsg(opts.sendcmd ++ "/fade", fade);
				fade = fade + step;
				opts.msgRate.reciprocal.wait
			});
		}).play;
	}
	
	recordBuffer{
		Routine({
			var bufdur, syn, buf;
			bufdur = server.sampleRate * bufferDur;
			buf = Buffer.alloc(server, bufdur);
			server.sync;
			syn = Synth.tail(srcgrp, \fxcapture, [\bufnum, buf, 
				\recamp, 1.0, \preamp, 0.0, \loop, 0.0]);
			(bufdur / server.sampleRate).wait;
			syn.free;
			buffers = buffers.add(buf);
		}).play
	}
	
	playWarpedSegment{
	
		var name, buf, start, dur, frac, sparams, skeys, vctrs, warp, freq;
		name = ("fxwrp" ++ Date.getDate.secStamp).asSymbol;
		
		warp = rrand(1 / (2**8), 1.0).round(1 / (2**8));
		
		dur = rrand(25, 35);
		
		activesynths.put(name, 
			Synth.head(atsgrp, \fxwarper, [\out, bfbus, \efx, efxbus, \buffer, currentBuffer, 
				\wrp, warp, \frq, [0.25, 0.5, 1.0].choose, \wsz, 0.1, \dns, rrand(4, 8), 
				\rnd, 0.01, \amp, 0.0, \efxamp, 0.1, \dur, dur, \azm, rrand(-pi, pi), 
				\rad, rrand(0.3, 0.7), \elv, 0])
				.setn(\env, Env([0.0, 1.0, 1.0, 0.0], [0.3, 0.4, 0.3], \lin).asArray)
		);

		paramaps.put(name, Event());
		sparams = (
			\wrp: ControlSpec(warp * 0.5, warp * 2.0, \lin),
			\wsz: ControlSpec(0.01, 0.2, \lin),
			\rnd: ControlSpec(0.0, 0.02, \lin),
			\azm: ControlSpec(-pi, pi, \lin), 
			\rad: ControlSpec(0.3, 0.9, \lin)
		);
		vctrs = invalues.keys(Array);
		skeys = sparams.keys(Array).scramble;
		skeys.do({|key|
			paramaps[name].put(key, 
				FxParam(key, vctrs.pop, sparams[key], [true, false].choose, this)
			)
		});		

		SystemClock.sched(dur, {
			this.removeSynth(name);
			nil
		});	
		
	}	
	
	playWarp{
		var buf, name, dur;
		if (buffers.size > 0)
		{
			buf = buffers[buffers.lastIndex];
			name = ("fxwarp" ++ buffers.lastIndex.asString).asSymbol;
			dur = [20.0, 30.0, \lin].asSpec.map(avg);
			activesynths.put(name,	
				Synth.tail(atsgrp, \fxwarper, [\out, bfbus, \buffer, buf, \dur, dur, \wrp, avg, 
					\frq, dev, \wsz, nw, \dns, 5, \rnd, ne, \amp, sw, \azm, se, \elv, 0, 
					\rad, sw])
					.setn(\env, Env([0.0, 1.0, 1.0, 0.0], [0.3, 0.4, 0.3]).asArray)
			);
			paramaps.put(name, Event());
			paramaps[name].put(\wrp, 
				FxParam(\wrp, \avgstate, ControlSpec(0.001, 0.1, \sine), false, this)
			);
			paramaps[name].put(\frq, 
				FxParam(\frq, \stddev, ControlSpec(0.1, 2.0, \sine), false, this)
			);
			paramaps[name].put(\wsz, 
				FxParam(\wsz, \avgnw, ControlSpec(0.01, 0.3, \sine), false, this)
			);
			paramaps[name].put(\rnd, 
				FxParam(\rnd, \avgne, ControlSpec(0.001, 0.1, \sine), false, this)
			);
			paramaps[name].put(\amp, 
				FxParam(\amp, \avgsw, ControlSpec(0.1, 1.0, \sine), false, this)
			);
			paramaps[name].put(\azm, 
				FxParam(\azm, \avgse, ControlSpec(-pi, pi, \sine), false, this)
			);
			paramaps[name].put(\rad, 
				FxParam(\rad, \avgsw, ControlSpec(0.0, 2.0, \lin), false, this)
			);
			SystemClock.sched(dur, {
				this.removeSynth(name);
				nil
			});
			^name
		}		
	}
	
	startRecording{
		recordBuf = Buffer.alloc(server, 65536, 1,
			{arg buf; buf.writeMsg(opts.fileRecPath, "aiff", "float", 0, 0, true);},
			server.options.numBuffers + 1);
		SynthDef("fx-record", { arg bufnum;
			DiskOut.ar(bufnum, SoundIn.ar([0])) 
		}).send(server);
		SystemClock.sched(0.2, { 
			recordNode = Synth.tail(RootNode(server), "fx-record", [\bufnum, 
				recordBuf.bufnum]);
			"Recording".postln
		})
	}
	
	stopRecording{
		if (recordNode.notNil)
		{
			recordNode.free;
			recordNode = nil;
			recordBuf.close({|buf| buf.free });
			recordBuf = nil;
			"Recording Stopped".postln
		}
	}
	
	fadeInPatch{|patch, time|
		visualdict[patch].active = 1.0;
		this.activatePatch(patch, 1.0);
		SystemClock.sched(0.2, {
			this.gradualPatchTransform(patch, \fade, 0.0, 1.0, opts.fadeTime);
			nil
		})
	}
	
	fadeOutPatch{|patch, time|
		this.gradualPatchTransform(patch, \fade, 1.0, 0.0, opts.fadeTime, {
			this.activatePatch(patch, 0.0);	
			visualdict[patch].active = 0.0;	
		});
	}
	
	gradualPatchTransform{|patch, command, startValue, targetValue, time, doneAction|
		Routine({
			var times, step, value;
			times = time * opts.msgRate;
			step = targetValue - startValue / times;
			value = startValue;
			times.do({
				vaddr.sendMsg(opts.sendcmd ++ "/patch", visuals[patch], actions[command], value);
				value = value + step;
				opts.msgRate.reciprocal.wait
			});
			opts.msgRate.reciprocal.wait;
			visualdict[patch][command] = targetValue;
			doneAction.value
		}).play
	}
	
	sendMsg{|cmd ... msg|
		vaddr.sendMsg(opts.sendcmd ++ "/" ++ cmd, *msg)
	}
	
	activatePatch{|patch, active|
		visualdict[patch].active = active;
		vaddr.sendMsg(opts.sendcmd ++ "/patch", visuals[patch], actions.active, active);
	}
	
	setPatch{|patch, action, value|
		visualdict[patch][action] = value;
		vaddr.sendMsg(opts.sendcmd ++ "/patch", visuals[patch], actions[action], value);
	}
	
	gradualTransform{|command, currentValue, targetValue, time|
		Routine({
			var times, step, value;
			times = time * opts.msgRate;
			step = targetValue - currentValue / times;
			value = currentValue;
			times.do({
				vaddr.sendMsg(command, value);
				value = value + step;
				opts.msgRate.reciprocal.wait
			});
			opts.msgRate.reciprocal.wait;
		}).play
	}
	
	resetWorld{|seed, habitat, radius, left, top, width, length ... weights|
		visualdict.world.seed = seed;
		visualdict.world.habitat = habitat;
		visualdict.world.radius = radius;
		visualdict.world.left = left;
		visualdict.world.top = top;
		visualdict.world.width = width;
		visualdict.world.length = length;
		visualdict.world.weights = weights;
		vaddr.sendMsg(opts.sendcmd ++ "/world", 1, seed, habitat, radius, 
			left, top, width, length, *weights)
	}
	
	setWeights{|... weights|
		visualdict.world.weights = weights;
		vaddr.sendMsg(opts.sendcmd ++ "/world", 2, 0, 0, 0, 0, 0, 0, 0, 0, *weights )
	}
	
	setAdd{|value|
		visualdict.globals[\add] = value;
		vaddr.sendMsg(opts.sendcmd ++ "/add", value)
	}
		
	quitVisual{
		vaddr.sendMsg(opts.sendcmd ++ "/quit", 0)
	}
	
	checkStateHistory{|current|
		var sd, hb, tp, lf, wd, hg, occ;
		
		occ = statehist.occurrencesOf(current);
		
		if (occ > 2)
		{
			if (avg > 0.5) { sd = 0 } { sd = 1 };
			if (dev < 0.5) { hb = 0 } { hb = 1 };
			lf = (nw * 10).asInteger;
			tp = (ne * 10).asInteger;
			wd = (sw * 10).asInteger;
			hg = (se * 10).asInteger;
			this.resetWorld(sd, hb, 1, lf, tp, wd, hg, mnw, mne, msw, mse, mse, msw, mne, mnw);
			this.setAdd(0.01);
			statehist = (1..6);
		}
		{
			statehist.insert(0, current).pop;	
		};
	}
		
	analyzeSegment{|synthAction|
		var start, len, pths, vals, found, next;
		start = (sf.numFrames * avg).floor;
		len = (maxlen * dev).floor;
		if (start + len > sf.numFrames) { len = sf.numFrames - start };
		pths = (opts.atsdir ++ "*").pathMatch;
		if (pths.size > 0)
		{
			pths.do({|path, i|  
				vals = path.basename.split($.)[0].split($_);
				vals = vals.collect(_.asInteger);
				if ((start > vals[0]).and(start < vals[1]))
				{
					found = vals;
					if (pths.size > 1)
					{
						next = pths[i + 1].basename.split($.)[0].split($_);
						next = next.collect(_.asInteger);
					}
				}
			});		
		};
		if (found.notNil)
		{
			start = found[1];
			if (start + len > next[0])
			{
				len = next[0] - start
			}
		};
		Buffer.readChannel(server, opts.filePath, start, len, [0], {|buf|
			this.bufferToAts(buf, start.asString ++ "_" ++ (start + len).asString, {
				
			})
		});
	}
	
	bufferToAts{|buffer, filename, action|
		fork({
			var tempPath, atsPath, atsfile;
			tempPath = opts.tempdir ++ "b2a_" ++ Date.getDate.stamp;
			atsPath = opts.atsdir ++ filename ++ ".ats";
			buffer.write(tempPath);
			server.sync;
			(BufferToAts.atsapath + tempPath + atsPath).unixCmdThen({
				("rm " ++ tempPath).unixCmd;	
				action.value	
			});
		})
	}
	
	makeSynthDef{
		Routine({
			1.wait;
			defname = currentAts.filename.basename.split($.)[0].asSymbol;
			this.makeAtsDef(defname, sfuncs.sine, nil, 2, {
				this.activateAtsSynth(defname)
			});
			
		}).play
	}
	
	playProcessedSegment{|path|
		var atsfile, name, func, nparts;
		Routine({	
			name = path.basename.split($.)[0].asSymbol;
			atsfile = AtsFile(path, server).loadToBuffer;
			server.sync;
			nparts = min(opts.maxPartials, atsfile.numPartials);
			func = sfuncs[sfuncs.keys(Array).choose];
			SynthDef(name, {|out, efx, buffer, rate, amp, efxamp, dur, fscale, ascale, 
				azm, elv, rad|
				var env, ptr, sig, amps, freqs, bf, partials;
				env = Control.names([\env]).kr(Env.newClear(8).asArray);
				ptr = LFSaw.kr(rate, 0, 0.5, 0.5);
				partials = (0..nparts);
				freqs = AtsFreq.kr(buffer, partials, ptr, fscale);
				amps = AtsAmp.kr(buffer, partials, ptr, ascale);
				sig = Mix(SynthDef.wrap(func, nil, [freqs, amps]))
					* EnvGen.kr(env, timeScale: dur, doneAction: 2);
				bf = BFEncode1.ar(sig, azm, elv, rad);
				Out.ar(efx, sig * efxamp);
				Out.ar(out, bf * amp)
			}).send(server);
			server.sync;
			this.activateAtsSynth(name, atsfile.buffer, atsfile.sndDur);
			
		}).play;
	}
	
	activateAtsSynth{|name, buffer, bufdur|
		var dr, rate;
		dr = 30;
		rate = (dr * bufdur).reciprocal; 
		activesynths.put(name, 
			Synth.head(atsgrp, name, [\out, bfbus, \buffer, buffer, \rate, rate, 
				\amp, 1.0, \azm, rrand(-pi, pi), \elv, 0, \rad, 0.2, \dur, dr,
				\fscale, 0.5, \ascale, 1.0 ])
				.setn(\env, Env([0.0, 1.0, 1.0, 0.0], [0.3, 0.4, 0.3], \sine).asArray)
		);
//		paramaps.put(name, Event());
//		paramaps[name].put(\amp, 
//			FxParam(\amp, \avgstate, ControlSpec(0.3, 0.7, \lin), false, this));
//		paramaps[name].put(\azm, 
//			FxParam(\azm, \stddev, ControlSpec(-pi, pi, \exp), false, this));
		SystemClock.sched(dr, {
			this.removeSynth(name);
			nil
		})
	}
	
	removeSynth{|name|
		activesynths.removeAt(name);
		paramaps.removeAt(name);
	}
	
	mapSynths{
		paramaps.keysValuesDo({|sname, params|
			params.keysValuesDo({|pname, fxp|
				activesynths[sname].set(pname, fxp.map)
			})
		});
	}
	
	sendSynthDefs{
		SynthDef(\fxdecoder, {|in, amp = 1.0|
			var w, x, y, z, decode;
			#w, x, y, z = In.ar(in, 4);
			decode = BFDecode1.ar(w, x, y, z, speakers.azimuth, speakers.elevation);
			Out.ar(0, decode)
		}).send(server);
		SynthDef(\fxlivein, {|out, efx, amp, efxamp|
			var sig, bf, amptr;
			sig = SoundIn.ar([0]);
			amptr = Amplitude.ar(sig);
			SendTrig.kr(Impulse.kr(10), 0, sig);
			bf = BFEncode1.ar(sig, LFNoise2.kr(23.reciprocal, pi), 0, 
				LFNoise2.kr(19.reciprocal, 0.5, 0.5));
			Out.ar(efx, sig * efxamp);
			Out.ar(out, bf * amp)
		}).send(server);
		SynthDef(\fxdiskin, {|out, efx, buffer, rate, dur, amp, efxamp, azm, elv, rad|
			var sig, bf, env;
			sig = VDiskIn.ar(1, buffer, rate);
			bf = BFEncode1.ar(sig, LFNoise2.kr(23.reciprocal, pi), 0, 
				LFNoise2.kr(19.reciprocal, 0.5, 0.5));
			Out.ar(efx, sig * efxamp);
			Out.ar(out, bf * amp)
		}).send(server);
		SynthDef(\fxplayer, {|out, efx, buffer, rate, dur, amp, efxamp, azm, elv, rad|
			var sig, bf, env;
			env = Control.names([\env]).kr(Env.newClear(8).asArray);
			sig = PlayBuf.ar(1, buffer);
			bf = BFEncode1.ar(sig, azm, elv, rad) * 
				EnvGen.kr(env, timeScale: dur, doneAction: 2);
			Out.ar(efx, sig * efxamp);
			Out.ar(out, bf * amp)
		}).send(server);
		SynthDef(\fxwarper, {|out, efx, buffer, dur, wrp, frq, wsz, dns, rnd, amp, efxamp,
		 	azm, elv, rad|
			var sig, bf, env;
			env = Control.names([\env]).kr(Env.newClear(8).asArray);
			sig = Warp1.ar(1, buffer, LFSaw.kr(wrp, 0, 0.5, 0.5), frq, wsz, -1, dns, rnd, 4) 
				* EnvGen.kr(env, timeScale: dur, doneAction: 2);
			bf = BFEncode1.ar(sig, azm, elv, rad);
			Out.ar(efx, sig * efxamp);
			Out.ar(out, bf * amp)
		}).send(server);
		SynthDef(\fxdustspace, {|out = 0, in, dust = 10, pow = 1.5, amp = 1.0|
			var input, l, r, bf;
			input = In.ar(in);
			#l, r = GVerb.ar(input ** pow + Dust.ar(dust, input), 1000, 
				LFNoise2.kr(0.05, 10.0, 35.0), 0.31, 0.5, 15, 0.1, 0.4, 1.0, 1000) * amp;
			bf = BFEncodeSter.ar(l, r, LFNoise2.kr(31.reciprocal, pi), 
				LFNoise2.kr(29.reciprocal, 0.25pi, 0.25pi), 0, 
				LFNoise2.kr(23.reciprocal, 0.5, 0.5));
			Out.ar(out, bf);
		}).send(server);
		SynthDef(\fxcapture, {|bufnum, recamp, preamp, loop = 1.0|
			var input;
			input = SoundIn.ar([0]);
			RecordBuf.ar(input, bufnum, 0, recamp, preamp, 1.0, loop);
		}).send(server);	
	
	}
	
	quit{
		this.quitVisual;
		responder.remove;
		mainloop.stop;
		SystemClock.sched(30, {
			server.quit;
		});
	}
	
}

FxLiveGui{
	
	var fx;
	var win, font, weights, seed, habitat, left, top, width, height, bufs;
	
	*new{|fx|
		^super.newCopyArgs(fx).init
	}
	
	init{
		var gui, atxt, etxt, awtxt, ewtxt;
		weights = (1.0 ! 8);
		seed = 0;
		habitat = 0;
		left = 5;
		top = 5;
		width = fx.opts.defaultWorldSize - 10;
		height = fx.opts.defaultWorldSize - 10;
		font = Font("Papyrus", 12);
		win = THSWindow("._.._..._.f(x)._..._.._.", Rect(100, 500, 900, 400), Color.grey(0.3));
		gui = GUI.current;
		gui.button.new(win, Rect(10, 10, 80, 20))
			.font_(font)
			.states_([["start visuals", Color.grey(0.8), Color.blue(0.2)]])
			.action_({
				Routine.run{
					fx.startVisual;
					1.wait;
					fx.sendMsg("clear", fx.visualdict.globals[\clear]);
					//set zoom
					fx.sendMsg("zoom", fx.visualdict.globals[\zoom]);
					//set add
					fx.sendMsg("add", fx.visualdict.globals[\add]);
				}				
			});
		gui.slider.new(win, Rect(95, 10, 150, 20))
			.action_({|sl|
				fx.sendMsg("fade", sl.value )
			});
		gui.staticText.new(win, Rect(95, 10, 150, 20))
			.stringColor_(Color.grey(0.8))
			.string_("fade")
			.align_(\center)
			.font_(font);
		gui.slider.new(win, Rect(250, 10, 150, 20))
			.value_(fx.visualdict.globals[\clear])
			.action_({|sl|
				fx.sendMsg("clear", sl.value )
			});
		gui.staticText.new(win, Rect(250, 10, 150, 20))
			.stringColor_(Color.grey(0.8))
			.string_("clear")
			.align_(\center)
			.font_(font);
		gui.slider.new(win, Rect(405, 10, 150, 20))
			.value_([-1.0, -32.0, \lin, 1].asSpec.unmap(fx.visualdict.globals[\zoom]))
			.action_({|sl|
				fx.sendMsg("zoom", [-2.0, -64.0, \lin].asSpec.map(sl.value) )
			});
		gui.staticText.new(win, Rect(405, 10, 150, 20))
			.stringColor_(Color.grey(0.8))
			.string_("zoom")
			.align_(\center)
			.font_(font);
		gui.slider.new(win, Rect(560, 10, 150, 20))
			.value_([0.001, 0.2, \lin, 0.001].asSpec.unmap(fx.visualdict.globals[\add]))
			.action_({|sl|
				fx.sendMsg("add", [0.001, 0.999, \cos].asSpec.map(sl.value) )
			});
		gui.staticText.new(win, Rect(560, 10, 150, 20))
			.stringColor_(Color.grey(0.8))
			.string_("add")
			.align_(\center)
			.font_(font);
			
		gui.button.new(win, Rect(800, 10, 75, 20))
			.font_(font)
			.states_([["quit", Color.grey(0.8), Color.red]])
			.action_({
				fx.quit
			});
		FxLive.visuals.keysValuesDo({|key, val, i|
			var container;
			container = gui.compositeView.new(win, Rect(10 + (i * 88), 35, 88, 100));
			container.decorator = FlowLayout(container.bounds);
			gui.staticText.new(container, Rect(width: 78, height: 20 ))
				.stringColor_(Color.grey(0.8))
				.string_(key.asString)
				.align_(\center)
				.font_(font);
			gui.button.new(container, Rect(width: 78, height: 25))
				.font_(font)
				.states_([["activate", Color.grey(0.8), Color.grey(0.2)], 
					["turn off", Color.green, Color.grey(0.2)]])
				.action_({|btn|
					if (btn.value == 1) {
						fx.fadeInPatch(key, fx.opts.fadeTime)
					}
					{
						fx.fadeOutPatch(key, fx.opts.fadeTime)
					};
					btn.enabled = false;
					AppClock.sched(fx.opts.fadeTime, { btn.enabled = true; nil })					
				});
			gui.button.new(container, Rect(width: 38, height: 25))
				.font_(font)
				.states_([
					["color", Color.grey(0.8), Color.grey(0.5)], 
					["color", Color.grey(0.8), Color.blue],
					["color", Color.grey(0.8), Color.green]
				])
				.action_({|btn|
					fx.setPatch(key, \color, btn.value.asFloat)
				});
			gui.button.new(container, Rect(width: 38, height: 25))
				.font_(font)
				.states_([
					["c map", Color.grey(0.8), Color.grey(0.2)], 
					["c map", Color.grey(0.2), Color.grey(0.8)]
				])
				.action_({|btn|
					fx.setPatch(key, \colormap, btn.value.asFloat)
				});
			gui.button.new(container, Rect(width: 78, height: 25))
				.font_(font)
				.states_([
					["alpha map", Color.grey(0.8), Color.grey(0.2)], 
					["alpha map", Color.grey(0.2), Color.grey(0.8)]
				])
				.action_({|btn|
					fx.setPatch(key, \alphamap, btn.value.asFloat)
				});
				
		});
		
		
		gui.button.new(win, Rect(10, 150, 80, 25))
			.font_(font)
			.states_([["audio", Color.white, Color.black]])
			.action_({
				fx.startAudio
			});
		
		// input controls
		
		gui.slider.new(win, Rect(10, 180, 35, 150))
			.value_(0.0)
			.action_({|sl|
				if (fx.input.notNil) { 
					fx.input.set(\amp, sl.value);
					atxt.string_(sl.value.ampdb.round(0.1).asString)
				}
			});
		
		atxt = gui.staticText.new(win, Rect(10, 265, 35, 25))
			.font_(font)
			.string_("-inf")
			.stringColor_(Color.grey(0.8))
			.align_(\center);
		
		gui.staticText.new(win, Rect(10, 305, 35, 25))
			.font_(font)
			.string_("amp")
			.stringColor_(Color.grey(0.6))
			.align_(\center);
		
		gui.slider.new(win, Rect(50, 180, 35, 150))
			.value_(0.0)
			.action_({|sl|
				if (fx.input.notNil) { 
					fx.input.set(\efxamp, sl.value);
					etxt.string_(sl.value.ampdb.round(0.1).asString);
				}
			});
		
		etxt = gui.staticText.new(win, Rect(50, 265, 35, 25))
			.font_(font)
			.string_("-inf")
			.stringColor_(Color.grey(0.8))
			.align_(\center);

		gui.staticText.new(win, Rect(50, 305, 35, 25))
			.font_(font)
			.string_("efx")
			.stringColor_(Color.grey(0.6))
			.align_(\center);
			
		gui.staticText.new(win, Rect(10, 335, 75, 25))
			.font_(font)
			.string_("input")
			.stringColor_(Color.grey(0.4))
			.align_(\center);
			
		// warp controls
			
		gui.slider.new(win, Rect(350, 180, 35, 150))
			.value_(0.0)
			.action_({|sl|
				fx.atsgrp.set(\amp, sl.value);
				awtxt.string_(sl.value.ampdb.round(0.1).asString)
			});
		
		awtxt = gui.staticText.new(win, Rect(350, 265, 35, 25))
			.font_(font)
			.string_("-inf")
			.stringColor_(Color.grey(0.8))
			.align_(\center);
		
		gui.staticText.new(win, Rect(350, 305, 35, 25))
			.font_(font)
			.string_("amp")
			.stringColor_(Color.grey(0.6))
			.align_(\center);
		
		gui.slider.new(win, Rect(390, 180, 35, 150))
			.value_(0.0)
			.action_({|sl|
				fx.atsgrp.set(\efxamp, sl.value);
				ewtxt.string_(sl.value.ampdb.round(0.1).asString);
			});
		
		ewtxt = gui.staticText.new(win, Rect(390, 265, 35, 25))
			.font_(font)
			.string_("-inf")
			.stringColor_(Color.grey(0.8))
			.align_(\center);

		gui.staticText.new(win, Rect(390, 305, 35, 25))
			.font_(font)
			.string_("efx")
			.stringColor_(Color.grey(0.6))
			.align_(\center);
			
		gui.staticText.new(win, Rect(350, 335, 75, 25))
			.font_(font)
			.string_("warp")
			.stringColor_(Color.grey(0.4))
			.align_(\center);			

		gui.button.new(win, Rect(100, 150, 80, 25))
			.font_(font)
			.states_([["rec", Color.white, Color.black], ["rec", Color.red, Color.black]])
			.action_({|btn|
				if (btn.value == 1)
				{
					fx.startRecording
				}
				{
					fx.stopRecording
				}
			});
		
		gui.button.new(win, Rect(200, 150, 75, 25))
			.font_(font)
			.states_([["record buf", Color.green, Color.black]])
			.action_({|btn|
				fx.recordBuffer;
				btn.enabled = false;
				AppClock.sched(3.0, { 
					bufs.items = fx.buffers.collect({|buf| 
						(buf.bufnum.asString ++ "_" ++ buf.numFrames.asString) 
					});
					btn.enabled = true; 
					nil 
				})
			});
		
		gui.numberBox.new(win, Rect(280, 150, 60, 25))
			.font_(font)
			.value_(1.0)
			.align_(\center)
			.background_(Color.grey(0.3))
			.action_({|box|
				fx.bufferDur = box.value
			});
			
		bufs = gui.popUpMenu.new(win, Rect(345, 150, 100, 25))
			.font_(font)
			.action_({|it|
				fx.currentBuffer = fx.buffers[it.value];
				fx.currentBuffer.bufnum.postln;	
			});
		
		gui.button.new(win, Rect(200, 200, 75, 25))
			.font_(font)
			.states_([["play warp", Color.blue, Color.black]])
			.action_({|btn|
				fx.playWarpedSegment;
			});
		
		// warp control
//		3.do({|i|
//			gui.button.new(win, Rect());
//		});
		
		// world control
		gui.button.new(win, Rect(550, 150, 50, 25))
			.font_(font)
			.states_([["reset", Color.grey(0.8), Color.red]])
			.action_({
				fx.resetWorld(seed, habitat, 1, left, top, width, height, 
					weights[0],
					weights[1],
					weights[2],
					weights[3],
					weights[4],
					weights[5],
					weights[6],
					weights[7]
					)
			});
		
		gui.button.new(win, Rect(550, 180, 50, 20))
			.font_(font)
			.states_([["rect", Color.grey(0.8), Color.grey(0.2)], 
				["rand", Color.grey(0.8), Color.grey(0.4)]])
			.action_({|btn|
				seed = btn.value
			});

		gui.button.new(win, Rect(550, 205, 50, 20))
			.font_(font)
			.states_([["moore", Color.grey(0.8), Color.grey(0.2)], 
				["neum", Color.grey(0.8), Color.grey(0.4)]])
			.action_({|btn|
				habitat = btn.value
			});

//		gui.numberBox.new(win, Rect(450, 220, 50, 20))
//			.font_(font)
//			.states_([["moore", Color.grey(0.8), Color.grey(0.2)], 
//				["neum", Color.grey(0.8), Color.grey(0.4)]])
//			.action_({|btn|
//				habitat = btn.value
//			});
		
		
		8.do({|i|
			var txt;
			gui.slider.new(win, Rect(i * 30 + 610, 150, 25, 150))
				.action_({|sl|
					weights.put(i, [1.0, 10.0, \lin, 0.1].asSpec.map(sl.value));
					txt.string = [1.0, 10.0, \lin, 0.1].asSpec.map(sl.value).asString;
				});
			txt = gui.staticText.new(win, Rect(i * 30 + 610, 200, 25, 25))
				.font_(font)
				.align_(\center)
				.string_("1")
				.stringColor_(Color.grey(0.8));
		})
		
	}
	
}