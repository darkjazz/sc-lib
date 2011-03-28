MxIIIIxM{
	
	var path, defs, win, <tracks, <server, slib, <xfade, xfadech1, xfadech2, send, selectTrack;
	var <ampR = 0.0, <ampL = 1.0, <>synthR, <>synthL, scope, scopeBuf, changePath, record, <font;
	var listen, sendBuffer, listitems, columnlen = 30, bpmsynth, bpmresp;
	var <archivepath = "/Users/alo/mxIIIIxm/archive/bpmarchive.sctxar";
	var <archive, <masterbpm = 120, masterbps, levels, outL, outR, cueL, cueR, txtCue, txtMain;
	var outResponder, cueResponder, eq, <xfader, volumeControl;

	*new{|path|
		^super.newCopyArgs(path).init
	}
	
	init{
		server = Server.internal;
		Server.default = server;
		font = Font("Skia", 12);
		archive = Archive.read(archivepath);
		if (server.serverRunning, {
			this.sendSynthDefs.makeGui.startLevelIndicators;
			if (path.isNil) { this.getPath } { this.fillListView(path ++ "*") }
		}, {
			server.waitForBoot({				
				this.sendSynthDefs.makeGui.startLevelIndicators;
				if (path.isNil) { this.getPath } { this.fillListView(path ++ "*") }
			});
		});
	}
	
	sendSynthDefs{
	
		defs = (
			\disk : SynthDef("dmx", {|out = 0, cue = 2, beat, bufnum, frames, warp = 1.0, 
						cueamp = 0.0, amp = 0.0, start = 0, id|
					var sig, ctr, vctr;
					vctr = Phasor.kr(0, warp * 64, start, frames);
					SendTrig.kr(Impulse.kr(20), id, vctr);
					sig = VDiskIn.ar(2, bufnum, warp);
					Out.ar(beat, Mix(sig));
					Out.ar(cue, sig * cueamp);
					Out.ar(out, sig * amp)
				}),
				
			\buf : SynthDef("bmx", {|out = 0, cue = 2, beat, bufnum, frames, warp = 1.0, 
						cueamp = 0.0, amp = 0.0, start = 0, id|
					var sig, ctr, vctr;
					vctr = Phasor.kr(0, BufRateScale.kr(bufnum) * (warp * 64), 
						start, BufFrames.kr(bufnum)
					);
					sig = PlayBuf.ar(2, bufnum, warp, 1.0, start);
					SendTrig.kr(Impulse.kr(20), id, vctr);
					Out.ar(beat, Mix(sig));
					Out.ar(cue, sig * cueamp);
					Out.ar(out, sig * amp);
				}),
				
			\warp : SynthDef("mx", {|out = 0, cue = 2, beat, bufnum, warp = 1.0, cueamp = 0.0, 
				amp = 0.0, start = 0, id|
				var sig, ctr, vctr;
				vctr = Phasor.kr(0, 
					BufRateScale.kr(bufnum) * (warp * 64), 
					start, BufFrames.kr(bufnum)
				);
				SendTrig.kr(Impulse.kr(20), id, vctr);
				sig = Warp1.ar(2, bufnum, LFSaw.ar(1/BufDur.kr(bufnum) * warp, start + 1, 0.5, 0.5), 1, 
					0.2, -1, 10, 0.02, 4);
				Out.ar(beat, Mix(sig));
				Out.ar(cue, sig * cueamp);
				Out.ar(out, sig * amp)
			})
		);
		
		if (server.serverRunning, {
			defs.keysValuesDo({|key, val|
				val.add
			});
			SynthDef(\bpmdetector, {|in, id, lock|
				var input, qrt, egt, six, bps, fft, fftbuf;
				fftbuf = LocalBuf(1024);
				input = In.ar(in);
				fft = FFT(fftbuf, input);
				#qrt, egt, six, bps = BeatTrack.kr(fft, lock);
				SendTrig.kr(qrt, id, bps);
			}).add;
			SynthDef(\masterbpmdetector, {|in|
				var input, qrt, egt, six, bps, fft, fftbuf;
				fftbuf = LocalBuf(1024);
				input = In.ar(in);
				fft = FFT(fftbuf, input);
				#qrt, egt, six, bps = BeatTrack.kr(fft);
				SendReply.kr(qrt, '/masterbpm', bps);
			}).add;			
			
		}, {"server not running!".warn});	
	}
	
	getPath{
		File.saveDialog("*", successFunc: {|fpath|
			path = fpath.dirname ++ "/";
			this.fillListView(fpath)
		})	
	}
	
	fillListView{|fpath|
		listitems = Array();	
		slib.items_(fpath.pathMatch.collect({|it| 
			var bpm, split, name;
			split = it.basename.split($.);
			name = split[0].copyRange(0, columnlen - 1);
			bpm = archive[\mxIIIIxm, \trackinfo, name.asSymbol] ?? 0.0;
			listitems = listitems.add( (\path: it.basename, \bpm: bpm, 
				\format: split[split.lastIndex]) );
//				(columnlen - name.size).do({ name = name ++ " " });
			name ++ " | " ++ bpm.round(0.01).asString; 
		})
		)		
	}
	
	startLevelIndicators{
		Routine({
			outResponder = OSCresponderNode(server.addr, '/main', {|ti, re, ms|
				{
					outL.value = ms[3].ampdb.linlin(-60, 0, 0, 1);
					outL.peakLevel = ms[4].ampdb.linlin(-60, 0, 0, 1);
					outR.value = ms[5].ampdb.linlin(-60, 0, 0, 1);
					outR.peakLevel = ms[6].ampdb.linlin(-60, 0, 0, 1);
				}.defer
			}).add;
			cueResponder = OSCresponderNode(server.addr, '/cue', {|ti, re, ms|
				{
					cueL.value = ms[3].ampdb.linlin(-60, 0, 0, 1);
					cueL.peakLevel = ms[4].ampdb.linlin(-60, 0, 0, 1);
					cueR.value = ms[5].ampdb.linlin(-60, 0, 0, 1);
					cueR.peakLevel = ms[6].ampdb.linlin(-60, 0, 0, 1);
				}.defer
			}).add;
			
			volumeControl = SynthDef(\volumes, {|main = 1, cue = 1|
				ReplaceOut.ar(2, In.ar(2, 2) * cue);
				ReplaceOut.ar(0, In.ar(0, 2) * main)
			}).play(1, addAction: \addToTail);
			
			server.sync;
			
			levels = SynthDef(\levelIndicators, {
				var trig, main, cue, del;
				trig = Impulse.kr(10);
				del = Delay1.kr(trig);
				main = In.ar(0, 2);
				cue = In.ar(2, 2);
				SendReply.kr(trig, '/main', [
					Amplitude.kr(main[0]),
					K2A.ar(Peak.ar(main[0], del).lag(0, 3)),
					Amplitude.kr(main[1]),
					K2A.ar(Peak.ar(main[1], del).lag(0, 3)),
				]
				);
				SendReply.kr(trig, '/cue', [
					Amplitude.kr(cue[0]),
					K2A.ar(Peak.ar(cue[0], del).lag(0, 3)),
					Amplitude.kr(cue[1]),
					K2A.ar(Peak.ar(cue[1], del).lag(0, 3)),
				]
				);
			}).play(volumeControl, addAction: \addAfter)
		}).play
	}
	
	makeGui{
		
		var bpmview, bpmtick;
		
		win = SCWindow("..MxIIIIxM..", Rect(10, 1000, 570, 320)).front.alpha_(0.9);
		win.view.background_(HiliteGradient(Color.grey(0.15), Color.grey(0.4), \v, 256, 0.5));
		win.onClose = {
			levels.free;
			outResponder.remove;
			outResponder = nil;
			cueResponder.remove;
			cueResponder = nil;
		};
		slib = SCListView(win, Rect(10, 10, 250, 300))
			.font_(font)
			.stringColor_(Color.grey(0.7))
			.selectedStringColor_(Color.green)
			.hiliteColor_(Color.grey(0.1));
				
		RoundButton(win, Rect(265, 10, 50, 20))
			.font_(font)
			.states_([["[wrp]", Color.grey, Color.black]])
			.action_({
				var it, trk;
				it = listitems[slib.value];
				trk = MxTrack(this, MxTrack.tracks + 1, slib.items[slib.value], true, it.bpm);
				tracks = tracks.add(trk);
				if (xfader.notNil) { xfader.update }; 
				if (this.needsConversion(it))
				{
					if (it.format == "mp3") 	{ trk.loadMP3(path++it.path, true) }
											{ trk.loadM4A(path++it.path, true) }
				}
				{
					trk.loadTrack(path++it.path, true);
				}
			});
		
		RoundButton(win, Rect(265, 35, 50, 20))
			.font_(font)
			.states_([["[dsk]", Color.grey, Color.black]])
			.action_({
				var it, trk;
				it = listitems[slib.value];
				trk = MxTrack(this, MxTrack.tracks + 1, slib.items[slib.value], false, it.bpm);
				tracks = tracks.add(trk);
				if (xfader.notNil) { xfader.update }; 
				if (this.needsConversion(it))
				{
					if (it.format == "mp3") 	{ trk.loadMP3(path++it.path, false) }
											{ trk.loadM4A(path++it.path, false) }
				}
				{
					trk.loadTrack(path++it.path, false);
				}
			});
		
		RoundButton(win, Rect(265, 60, 50, 20))
			.font_(font)
			.states_([["[bfr]", Color.grey, Color.black]])
			.action_({
				var it, trk;
				it = listitems[slib.value];
				trk = MxTrack(this, MxTrack.tracks + 1, slib.items[slib.value], false, it.bpm);
				tracks = tracks.add(trk);
				if (xfader.notNil) { xfader.update }; 
				if (this.needsConversion(it))
				{
					if (it.format == "mp3") 	{ trk.loadMP3(path++it.path, true) }
											{ trk.loadM4A(path++it.path, true) };
				}
				{
					trk.loadTrack(path++it.path, true);
				}
			});	
		
		RoundButton(win, Rect(265, 100, 40, 20))	
			.font_(font)
			.states_([["../", Color.yellow, Color.black]])
			.action_({ this.getPath });
		
		record = RoundButton(win, Rect(265, 130, 40, 20))
			.states_([[":-:", Color.red, Color.black], [".o.", Color.grey, Color.red]])
			.action_({|btn|
				if (btn.value == 0, {
					server.stopRecording
				}, {
					Routine({
						server.recSampleFormat = "int24";
						server.prepareForRecord("/Users/alo/sounds/live/dj" 
							++ Date.getDate.stamp ++ ".aif");
						1.wait;
						server.record
					}).play
				})
			});
		
		RoundButton(win, Rect(265, 160, 40, 20))
			.font_(font)
			.states_([[".X.", Color.blue, Color.black]])
			.action_({ this.makeXFader });	
			
		RoundButton(win, Rect(265, 190, 40, 20))	
			.font_(font)
			.states_([["|:|", Color.green, Color.black]])
			.action_({ eq = MasterEQ(4) });
		
		RoundButton(win, Rect(265, 220, 40, 20))	
			.font_(font)
			.states_([[":.:.", Color.grey, Color.black], [".:.:", Color.green, Color.black]])
			.action_({|btn|
				if (btn.value == 1)
				{
					bpmsynth = Synth.after(1, \masterbpmdetector, [\in, 0]);
					bpmview.stringColor_(Color.green);
					bpmresp = OSCresponderNode(server.addr, '/masterbpm', {|rs, ti, ms| 
						masterbps = ms[3];
						masterbpm = masterbps * 60;
						{
							bpmview.string_(masterbpm.round(0.001).asString);
							bpmtick.value = 1;
							AppClock.sched(0.1, { bpmtick.value = 0; nil });
						}.defer;
					}).add;
					
				}
				{
					bpmview.stringColor_(Color.white);
					bpmsynth.free;
					bpmsynth = nil;
					bpmresp.remove;
					bpmresp = nil;
				}
			});
			
		StaticText(win, Rect(265, 250, 45, 20))
			.align_(\center)
			.font_(font)
			.string_("bpm:")
			.background_(Color.grey(0.2))
			.stringColor_(Color.white);
						
		bpmview = StaticText(win, Rect(265, 270, 45, 20))
			.align_(\center)
			.font_(Font(font.name, font.size - 1))
			.background_(Color.grey(0.2))
			.stringColor_(Color.white)
			.string_("0.000");

		bpmtick = RoundButton(win, Rect(315, 270, 10, 10))
			.states_([["", Color.grey, Color.grey], ["", Color.yellow, Color.yellow]]);
			
		outL = SCLevelIndicator(win, Rect(320, 10, 40, 300))
			.drawsPeak_(true)
			.critical_(0.dbamp)
			.warning_(-1.dbamp)
			.style_(2)
			.numSteps_(18)
			.numTicks_(9);

		outR = SCLevelIndicator(win, Rect(360, 10, 40, 300))
			.drawsPeak_(true)
			.critical_(0.dbamp)
			.warning_(-1.dbamp)
			.style_(2)
			.numSteps_(18)
			.numTicks_(9);
			
		SmoothSlider(win, Rect(400, 10, 30, 300))
			.value_(1)
			.action_({|slider|
				var sp = ControlSpec(0.001, 1.0, \exp);
				volumeControl.set(\main, sp.map(slider.value));
				txtMain.string_(sp.map(slider.value).ampdb.round(1).asString)
			});
		
		txtMain = StaticText(win, Rect(400, 10, 30, 300))
			.align_(\center)
			.font_(font)
			.stringColor_(Color.green)
			.string_("0");

		cueL = SCLevelIndicator(win, Rect(435, 10, 40, 300))
			.drawsPeak_(true)
			.critical_(0.dbamp)
			.warning_(-1.dbamp)
			.style_(2)
			.numSteps_(18)
			.numTicks_(9);
			
		cueR = SCLevelIndicator(win, Rect(475, 10, 40, 300))			.drawsPeak_(true)
			.critical_(0.dbamp)
			.warning_(-1.dbamp)
			.style_(2)
			.numSteps_(18)
			.numTicks_(9);

		SmoothSlider(win, Rect(515, 10, 30, 300))
			.value_(1)
			.action_({|slider|
				var sp = ControlSpec(0.001, 1.0, \exp);
				volumeControl.set(\cue, sp.map(slider.value));
				txtCue.string_(sp.map(slider.value).ampdb.round(1).asString)
			});

		txtCue = StaticText(win, Rect(515, 10, 30, 300))
			.align_(\center)
			.font_(font)
			.stringColor_(Color.green)
			.string_("0");
		
		OSCresponderNode(server.addr, '/tr', {|time, resp, msg|
			if (msg[2] < 1000)
			{
				this.updateTrack(msg[2], msg);
			}

		}).add
	}
	
	makeXFader{ xfader = MxXFader(this) }
	
	removeXFader{ xfader = nil; }
	
	loadArchive{
		archive = Archive.read(archivepath);
	}
	
	updateTrack{|id, msg|
		var current;
		current = tracks.select({|trk| trk.id == id }).first;
		current.position = msg[3].asInteger;
		{
			current.waveview.timeCursorPosition = msg[3].asInteger;
			current.updateZoom;
			current.updateTimeDisplay
		}.defer;
				
	}
	
	needsConversion{|item|
		var return = false;
		if ((item.format == "mp3").or(item.format == "m4a"))
		{
			return = true;
		}
		^return
	}
		
}

