FxOptions{
	var <>fxip = "127.0.0.1"; 
	var <>fxport = 7770;
	var <>fxcmd = '/fx';
	var <>fxapp = "/Users/alo/ScienceGallery/Fx/build/Release/Fx.app";
	var <>sendcmd = "/fx";
	var <>filePath = "/Users/alo/sounds/simon_LIVE_SG_FINAL.aif";
	var <>maxPartials = 60;
	var <>atsdir = "/Users/alo/atsfiles/";
	var <>tempdir = "/Users/alo/sounds/";
	var <>fadeTime = 1;
	var <>msgRate = 15;
	var <>defaultSampleRate = 44100;
	var <>defaultAvgSum = 1000;
	var <>defaultWorldSize = 40;
	var <>maxlen = 4;
	var <>defaultSegDur = 30;
	var <>defaultMaxDurDev = 10;
	var <>dayDur = 14400; // 4 hours
	var <>cursorWindow = 1200; // 20 minutes
	var <>startMonth = 4;
	var <>startDay = 17;
}

Fx{
	
	classvar <visuals, <actions;

	var opts, gui;
	var <vaddr, responder, sfuncs, server, speakers, maxlen;
	var cmd, avg, dev, nw, ne, sw, se, mnw, mne, msw, mse;
	var bfbus, inbus, ambigrp, atsgrp, srcgrp, <decoder, sf, <reverb, efxbus;
	var currentAts, machine, accavg = 0, statehist, checkcount, defname;
	var activesynths, totaldur = 0, seconds, fxstart, eventcount = 0, paramaps;
	var <invalues, mainloop, graphloop, cursor = 0, <visualdict, currentSegPath;
	var globloop;
	
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
		{ gui = FxGUI(this) }.defer;
		statehist = (1..6);
		checkcount = 0;
		fxstart = AppClock().seconds;
		seconds = 0;
		vaddr = NetAddr(opts.fxip, opts.fxport);
		activesynths = Event();
		paramaps = Event();
		invalues = Event();
		visualdict = Event();
		visualdict.globals = (
			clear: 0.2,
			zoom: -10.0,
			fade: 0.0,
			add: 0.008
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
			\sine: {|freqs, amps| SinOsc.ar(freqs, 0, amps) * 0.5 },
			\varsaw: {|freqs, amps| VarSaw.ar(freqs, 0, amps, amps) * 0.5 },
			\dust: {|freqs, amps| Resonz.ar(Dust.ar(freqs), freqs, 0.01, amps) * 500 },
			\pulse: {|freqs, amps| Pulse.ar(freqs, amps, amps) * 0.5 }
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
			this.checkStateHistory(avg);

		}).add;
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
			Routine.run {
				server.quit;
				server.wait(\done);
				0.5.wait;
				server.waitForBoot({ this.startMainEventLoop })
			}
		}	
		{
			server.waitForBoot({ this.startMainEventLoop })
		}	
	}
	
