Eclambone{
	
	classvar <addActions;
	
	var path, speakers, <remote, liveInput, <autoOutput;
	var args, funcs, inbus, ambibus, outbus, audiosynth, efxbus;
	var <server, buffer, win, scopein, scopeout, scopeinbuf, scopeoutbuf, start, rec, cols;
	var filespath, files, playbuf, sched, time, <liveBuffers, ctrdefs;
	var decsynth, mgrp, <trgtgrp, <srcgrp, recpath, rcount = 1, trigsynth;
	var recordBuf, rnode, pfuncs, avgcpu, peakcpu, numsynths, font, livesynths, lsitems;
	var srcamp, glamp, efxamp, recChannels = 2, recBus = 0, <loopon = true, triggerer;
		
	*initClass{
			addActions = (
				addToHead: 0, 
				addToTail: 1, 
				addBefore: 2, 
				addAfter: 3, 
				addReplace: 4,
				h: 0,
				t: 1
			);

	}
	
	*new{|path, speakers, remote, liveInput = false, autoOutput = false|
		^super.newCopyArgs(path, speakers, remote, liveInput, autoOutput).init
	}
	
	init{
		server = Server.internal;
		Server.default = server;
		server.options.memSize = 32769.nextPowerOfTwo;
		font = if (autoOutput, {Font("Helvetica", 30)}, {Font("Century Gothic", 10)});
		recpath = "/Users/ise/sounds/ecomar/liverec/rehearsal";
		filespath = "/Users/ise/sounds/ecomar/takes/";	
		lsitems = IdentityDictionary.new;
		liveBuffers = Array.new;
		if (server.serverRunning, {
			server.reboot({
				server.waitForBoot({
					this.loadSynthDefs.prepareServer.makeGui
				})			
			})
		}, {
			server.waitForBoot({
				this.loadSynthDefs.prepareServer.makeGui
			})
		})
	}
	
	loadSynthDefs{
		
		if (liveInput, {
			SynthDef("ecoin", {|out, sout, aux, amp = 1, ramp = 1|
				var sig, point, trig, ampctr;
				sig = AudioIn.ar([1]);
//				ampctr = Amplitude.kr(sig, 0.01, 0.01);
//				trig = Lag.kr(ampctr, 0.05);
//				trig = Slope.kr(trig - 0.5);
//				SendTrig.kr(Trig.kr(trig > tgate, tfreq), 1, ampctr);
				point = Polar(LFNoise2.kr(0.1, 0.1, 0.2), LFNoise2.kr(0.22, 0.5pi, 0.5pi));
				Out.ar(aux, sig * ramp);
				Out.ar(sout, sig * amp);
				Out.ar(out, BFEncode1.ar(sig, point.theta, 0, point.rho) * amp)
			}).send(server);
		}, {
			SynthDef("ecoplay", {|out, sout, aux, amp = 1, ramp = 1, bufnum, dur|
				var sig, point;
				sig = DiskIn.ar(1, bufnum);
				point = Polar(LFNoise2.kr(0.1, 0.1, 0.2), LFNoise2.kr(0.22, 0.5pi, 0.5pi));
				Out.ar(aux, sig * ramp);
				Out.ar(sout, sig * amp);
				Out.ar(out, BFEncode1.ar(sig, point.theta, 0, point.rho) * amp)
			}).send(server);
		});
		
		SynthDef("atrigger", {|in, tfreq = 0.1, tgate = 0.1|
			var sig, ampctr, trig;
			sig = In.ar(in);
			ampctr = Amplitude.kr(sig, 0.01, 0.01);
			trig = Lag.kr(ampctr, 0.05);
			trig = Slope.kr(trig - 0.5);
			SendTrig.kr(Trig.kr(trig > tgate, tfreq), 1, ampctr);
		}).send(server);
		
		SynthDef("decoder", {|out, in, amp|
			var w, x, y, z, sig, trig, ampctr;
			#w, x, y, z = In.ar(in, 4);
			sig = BFDecode1.ar(w, x, y, z, speakers) * amp;
			Out.ar(out, sig)
		}).send(server);

		funcs = path.loadPaths[0];
//		ctrdefs = (path ++ ctrfile).loadPaths[0];
		
		Library.put(\ecomar, \funcs, funcs);
			
	}
	
	prepareServer{
	
		var sfunc;
	
		sfunc = {|bufnum, bus, nch|
			SynthDef("scoper"++bufnum.asString, {
				ScopeOut.ar(In.ar(bus, nch), bufnum)
			}).play(1, addAction: \addToTail)
		};
		
		inbus = Bus.audio(server);
		ambibus = Bus.audio(server, 3);
		efxbus = Bus.audio(server);	
		mgrp = Group.new(1, \addBefore);
		trgtgrp = Group.new(mgrp, \addBefore);
		srcgrp = Group.new(trgtgrp, \addBefore);
		
		ctrdefs.do({|def|
			def.send(server)
		});
		
		Library.put(\ecomar, \srcbus, inbus.index);
		Library.put(\ecomar, \ambibus, ambibus.index);
		Library.put(\ecomar, \efxbus, efxbus.index);
		
		Library.put(\ecomar, \maingrp, mgrp);
		Library.put(\ecomar, \targetgrp, trgtgrp);
		Library.put(\ecomar, \sourcegrp, srcgrp);
		
		scopeinbuf = Buffer.alloc(server, 4096, 1, completionMessage: {|buf|
			sfunc.value(buf.bufnum, inbus.index, buf.numChannels)
		});
		
		scopeoutbuf = Buffer.alloc(server, 4096, 4, completionMessage: {|buf|
			sfunc.value(buf.bufnum, 0, buf.numChannels)
		});
		
		AppClock.sched(0.1, {
			decsynth = Synth.tail(mgrp, "decoder", [\out, 0, \in, ambibus.index, \amp, 1]);
			nil
		});
		
	}
	
	makeGui{
		if (autoOutput, {
			this.makeMonitorGui
		}, {
			this.makePerformanceGui
		})
	}
	
	makeMonitorGui{
	
		var font2, alpha = 0;
		font2 = Font("Helvetica", 35);
	
		cols = [Color.grey(0.2), Color.new255(69, 139, 116, 0.99), Color.grey(0.2), Color.new255(69, 139, 116, 1.01)];
		
		win = SCWindow(":::  . .  .. .. .. .  .. :::", Rect(5, 5, 1019, 750)).alpha_(0.98).front;
		win.onClose_({
			server.freeAll;
		});
		
		TriggerMonitor.eventDictionary.keys(Array).sort.do({|key, i|
			var string;
			string = (key.asString ++ " .. " ++ TriggerMonitor.eventDictionary[key]).collect({|ch|
				ch.toUpper
			});
			string.postln;
			SCStaticText(win, Rect(10, i * 35 + 10, 400, 35))
				.align_(\left)
				.font_(font)
				.stringColor_(Color.white)
				.string_(string);
		});
		
		time = TimeDisplay(win, Rect(350, 400, 300, 40), 0, Font("Helvetica", 30))
			.background_(Color.clear)
			.stringColor_(Color.grey(0.9));
		
		livesynths = SCListView(win, Rect(350, 20, 300, 370))
			.font_(font);	

		SCStaticText(win, Rect(670, 200, 150, 35))
			.font_(font2)
			.align_(\left)
			.stringColor_(Color.white)
			.string_("AVG CPU:");
		avgcpu = SCStaticText(win, Rect(820, 200, 50, 35))
			.font_(font2)
			.align_(\right)
			.stringColor_(Color.white)
			.string_(server.avgCPU.round(0.1));
		SCStaticText(win, Rect(870, 200, 50, 35))
			.font_(font2)
			.align_(\left)
			.stringColor_(Color.white)
			.string_("%");
		
		SCStaticText(win, Rect(670, 260, 150, 35))
			.font_(font2)
			.align_(\left)
			.stringColor_(Color.white)
			.string_("MAX CPU:");
		peakcpu = SCStaticText(win, Rect(820, 260, 50, 35))
			.font_(font2)
			.align_(\right)
			.stringColor_(Color.white)
			.string_(server.peakCPU.round(0.1));
		SCStaticText(win, Rect(870, 260, 50, 35))
			.font_(font2)
			.align_(\left)
			.stringColor_(Color.white)
			.string_("%");

		SCStaticText(win, Rect(670, 320, 150, 35))
			.font_(font2)
			.align_(\left)
			.stringColor_(Color.white)
			.string_("SYNTHS:");
		numsynths = SCStaticText(win, Rect(820, 320, 50, 35))
			.font_(font2)
			.align_(\right)
			.stringColor_(Color.white)
			.string_(server.numSynths);
					
		sched = AppClock.sched(1, {
			avgcpu.string_(server.avgCPU.round(0.1));
			peakcpu.string_(server.peakCPU.round(0.1));
			numsynths.string_(server.numSynths);
			if (server.avgCPU < 80, {
				avgcpu.stringColor_(Color.white)
			}, {
				avgcpu.stringColor_(Color.red)
			});
			if (server.peakCPU < 80, {
				peakcpu.stringColor_(Color.white)
			}, {
				peakcpu.stringColor_(Color.red)
			});
			1
		});

		start = SCButton(win, Rect(670, 20, 150, 70))
			.states_([["start", Color.grey, Color.white], ["stop", Color.white, Color.grey]])
			.font_(font)
			.action_({|btn|
				var dur, pth;
				if (btn.value == 1, {
				
					time.start;

					audiosynth = Synth.tail(srcgrp, "ecoin", [\out, ambibus.index, 
							\sout, inbus.index, \aux, efxbus.index]
						);
					if (remote.notNil, {
							trigsynth = Synth.after(audiosynth, "atrigger", [\in, inbus.index])
						});	

					if (remote.notNil.or(autoOutput), {
						triggerer = TriggerMonitor(this);
					});
										
				}, {
					time.stop;
					audiosynth.free;
					trigsynth.free;
					if (liveInput.not, {
						playbuf.close;
						playbuf = nil;
					})
				})
			});	
		
		SCButton(win, Rect(670, 100, 150, 70))
			.font_(font)
			.states_([["reboot", Color.grey, Color.white]])
			.action_({
				server.reboot({
					server.waitForBoot({
						this.loadSynthDefs.prepareServer;
					})			
				})			
			});
		
		scopein = SCScope(win, Rect(320, 500, 400, 175))
			.background_(HiliteGradient(Color.grey(0.3), Color.clear, \v, 64, 0.66))
			.bufnum_(scopeinbuf.bufnum)
			.waveColors_(({Color.grey(0.7)} ! 1));				
		
		5.do({|i|
			var or, st;
			or = [\h, \v].wchoose([0.3, 0.7]);
			st = if (or == \h, {256}, {128});
			SCCompositeView(win, Rect(0, i*150, 10019, 150))
				.background_(Gradient(cols.wrapAt(i), cols.wrapAt(i+1), or, st))
		});
		
//		Routine({
//			400.do({
//				alpha = alpha + (20/8000);
//				win.alpha_(alpha);
//				0.05.wait
//			})
//		}).play(AppClock)			
	}
	
	makePerformanceGui{
				
		cols = [Color.grey(0.3), Color.new255(32, 178, 170, 0.9), Color.grey(0.3), Color.new255(32, 178, 170, 1.1)];
		
		win = SCWindow(":::  . .  .. .. .. .  .. :::", Rect(10, 10, 900, 700)).front;
		win.onClose_({
			server.freeAll;
		});

		5.do({|i|
			SCCompositeView(win, Rect(0, i*140, 900, 140))
				.background_(Gradient(cols.wrapAt(i), cols.wrapAt(i+1), [\h, \v].choose, 128))
		});			
		
		funcs.keys(Array).sort.do({|key, i|
			SCButton(win, Rect(20, i * 30 + 20, 150, 25))
				.font_(font)
				.states_([[key.asString, Color.white, Color.grey(0.3)]]) 
				.action_({
					this.playFunc(key)
				})
				.enabled_(autoOutput.not)
		});
		
//		funcs.keysValuesDo({|key, val, i|
//			SCButton(win, Rect(20, i * 30 + 20, 150, 25))
//				.font_(font)
//				.states_([[key.asString, Color.white, Color.grey(0.3)]]) 
//				.action_({
//					var id; 
//					if (server.peakCPU < 85, {
//						id = server.nextNodeID;
//						lsitems.put((key ++ "." ++ id).asSymbol, val);
//						livesynths.items_(lsitems.keys(Array));
//						val.value(server, id, trgtgrp, addActions[\addToTail], this,
//							[\out, ambibus.index, \in, efxbus.index])
//					})
//				})		
//		});
		
		time = TimeDisplay(win, Rect(480, 20, 100, 20), 0, Font("Helvetica", 14))
			.background_(Color.clear)
			.stringColor_(Color.grey(0.9));
		
		livesynths = SCListView(win, Rect(180, 20, 200, 380))
			.font_(font);
		
		scopein = SCScope(win, Rect(180, 420, 400, 120))
			.background_(HiliteGradient(Color.grey(0.3), Color.clear, \v, 64, 0.66))
			.bufnum_(scopeinbuf.bufnum)
			.waveColors_(({Color.grey(0.8)} ! 1));
	
		scopeout = SCScope(win, Rect(415, 50, 465, 300))
			.background_(HiliteGradient(Color.grey(0.3), Color.clear, \v, 64, 0.33))
			.bufnum_(scopeoutbuf.bufnum)
			.waveColors_(({Color.grey(0.8)} ! 4));
		
		srcamp = SCSlider(win, Rect(600, 420, 40, 200))
			.value_(1.0)
			.action_({|me|
				audiosynth.set("amp", me.value)
			});
			
		SCStaticText(win, Rect(600, 520, 40, 20))
			.align_(\center)
			.font_(font)
			.stringColor_(Color.white)
			.string_(":src:");
			
		efxamp = SCSlider(win, Rect(650, 420, 40, 200))
			.value_(1.0)
			.action_({|me|
				audiosynth.set("ramp", me.value)		
			});

		SCStaticText(win, Rect(650, 520, 40, 20))
			.align_(\center)
			.font_(font)
			.stringColor_(Color.white)
			.string_(":efx:");

			
		glamp = SCSlider(win, Rect(700, 420, 40, 200))
			.value_(1.0)
			.action_({|me|
				decsynth.set("amp", me.value)		
			});

		SCStaticText(win, Rect(700, 520, 40, 20))
			.align_(\center)
			.font_(font)
			.stringColor_(Color.white)
			.string_(":main:");
			
		SCSlider(win, Rect(760, 420, 40, 200))
			.value_(0.1)
			.action_({|sl|
				trigsynth.set("tfreq", [0.0, 0.5].asSpec.map(sl.value))
			});
			
		SCStaticText(win, Rect(760, 520, 40, 20))
			.align_(\center)
			.font_(font)
			.stringColor_(Color.white)
			.string_(":tfreq:");		
			
		SCSlider(win, Rect(810, 420, 40, 200))
			.value_(0.1)
			.action_({|sl|
				trigsynth.set("tgate", [0.0, 5.0].asSpec.map(sl.value))
			});

		SCStaticText(win, Rect(810, 520, 40, 20))
			.align_(\center)
			.font_(font)
			.stringColor_(Color.white)
			.string_(":tgate:");
			
		start = SCButton(win, Rect(180, 540, 100, 40))
			.states_([["start", Color.grey, Color.white], ["stop", Color.white, Color.grey]])
			.font_(font)
			.action_({|btn|
				var dur, pth;
				if (btn.value == 1, {
				
					time.start;
										
					if (liveInput, {
						audiosynth = Synth.tail(srcgrp, "ecoin", [\out, ambibus.index, 
							\sout, inbus.index, \aux, efxbus.index]
						);
						if (remote.notNil.or(autoOutput), {
							trigsynth = Synth.after(audiosynth, "atrigger", [\in, inbus.index])
						});	
					}, {
						Routine({
							pth = filespath++files.items[files.value];
							playbuf = Buffer.cueSoundFile(server, pth, 0, 1);
							0.1.wait;	
							audiosynth = Synth.tail(srcgrp, "ecoplay", [\out, ambibus.index, 
								\sout, inbus.index, \aux, efxbus.index, 
								\bufnum, playbuf.bufnum]
							);
							if (remote.notNil.or(autoOutput), {
								trigsynth = Synth.after(audiosynth, "atrigger", 
									[\in, inbus.index])
							});
						}).play(AppClock);
					});
					
					if (remote.notNil.or(autoOutput), {
						triggerer = TriggerMonitor(this);
					});
				}, {
					time.stop;
					audiosynth.free;
					trigsynth.free;
					if (liveInput.not, {
						playbuf.close;
						playbuf = nil;
					})
				})
			});
			
		rec = SCButton(win, Rect(285, 540, 100, 40))
			.states_([["record", Color.grey, Color.white], 
				["stop recording", Color.white, Color.grey]])
			.font_(font)
			.action_({|btn|
				if (btn.value == 1, {
					Routine({
						this.prepareForRecord(recChannels);
						0.1.wait;
						this.record(recBus)
					}).play
				}, {
					this.stopRecording
				})
			});
			
		SCButton(win, Rect(410, 540, 40, 40))
			.states_([["on", Color.grey, Color.white], 
				["off", Color.white, Color.grey]])
			.font_(font)
			.action_({|btn|
				if (btn.value == 0, {
					loopon = true
				}, {
					loopon = false;
					server.sendBundle(nil, ["/n_set", trgtgrp.nodeID, \loop, 0.0])				})
			});
		
		if (liveInput.not, {
			files = SCPopUpMenu(win, Rect(180, 585, 100, 20))
				.font_(font)
				.items_((filespath++"*").pathMatch.collect({|it| it.basename}))
		});
		
		SCButton(win, Rect(185, 585, 100, 20))
			.font_(font)
			.states_([["reboot", Color.grey, Color.white]])
			.action_({
				server.reboot({
					server.waitForBoot({
						this.loadSynthDefs.prepareServer;
					})			
				})			
			});
		
		SCStaticText(win, Rect(20, 610, 60, 25))
			.font_(font)
			.align_(\left)
			.stringColor_(Color.white)
			.string_("Avg CPU:");
		avgcpu = SCStaticText(win, Rect(80, 610, 30, 25))
			.font_(font)
			.align_(\right)
			.stringColor_(Color.white)
			.string_(server.avgCPU.round(0.1));
		SCStaticText(win, Rect(115, 610, 50, 25))
			.font_(font)
			.align_(\left)
			.stringColor_(Color.white)
			.string_("%");
		
		SCStaticText(win, Rect(20, 635, 60, 25))
			.font_(font)
			.align_(\left)
			.stringColor_(Color.white)
			.string_("Peak CPU:");
		peakcpu = SCStaticText(win, Rect(80, 635, 30, 25))
			.font_(font)
			.align_(\right)
			.stringColor_(Color.white)
			.string_(server.peakCPU.round(0.1));
		SCStaticText(win, Rect(115, 635, 50, 25))
			.font_(font)
			.align_(\left)
			.stringColor_(Color.white)
			.string_("%");

		SCStaticText(win, Rect(20, 660, 60, 25))
			.font_(font)
			.align_(\left)
			.stringColor_(Color.white)
			.string_("Synths:");
		numsynths = SCStaticText(win, Rect(80, 660, 30, 25))
			.font_(font)
			.align_(\right)
			.stringColor_(Color.white)
			.string_(server.numSynths);
					
		sched = AppClock.sched(1, {
			avgcpu.string_(server.avgCPU.round(0.1));
			peakcpu.string_(server.peakCPU.round(0.1));
			numsynths.string_(server.numSynths);
			if (server.avgCPU < 80, {
				avgcpu.stringColor_(Color.white)
			}, {
				avgcpu.stringColor_(Color.red)
			});
			if (server.peakCPU < 80, {
				peakcpu.stringColor_(Color.white)
			}, {
				peakcpu.stringColor_(Color.red)
			});
			1
		});
	
	}
	
	playFunc{|key|
		var id; 
		if (server.peakCPU < 80, {
			id = server.nextNodeID;
			AppClock.sched(0.01, {
				lsitems.put((key ++ "." ++ id).asSymbol, funcs[key]);
				livesynths.items_(lsitems.keys(Array));
				nil
			});
			funcs[key].value(server, id, trgtgrp, addActions[\addToTail], this,
				[\out, ambibus.index, \in, efxbus.index])
		})
	}
	
	prepareForRecord{|channels|
		recordBuf = Buffer.alloc(server, 65536, channels,
			{arg buf; buf.writeMsg(recpath++rcount.asString, "aiff", "int16", 0, 0, true);},
			server.options.numBuffers + 1);
		SynthDef("record", { arg bufnum, bus;
			DiskOut.ar(bufnum, In.ar(bus, channels)) 
		}).send(server);
	
	}
	
	record{|bus|
		if (bus.isKindOf(Bus), {bus = bus.index});
		rcount = rcount + 1;
		rnode = Synth.tail(mgrp, "record", [\bufnum, recordBuf.bufnum, \bus, bus]);
		"recording ecomar".postln;
	}
	
	stopRecording{
		rnode.notNil.if({
			rnode.free;
			rnode = nil;
			recordBuf.close({|buf| buf.free});
			recordBuf = nil;
			"recording stopped".postln
		})
	}
	
	removeSynth{|key|
		AppClock.sched(0.1, {
			lsitems.removeAt(key.asSymbol);
			livesynths.items_(lsitems.keys(Array));
			nil
		})
	}
	
	addBuffer{|bufnum|
		liveBuffers = liveBuffers.add(bufnum)
	}
	
}
