Mikro{
	
	var <remote, liveInput, <decoder, recorderBuffers, <inputBuffers, <intervals, inputBus, group, gui, <input;
	var <somDict, <testBuffers, input, <bpm = 140, quant, <>qround = 0.001, <mfccSynth, <>inputGate = 0.01;
	var <recorder, recResponder, bmuResponder, mfccResponder, <server, recorderArgs, bmu, distSlope, vecSlope;
	var <bmuFunctions, addingBuffer = false, lastTime = 0, <liveProcs, <bufProcs, <liveProcDict, <bufProcDict; 
	var <>procPath = "/Users/alo/Development/som/audio/", <activeLiveSynths, <activeBufSynths, <synthLimit = 6; 
	var <vecSlopeTh = 0.2, <distSlopeTh = 7, actions, <defEvents, <lastDefEvent, lastLiveSynth, synthSeq = 0;
	var <regions, <>regionMap, lastRegion, recorderRout, >ixamp = 1.0, >imamp = 1.0, <>seqActive = false;
	var <>liveProcFile = "mikroPROClive.rtf", <>bufProcFile = "mikroPROCbuf.rtf";
	
	*new{|remoteAddr, liveInput, decoder|
		^super.newCopyArgs(remoteAddr, liveInput, decoder).init
	}
	
	init{
		server = Server.default;
		remote = remote ? NetAddr("127.0.0.1", 7770);
		vecSlope = 0;
		distSlope = 0;
		intervals = MarkovSet();
		defEvents = MarkovSet();
		quant = (60/bpm/4).round(qround);
		liveProcs = (procPath ++ liveProcFile).load;
		bufProcs = (procPath ++ bufProcFile).load;
		activeLiveSynths = Event();
		activeBufSynths = Event();
		actions = (
			startSynth: (order: 0, weight: 0.1),
			setParams: (order: 1, weight: 0.8),
			freeSynth: (order: 2, weight: 0.1)
		);
		this.sendSynthDefs;
	}
	
	sendSynthDefs{		
		SynthDef(\inputlive, {|main, aux, xamp, mamp, xang, yang, zang, maxdel|
			var sig, a, b, c, d, w, x, y, z, del, shift;
			sig = SoundIn.ar(0);
			del = ArrayControl.kr(\delays, 4, 0);
			shift = ArrayControl.kr(\shifts, 4, 1);	
			Out.ar(aux, sig * xamp);
			#a, b, c, d = Array.fill(4, {|i|
				PitchShift.ar(DelayN.ar(sig, maxdel, del[i]), shift[i]);
			});
			#w, x, y, z = A2B.ar(a, b, c, d);
			Out.ar(main, AtkRotateXYZ.ar(w, x, y, z, xang, yang, zang) * mamp);
		}).add;

		SynthDef(\recorder, {|in, th, recEnabled, lag, time|
			var input, amprate, amptrig, timer, bufindex, bufnums, detected, sampleCounter;
			bufnums = ArrayControl.kr(\bufnums, 5, {|i| i });
			input = In.ar(in);
			amprate = Slope.ar(Lag2.ar(Amplitude.ar(input), lag));
			amptrig = amprate > th;
			timer = Timer.kr(A2K.kr(amptrig));
			detected = timer >= time;
			bufindex = Stepper.kr(detected, min: 0, max: 4);
			sampleCounter = Phasor.kr(
				DelayN.kr(detected, 1/ControlRate.ir, 1/ControlRate.ir), 64, 0, 
					SampleRate.ir * 5 - 1, 0 );
			RecordBuf.ar(input, Select.kr(bufindex, bufnums), run: amptrig * recEnabled, 
				loop: 0.0, trigger: detected);
			SendReply.kr(detected, '/timer', [timer, bufindex, sampleCounter]);
		}).add;
		
		liveProcDict = Array();
		
		liveProcs.keysValuesDo({|name, proc|
			liveProcDict = liveProcDict.add(name);
			proc.def.add
		});
		
		bufProcDict = Array();
		
		bufProcs.keysValuesDo({|name, proc|
			bufProcDict = bufProcDict.add(name);
			proc.def.add
		});

	}
	
	loadTestBuffers{|testPath|
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
		}).add;		
		
		testBuffers = Array();
		
		Routine({
		
			testPath.pathMatch.do({|path|
				testBuffers = testBuffers.add(Buffer.read(server, path));
				server.sync;
			});
			
			gui.post("test buffers loaded")
		
		}).play
		
	}
	
	initPerformance{|xamp = 1.0, mamp = 1.0|
		inputBus = Bus.audio(server);
		group = Group();
		decoder.start(group, \addAfter);
		ixamp = xamp;
		imamp = mamp;
		
		regionMap = #[
			bufplay,bufplay,tyhi,rwarp,
			bufplay,bufplay,rwarp,tyhi,
			swarp,swarp,bufmod,bufmod,
			swarp,swarp,bufmod,bufmod
		];
		
		if (liveInput)
		{
			input = Synth.before(group, \inputlive, [\main, decoder.bus, \aux, inputBus, \xamp, 1.0, 
				\mamp, 1.0, \xang, 0.0, \yang, 0.0, \zang, 0.0, \maxdel, 0.1])
				.setn(\delays, Array.geom(4, 0.01, 1.618))
				.setn(\shifts, Array.geom(4, 36/35, 35/36))
		}
		{
			input = Routine({
				inf.do({
					testBuffers.scramble.do({|buf|
						var dur;
						dur = buf.numFrames / buf.sampleRate;
						Synth.before(group, \inputbuf, [\main, decoder.bus, \aux, inputBus, \xamp, ixamp,
						 	\mamp, imamp, \xang, 0.0, \yang, 0.0, \zang, 0.0, \maxdel, 0.1, \buf, buf])
							.setn(\delays, Array.geom(4, 0.01, 1.618))
							.setn(\shifts, Array.geom(4, 36/35, 35/36));
						dur.wait;
					})
				})				
			}).play	
		};
		
		gui.post("performance initialised");
		
	}
	
	freeInput{
		group.free;
		if (liveInput)
		{
			input.free;
			input = nil
		}
		{
			input.stop;
			input = nil
		}
	}
	
	addBmuFunction{|key, func|
		if (bmuFunctions.isNil) { bmuFunctions = () };
		bmuFunctions[key] = func;
	}
	
	removeBmuFunction{|key|
		bmuFunctions[key] = nil
	}
	
	clearBmuFunctions{
		bmuFunctions.clear
	}
	
	startMFCC{
		
		remote.sendMsg("/som/zoom", -49);
				
		mfccResponder = OSCresponderNode(server.addr, '/mfcc', {|ti, re, ms|
			remote.sendMsg(*(["/som/vector"] ++ ms[3..8]))
		}).add;
		
		bmuResponder = OSCresponderNode(nil, '/som/bmu', {|ti, re, ms| 
			distSlope = ((bmu.x - ms[1]).squared + (bmu.y - ms[2]).squared).sqrt;
			vecSlope = (bmu.vector - ms[3..8]).squared.sum.sqrt;
			bmuFunctions.do(_.value(bmu, distSlope, vecSlope, intervals));
			bmu.x = ms[1];
			bmu.y = ms[2];
			bmu.vector = ms[3..8];
		}).add;
		
		mfccSynth = Synth.head(group, \mfcc, [\in, inputBus, \th, inputGate]);
		
		this.addBmuFunction(\generic, {|abmu, dslope, vslope, intr|
			if (distSlope > distSlopeTh)
			{
				this.readRegion;
			};
			if (vecSlope > vecSlopeTh)
			{				
				this.readRegion
			}
		});
		
		lastDefEvent = regionMap[this.bmuRegionIndex]
		
	}
	
	stopMFCC{
		
		mfccResponder.remove;
		mfccResponder = nil;
		bmuResponder.remove;
		bmuResponder = nil;
		mfccSynth.free;
		mfccSynth = nil;
		this.removeBmuFunction(\generic);
	}
	
	readRegion{
		var newDefEvent;
		newDefEvent = regionMap[this.bmuRegionIndex];
		Post << "updating events chain: " << [lastDefEvent, regionMap[this.bmuRegionIndex]] << Char.nl;
		defEvents.read(lastDefEvent, newDefEvent);
		lastDefEvent = newDefEvent;
	}

	initSOM{|somSizeX, somSizeY, somVectorSize, somTrainDur, somLearningRate|
		
		somDict = (
			sizeX: somSizeX,
			sizeY: somSizeY,
			vectorSize: somVectorSize,
			trainDur: somTrainDur,
			learningRate: somLearningRate
		);		
		bmu = (
			x: 0,
			y: 0,
			vector: (0!somVectorSize)
		);	
		
		regions = Array();
		
		forBy(somSizeX / 4 - 1, somSizeX, somSizeX / 4, {|valx, i|
			forBy(somSizeY / 4 - 1, somSizeY, somSizeY / 4, {|valy, j|
				regions = regions.add(
					(
						xa: valy - (somSizeY / 4) - 1,
						xb: valy,
						ya: valx - (somSizeX / 4) - 1, 
						yb: valx
					)
				)
			})
		});

		SynthDef(\mfcc, {|in, th|
			var input, fft, mfcc, onsets;
			input = In.ar(in);
			fft = FFT(LocalBuf(1024), input);
			onsets = Onsets.kr(fft, th);
			mfcc = MFCC.kr(fft, somDict.vectorSize - 1);
			SendReply.kr(onsets, '/mfcc', mfcc ++ [SpecFlatness.kr(fft)] );
		}).add;

		remote.sendMsg("/som/init", somDict.trainDur, somDict.sizeX, somDict.sizeY, 
			somDict.learningRate);
			
		gui.post("SOM ready");
	}
	
	startSOM{|appPath| ("open" + appPath).unixCmd }
	
	quitSOM{ 	remote.sendMsg("/som/quit", 0) }	
	
	bmuRegionIndex{
		^regions.selectIndices({|reg| 
			(bmu.x >= reg.xa).and(bmu.x <= reg.xb).and(bmu.y >= reg.ya).and(bmu.y <= reg.yb )
		}).at(0)
	}
	
	startRecorder{|threshold = 0.1, lag = 0.1, recEnabled = 1.0, time = 0.2 |
		Routine({
			if (recorderBuffers.isNil)
			{
				recorderBuffers = ({|i| Buffer.alloc(server, server.sampleRate * 6) } ! 5);
				server.sync
			};
			if (recorderArgs.isNil)
			{
				recorderArgs = (
					th: threshold,
					lag: lag,
					recEnabled: recEnabled,
					time: time
				)	
			};
			recorder = Synth.head(group, \recorder, [\in, inputBus] ++ recorderArgs.asKeyValuePairs)
				.setn(\bufnums, recorderBuffers.collect(_.bufnum));
			server.sync;
			recResponder = OSCresponderNode(server.addr, '/timer', {|ti, re, ms|
				var buf;
				if ((ms[5] < (server.sampleRate * 5)).and(ms[5] - (server.sampleRate * recorderArgs.time) > 0))
				{
					addingBuffer = true;
					if ((lastTime > 0).and(lastTime < 1).and(ms[3].round(quant) < 1))
					{
						intervals.read(lastTime, ms[3].round(quant))
					};
					lastTime = ms[3].round(quant);
					Routine({
						buf = Buffer.alloc(server, 
							ms[5] - (server.sampleRate * recorderArgs.time));
						server.sync;
						recorderBuffers[(ms[4] - 1).wrap(0, 4)].copyData(buf, 0, 0, buf.numFrames);
						server.sync;
						if (inputBuffers.isNil) { inputBuffers = Array() };
						inputBuffers = inputBuffers.add(buf);
						addingBuffer = false;
						Post << "added buffer dur:" << (buf.numFrames / buf.sampleRate) << Char.nl;
					}).play;
					
					if ((synthSeq < 4).and(seqActive))
					{
						this.startBufSynthSequence([2, 3, 5, 8].choose)
					}
				}
				
			}).add
		}).play
	}
	
	stopRecorder{
		recResponder.remove;
		recResponder = nil;
		recorder.free;
		recorder = nil;
		recorderBuffers.do(_.zero);
	}
	
	startSynth{
		var defEvent;
		if (activeLiveSynths.size + activeBufSynths.size < synthLimit )
		{
			defEvent = defEvents.next(lastDefEvent) ? lastDefEvent;
			if (this.isLiveSynth(defEvent))
			{
				this.startLiveSynth(defEvent, Env([0, 1, 1, 0], [0.1, 0.4, 0.5], \sine, 2, 1) )
			}
			{
				this.startBufSynth(defEvent, Env([0, 1, 1, 0], [0.1, 0.4, 0.5], \sine, 2, 1), 
					inputBuffers.wchoose(Array.geom(inputBuffers.size, 1.0, 1.107).normalizeSum ))
			}
		}
	}
	
	isLiveSynth{|key|
		^liveProcs.includesKey(key)
	}
	
	startLiveSynth{|key, env|
		var abmu, params, proc, synth;
		env = env ? Env([0, 1, 1, 0], [0.3, 0.4, 0.3]);
		abmu = bmu.vector ? Array.rand(somDict.vectorSize, 0.0, 1.0);
		abmu = Pseq(abmu.clip(0.0, 1.0), inf).asStream;
		proc = liveProcs[key];
		params = [\out, decoder.bus, \in, inputBus];
		params = params ++	proc.specs.collect({|spec| spec.map(abmu.next) }).asKeyValuePairs;
		synth = Synth.tail(group, key, params).setn(\env, env.asArray);
		if (proc.includesKey('setn')) {
			proc.setn.keysValuesDo({|key, val|
				synth.setn(key, 
					if (val.curve == \exp) 
						{ Array.geom(val['size'], val['start'], val['step']) }
						{ Array.series(val['size'], val['start'], val['step']) } 
				)
			});
		};
		activeLiveSynths[synth.nodeID.asSymbol] = synth;
		^synth.nodeID
	}
	
	startBufSynthSequence{|length = 1|
		var defEvent, interval, bufs;
		defEvent = lastDefEvent;
		interval = lastTime;
		bufs = Pstutter(
			Pwhite(5, 8), 
			Pseq(
				Array.fill(length, { 
					inputBuffers.wchoose((0..inputBuffers.size).normalizeSum) 
				}), 
				inf
			)
		).asStream;
		Routine({
			synthSeq = synthSeq + 1;
			length.do({|i|
				this.startBufSynth(defEvent, Env([0.001, 1.0, 1.0, 0.001], [0.1, 0.5, 0.4], \sine), 
					bufs.next, bufs.next, interval * [2, 4, 8].choose, (1..4).choose);
				interval.wait;
				interval = intervals.next(interval) ? interval;
				defEvent = defEvents.next(defEvent);
			});
			synthSeq = synthSeq - 1;
		}).play
	}
	
	startBufSynth{|key, env, buffer, buffer2, dur, releaseTime = 1|
		var abmu, params, proc, synth, bufm;
		
		Post << "starting synth: " << key << Char.nl;
		
		env = env ? Env([0, 1, 1, 0], [0.3, 0.4, 0.3]);
		abmu = bmu.vector ? Array.rand(somDict.vectorSize, 0.0, 1.0);
		abmu = Pseq(abmu.clip(0.0, 1.0), inf).asStream;
		proc = bufProcs[key];
		params = [\out, decoder.bus, \in, inputBus, \buf, buffer];
		if (proc.def.name == \bufmod) { params = params ++ [\bufm, buffer2] };
		params = params ++	proc.specs.collect({|spec| spec.map(abmu.next) }).asKeyValuePairs;
		synth = Synth.tail(group, key, params).setn(\env, env.asArray);
		if (proc.includesKey('setn')) {
			proc.setn.keysValuesDo({|key, val|
				synth.setn(key, 
					if (val.curve == \exp) 
						{ Array.geom(val['size'], val['start'], val['step']) }
						{ Array.series(val['size'], val['start'], val['step']) } 
				)
			});	
		};
		if (proc.includesKey('array')) {
			proc['array'].keysValuesDo({|key, val|
				synth.setn(key, val)
			})
		};
		
		if (proc.includesKey('envs')) {
			proc['envs'].keysValuesDo({|key, val|
				synth.setn(key, val.value(abmu.next))
			})
		};
		
		SystemClock.sched(dur, {
			synth.set(\gate, releaseTime.neg);
			nil
		})
		
	}
		
	freeSynth{|id, releaseTime = 1|
		var weights;
		if (id.isNil)
		{
			if (activeLiveSynths.size > activeBufSynths.size) {
				weights = activeLiveSynths.keys(Array).asInt - 1000;
				id = activeLiveSynths.keys(Array).wchoose(weights.reverse.normalizeSum);
			}
			{
				weights = activeBufSynths.keys(Array).asInt - 1000;
				id = activeBufSynths.keys(Array).wchoose(weights.reverse.normalizeSum);
			}	
		};
		if (activeLiveSynths.includesKey(id.asSymbol))
		{
			activeLiveSynths[id.asSymbol].set(\gate, releaseTime.neg);
			activeLiveSynths[id.asSymbol] = nil;
		};
		if (activeBufSynths.includesKey(id.asSymbol))
		{
			activeBufSynths[id.asSymbol].set(\gate, releaseTime.neg);
			activeBufSynths[id.asSymbol] = nil;		
		}
	}
		
	setBPM{|value|
		bpm = value;
		quant = (60/bpm/4).round(qround)
	}
	
	gui{
		gui = MikroGui(this);
	}
	
}