//	startMainEventLoop{
//		this.sendSynthDefs;
//		this.initResponder;
//		("open" + opts.fxapp).unixCmdThen({
//			this.startVisual;
//			this.startAudio;
//		});
//	}
	startMainEventLoop{
		this.sendSynthDefs;
		this.initResponder;
		("open" + opts.fxapp).unixCmd;
		SystemClock.sched(3, {
			this.startVisual;
			this.startAudio;
			nil
		});
	}
	startVisual{
		var permarr, permute;
		permarr = visuals.keys(Array).scramble;
		Routine({
			var times, step, fade;
			2.wait;
			//set background
			vaddr.sendMsg(opts.sendcmd ++ "/clear", visualdict.globals[\clear]);
			//set zoom
			vaddr.sendMsg(opts.sendcmd ++ "/zoom", visualdict.globals[\zoom]);
			//set add
			vaddr.sendMsg(opts.sendcmd ++ "/add", visualdict.globals[\add]);
			//fade in global			
			times = opts.fadeTime * opts.msgRate;
			fade = 0.0;
			step = times.reciprocal;
			times.do({
				vaddr.sendMsg(opts.sendcmd ++ "/fade", fade);
				fade = fade + step;
				opts.msgRate.reciprocal.wait
			});
			visualdict.globals[\fade] = 1.0;
		}).play;

		graphloop = Routine({
			4.wait;
			inf.do({|i|
				permute = permarr.permute(i.asInteger);
				permute.do({|patch|
					var totaldur = 0, wait;
					patch.postln;
					this.fadeInPatch(patch, opts.fadeTime);
					wait = rrand(100, 140);
					totaldur = wait;
					wait.wait;
					if (0.7.coin) {
						this.setPatch(patch, [\alphamap, \colormap].choose, [0.0, 1.0].choose)
					}
					{
						if (0.2.coin) { this.setPatch(patch, \color, 3.rand) }
					};
					wait = rrand(100, 140);
					totaldur = totaldur + wait;
					wait.wait;
					if (0.5.coin)
					{
						this.setPatch(patch, [\alphamap, \colormap].choose, [0.0, 1.0].choose)
					};
					(300 - totaldur).wait;
					SystemClock.sched(60, {
						this.fadeOutPatch(patch, opts.fadeTime);						nil
					});	
				});
			})
		}).play;
		
		globloop = Routine({
			var start, end, wts;
			120.wait;
			inf.do({
				[
				{ 
					start = visualdict.globals[\zoom];
					end = Array.series(12, -2, -2).wchoose(
							Array.geom(6, 1, 1.618).mirror2.normalizeSum
					);
					(seconds.asString ++ ": set global zoom - " ++ end.asString).postln;
					visualdict.globals[\zoom] = end;
					this.gradualTransform("zoom", start, end, rrand(20, 40))
				},
				{
					start = visualdict.globals[\clear];
					end = [rrand(0.05, 0.3), rrand(0.7, 0.9)].wchoose(
						(start < 0.5).if({ [0.8, 0.2] }, { [0.4, 0.6] })
					);
					(seconds.asString ++ ": set global clear - " ++ end.asString).postln;
					visualdict.globals[\clear] = end;
					this.gradualTransform("clear", start, end, rrand(20, 40))
				},
				{
					start = visualdict.globals[\fade];
					end = Array.series(6, 0.75, 0.05).wchoose(
						Array.geom(6, 1, 19/13).normalizeSum
					);
					(seconds.asString ++ ": set global fade - " ++ end.asString).postln;
					visualdict.globals[\fade] = end;
					this.gradualTransform("fade", start, end, rrand(20, 40));
				},
				{
					start = visualdict.globals[\add];
					end = rrand(0.007, 0.02);
					(seconds.asString ++ ": set global add - " ++ end.asString).postln;
					visualdict.globals[\add] = end;
					this.gradualTransform("add", start, end, rrand(20, 40));
				},
				{
					(seconds.asString ++ ": scheduled reset world").postln;
					wts = [
						(1.0 ! 8),
						Array.rand(8, 1.0, 3.0),
						Array.rand(4, 1.0, 4.0).mirror2,
						[nw, ne, sw, se].scramble.mirror2 * 10,
						[mnw, mne, msw, mse].scramble.mirror2 * 10,
						[nw, mnw, ne, mne, mse, se, msw, sw] * 10,
						[avg, dev, avg, dev, dev, avg, dev, avg] * 10
					].wchoose([0.25, 0.1, 0.15, 0.15, 0.1, 0.15, 0.1]).round(0.1);
					this.resetWorld(2.rand, 0, 1, 10.rand, 10.rand, 10.rand, 10.rand, 
						wts[0], wts[1], wts[2], wts[3], wts[4], wts[5], wts[6], wts[7]
					)
				}, 
				{
					(seconds.asString ++ ": set weights").postln;
					wts = [
						(1.0 ! 8),
						Array.rand(8, 1.0, 3.0),
						Array.rand(4, 1.0, 4.0).mirror2,
						[nw, ne, sw, se].scramble.mirror2 * 10,
						[mnw, mne, msw, mse].scramble.mirror2 * 10,
						[nw, mnw, ne, mne, mse, se, msw, sw] * 10,
						[avg, dev, avg, dev, dev, avg, dev, avg] * 10
					].wchoose([0.25, 0.1, 0.15, 0.15, 0.1, 0.15, 0.1]).round(0.1);
					this.setWeights(wts[0], wts[1], wts[2], wts[3], 
						wts[4], wts[5], wts[6], wts[7]);
				}
				].wchoose([0.2, 0.2, 0.05, 0.25, 0.1, 0.2 ]).value;
				rrand(60.0, 100.0).wait;
			})
		}).play
			
	}	
	
	startAudio{
//		var paths;
//		paths = (opts.atsdir ++ "*").pathMatch;
		ambigrp = Group();
		atsgrp = Group.before(ambigrp);
		srcgrp = Group.before(srcgrp);
		bfbus = Bus.audio(server, 4);
		inbus = Bus.audio(server);	
		efxbus = Bus.audio(server);
		mainloop = Routine({
			decoder = Synth.tail(ambigrp, \fxdecoder, [\in, bfbus]);
			activesynths.put(\reverb, 
				Synth.tail(atsgrp, \fxdustspace, [\out, bfbus, \in, efxbus, \dust, 30])
			);
			paramaps.put(\reverb, Event());
			paramaps[\reverb].put(\dust, 
				FxParam(\dust, \stddev, ControlSpec(10.0, 50.0, \lin), false, this)
			);			
			2.wait;
			inf.do({|i|
				seconds = AppClock().seconds - fxstart;
				
				if (this.segmentExists)
				{
					this.playProcessedSegment(currentSegPath);
				}
				{
					if ((seconds / opts.dayDur * 0.2).coin)
					{
						this.playWarpedSegment
					}
					{
						this.playLiveSegment
					}
				
				};
				
				rrand(opts.defaultSegDur * 0.4, opts.defaultSegDur * 0.75).round(0.25).wait;
			});
		}).play;
		
		Routine({
			300.wait;
			inf.do({
				this.analyzeSegment;
				rrand(3400, 3800).wait;
			})
		}).play
	}
		
