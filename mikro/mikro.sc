Mikro{
	
	var <input, <graphics, <analyzer;
	var <decoder, <>group, <gui, initFuncs, cleanFuncs;
		
	*new{|input, graphics, analyzer, duration, nCoef|
		^super.newCopyArgs(input, graphics, analyzer).init(duration, nCoef)
	}
	
	init{|duration, nCoef|
		
		if (input.isNil) { 
			input = MikroInput(Decoder())
		};
		
		decoder = input.decoder;
		
		if (graphics.isNil) {
			graphics = MikroGraphics(
				width: 800,
				height: 600,
				sizeX: 40,
				sizeY: 40,
				frameRate: 30,
				remoteAddr: NetAddr("127.0.0.1", 7770),
				vectorSize: nCoef,
				trainDur: 5000,
				lRate: 0.1
			)
		};
		
		if (analyzer.isNil) {
			analyzer = MikroAnalyzer(duration, nCoef)
		};
	}
	
	addInitFunc{|func|
		if (initFuncs.isNil) {
			initFuncs = Array()
		};
		
		initFuncs = initFuncs.add(func);
	}
	
	addCleanFunc{|func|
		if (cleanFuncs.isNil) {
			cleanFuncs = Array()
		};
		
		cleanFuncs = cleanFuncs.add(func);		
	}

	initPerformance{|debug = 1|
		group = Group();
		decoder.start(group, \addAfter);

		input.prepare(group);

		graphics.start(debug);
		
		initFuncs.do(_.value);
		
	}
	
	initRemote{
		group = Group();
		decoder.start(group, \addAfter);

		input.prepare(group);
		
		initFuncs.do(_.value);
		
	}
	
	start{|onsetGate, lag, msgRate|
		analyzer.start(input.bus, group, \addToHead, onsetGate, lag, msgRate);
		
		analyzer.putEventResponderFunction(\sendWeights, {|time, re, ms|
			if (ms[2] == 3) {
				 graphics.sendWeights(*ms[3..analyzer.numcoef+2])
			}
		});

		input.start;		
	}
	
	stop{
		analyzer.free;
		input.stop;
	}
	
	quit{|quitDecoder=true, quitGraphics=true|
		group.free;
		input.free;
		if (quitGraphics) { graphics.quit };
		if (quitDecoder) { decoder.free };
		cleanFuncs.do(_.value);		
	}
		
	makeGui{|composer, recognizer|
		gui = MikroGui(this, composer, recognizer);
	}
		
}


MikroLambda : Mikro{

	start{|onsetGate, lag, msgRate|
		analyzer.start(input.bus, group, \addToHead, onsetGate, lag, msgRate);
		
		analyzer.putEventResponderFunction(\sendWeights, {|time, re, ms|
			if (ms[2] == 3) {
				 graphics.sendSOMVector(ms[3..analyzer.numcoef+2])
			}
		});

		input.start;	
		
	}
	
}