MikroGui{
	
	var mikro, window, postwin, postview, synthview, poststring, queryclock, ctrwin, ampspec, recth = 0.1; 
	var gaptime = 0.2, freesynth, release;
	
	*new{|mikro|
		^super.newCopyArgs(mikro).init	
	}
	
	init{
		var font;
		
		font = Font("Lucida Grande", 9);
		ampspec = ControlSpec(0.001, 1.0, \exp);
		
		window = Window("----<><><><><>----", Rect(200, 200, 600, 510)).alpha_(0.98).front;
		window.background_(Color.grey(0.3));
		
		ctrwin = CompositeView(window, Rect(5, 5, 390, 490));
		
		RoundButton(ctrwin, Rect(5, 5, 60, 25))
			.font_(font)
			.states_([["som off", Color.yellow, Color.black], ["som on", Color.black, Color.yellow]])
			.action_({|btn|
				if (btn.value == 1)
				{
					mikro.startSOM("/Users/alo/Development/som/visual/build/Debug/som.app")
				}
				{
					mikro.quitSOM
				}
			});
			
		RoundButton(ctrwin, Rect(5, 35, 60, 25))
			.font_(font)
			.states_([["init som", Color.yellow, Color.black], ["som on", Color.black, Color.yellow]])
			.action_({|btn|
				mikro.initSOM(40, 40, 6, 1000, 0.2)
			});
			
		RoundButton(ctrwin, Rect(5, 65, 60, 25))
			.font_(font)
			.states_([["init input", Color.yellow, Color.black], ["free input", Color.black, Color.yellow]])
			.action_({|btn|
				if (btn.value == 1)
				{
					mikro.initPerformance(1.0, 0.0)
				}
				{
					mikro.freeInput
				}
			});

		RoundButton(ctrwin, Rect(5, 95, 60, 25))
			.font_(font)
			.states_([["rec off", Color.yellow, Color.black], ["rec on", Color.black, Color.yellow]])
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
			.action_({|btn|
				mikro.startBufSynth(\bufplay, Env([0.001, 1.0, 1.0, 0.001], [0.1, 0.5, 0.4], \sine), 
					mikro.inputBuffers.last, mikro.inputBuffers.last, rrand(1, 4), 1);
			});			

		
		SmoothSlider(ctrwin, Rect(75, 5, 30, 200))
			.stringColor_(Color.green)
			.string_("0(db)")
			.value_(1)
			.action_({|sl|
				mikro.decoder.set(\amp, ampspec.map(sl.value));
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
				mikro.imamp = ampspec.map(sl.value);
				mikro.input.set(\mamp, ampspec.map(sl.value));
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
				mikro.ixamp = ampspec.map(sl.value); 
				mikro.input.set(\xamp, ampspec.map(sl.value));
				sl.string_(ampspec.map(sl.value).ampdb.round(1).asString ++ "(db)")
			});

		StaticText(ctrwin, Rect(145, 205, 30, 20))
			.align_(\center)
			.stringColor_(Color.grey(0.8))
			.font_(font)
			.string_("aux");

		SmoothSlider(ctrwin, Rect(180, 5, 30, 200))
			.stringColor_(Color.green)
			.string_("-20(db)")
			.value_(ampspec.unmap(0.1))
			.action_({|sl|
				recth = ampspec.map(sl.value);
				mikro.recorder.set(\th, recth);
				sl.string_(recth.ampdb.round(1).asString ++ "(db)")
			});

		StaticText(ctrwin, Rect(180, 205, 30, 20))
			.align_(\center)
			.stringColor_(Color.grey(0.8))
			.font_(font)
			.string_("rec th");
			
		SmoothSlider(ctrwin, Rect(215, 5, 30, 200))
			.stringColor_(Color.green)
			.string_("0.2(s)")
			.value_(0.2)
			.action_({|sl|
				gaptime = sl.value;
				mikro.recorder.set(\time, sl.value);
				sl.string_(sl.value.round(0.01).asString ++ "(s)")
			});

		StaticText(ctrwin, Rect(215, 205, 30, 20))
			.align_(\center)
			.stringColor_(Color.grey(0.8))
			.font_(font)
			.string_("gap");

		SmoothSlider(ctrwin, Rect(250, 5, 30, 200))
			.stringColor_(Color.green)
			.string_("0.01")
			.value_(0.01)
			.action_({|sl|
				mikro.inputGate = sl.value.round(0.01);
				mikro.mfccSynth.set(\th, sl.value.round(0.01));
				sl.string_(sl.value.round(0.01).asString)
			});

		StaticText(ctrwin, Rect(250, 205, 30, 20))
			.align_(\center)
			.stringColor_(Color.grey(0.8))
			.font_(font)
			.string_("mfcc");
			
		mikro.liveProcs.keys(Array).do({|def, i|
			RoundButton(ctrwin, Rect(i*60+5, 225, 60, 25))
				.font_(font)
				.states_([[def.asString, Color.yellow, Color.grey(0.2)]])
				.action_({
					var id;
					id = mikro.startLiveSynth(def, Env([0, 1, 1, 0], [0.1, 0.4, 0.5], \sine, 2, 1));
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
				mikro.freeSynth(id, release.value);
				arr = freesynth.items;
				arr.remove(str);
				freesynth.items = arr;
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
							
		window.drawHook = {
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
		queryclock = SystemClock.sched(2, { this.queryServer(mikro.server); 1 });
	}
	
	stopServerQuery{
		queryclock.clear	
	}
	
	
}