//	doVisualLifeCycle{|patch|
//		Routine({
//			var totaldur = 0, wait;
//			this.fadeInPatch(patch, opts.fadeTime);
//			wait = rrand(100, 140);
//			totaldur = totaldur + wait;
//			wait.wait;
//			if (0.7.coin) {
//				this.setPatch(patch, [\alphamap, \colormap].choose, [0.0, 1.0].choose)
//			};
//			wait = rrand(100, 140);
//			totaldur = totaldur + wait;
//			wait.wait;
//			if (0.5.coin) {
//				this.setPatch(patch, [\color], 3.rand)
//			}
//			{
//				this.setPatch(patch, [\alphamap, \colormap].choose, [0.0, 1.0].choose)
//			};
//			(360 - totaldur).wait;
//			this.fadeOutPatch(patch, opts.fadeTime)
//		}).play
//	}
	
	segmentExists{
		var paths, found, vals;
		paths = (opts.atsdir ++ "*").pathMatch;
		currentSegPath = nil;
		found = false;
		paths.do({|path|
			vals = path.basename.split($.)[0].split($_);
			if ((vals[0].asInteger < cursor).and(vals[1].asInteger > cursor))
			{
				found = true;
				currentSegPath = path
			}
		});
		^found
	}
	
	playLiveSegment{	
		var name, buf, start, dur, frac, sparams, skeys, vctrs;
		name = ("fxd" ++ Date.getDate.secStamp).asSymbol;
		
		// to make the start times stutter over the course of a day, first a fraction of time
		// is calculated that has passed since the beginning of the app, the result is multiplied 
		// by the allowed stutter window, then converted to samples from seconds and finally 
		// whether the start jumps ahead or behind the cursor is picked randomly
		
		frac = seconds / opts.dayDur;
		start = cursor + (frac * opts.cursorWindow * sf.sampleRate 
			* [1, -1].choose);
			
		// necessary wrap in case the stutter jump goes beyond the number of frames in the file
		
		start = start.wrap(0, sf.numFrames - (sf.sampleRate * 30));
		
		// vary duration in the same way as the stutter
		
		dur = opts.defaultSegDur + (frac * opts.defaultMaxDurDev * [1, -1].choose);
		buf = Buffer.read(server, opts.filePath, cursor, dur * sf.sampleRate);
		server.sync;

		activesynths.put(name, 
			Synth.head(srcgrp, \fxplayer, [\out, bfbus, \efx, efxbus, \buffer, buf, \rate, 1.0, 
				\eamp, 0.1, \dur, dur, \amp, 0.0, \azm, rrand(-pi, pi), \rad, rrand(0.3, 0.7), 
				\elv, 0])
				.setn(\env, Env([0.0, 1.0, 1.0, 0.0], [0.3, 0.4, 0.3], \lin).asArray)
		);

		paramaps.put(name, Event());
		sparams = (
			\amp: ControlSpec(0.5, 0.7, \lin), 
			\eamp: ControlSpec(0.2, 0.4, \lin),
			\rate: ControlSpec(0.5, 2.0, \lin, 2**(1/24)), 
			\azm: ControlSpec(-pi, pi, \lin), 
			\rad: ControlSpec(0.3, 0.9, \lin)
		);
		vctrs = invalues.keys(Array);
		skeys = sparams.keys(Array).scramble;
		[1, 2].choose.do({
			var key;
			key = skeys.pop;
			paramaps[name].put(key, 
				FxParam(key, vctrs.pop, sparams[key], [true, false].choose, this)
			)
		});		

		SystemClock.sched(dur, {
			this.removeSynth(name);
			buf.free;
			buf = nil;
			nil
		});
		
		cursor = cursor + (dur * sf.sampleRate);
		if (cursor > (sf.numFrames - (opts.defaultSampleRate * 60))) { cursor = 0 };
	
	}
	
	playWarpedSegment{
	
		var name, buf, start, dur, frac, sparams, skeys, vctrs, warp, freq;
		name = ("fxwrp" ++ Date.getDate.secStamp).asSymbol;
		
		frac = seconds / opts.dayDur;
		start = cursor + (frac * opts.cursorWindow * sf.sampleRate 
			* [1, -1].choose);
		
		start = start.wrap(0, sf.numFrames - (sf.sampleRate * 30));
		
		dur = opts.defaultSegDur + (frac * opts.defaultMaxDurDev * [1, -1].choose);
		buf = Buffer.read(server, opts.filePath, cursor, 4 - (frac * 3) * sf.sampleRate );
		server.sync;

		warp = rrand(1 / (2**8), 1.0).round(1 / (2**8));
		
		activesynths.put(name, 
			Synth.head(srcgrp, \fxwarper, [\out, bfbus, \efx, efxbus, \buffer, buf, 
				\wrp, warp, \frq, [0.25, 0.5, 1.0].choose, \wsz, 0.1, \dns, rrand(4, 8), 
				\rnd, 0.01, \amp, 0.0, \eamp, 0.1, \dur, dur, \azm, rrand(-pi, pi), 
				\rad, rrand(0.3, 0.7), \elv, 0])
				.setn(\env, Env([0.0, 1.0, 1.0, 0.0], [0.3, 0.4, 0.3], \lin).asArray)
		);

		paramaps.put(name, Event());
		sparams = (
			\wrp: ControlSpec(warp * 0.5, warp * 2.0, \lin),
			\wsz: ControlSpec(0.01, 0.2, \lin),
			\rnd: ControlSpec(0.0, 0.02, \lin),
			\amp: ControlSpec(0.1, 0.3, \lin), 
			\eamp: ControlSpec(0.1, 0.25, \lin),
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
			buf.free;
			buf = nil;
			nil
		});
		
		cursor = cursor + (dur * sf.sampleRate);
		if (cursor > (sf.numFrames - (opts.defaultSampleRate * 60))) { cursor = 0 };
	
		
	}
	
	playProcessedSegment{|path|
		var atsfile, name, func, nparts, key;
		Routine({	
			name = path.basename.split($.)[0].asSymbol;
			atsfile = AtsFile(path, server);
			1.wait;
			if (this.checkValidFile(atsfile))
			{
				atsfile.loadToBuffer;
				3.wait;
				("ats synth def: " ++ key.asString ++ ": " ++ nparts.asString ++ ": " 
					++ atsfile.sndDur.round(0.1).asString).postln;
				this.activateAtsSynth(name, atsfile);
			}
			{
				this.playLiveSegment;
			}			
		}).play;
	}
	
	checkValidFile{|atsfile|
		var valid = false;
		if (atsfile.numPartials.notNil) { 
			if (atsfile.numPartials > 0) { valid = true }
		}
		^valid
	}
	
	activateAtsSynth{|name, atsfile|
		var dur, sparams, vctrs, skeys, nparts;
		dur = [30.0, 50.0].asSpec.map(avg);
		nparts = min(opts.maxPartials, atsfile.numPartials);
		activesynths.put(name, 
			Synth.head(atsgrp, \fxresynth, [\out, bfbus, \efx, efxbus, \buffer, atsfile.buffer, 
				\parts, nparts, \pstart, 0, \pskip, 1, \rate, 1.0, \efxamp, 0.1, \amp, 0.4, 
				\fmul, 1.0, \fadd, 0, \spct, 0.5, \npct, 0.5, \azm, rrand(-pi, pi), \elv, 0,
				\rad, 0.2, \dur, dur])
				.setn(\env, Env([0.0, 1.0, 1.0, 0.0], [0.3, 0.4, 0.3], \sine).asArray)
		);
		paramaps.put(name, Event());
		sparams = (
			\amp: ControlSpec(0.5, 1.0, \lin), 
			\efxamp: ControlSpec(0.4, 0.8, \lin),
			\rate: ControlSpec(0.1, 1.0, \lin, 0.0625), 
			\fmul: ControlSpec(0.125, 1.0, \lin, 2**(1/24)),
			\spct: ControlSpec(0.3, 1.0, \lin),
			\npct: ControlSpec(0.3, 0.8, \lin),
			\azm: ControlSpec(-pi, pi, \lin), 
			\rad: ControlSpec(0.4, 2.0, \lin)
		);
		vctrs = invalues.keys(Array).scramble;
		skeys = sparams.keys(Array).scramble;
		skeys.do({|key|
			paramaps[name].put(key, 
				FxParam(key, vctrs.pop, sparams[key], [true, false].choose, this)
			)
		});

		cursor = cursor + (dur * sf.sampleRate);
		if (cursor > (sf.numFrames - (opts.defaultSampleRate * 60))) { cursor = 0 };

		SystemClock.sched(dur, {
			this.removeSynth(name);
			atsfile.buffer.free;
			atsfile = nil;
			nil
		})
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
		visualdict[patch].active = 1.0;
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
				this.sendMsg(command, value);
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
		var sd, hb, tp, lf, wd, hg, occ, wts;
		
		occ = statehist.occurrencesOf(current.round(0.0001));
		
		if ((occ > 1).and((current > 0.99).or(current < 0.002)))
		{
			wts = [
				(1.0 ! 8),
				Array.rand(8, 1.0, 3.0),
				Array.rand(4, 1.0, 4.0).mirror2
			].choose.round(0.1);
			this.resetWorld(2.rand, 0, 1, 10.rand, 10.rand, 10.rand, 10.rand, 
				wts[0], wts[1], wts[2], wts[3], wts[4], wts[5], wts[6], wts[7]
			);
			this.setAdd(rrand(0.01, 0.03));
			statehist = (1..6);
			"emergency world reset".postln;
		}
		{
			statehist.insert(0, current.round(0.0001)).pop;
		};
	}
		
	analyzeSegment{
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
				(start.asString ++ "_" ++ (start + len).asString).postln
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
			Out.ar(0, decode * amp)
		}).send(server);
		SynthDef(\fxdiskin, {|out, efx, buffer, rate, dur, amp, eamp, azm, elv, rad|
			var sig, bf, env;
			env = Control.names([\env]).kr(Env.newClear(8).asArray);
			sig = VDiskIn.ar(1, buffer, rate);
			bf = BFEncode1.ar(sig, azm, elv, rad) * 
				EnvGen.kr(env, timeScale: dur, doneAction: 2);
			Out.ar(efx, sig * eamp);
			Out.ar(out, bf * amp)
		}).send(server);
		SynthDef(\fxplayer, {|out, efx, buffer, rate, dur, amp, eamp, azm, elv, rad|
			var sig, bf, env;
			env = Control.names([\env]).kr(Env.newClear(8).asArray);
			sig = PlayBuf.ar(1, buffer) ** 0.5;
			bf = BFEncode1.ar(sig, azm, elv, rad) * 
				EnvGen.kr(env, timeScale: dur, doneAction: 2);
			Out.ar(efx, sig * eamp);
			Out.ar(out, bf * amp)
		}).send(server);
		SynthDef(\fxwarper, {|out, efx, buffer, wrp, frq, wsz, dns, rnd, amp, eamp, dur, 
			azm, elv, rad|
			var sig, bf, env;
			env = Control.names([\env]).kr(Env.newClear(8).asArray);
			sig = Warp1.ar(1, buffer, LFSaw.kr(wrp, 0, 0.5, 0.5), frq, wsz, -1, dns, rnd, 4) 
				* EnvGen.kr(env, timeScale: dur, doneAction: 2);
			bf = BFEncode1.ar(sig, azm, elv, rad);
			Out.ar(efx, sig * eamp);
			Out.ar(out, bf * amp)
		}).send(server);
		SynthDef(\fxdustspace, {|out = 0, in, dust = 10, pow = 1.5, amp = 1.0|
			var input, l, r, bfl, bfr, azm, wid, rho;
			input = In.ar(in);
			#l, r = GVerb.ar(input ** pow + Dust.ar(dust, input), 1000, 
				LFNoise2.kr(0.05, 10.0, 35.0), 0.31, 0.5, 15, 0.1, 0.4, 1.0, 1000) * amp;
			wid = LFNoise2.kr(29.reciprocal, 0.25pi, 0.25pi);
			azm = LFNoise2.kr(31.reciprocal, pi);
			rho = LFNoise2.kr(23.reciprocal, 0.5, 0.5);
			bfl = BFEncode1.ar(l, azm - (wid * 0.5), 0, rho);
			bfr = BFEncode1.ar(l, azm + (wid * 0.5), 0, rho);
			Out.ar(out, bfl + bfr);
		}).send(server);
		SynthDef(\fxresynth, {|out, efx, buffer, parts, pstart, pskip, rate, efxamp, 
			fmul, fadd, spct, npct, azm, elv, rad, amp, dur|
			var env, sig, ptr, bf;
			env = Control.names([\env]).kr(Env.newClear(8).asArray);
			ptr = LFSaw.kr(rate, 1, 0.5, 0.5);
			sig = AtsNoiSynth.ar(buffer, parts, pstart, pskip, ptr, spct, npct, fmul, fadd) 
				* EnvGen.kr(env, timeScale: dur, doneAction: 2);
			bf = BFEncode1.ar(sig, azm, elv, rad);
			Out.ar(efx, sig * efxamp);
			Out.ar(out, bf * amp)		
		}).send(server);

	}
	
	quit{
		this.quitVisual;
		responder.remove;
		mainloop.stop;
		graphloop.stop;
		globloop.stop;
		SystemClock.sched(30, {
			server.quit;
		});
	}
	
}