MxTrack{
	
	classvar <decodepath = "/Users/alo/sounds/decode/";
	classvar <>tracks = 0;
	
	var master, <id, <name, useWarp = false, <bpm, <warpslider, <ampslider, <cueslider; 
	var <waveview, view, pitch = 1.0, play, cue = 0.0, <amp = 0.0, sf; 
	var <>synth, wspec, bufferB, bufferD, rout, <xfadeTrack, <>position = 0;
	var fadeL = false, fadeR = false, viewscroll, bpmview;
	var done = false, bpmToggle, filepath, <>synthRunning = false, bpmSave, <numFrames = 0;
	var filename, zoom, zoomed = false, frameSize = 20, zoomPos = 0, timeDisplay, bpmstart;
	var font, uc = 0, bufferInfo, hasBuffer = false, tmpPath, height = 180, hitcount = 0;
	var txtAmp, txtCue, txtWarp, bpmsynth, bpmbuf, bpmresp, bps, bbus, bpmtick;

	*new{|master, id, name, useWarp, bpm|
		^super.newCopyArgs(master, id, name, useWarp, bpm).init
	}
	
	init{
		MxTrack.tracks = id;
		wspec = ControlSpec(0.8, 1.2);
		view = SCWindow(name, Rect(5, 400, 1000, height)).front.alpha_(0.9);
		view.view.background_(HiliteGradient(Color.grey(0.2), Color.grey(0.8), \v, 256, 0.5));
		view.onClose_({
			this.free;
		});
		view.view.keyDownAction = {|view, char, mod, uni, key|
			if (key == 49) { 
				play.value = (play.value + 1).wrap(0, play.states.size - 1);
			 	play.doAction 
			 }
		};
		font = Font("Skia", 10);

		warpslider = SmoothSlider(view, Rect(5, 5, 195, 20))
			.hilightColor_(Color.clear)
			.value_(0.5)
			.action_({|sl|
				pitch = wspec.map(sl.value);
				txtWarp.string_(pitch.round(0.001).asString);
				if (synthRunning, {
					synth.set("warp", pitch)
				});
			});
		
		txtWarp = StaticText(view, Rect(5, 5, 200, 20))
			.align_(\center)
			.font_(font)
			.stringColor_(Color.white)
			.string_(1);	
		
		SCStaticText(view, Rect(10, 5, 15, 20))
			.stringColor_(Color.white)
			.string_("-");
			
		SCStaticText(view, Rect(185, 5, 15, 20))
			.stringColor_(Color.white)
			.string_("+");
		
		play = RoundButton(view, Rect(5, 30, 40, 20))
			.states_([["..::..", Color.red, Color.black], ["::..::", Color.green, Color.black]])
			.action_({|me|
				if (me.value == 0, {
					synth.free;
					synthRunning = false;
					viewscroll.enabled = true;
					bpmToggle.enabled = false;
					bbus.free;
					bbus = nil;
					if (bpmsynth.notNil) {
						bpmsynth.free;
						bpmbuf.free;
						bpmsynth = nil
					};
					synth = nil;
					if (hasBuffer.not) {bufferD.close; bufferD = nil};
				}, {
					bbus = Bus.audio(master.server);
					if (hasBuffer)
					{
						if (useWarp)
						{
							synth = Synth("mx", [\beat, bbus, \bufnum, bufferB.bufnum,
								\amp, amp, \cueamp, cue, \warp, pitch, 
								\start, position, \id, id]);
						}
						{
							synth = Synth("bmx", [\beat, bbus, \bufnum, bufferB.bufnum, 
								\amp, amp, \cueamp, cue, \warp, pitch, 
								\start, position, \id, id]);
						}
					}
					{
						bufferD = Buffer.cueSoundFile(master.server, filepath, position, 2);
						AppClock.sched(0.1, {
							synth = Synth("dmx", [\beat, bbus, \bufnum, bufferD.bufnum, 
								\frames, bufferInfo.numFrames, \amp, amp, 
								\cueamp, cue, \start, position, \id, id]);
							nil
						});

					};
					synthRunning = true;
					viewscroll.enabled = false;
					bpmToggle.enabled = true;

				})
			});

		zoom = RoundButton(view, Rect(50, 30, 40, 20))
			.states_([["||", Color.blue, Color.black], ["||||", Color.blue, Color.black]])
			.action_({|btn|
				if (btn.value == 1, {
					zoomed = false;
					waveview.zoomAllOut;
					viewscroll.enabled = false;
				}, {
					zoomed = true;
					zoomPos = position - (frameSize * bufferInfo.sampleRate * 0.5);
					{waveview.zoom(
						frameSize/
							(bufferInfo.numFrames/bufferInfo.sampleRate))}.defer;
					this.updateZoom;
					if (synthRunning.not, {
						viewscroll.enabled = true
					})
				})
			});
			
		filename = SCStaticText(view, Rect(5, 55, 145, 20))
			.align_(\center)
			.font_(font)
			.background_(Color.grey)
			.stringColor_(Color.white)
			.string_("no file loaded");
	
			
		bpmToggle = RoundButton(view, Rect(5, 80, 40, 20))
			.states_([[":.:.", Color.grey, Color.black], [".:.:", Color.green, Color.black]])
			.action_({|btn|
				if (btn.value == 1)
				{
					if (synthRunning) {
						fork({
							bpmsynth = Synth.after(synth, \bpmdetector, [\in, bbus, 
								\id, id + 1000, \lock, 0]);
							bpmresp = OSCresponderNode(master.server.addr, '/tr', {|rs, ti, ms| 
								if (ms[2] == (id + 1000))
								{
									bps = ms[3];
									bpm = bps * 60;
									{
										bpmview.string_("BPM: " 
											++ bpm.round(0.001).asString);
										bpmtick.value = 1;
										AppClock.sched(0.1, { bpmtick.value = 0; nil });
									}.defer;
								}
							}).add;
						})
					}
					{
						bpmToggle.value = 0;
					}
				}
				{
					if (synthRunning) {
						bpmsynth.free;
						bpmsynth = nil;
						bpmresp.remove
					}
				}
			});

		bpmSave = RoundButton(view, Rect(50, 80, 40, 20))
			.states_([["[::]", Color.red, Color.black]])
			.action_({
				this.addToArchive
			});	
		
		bpmtick = RoundButton(view, Rect(100, 85, 10, 10))
			.states_([["", Color.grey, Color.grey], ["", Color.yellow, Color.yellow]]);
					
		bpmview = SCStaticText(view, Rect(5, 105, 100, 20))
			.align_(\center)
			.font_(font)
			.background_(Color.grey)
			.stringColor_(Color.white)
			.string_("BPM: 0.000");
			
		RoundButton(view, Rect(5, 130, 40, 20))
			.states_([["| = |", Color.yellow, Color.black]])
			.action_({
				if ((bpm.notNil).and(bpm > 0).and(master.masterbpm.notNil))
				{
					pitch = master.masterbpm / bpm;
					warpslider.value = wspec.unmap(pitch);
					txtWarp.string_(pitch.round(0.001).asString);
					if (synth.notNil)
					{
						synth.set(\warp, pitch)
					};
				};
			});
												
		waveview = SCSoundFileView(view, Rect(205, 5, 790, height - 25))
			.background_(Color.grey(0.2))
			.elasticMode_(true)
			.timeCursorOn_(true)
			.timeCursorColor_(Color.green)
			.waveColors_([Color.new255(75, 178, 255), Color.new255(75, 178, 255)])
			.timeCursorPosition_(0)
			.drawsWaveForm_(true)
			.gridOn_(false)
			.gridResolution_(5.0)
			.action_({|me|
				position = me.selectionStart(0);
				this.updateTimeDisplay;
			});
								
		ampslider = SmoothSlider(view, Rect(155, 30, 20, height - 35))
			.value_(0)
			.action_({|sl|
				var sp;
				sp = ControlSpec(0.001, 1.0, \exp);
				amp = sp.map(sl.value);
				if (synthRunning, {
					synth.set("amp", amp)
				});
				txtAmp.string_(amp.ampdb.round(1).asString);
			});
		txtAmp = SCStaticText(view, Rect(155, 20, 20, height - 30))
			.stringColor_(Color.white)
			.align_(\center)
			.string_(0.ampdb);
		cueslider = SmoothSlider(view, Rect(180, 30, 20, height - 35))
			.value_(0)
			.action_({|sl|
				var sp;
				sp = ControlSpec(0.001, 1.0, \exp);
				cue = sp.map(sl.value);
				if (synthRunning, {
					synth.set("cueamp", cue)
				});
				txtCue.string_(cue.ampdb.round(1).asString);
			});
		txtCue = SCStaticText(view, Rect(180, 20, 20, height - 30))
			.stringColor_(Color.white)
			.align_(\center)
			.string_(1.ampdb.round(1).asString);
		timeDisplay = SCStaticText(view, Rect(710, height - 20, 40, 20))
			.font_(font)
			.stringColor_(Color.grey(0.9)).string_("00:00");
		
		viewscroll = SCSlider(view, Rect(205, height - 15, 500, 10))
				.enabled_(false)
				.action_({|slider|
					waveview.scrollTo(slider.value)
				})
	}
	
	loadTrack{|path, loadBuffer = false|
		var archbpm;
		filepath = path;
		sf = SoundFile.new;
		sf.openRead(filepath);
		waveview.soundfile = sf;
		numFrames = sf.numFrames;
		filename.string_(path.basename.split($.)[0]);
		filename.stringColor_(Color.new255(155, 205, 155));
		if (bpm.notNil) { archbpm = bpm.round(0.01).asString };
		bpmview.string_("BPM: " ++ (archbpm ?? "0.000"));
		bufferInfo = SoundFile.openRead(path);
		hasBuffer = loadBuffer;
		waveview.readWithTask(0, sf.numFrames, 64, {
			if (loadBuffer)
			{
				bufferB = Buffer.read(master.server, path, action:
				 { 
				 	this.doneAction 
				 });
			}
			{
				this.doneAction;
			}
		})
	}
	
	loadMP3{|path, loadBuffer = false, lameopts=""|
		tmpPath = decodepath ++ (path.basename.split($.)[0]) ++ ".wav";
		if ((MP3.lamepath + "--decode" + lameopts + "\"" ++ path ++ "\"" + tmpPath).systemCmd == 0)
		{
			this.loadTrack(tmpPath, loadBuffer, true)
		}
		{
			("MP3: unable to read file:" + path).warn;
			("rm" + tmpPath).unixCmd;		
		}
	}
	
	loadM4A{|path, loadBuffer = false|
		var converter, inform;
		tmpPath = decodepath ++ (path.basename.split($.)[0]) ++ ".aif";
		converter = AFConvert(path, tmpPath);
		inform = Inform("Converting file, please wait...");
		converter.convert({
			inform.close;
			this.loadTrack(tmpPath, loadBuffer, true)
		})
	}
	
	loadBuffer{|buffer|
		tmpPath = decodepath ++ "tmp" ++ Date.stamp ++ ".aif";
		fork{ 
			buffer.write(tmpPath);
			master.server.sync;
			this.loadTrack(tmpPath);
		}
	}
	
	free{	
		master.tracks.remove(this);
		if (master.xfader.notNil) { master.xfader.update };
		if (bufferB.notNil) { bufferB.free };
		if (bufferD.notNil) { bufferD.free; bufferD.close; bufferD = nil };
		if (tmpPath.notNil) { ("rm" + tmpPath).unixCmd }
	}
	
	doneAction{
		zoomed = true;
		zoomPos = position - (frameSize * bufferInfo.sampleRate * 0.5);
		{waveview.zoom(frameSize/(bufferInfo.numFrames/bufferInfo.sampleRate))}.defer;
		{viewscroll.enabled = true}.defer
	}
	
	updateZoom{
		var cfactor;
		if ((zoomed).and(position > (zoomPos - (frameSize * bufferInfo.sampleRate * 0.5))), {
			cfactor = (bufferInfo.numFrames / bufferInfo.sampleRate).reciprocal / 5.521e-08;
			zoomPos = position - (frameSize * bufferInfo.sampleRate * 0.5) + 
				(position / (frameSize * bufferInfo.sampleRate) * cfactor);
			waveview.scrollTo([0, bufferInfo.numFrames - 1].asSpec.unmap(zoomPos));
		})
	}
	
	updateTimeDisplay{
		var time, hr, min, sec, sstr, mstr, hstr, timestr;
	
		if (position < bufferInfo.numFrames, {
	
			time = bufferInfo.numFrames - position / bufferInfo.sampleRate;
			min = (time/60%60).floor;
	    		sec = (time%60).round(1);
			if (sec < 10, {sstr = "0"++sec.asString}, {sstr = sec.asString});
			if (min < 10, {mstr = "0"++min.asString}, {mstr = min.asString});
			timestr = mstr++":"++sstr;
			timeDisplay.string_(timestr)
		
		})
	}
	
	addToArchive{
		master.archive.put(\mxIIIIxm, \trackinfo, 
			filename.string.replace(" ", "_").asSymbol, bpm );
		Archive.write(master.archivepath)
	}

}