MikroInput{
	
	var <decoder, <>testBufferPath, <thruBus, <inputBus, <isLive, <bus, <group;
	var <testBuffers, auxamp = 1.0, mainamp = 1.0;
	var <synth, routine;
	var <currentPatch, <>patchChangeAction, <defs;
	
	*new{|decoder, testBufferPath, thruBus, inputBus=0|
		^super.newCopyArgs(decoder, testBufferPath, thruBus, inputBus).init
	}
	
	init{
		if (testBufferPath.isNil) { isLive = true } { isLive = false };
		this.sendSynthDefs;
	}
	
	sendSynthDefs{	
		defs = Array.with(	
			SynthDef(\inputlive, {|main, aux, xamp, mamp, xang, yang, zang, maxdel|
				var sig, a, b, c, d, w, x, y, z, del, shift;
				sig = SoundIn.ar(inputBus);
				del = ArrayControl.kr(\delays, 4, 0);
				shift = ArrayControl.kr(\shifts, 4, 1);	
				Out.ar(aux, sig * xamp);
				#a, b, c, d = Array.fill(4, {|i|
					PitchShift.ar(DelayN.ar(sig, maxdel, del[i]), 0.2, shift[i]);
				});
				#w, x, y, z = A2B.ar(a, b, c, d);
				Out.ar(main, AtkRotateXYZ.ar(w, x, y, z, xang, yang, zang) * mamp);
			}).add,	
	
			SynthDef(\inputbuf, {|main, aux, xamp, mamp, xang, yang, zang, maxdel, buf|
				var sig, a, b, c, d, w, x, y, z, del, shift;
				sig = PlayBuf.ar(1, buf, doneAction: 2);
				del = ArrayControl.kr(\delays, 4, 0);
				shift = ArrayControl.kr(\shifts, 4, 1);	
				Out.ar(aux, sig * xamp);
				#a, b, c, d = Array.fill(4, {|i|
					PitchShift.ar(DelayN.ar(sig, maxdel, del[i]), shift[i]);
				});
				#w, x, y, z = A2B.ar(a, b, c, d);
				Out.ar(main, AtkRotateXYZ.ar(w, x, y, z, xang, yang, zang) * mamp);
			}).add,
			
			SynthDef(\inputhru, {|main, aux, in, xamp, mamp|
				var bformat;
				bformat = In.ar(in, 4);
				Out.ar(aux, bformat.first * xamp);
				Out.ar(main, bformat * mamp);
			}).add
		)

	}
		
	loadTestBuffers{|action|
				
		testBuffers = Array();
		
		Routine({
		
			testBufferPath.pathMatch.do({|path|
				testBuffers = testBuffers.add(Buffer.read(Server.default, path));
				Server.default.sync;
			});
			
			action.value
		
		}).play
		
	}
	
	prepare{|target, doneAction|
		
		bus = Bus.audio;
		group = Group.before(target);
		
		if (isLive)
		{
			if (thruBus.isNil)
			{
				synth = Synth.tail(group, \inputlive, [\main, decoder.bus, \aux, bus, \xamp, auxamp, 
					\mamp, mainamp, \xang, 0.0, \yang, 0.0, \zang, 0.0, \maxdel, 0.1])
					.setn(\delays, Array.geom(4, 0.01, 1.618))
					.setn(\shifts, Array.geom(4, 36/35, 35/36))
			}
			{
				synth = Synth.tail(group, \inputhru, [\main, decoder.bus, \aux, bus, \in, thruBus, \xamp, auxamp, 
					\mamp, mainamp] )
			}
			
		}
		{
			this.loadTestBuffers({
				
				doneAction.();
				
				if (testBuffers.size > 1) 
				{
					routine = Routine({
						inf.do({
							testBuffers.scramble.do({|buf|								synth = Synth.head(group, \inputbuf, [\main, decoder.bus, \aux, bus, \xamp, auxamp,
								 	\mamp, mainamp, \xang, 0.0, \yang, 0.0, \zang, 0.0, \maxdel, 0.1, \buf, buf])
									.setn(\delays, Array.geom(4, 0.01, 1.618))
									.setn(\shifts, Array.geom(4, 36/35, 35/36));
								currentPatch = buf.path.basename.split($.).first.keep(6).asSymbol;
								patchChangeAction.value(currentPatch);
								buf.duration.wait;
							})
						})				
					});
				}
			})
		};
		
	}
	
	start{
		if (isLive.not) {
			if (testBuffers.size == 1) {
				synth = Synth.head(group, \inputbuf, [\main, decoder.bus, \aux, bus, 
					\xamp, auxamp, \mamp, mainamp, \xang, 0.0, \yang, 0.0, \zang, 0.0, \maxdel, 0.1, 
					\buf, testBuffers.first]
				).setn(\delays, Array.geom(4, 0.01, 1.618)).setn(\shifts, Array.geom(4, 36/35, 35/36))
			}
			{
				routine.play 
			}
		}
		
	}
	
	stop{
		if (isLive.not) {
			if (testBuffers.size == 1) {
				synth.free;
				synth = nil;
			}
			{
				routine.stop	
			}
		}
	}
	
	free{
		if (isLive) { 
			synth.free;  
			synth = nil;
		}
		{
			testBuffers.do({|buf|
				buf.free
			});
			testBuffers = nil;
		};
		
		bus.free;
		group.free;
		bus = nil;
		group = nil;
		
	}
	
	auxamp_{|value|
		auxamp = value;
		if (synth.notNil) { synth.set(\xamp, auxamp) };
	}
	
	mainamp_{|value|
		mainamp = value;
		if (synth.notNil) { synth.set(\mamp, mainamp) };
	}
	
}