FxParam{

	var <name, inparam, <spec, reval, fx;
	
	*new{|name, inparam, spec, reval, fx|
		^super.newCopyArgs(name, inparam, spec, reval, fx)
	}
	
	map{
		if (reval) { ^spec.map(1.0 - fx.invalues[inparam]) }
		{ ^spec.map(fx.invalues[inparam]) }
	}
	
	mapValue{|value|
		^spec.map(value)
	}
	
}

FxGUI{
	var fx, win;
	
	*new{|fx|
		^super.newCopyArgs(fx).init
	}
	
	init{
		var gui, font, aspec, atxt, prompt, itxt;
		font = Font("Skia", 28);
		win = THSWindow(". ._. . f(x) ._. . .", Rect(300, 300, 400, 400), Color.grey(0.6));
		gui = GUI.current;
		gui.button.new(win, Rect(20, 20, 150, 60))
			.font_(font)
			.states_([["quit", Color.black, Color.red]])
			.action_({|btn|
				prompt = gui.window.new("._. shutdown ._.", 
					Rect(400, 400, 400, 100), false, false)
					.alwaysOnTop_(true)
					.front;
				itxt = gui.staticText.new(prompt, Rect(10, 10, 380, 40))
					.font_(Font("Skia", 14))
					.align_(\center)
					.string_("Shut down the installation?");
				gui.button.new(prompt, Rect(50, 55, 75, 30))
					.font_(Font("Skia", 14))
					.states_([["yes", Color.green, Color.black]])
					.action_({
						fx.quit;
						prompt.close;
						prompt = nil;
						thisProcess.shutdown;
						0.exit;
					});
				gui.button.new(prompt, Rect(250, 55, 75, 35))
					.font_(Font("Skia", 14))
					.states_([["no", Color.red, Color.black]])
					.action_({
						prompt.close;
						prompt = nil
					});
			});
		gui.button.new(win, Rect(20, 300, 150, 60))
			.font_(font)
			.states_([["reset", Color.black, Color.grey(0.6)]])
			.action_({|btn|
				btn.enabled = false;
				fx.resetWorld(0, 0, 1, 5, 5, 10, 10, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
				fx.setAdd(rrand(0.009, 0.15));
				AppClock.sched(2, { btn.enabled = true; nil })
			});
		
		gui.slider.new(win, Rect(180, 20, 80, 360))
			.value_(0.5)
			.action_({|sl|
				fx.decoder.set(\amp, sl.value);
				atxt.string_(sl.value.ampdb.round(0.01).asString)
			});
		atxt = gui.staticText.new(win, Rect(180, 180, 80, 40))
			.font_(font)
			.string_(0.5.ampdb.round(0.01))
			.stringColor_(Color.white)
			.align_(\center);
		gui.staticText.new(win, Rect(180, 320, 80, 60))
			.font_(Font("Skia", 14))
			.align_(\center)
			.string_("volume")
			.stringColor_(Color.white)
	}
}