MxXFader{
	
	var mx;
	var win, <cross1, c1items, <cross2, c2items, fader, actbtn, active = false;
	var track1, track2, curve = 8, txtcurve;
	
	*new{|mx|
		^super.newCopyArgs(mx).init
	}
	
	init{
		var tr1sp, tr2sp, curvesp;
		
		curvesp = ControlSpec(2, 16, \lin, 1);
		tr1sp = CurveWarp(ControlSpec(1.0, 0.001, \exp), curve);
		tr2sp = CurveWarp(ControlSpec(0.001, 1.0, \exp), curve.neg);
		
		win = SCWindow("..Mx.X.xM..", Rect(525, 1000, 600, 120)).front.alpha_(0.9);
		win.view.background_(HiliteGradient(Color.grey(0.15), Color.grey(0.4), \v, 256, 0.5));
		win.onClose = { mx.removeXFader };
		
		c1items = ["..."] ++ mx.tracks.collect(_.name);
		c2items = ["..."] ++ mx.tracks.collect(_.name);
		
		cross1 = PopUpMenu(win, Rect(5, 5, 250, 30))
			.value_(0)
			.font_(mx.font)
			.stringColor_(Color.green)
			.items_(c1items)
			.action_({|menu|
				if (menu.items[menu.value] != "...")
				{
					track1 = mx.tracks.select({|trk| trk.name == menu.items[menu.value] }).first;
					c2items.remove(track1.name);
				}
				{
					track1 = nil;
					c2items = ["..."] ++ mx.tracks.collect(_.name);
				};
				cross2.items = c2items;
				if (track1.notNil.and(track2.notNil))	{ actbtn.enabled = true; 
												  fader.enabled = true }
												{ actbtn.enabled = false; 
												  fader.enabled = false }
			});

		cross2 = PopUpMenu(win, Rect(345, 5, 250, 30))
			.value_(0)
			.font_(mx.font)
			.stringColor_(Color.green)
			.items_(c2items)
			.action_({|menu|
				if (menu.items[menu.value] != "...")
				{
					track2 = mx.tracks.select({|trk| trk.name == menu.items[menu.value] }).first;
					c1items.remove(track2.name);
				}
				{
					track2 = nil;
					c1items = ["..."] ++ mx.tracks.collect(_.name);
				};
				cross1.items = c1items;				
				if (track1.notNil.and(track2.notNil))	{ actbtn.enabled = true; 
												  fader.enabled = true }
												{ actbtn.enabled = false; 
												  fader.enabled = false }
			});
		
		actbtn = RoundButton(win, Rect(270, 5, 60, 30))
			.enabled_(false)
			.font_(mx.font)
			.states_([["+", Color.red, Color.black], ["-", Color.black, Color.green]])
			.action_({|btn| 
				active = (btn.value == 1) 
			});
		
		SmoothSlider(win, Rect(260, 40, 80, 15))
			.value_(curvesp.unmap(curve))
			.action_({|slider|
				curve = curvesp.map(slider.value);
				txtcurve.string_(curve.asString);
				tr1sp = CurveWarp(ControlSpec(1.0, 0.001, \exp), curve);
				tr2sp = CurveWarp(ControlSpec(0.001, 1.0, \exp), curve.neg);
			});
		
		txtcurve = StaticText(win, Rect(260, 40, 80, 15))
			.align_(\center)
			.font_(mx.font)
			.stringColor_(Color.yellow)
			.string_(curve.asString);
		
		fader = SmoothSlider(win, Rect(5, 60, 590, 50))
			.enabled_(false)
			.hilightColor_(Color.clear)
			.action_({|slider|
				if (active)
				{
					track1.ampslider.value = tr1sp.map(slider.value);
					track1.ampslider.doAction;
					track2.ampslider.value = tr2sp.map(slider.value);
					track2.ampslider.doAction;
				}
			});
		
	}
	
	update{
		var val1, val2;
		
		val1 = cross1.items[cross1.value];
		val2 = cross2.items[cross2.value];
		
		c1items = ["..."] ++ mx.tracks.collect(_.name);
		cross1.items = c1items;
		cross1.value = cross1.items.indexOf(val1) ? 0;
		
		c2items = ["..."] ++ mx.tracks.collect(_.name);
		cross2.items = c2items;
		cross2.value = cross2.items.indexOf(val2) ? 0;				
	}
	
}