MikroFoaInput : MikroInput{

	*new{|decoder, testBufferPath, thruBus, inputBus=0|
		^super.new(decoder, testBufferPath, thruBus, inputBus).init
	}
	
	init{
		if (testBufferPath.isNil) { isLive = true } { isLive = false };
		this.sendSynthDefs;
	}
	
	sendSynthDefs{
		defs = Array.with(	
		
			SynthDef(\inputlive, {|main, aux, xamp, mamp, xang, yang, zang, maxdel|
				var input, sig, bfrm, fft;
				input = SoundIn.ar(inputBus);	
				Out.ar(aux, input * xamp);
				fft = FFT(LocalBuf(1024), input);
				sig = Array.fill(4, { IFFT(PV_Diffuser(fft, Dust.kr(10))) });
				bfrm = FoaEncode.ar(sig, FoaEncoderMatrix.newAtoB );
				Out.ar(main, FoaTransform.ar(bfrm, 'rtt', xang, yang, zang) * mamp);
			}).add,		
	
			SynthDef(\inputbuf, {|main, aux, xamp, mamp, xang, yang, zang, maxdel, buf|
				var input, sig, bfrm, fft;
				input = PlayBuf.ar(1, buf, BufRateScale.kr(buf), doneAction: 2);
				Out.ar(aux, input * xamp);
				fft = FFT(LocalBuf(1024), input);
				sig = Array.fill(4, { IFFT(PV_Diffuser(fft, Dust.kr(10))) });
				bfrm = FoaEncode.ar(sig, FoaEncoderMatrix.newAtoB );
				Out.ar(main, FoaTransform.ar(bfrm, 'rtt', xang, yang, zang) * mamp);
			}).add,
			
			SynthDef(\inputhru, {|main, aux, in, xamp, mamp|
				var bformat;
				bformat = In.ar(in, 4);
				Out.ar(aux, bformat.first * xamp);
				Out.ar(main, bformat * mamp);
			}).add
			
		)
	}
	
}

MikroInputOnsetTracker{
	
	var <input, <graphics, <synth, <responder, <>onsetFunc;
	
	*new{|input, graphics|
		^super.newCopyArgs(input, graphics).init
	}
	
	init{
		SynthDef(\onsetTracker, {|th, lag|
			var sig, fft, trig;
			sig = SoundIn.ar(input.inputBus);
			fft = FFT(LocalBuf(1024), sig);
			trig = Onsets.kr(fft, th);
			SendReply.kr(trig, '/onsetTracker', Amplitude.kr(sig, lag))
		}).add
	}

	start{|th=0.01, lag=0.05|
		synth = Synth(\onsetTracker, [\th, th, \lag, lag ]);
		responder = OSCFunc({|msg|
			onsetFunc.(msg)
		}, '/onsetTracker');
	}

	stop{
		synth.free;
		synth = nil;
		responder.disable;
		responder = nil;
	}
	
}

