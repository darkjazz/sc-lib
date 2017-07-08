SpEnvir{

	var <settings, <doneAction, serverReplyTime = 0;

	*new{|settings, doneAction|
		^super.newCopyArgs(settings, doneAction).init
	}

	init{

		var action, addr;

		this.configure;

		{

			CouchDB.startServer;

			1.wait;

			currentEnvironment[\decoder] = FoaDecoder(decoderType: currentEnvironment[\decoderType]);
			addr = NetAddr(settings['ip'], 7000);
			currentEnvironment[\graphics] = CinderApp(
				currentEnvironment[\screenX],
				currentEnvironment[\screenY],
				ciAddr: addr,
				mode: currentEnvironment[\mode]
			);

			1.wait;

			action = {|matrix|
				Tdef('loadAction', {
					matrix.loadPatternDefs(currentEnvironment[\patdefs]);
					Post << "pattern definitions loaded.." << Char.nl;
					1.wait;
					currentEnvironment[\loader] = JsonLDLoader(currentEnvironment[\dbname]);
					currentEnvironment[\defnames] = currentEnvironment[\loader]
					.getIDsByDateRange("160801", "999999")
					.collect({|def| def['value'].first });
					Post << "ges synth def names loaded.." << Char.nl;
					1.wait;
					currentEnvironment[\matrix].setBPM(currentEnvironment[\bpm]);
					currentEnvironment[\player].setBPM(currentEnvironment[\bpm]);
					matrix.prepareAudio;
					Pdef('matrix', Ppar(currentEnvironment['initPdefs'].collect({|name|
						Pdef(name)
					}))).quant(64);
					Pdef('matrix').play;
					currentEnvironment[\defs] = matrix.patterndefs;
					Post << "audio activated, pdefs initialized.." << Char.nl;
					//this.startServerMonitor;
					{
						MasterEQ(currentEnvironment[\channels]);
						Server.default.scope(matrix.decoder.numChannels);
					}.defer;
					doneAction.(this)
				}).play
			};

			Post << "making sparsematrix.." << Char.nl;

			currentEnvironment[\matrix] = SparseMatrix(
				currentEnvironment[\decoder],
				currentEnvironment[\graphics],
				currentEnvironment[\quant],
				currentEnvironment[\ncoef],
				action
			);

			currentEnvironment[\player] = JGepPlayer(
				currentEnvironment[\decoder], dbname: currentEnvironment[\dbname]
			);

			currentEnvironment[\player].getDefNamesByHeader(
				currentEnvironment[\headsize], currentEnvironment[\numgenes]
			);

			currentEnvironment[\sender] = CodeSender(currentEnvironment[\graphics]);

			SparseMatrixPattern.useTwinPrimes = true;

		}.fork

	}

	initGraphics{|isLocal=true, doc|
		Tdef('initGraphics', {
			if (isLocal) {
				currentEnvironment[\graphics].open;
				3.wait;
			};
			currentEnvironment[\graphics].showCodePanel;
			0.5.wait;
			if (doc.notNil) {
				currentEnvironment[\graphics].assignCodeWindow(doc)
			};
			0.5.wait;
			currentEnvironment[\graphics].initWorld(*((currentEnvironment[\worldDim]!3)++currentEnvironment[\ncoef]));
			0.5.wait;
			currentEnvironment[\graphics].initGenerations([4,6,8], [3,5,7,9], 16);
			0.5.wait;
			currentEnvironment[\graphics].setSymmetry(11);
			0.5.wait;
			currentEnvironment[\graphics].setViewpoint(0.0, 0.0, -120.0, 0.0, 0.0, 0.0);
			Post << "Graphics initialized..";
		}).play

	}

	automateGraphics{
		var maxPatterns, ruleWeight, changeWeight, patches, graphPatterns, activePatterns, graphics, rot;
		maxPatterns = 4;
		ruleWeight = 0.2;
		changeWeight = 0.4;

		graphics = currentEnvironment[\graphics];

		patches = [
			[1.0, 0, 0, 0.0, 0.7, 1.0], //0
			[1.0, 1, 1, 0.9, 0.1, 0.4],
			[1.0, 1, 1, 1.0, 0.8, 0.0],
			[1.0, 1, 1, 1.0, 1.0, 1.0],
			[1.0, 0, 0, 1.0, 1.0, 1.0],
			[1.0, 1, 1, 0.5, 1.0, 1.0], //5
			[1.0, 1, 1, 0.5, 0.9, 0.8],
			[1.0, 1, 1, 1.0, 0.3, 0.5],
			[1.0, 0, 0, 0.6, 0.7, 0.9],
			[1.0, 1, 1, 0.5, 0.8, 0.9],
			[1.0, 1, 1, 0.2, 0.8, 0.8], //10
			[1.0, 0, 0, 0.0, 0.5, 1.0],
			[1.0, 0, 0, 0.6, 0.8, 1.0],
			[1.0, 0, 0, 0.2, 0.8, 1.0],
			[1.0, 0, 0, 1.0, 1.0, 1.0],
			[1.0, 0, 0, 1.0, 1.0, 1.0], //15
			[1.0, 1, 1, 0.1, 1.0, 0.3],
			[1.0, 1, 1, 0.7, 1.0, 0.0],
			[1.0, 1, 1, 1.0, 1.0, 1.0],
			[1.0, 0, 0, 1.0, 1.0, 1.0],
			[1.0, 1, 1, 1.0, 1.0, 0.2], //20
			[1.0, 1, 1, 1.0, 0.0, 1.0],
			[1.0, 1, 1, 0.4, 0.8, 1.0],
			[1.0, 1, 1, 0.1, 0.2, 0.3],
			[1.0, 1, 1, 0.5, 0.6, 0.7],
			[1.0, 1, 1, 0.9, 0.8, 0.7], //25
			[1.0, 1, 1, 0.6, 0.7, 0.8],
			[1.0, 1, 1, 0.2, 0.7, 0.6],
			[1.0, 0, 0, 1.0, 1.0, 1.0],
			[1.0, 0, 0, 0.6, 0.9, 1.0],
			[1.0, 0, 0, 1.0, 1.0, 1.0], //30
			[1.0, 0, 0, 1.0, 1.0, 0.0],
			[1.0, 0, 0, 0.0, 0.2, 0.0],
			[1.0, 1, 1, 0.0, 0.4, 0.4],
			[1.0, 0, 0, 1.0, 0.0, 0.5],
			[1.0, 0, 0, 1.0, 0.2, 0.8], //35
			[1.0, 0, 0, 1.0, 1.0, 1.0],
			[1.0, 0, 0, 0.0, 1.0, 1.0],
			[1.0, 0, 0, 1.0, 1.0, 1.0],
			[1.0, 0, 0, 1.0, 1.0, 1.0],
		];

		patches.do({|pa, i|
			graphics.setPattern(*([i, 0] ++ pa))
		});

		graphPatterns = (0..39);

		activePatterns = [1];
		graphics.setPattern(1, 1.0, 1, 1, 0.9, 0.1, 0.4);


		Tdef('ciAuto', {|mfcc|
			10.wait;
			activePatterns = [1, 24];
			graphics.setPattern(24, 1.0, 1, 1, 0.5, 0.6, 0.7);

			60.wait;

			loop({
				var curr, args;

				if (changeWeight.coin)
				{

					if (activePatterns.size > maxPatterns)
					{
						activePatterns.size.rand.do({
							curr = activePatterns.choose;
							activePatterns.remove(curr);
							args = [curr, 0] ++ patches[curr];
							graphics.setPattern(*args);
						})
					}
					{
						curr = graphPatterns.choose;
						activePatterns = activePatterns.add(curr);
						args = [curr, 1] ++ patches.choose;
						graphics.setPattern(*args);
					}
				};
				if (ruleWeight.coin) {
					graphics.sendPredefined3DRule(
						[
							\flamingstarbow, \chenille, \belzhab, \glissergy,
							\faders, \frozenspirals, \glisserati, \sedimental,
							\nova, \orthogo, \rainzha, \rake,
							\snake, \transers, \worms, \xtasy
						].choose;
					);
				};
				if (0.3.coin) {
					graphics.stopRotation;
					graphics.setViewpoint(
						rrand(30.0, 60.0)*[1.0,-1.0].choose,
						rrand(30.0, 60.0)*[1.0,-1.0].choose,
						rrand(30.0, 60.0)*[1.0,-1.0].choose,
						0.0, 0.0, 0.0
					);
					graphics.unmapCodePanel;
				}{
					if (0.5.coin) {
						rot = Rotation(rrand(-140.0, 140.0), rrand(0.005, 0.05), rrand(-40.0, 40.0),
							rrand(-140.0, 140.0),
							rrand(-pi, pi), rrand(0.01, 0.08), rrand(-pi, pi), rrand(0.01, 0.07));
					}
					{
						rot = Rotation(rrand(100.0, 140.0).neg, 0.0, rrand(100.0, 140.0).neg, 0.0, 0.0, -0.02, 0.0, 0.0);
					};
					graphics.setCameraRotation(rot, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
					graphics.rotateCamera;
					graphics.mapCodePanel;
				};
				rrand(0.2, 0.6).round(0.1).wait;
			})
		}).play

	}

	clearAutoGraphics{ Tdef('ciAuto').clear;  }

	configure{
		if (settings.isNil) { this.setDefaultSettings };
		settings.keysValuesDo({|key, val|
			currentEnvironment[key] = val
		})
	}

	startServerMonitor{

		Tdef('check-server', {
			Post << "starting server monitor.." << Char.nl;
			loop({
				if (Server.default.serverRunning.not.and(Server.default.serverBooting.not))
				{
					Post << "server appears to have unexpectedly quit, attempting to recover..." << Char.nl;
					this.recoverServer;
				};
				1.wait;
			})
		}).play

	}

	recoverServer{
		Post << "quitting server monitor for boot.." << Char.nl;
		Tdef('check-server').clear;
		Server.killAll;
		SystemClock.sched(2, {
			Server.default.waitForBoot({
				{
					currentEnvironment[\matrix].quit(false);
					currentEnvironment[\matrix].loadBuffers;
					Server.default.sync;
					currentEnvironment[\decoder].resetRunningFlag;
					currentEnvironment[\decoder].init(settings['decoderType'], false);
					Server.default.sync;
					currentEnvironment[\matrix].loadDefFuncs;
					currentEnvironment[\matrix].defpath.load;
					currentEnvironment[\matrix].skismDefs.do(_.add);
					currentEnvironment[\matrix].group = Group();
					Server.default.sync;
					currentEnvironment[\matrix].efxgroup = Group.after(currentEnvironment[\matrix].group);
					Server.default.sync;
					currentEnvironment[\matrix].loadPatternDefs(currentEnvironment[\patdefs]);
					currentEnvironment[\matrix].prepareAudio;
					SystemClock.sched(3, { this.startServerMonitor; nil });
				}.fork
			});
			nil
		});
	}

	loadChordAnalysis{
		currentEnvironment[\chordloader] = ChordLoader(Paths.dataDir +/+ "pixies/pixieschords.csv");

		currentEnvironment[\chordloader].load;

		currentEnvironment[\durset] = MarkovSet();
		currentEnvironment[\deltaset] = MarkovSet();
		currentEnvironment[\chordset] = MarkovSet();

		currentEnvironment[\uchords] = ();

		currentEnvironment[\chordloader].chords.do({|chord|
			currentEnvironment[\uchords][chord.getKey] = chord.getFreqs
		});

		currentEnvironment[\deltas] = [];

		currentEnvironment[\chordloader].chords.doAdjacentPairs({|chA, chB|
			currentEnvironment[\chordset].read(chA.getKey, chB.getKey);
			currentEnvironment[\durset].read(chA.getDur.round(1/16), chB.getDur.round(1/16));
			currentEnvironment[\deltas] = currentEnvironment[\deltas].add(
				(chB.getStartTime - chA.getStartTime).round(1/16)
			);
		});

		currentEnvironment[\deltas].doAdjacentPairs({|dA, dB|
			currentEnvironment[\deltaset].read(dA, dB)
		});

		currentEnvironment[\chordLog] = ();

		Post << "Chord analysis loaded.." << Char.nl;
	}

	makeChordPattern{|name, defname, length, beats=1|
		var key, chords, dur, delta, pbind;
		key = currentEnvironment[\uchords].keys(Array).choose;
		chords = length.collect({
			key = currentEnvironment[\chordset].next(key)
		});
		currentEnvironment[\chordfreqs] = chords.collect({|key|
			currentEnvironment[\uchords][key].collect(_.asInt).collect(_.prevPrime)
		});
		dur = currentEnvironment[\chordloader].chords.choose.getDur.round(1/16);
		currentEnvironment[\chorddurs] = length.collect({
			dur = currentEnvironment[\durset].next(dur)
		});
		delta = currentEnvironment[\deltas].choose;
		currentEnvironment[\deltas] = length.collect({
			delta = currentEnvironment[\deltaset].next(delta)
		});
		pbind = Pbind(
			\instrument, defname,
			\out, currentEnvironment[\decoder].bus,
			\target, currentEnvironment[\matrix].efxgroup, \addAction, \addBefore,
			\efx, Pdefn((name.asString ++ "efx").asSymbol, currentEnvironment[\matrix].nofxbus),
			\frqs, Pseq(currentEnvironment[\chordfreqs].collect(_.bubble), inf),
			\amps, Array.geom(5, 1.0, 13/17).bubble,
			\dur, Pseq(currentEnvironment[\chorddurs], inf).round(
				currentEnvironment[\matrix].beatdur / 4
			),
			\delta, Pseq(currentEnvironment[\deltas].normalizeSum *
				currentEnvironment[\matrix].beatdur * beats, inf),
			\amp, Pdefn((name.asString ++ "amp").asSymbol, 0),
			\emp, Pdefn((name.asString ++ "emp").asSymbol, 0),
			\env, Pfunc({ currentEnvironment[\matrix].envs[#[perc00,perc01].choose] })
		);
		if (currentEnvironment[\chordLog].includesKey(name.asSymbol).not)
		{
			currentEnvironment[\chordLog][name] = Array()
		};
		currentEnvironment[\chordLog][name] = currentEnvironment[\chordLog][name].add(pbind);
		Pdef(name, pbind)
	}

	setDefaultSettings{
		settings = ( ncoef: 20, rate: 20, headsize: 14, numgenes: 4, quant: 2, screenX: 1024, screenY: 768, mode: 1, decoderType: 'stereo', bpm: 140, channels: 2, foa: #[zoom,push], dbname: "ges_02", patdefs: "patternDefsAppend.scd", initPdefs: ['r00', 'r01', 'r02', 'b02'], worldDim: 17);
	}

}