MikroGui{
	
	var mikro, composer, recognizer, window, postwin, postview, synthview, poststring, queryclock, ctrwin; 
	var	ampspec, recth, addspec, liveProcs, patch, qrypatch, actual, common, btns; 
	var lag = 0.05, lagspec, msgrate = 20, ratespec, freesynth, release, debug = 1, time, graphSliders, graphwin; 
	
	*new{|mikro, composer, recognizer|
		^super.newCopyArgs(mikro, composer, recognizer).init
	}
	
	init{
		var font;
		
		font = Font("Lucida Grande", 9);
		ampspec = ControlSpec(-80.dbamp, 0.dbamp, \exp);
		lagspec = ControlSpec(0.01, 0.5, step: 0.01);
		ratespec = ControlSpec(10, 30, step: 1);
		addspec = CosineWarp(ControlSpec(0.001, 0.999));
		
		recth = -80.dbamp;
		
		btns = Array.newClear(mikro.graphics.numPatterns);
		
		window = Window("----<><><><><>----", Rect(200, 200, 600, 510)).alpha_(0.98).front;
		window.background_(Color.grey(0.3));
		
		window.onClose = { this.stopServerQuery };
		
		ctrwin = CompositeView(window, Rect(5, 5, 390, 490));
		
		RoundButton(ctrwin, Rect(5, 5, 60, 25))
			.font_(font)
			.states_([[".debug.", Color.yellow, Color.black], ["!LIVE!", Color.black, Color.yellow]])
			.action_({|btn|
				if (btn.value == 1) { debug = 2 } { debug = 1 }
			});
			
		RoundButton(ctrwin, Rect(5, 35, 60, 25))
			.font_(font)
			.states_([["init", Color.yellow, Color.black], ["quit", Color.black, Color.yellow]])
			.action_({|btn|
				if (btn.value == 1)
				{
					mikro.initPerformance(debug);
					this.post("performance initialized");
				}
				{
					mikro.quit
				}
			});
			
		RoundButton(ctrwin, Rect(5, 65, 60, 25))
			.font_(font)
			.states_([["start", Color.yellow, Color.black], ["stop", Color.black, Color.yellow]])
			.action_({|btn|
				if (btn.value == 1) {
					mikro.start(recth, lag, msgrate);
					time.start;
					mikro.graphics.settings[\groupx] = 5;
					mikro.graphics.settings[\groupy] = 5;
					mikro.graphics.settings[\transz] = 40;
					mikro.graphics.settings[\transx] = -40;
					mikro.graphics.settings[\transy] = -30;
					mikro.graphics.sendSettings;
					this.post("performance running");
					composer.start(5.0);
					
				}
				{
					mikro.stop;
					time.stop;
					composer.stop;
				}
			});

		RoundButton(ctrwin, Rect(5, 95, 60, 25))
			.font_(font)
			.states_([["rec off", Color.yellow, Color.black], ["rec on", Color.black, Color.yellow]])
			.enabled_(false)
			.action_({|btn|
				if (btn.value == 1)
				{
					mikro.startRecorder(recth)
				}
				{
					mikro.stopRecorder
				}
			});			

		RoundButton(ctrwin, Rect(5, 125, 60, 25))
			.font_(font)
			.states_([["mfcc off", Color.yellow, Color.black], ["mfcc on", Color.black, Color.yellow]])
			.enabled_(false)
			.action_({|btn|
				if (btn.value == 1)
				{
					mikro.startMFCC
				}
				{
					mikro.stopMFCC
				}
			});

		RoundButton(ctrwin, Rect(5, 155, 60, 25))
			.font_(font)
			.states_([["seq off", Color.yellow, Color.black], ["seq on", Color.black, Color.yellow]])
			.enabled_(false)
			.action_({|btn|
				if (btn.value == 1)
				{
					mikro.seqActive = true
				}
				{
					mikro.seqActive = false
				}
			});		
			
		RoundButton(ctrwin, Rect(5, 185, 60, 25))
			.font_(font)
			.states_([["last buf", Color.yellow, Color.black]])
			.enabled_(false)
			.action_({|btn|
				mikro.startBufSynth(\bufplay, Env([0.001, 1.0, 1.0, 0.001], [0.1, 0.5, 0.4], \sine), 
					mikro.inputBuffers.last, mikro.inputBuffers.last, rrand(1, 4), 1);
			});			

		
		SmoothSlider(ctrwin, Rect(75, 5, 30, 200))
			.stringColor_(Color.green)
			.string_("0(db)")
			.value_(1)
			.action_({|sl|
				mikro.decoder.synth.set(\amp, ampspec.map(sl.value));
				sl.string_(ampspec.map(sl.value).ampdb.round(1).asString ++ "(db)")
			});

		StaticText(ctrwin, Rect(75, 205, 30, 20))
			.align_(\center)
			.stringColor_(Color.grey(0.8))
			.font_(font)
			.string_("main");

		SmoothSlider(ctrwin, Rect(110, 5, 30, 200))
			.stringColor_(Color.green)
			.string_("0(db)")
			.value_(1)
			.action_({|sl|
				mikro.input.mainamp_(ampspec.map(sl.value));
				sl.string_(ampspec.map(sl.value).ampdb.round(1).asString ++ "(db)")
			});

		StaticText(ctrwin, Rect(110, 205, 30, 20))
			.align_(\center)
			.stringColor_(Color.grey(0.8))
			.font_(font)
			.string_("input");

		SmoothSlider(ctrwin, Rect(145, 5, 30, 200))
			.stringColor_(Color.green)
			.string_("0(db)")
			.value_(1)
			.action_({|sl|
				mikro.input.auxamp_(ampspec.map(sl.value));
				sl.string_(ampspec.map(sl.value).ampdb.round(1).asString ++ "(db)")
			});

		StaticText(ctrwin, Rect(145, 205, 30, 20))
			.align_(\center)
			.stringColor_(Color.grey(0.8))
			.font_(font)
			.string_("aux");

		SmoothSlider(ctrwin, Rect(180, 5, 30, 200))
			.stringColor_(Color.green)
			.string_("-80(db)")
			.value_(ampspec.unmap(-80.dbamp))
			.action_({|sl|
				recth = ampspec.map(sl.value);
				mikro.analyzer.synth.set(\onsetth, recth);
				sl.string_(recth.ampdb.round(1).asString ++ "(db)")
			});

		StaticText(ctrwin, Rect(180, 205, 30, 20))
			.align_(\center)
			.stringColor_(Color.grey(0.8))
			.font_(font)
			.string_("rec th");
			
		SmoothSlider(ctrwin, Rect(215, 5, 30, 200))
			.stringColor_(Color.green)
			.string_("0.05(s)")
			.value_(lagspec.unmap(lag))
			.action_({|sl|
				lag = lagspec.map(sl.value);
				mikro.analyzer.synth.set(\lag, lag);
				sl.string_(lag.round(0.01).asString ++ "(s)")
			});

		StaticText(ctrwin, Rect(215, 205, 30, 20))
			.align_(\center)
			.stringColor_(Color.grey(0.8))
			.font_(font)
			.string_("lag");

		SmoothSlider(ctrwin, Rect(250, 5, 30, 200))
			.stringColor_(Color.green)
			.string_("20")
			.value_(ratespec.unmap(msgrate))
			.action_({|sl|
				msgrate = ratespec.map(sl.value);
				mikro.analyzer.synth.set(\msgrate, msgrate);
				sl.string_(msgrate.asString)
			});

		StaticText(ctrwin, Rect(250, 205, 30, 20))
			.align_(\center)
			.stringColor_(Color.grey(0.8))
			.font_(font)
			.string_("rate");
			
		time = TimeDisplay(ctrwin, Rect(280, 5, 110, 20), 0, Font("Courier", 10)).background_(Color.clear);
		
		SmoothSlider(ctrwin, Rect(290, 30, 30, 170))
			.stringColor_(Color.green)
			.string_("0.995")
			.value_(addspec.unmap(0.995))
			.action_({|sl|
				mikro.graphics.sendSetting(\add, addspec.map(sl.value));
				sl.string_(addspec.map(sl.value).round(0.001).asString)
			});		
		
		liveProcs = [\fbgverb, \latch, \cliq, \grains, \streamverb, \arhythmic];
		
		liveProcs.do({|def, i|
			RoundButton(ctrwin, Rect(i*60+5, 225, 60, 25))
				.font_(font)
				.states_([[def.asString, Color.yellow, Color.grey(0.2)]])
				.action_({
					var id, ind;
					ind = Pseq((0..mikro.graphics.states.size-1), inf).asStream;
					id = composer.play(def, Env([0.001, 1.0, 1.0, 0.001], [0.3, 0.4, 0.3], \sine, 2, 1), 
						Pseq(mikro.graphics.states ? Array.rand(16, 0.0, 1.0), inf).asStream);
					composer.mapStates(id, composer.descLib[def].metadata.specs.collect({ ind.next }));
					if (freesynth.items.size > 0)
					{
						freesynth.items = (Array.with(*freesynth.items) ++ [(def.asString ++ "(" ++ id ++ ")")]);
					}
					{
						freesynth.items = Array.with(def.asString ++ "(" ++ id ++ ")")
					}
				})
		});

		freesynth = ListView(window, Rect(10, 260, 100, 100))
			.font_(font)
			.stringColor_(Color.grey(0.8));
			
		release = SCNumberBox(ctrwin, Rect(110, 255, 40, 25))
			.align_(\center)
			.font_(Font("Courier", 12))
			.stringColor_(Color.grey(0.8))
			.value_(5)
			.background_(Color.clear);
			
		RoundButton(ctrwin, Rect(155, 255, 60, 25))				.font_(font)
			.states_([["free", Color.yellow, Color.grey(0.2)]])
			.action_({
				var str, id, arr;
				str = freesynth.items[freesynth.value];
				id = str[(str.find("(")+1)..(str.find(")")-1)];
				composer.releaseSynth(id.asInteger, release.value);
				arr = freesynth.items;
				arr.remove(str);
				freesynth.items = arr;
			});
			
		RoundButton(ctrwin, Rect(220, 255, 60, 25))
			.font_(font)
			.states_([["patch", Color.yellow, Color.grey(0.2)], ["patch", Color.grey(0.2), Color.yellow]])
			.action_({|btn|
				if (btn.value == 1)
				{
					recognizer.run;
					qrypatch = Routine({
						inf.do({
							patch.string_((recognizer.currentGuess ? "").asString);
							common.string_((recognizer.mostCommon ? "").asString);
							if (debug == 1) { actual.string_((mikro.currentPatch ? "")) };
							0.2.wait;
						})
					}).play(AppClock)
				}
				{
					patch.string_("");
					common.string_("");
					if (debug == 1) { actual.string_("")};
					recognizer.stop;
					qrypatch.stop;
					qrypatch = nil;
				}
			});
			
		patch = StaticText(ctrwin, Rect(280, 255, 60, 20))
			.font_(font)
			.align_(\center)
			.stringColor_(Color.white);		
		
		common = StaticText(ctrwin, Rect(280, 275, 60, 20))
			.font_(font)
			.align_(\center)
			.stringColor_(Color.black);
						
		actual = StaticText(ctrwin, Rect(280, 295, 60, 20))
			.font_(font)
			.align_(\center)
			.stringColor_(Color.green);		
			
		composer.procs.keys(Array).sort.do({|name, i|
			RoundButton(ctrwin, Rect(i * (ctrwin.bounds.width - 110) / composer.procs.size + 110, 320, 
				(ctrwin.bounds.width - 155) / composer.procs.size, 25))
				.font_(font)
				.states_([[name.asString, Color.green, Color.black]])
				.action_({ composer.procs[name].(composer, mikro.analyzer) })
		});
						
		graphwin = CompositeView(window, Rect(5, 365, 390, 120));
		
		graphwin.decorator = FlowLayout(graphwin.bounds, 5@5, 5@5);
				
		graphSliders = Array.newClear(mikro.graphics.numPatterns);
		
		mikro.graphics.numPatterns.do({|i|
			btns[i] = RoundButton(graphwin, Rect(width: graphwin.bounds.width / mikro.graphics.numPatterns - 6, height: 20))
				.font_(font)
				.states_([
					[i.asStringToBase(10, 2), Color.yellow, Color.black], 
					[i.asStringToBase(10, 2), Color.black, Color.yellow]
				])
				.action_({|btn|
					if (btn.value == 1)
					{
						mikro.graphics.sendPattern(i, 1, 0.0);
//						Routine({
//							100.do({
//								graphSliders[i].value = graphSliders[i].value + 0.01;
//								0.1.wait;
//							})
//						}).play;
					}
					{
						mikro.graphics.sendPattern(i, 0, 0.0);
//						Routine({
//							100.do({
//								graphSliders[i].value = graphSliders[i].value - 0.01;
//								0.1.wait;
//							})
//						}).play;
					}
				});
				
		});

		mikro.graphics.numPatterns.do({|i|
		
			graphSliders.put(i, SmoothSlider(graphwin, 
				Rect(width: graphwin.bounds.width / mikro.graphics.numPatterns - 6, height: 100))
					.action_({|slider|
						mikro.graphics.sendPattern(i, btns[i].value, slider.value)
					})
			)
			
		});
		
		postwin = CompositeView(window, Rect(400, 5, 195, 490));
		StaticText(postwin, Rect(0, 0, 195, 20))
			.font_(font)
			.stringColor_(Color.new255(28, 134, 238))
			.align_(\center)
			.string_("post");
		postview = TextView(postwin, Rect(5, 20, 185, 220))
			.background_(Color.grey(0.2))
			.stringColor_(Color.grey(0.8))
			.font_(font);
		StaticText(postwin, Rect(0, 240, 195, 20))
			.font_(font)
			.stringColor_(Color.new255(28, 134, 238))
			.align_(\center)
			.string_("synth");		
		synthview = TextView(postwin, Rect(5, 260, 185, 220))
			.background_(Color.grey(0.2))
			.stringColor_(Color.grey(0.8))
			.font_(font);			
									
		window.drawFunc = {
			Pen.color = Color.grey(0.5); 
			Pen.strokeRect(Rect(5, 5, 390, 490));
			Pen.strokeRect(Rect(400, 5, 195, 490));
		};
		
		this.startServerQuery;
			
	}
	
	post{|text|
		poststring = text ++ "\n" ++ poststring;
		{ postview.string_(poststring) }.defer
	}
	
	clearPost{
		poststring = "";
		postview.string_("");	
	}
		
	queryServer{|server|
		var done = false, synths, resp;
		synths = "";
		resp = OSCresponderNode(server.addr, '/g_queryTree.reply', { arg time, responder, msg;
			var i = 2, tabs = 0, printControls = false, dumpFunc;
			if(msg[1] != 0, {printControls = true});
			//("NODE TREE Group" + msg[2]);
			if(msg[3] > 0, {
				dumpFunc = {|numChildren|
					var j;
					tabs = tabs + 1;
					numChildren.do({
						if(msg[i + 1] >=0, {i = i + 2}, {
							i = i + 3 + if(printControls, {msg[i + 3] * 2 + 1}, {0});
						});
						tabs.do({ synths = synths ++ "   " });
						synths = synths ++ msg[i]; // nodeID
						if(msg[i + 1] >=0, {
							synths = synths ++ " group\n";
							if(msg[i + 1] > 0, { dumpFunc.value(msg[i + 1]) });
						}, {
							synths = synths ++ (" " ++ msg[i + 2]) ++ "\n"; // defname
							if(printControls, {
								if(msg[i + 3] > 0, {
									synths = synths ++ " ";
									tabs.do({ synths = synths ++ "   " });
								});
								j = 0;
								msg[i + 3].do({
									synths = synths ++ " ";
									if(msg[i + 4 + j].isMemberOf(Symbol), {
										synths = synths ++ (msg[i + 4 + j] ++ ": ");
									});
									synths = synths ++ msg[i + 5 + j];
									j = j + 2;
								});
								synths = synths ++ "\n";
							});
						});
					});
					tabs = tabs - 1;
				};
				dumpFunc.value(msg[3]);
			});
			{ synthview.string_(synths) }.defer;
			done = true;
		}).add.removeWhenDone;
		
		server.sendMsg("/g_queryTree", 0, 0);
		
	}
	
	startServerQuery{
		queryclock = SystemClock.sched(2, { this.queryServer(Server.default); 1 });
	}
	
	stopServerQuery{
		queryclock.clear	
	}
	
	
}