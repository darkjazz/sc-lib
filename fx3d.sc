Fx3D{
	classvar <visuals, <actions;

	var <opts;
	var <vaddr, glResponder, glFuncs, stResponder, stFuncs, trResponder, trFuncs, rnResponder, rnFuncs;
	var phResponder, phFuncs, <>zoneResetFunc;
	var <invalues, <visualdict;
	var <glOrder, <patchOrder, <oglGui, <weightPresets, <perfGui;
	
	*initClass{
	
		visuals = (
			elastic: 8,
			kanji: 0,
			ringz: 1,
			wobble: 2,
			grid: 3,
			horizons: 4,
			blinds: 5,
			axial: 6, 
			radial: 7,
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
			param: 10
		);
		
	}
	
	*new{
		^super.new.init;
	}
	
	init{
		opts = FxOptions();
		opts.fxapp = "/Users/alo/Development/Fx3D/Visual/build/Release/fx.app";
		opts.debugapp = "/Users/alo/Development/Fx3D/Visual/build/Debug/fx.app";
		vaddr = NetAddr(opts.fxip, opts.fxport);
		invalues = Event();
		visualdict = Event();
		visualdict.globals = (
			alpha: 1.0,
			clear: 0.0,
			add: 0.03,
			transx: 0.0,
			transy: 0.0,
			transz: -12.0,
			angle: 0.0,
			rotX: 0.0,
			rotY: 0.0,
			rotZ: 0.0,
			frame: 4,
			phase: 72
		);	
		
		visualdict.glSpecs = (
			add: ControlSpec(0.001, 0.999, \cos),
			transx: ControlSpec(-16.0, 16.0, \lin),
			transy: ControlSpec(-6.0, 6.0, \lin), 
			transz: ControlSpec(-64, 4, \lin, 1),
			angle: ControlSpec(-1.0, 1.0, \lin),
			frame: ControlSpec(1, 12, \lin, 1),
			phase: ControlSpec(24, 120, \lin, 24)
		);
		
		glOrder = [\alpha, \clear, \add, \transx, \transy, \transz, \angle, \rotX, \rotY, \rotZ, 
			\frame, \phase];
		patchOrder = [\elastic, \kanji, \ringz, \wobble, \grid, \horizons, \blinds, \axial, \radial, 
			 \mesh];
		
		visuals.keysDo({|key|
			var temp;
			temp = Event();
			actions.keysDo({|akey| temp.put(akey, 0.0) });
			visualdict.put(key, temp)
		});
	
		visualdict.world = (
			reset: 0,
			seed: 0,
			habitat: 0,
			radius: 1,
			left: 6,
			bottom: 6,
			front: 6,
			width: 4,
			height: 4,
			depth: 4,
			weights: (1.0 ! 26)
		);
		
		weightPresets = (
			equal: (1 ! 26),
			neumann: [1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 3, 1, 3, 3, 1, 3, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1],
			cross: [1, 3, 1, 1, 3, 1, 1, 3, 1, 3, 3, 3, 3, 3, 3, 3, 3, 1, 3, 1, 1, 3, 1, 1, 3, 1],
			symmetry: { Array.rand(13, 1, 5).mirror2 },
			rand: { Array.rand(26, 1, 5) }
		);
		
		glFuncs = (); stFuncs = (); trFuncs = (); rnFuncs = (); phFuncs = ();
	}
	
	addGlobalsResponder{
		
		if (glResponder.notNil) { glResponder.remove; glResponder = nil };
		
		glResponder = OSCresponderNode(nil, '/fx/globals', {|ti, re, ms|
				
			invalues.avgstate = ms[1];
			invalues.stddev = ms[2];
			  
			invalues.avglbf = ms[3];
			invalues.avgrbf = ms[4];
			invalues.avgrbb = ms[5]; 
			invalues.avglbb = ms[6];
			invalues.avgltf = ms[7];
			invalues.avgrtf = ms[8];
			invalues.avgrtb = ms[9];
			invalues.avgltb = ms[10];
			
			glFuncs.do({|func| func.value(invalues) });
		
		}).add;

	}
	
	addGlobalsFunction{|key, func| glFuncs[key] = func }
	
	removeGlobalsFunction{|key| glFuncs[key] = nil }
	
	removeAllGlobalsFunctions{ glFuncs.clear }
	
	removeGlobalsResponder{
		glResponder.remove;
		glResponder = nil;
	}
	
	addStatesResponder{

		if (stResponder.notNil) { stResponder.remove; stResponder = nil };
		
		stResponder = OSCresponderNode(nil, '/fx/states', {|ti, re, ms|
			if (ms[1].notNil)
			{
				invalues.states = ms[1].asFloatArray;
				stFuncs.do({|func| func.value(invalues) });
			}
		}).add;
	}
	
	removeStatesResponder{
		stResponder.remove;
		stResponder = nil;	
	}

	addStatesFunction{|key, func| stFuncs[key] = func }
	
	removeStatesFunction{|key| stFuncs[key] = nil }
	
	removeAllStatesFunctions{ stFuncs.clear }

	addTriggerResponder{

		if (trResponder.notNil) { trResponder.remove; trResponder = nil };
		
		trResponder = OSCresponderNode(nil, '/fx/trigger', {|ti, re, ms|
			invalues.coordX = ms[1]; 
			invalues.coordY = ms[2];
			invalues.coordZ = ms[3];
			invalues.maxstate = ms[4];
			trFuncs.do({|func| func.value(invalues) });
		}).add;
	}
	
	removeTriggerResponder{
		stResponder.remove;
		stResponder = nil;		
	}

	addTriggerFunction{|key, func| trFuncs[key] = func }
	
	removeTriggerFunction{|key| trFuncs[key] = nil }
	
	removeAllTriggerFunctions{ trFuncs.clear }	

	addRenewResponder{

		if (rnResponder.notNil) { rnResponder.remove; rnResponder = nil };
		
		rnResponder = OSCresponderNode(nil, '/fx/renew', {|ti, re, ms|
			rnFuncs.do({|func| func.value });
		}).add;
	}
	
	removeRenewResponder{
		rnResponder.remove;
		rnResponder = nil;		
	}

	addRenewFunction{|key, func| rnFuncs[key] = func }
	
	removeRenewFunction{|key| rnFuncs[key] = nil }
	
	removeAllRenewFunctions{ rnFuncs.clear }

	addPhaseResponder{

		if (phResponder.notNil) { phResponder.remove; phResponder = nil };
		
		rnResponder = OSCresponderNode(nil, '/fx/phase', {|ti, re, ms|
			phFuncs.do({|func| func.value(ms[1]) });
		}).add;
	}
	
	removePhaseResponder{
		phResponder.remove;
		phResponder = nil;		
	}

	addPhaseFunction{|key, func| phFuncs[key] = func }
	
	removePhaseFunction{|key| phFuncs[key] = nil }
	
	removeAllPhaseFunctions{ phFuncs.clear }	
	
	sendMsg{|cmd ... msg|
		vaddr.sendMsg(opts.sendcmd ++ "/" ++ cmd, *msg)
	}
	
	sendPatchCmd{|patch, command, startValue, targetValue, time, doneAction|
		if (time > 0)
		{
			Routine({
				var times, step, value;
				times = time * opts.msgRate;
				step = targetValue - startValue / times;
				value = startValue;
				times.do({
					this.sendMsg("patch", visuals[patch], actions[command], value);
					value = value + step;
					opts.msgRate.reciprocal.wait
				});
				opts.msgRate.reciprocal.wait;
				visualdict[patch][command] = targetValue;
				doneAction.value(this)
			}).play
		}
		{
			this.sendMsg("patch", visuals[patch], actions[command], targetValue);
			visualdict[patch][command] = targetValue;
			doneAction.value(this);
		}
	}
	
	activatePatch{|patch|
		this.sendPatchCmd(patch, \active, 0.0, 1.0, 0);
	}

	deactivatePatch{|patch|
		this.sendPatchCmd(patch, \active, 1.0, 0.0, 0);
	}
	
	setGlobal{|key, value|
		visualdict.globals[key] = value;
	}
	
	setGlobals{|alpha, clear, add, transx, transy, transz, angle, rotX, rotY, rotZ, frame, phase|
		visualdict.globals[\alpha] = alpha;
		visualdict.globals[\clear] = clear;
		visualdict.globals[\add] = add;
		visualdict.globals[\transx] = transx;
		visualdict.globals[\transy] = transy;
		visualdict.globals[\transz] = transz;
		visualdict.globals[\angle] = angle;
		visualdict.globals[\rotX] = rotX;
		visualdict.globals[\rotY] = rotY;
		visualdict.globals[\rotZ] = rotZ;
		visualdict.globals[\frame] = frame;
		visualdict.globals[\phase] = phase;
	}
	
	setWorld{|seed, habitat, radius, left, bottom, front, width, height, depth|
		visualdict.world.seed = seed;
		visualdict.world.habitat = habitat;
		visualdict.world.radius = radius;
		visualdict.world.left = left;
		visualdict.world.bottom = bottom;
		visualdict.world.front = front;
		visualdict.world.width = width;
		visualdict.world.height = height;
		visualdict.world.depth = depth;		
	}
	
	setWeights{|weights|
		visualdict.world.weights = weights;			
	}
		
	sendSettings{|time, alpha, clear, add, transx, transy, transz, 
									angle, rotX, rotY, rotZ, frame, phase, doneAction|
		
		if (time > 0)
		{
			Routine({
				var times, steps, values;
				times = time * opts.msgRate;
				values = Array.fill(visualdict.globals.size, {|i| visualdict.globals[glOrder@i] });
				steps = [alpha, clear, add, transx, transy, transz, angle, rotX, rotY, rotZ, frame, phase] -
					values / times;
				times.do({
					this.sendMsg("settings", *values.flat);
					values = values + steps;
					opts.msgRate.reciprocal.wait
				});
				opts.msgRate.reciprocal.wait;
				this.setGlobals(alpha, clear, add, transx, transy, transz, angle, rotX, rotY, rotZ, 
					frame, phase);
				doneAction.value(this);
			}).play
		}
		{
			this.sendMsg("settings", *[alpha, clear, add, transx, transy, transz, 
				angle, rotX, rotY, rotZ, frame, phase]);
			this.setGlobals(alpha, clear, add, transx, transy, transz, angle, rotX, rotY, rotZ, frame, phase);
			doneAction.value(this);
		};
	}
	
	sendCurrentSettings{
		this.sendMsg("settings", 
			visualdict.globals[\alpha],
			visualdict.globals[\clear],
			visualdict.globals[\add],
			visualdict.globals[\transx],
			visualdict.globals[\transy],
			visualdict.globals[\transz],
			visualdict.globals[\angle],
			visualdict.globals[\rotX],
			visualdict.globals[\rotY],
			visualdict.globals[\rotZ],			
			visualdict.globals[\frame],
			visualdict.globals[\phase]			
		)
	}
	
	sendReset{|seed, habitat, radius, left, bottom, front, width, height, depth ... weights|
		this.sendMsg("world", 1, seed, habitat, radius, left, bottom, front, width, height, depth, 
			*weights
		);
		this.setWorld(seed, habitat, radius, left, bottom, front, width, height, depth);
		this.setWeights(weights)
	}
	
	sendWeights{|... weights|
		weights = weights.flat;
		this.sendMsg("world", 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, *weights);
		this.setWeights(weights);
	}
	
	sendCell{|posX, posY, posZ, state|
		this.sendMsg("cell", posX, posY, posZ, state);
	}
	
	sendPollIndices{|... indices|
		indices = indices.flat;
		this.sendMsg("poll", indices.size, *indices)
	}
	
	getPollIndices{|switch|
		var arr = Array();
		var arrA, arrB;

		switch.switch(
			0, {
				arrA = Array();
				arrB = Array();
				
				forBy(1, 15, 4, {|j|
					forBy(1, 15, 4, {|k|
						arr = arr.add([0, j, k])
					})
				});
				
				forBy(1, 15, 4, {|i|
					forBy(1, 15, 4, {|j|
						arrA = arrA.add([i, j, 0])
					})
				});
				
				forBy(1, 15, 4, {|i|
					forBy(1, 15, 4, {|j|
						arrB = arrB.add([i, j, 15])
					})
				});
				
				arr = arr.addAll([arrA, arrB].lace(arrA.size + arrB.size) );
				
				forBy(1, 15, 4, {|j|
					forBy(1, 15, 4, {|k|
						arr = arr.add([15, j, k])
					})
				});				
			},
			1, {
				forBy(2, 15, 4, {|i|
					forBy(2, 15, 4, {|j|
						forBy(2, 15, 4, {|k|
							arr = arr.add([i, j, k])
						})
					})
				});	
			},
			2, {
				16.do({|i|
					16.do({|j|
						16.do({|k|
							if (
								((i == 3).and(j > 3).and(j < 12).and(k == 4)).or
								((j == 3).and(i > 3).and(i < 12).and(k == 4)).or
								((i == 12).and(j > 3).and(j < 12).and(k == 4)).or
								((j == 12).and(i > 3).and(i < 12).and(k == 4)).or
				
								((i == 3).and(j > 3).and(j < 12).and(k == 11)).or
								((j == 3).and(i > 3).and(i < 12).and(k == 11)).or
								((i == 12).and(j > 3).and(j < 12).and(k == 11)).or
								((j == 12).and(i > 3).and(i < 12).and(k == 11))
							)
							{arr=arr.add([i,j,k])}
						})
					})
				})
				
			},
			3, {
				forBy(6, 9, 1, {|i| 
					forBy(6, 9, 1, {|j|
						forBy(6, 9, 1, {|k| 
							arr=arr.add([i,j,k])
						})
					})
				})				
			},
			
			4, { arr = Array.fill(16, {|x| Array.fill(16, {|y| [x, y, 9] }) }) },
			
			5, { 
				16.do({|x|
					16.do({|y|
						16.do({|z|
							if ((x+3%4==0).and(y+3%4==0)) {
								arr=arr.add([x, y, z])
							}			
						})
					})
				})				
			}

		);
		
		^arr.flat
		
	}
	
	quitOpenGL{
		this.sendMsg("quit", 0)
	}
	
	makeOglGui{
		oglGui = FxOpenGL(this)
	}
	
	initLive{|numZones, resetFunc|
		zoneResetFunc = resetFunc;
		perfGui = FxPerformanceGUI(this, numZones)
	}
	
	startFx{|debug=false|
		if (debug) 
		{
			("open" + opts.debugapp).unixCmd;
		}
		{
			("open" + opts.fxapp).unixCmd;
		}
	}
	
	wait{|numCycles=1|
		(numCycles/24).wait	
	}
		
}

FxPerformanceGUI{
	
	var fx, window, <panels, <zPanel, postwin, text = "", postview, synthview, mapFuncs, poststring, timer, tctr;
	var bPanel, patfuncs;
	
	var patpath = "/Users/alo/Development/Fx3D/Audio/Live/patterns/*";
	
	*new{|fx, numZones|
		^super.newCopyArgs(fx).init(numZones)
	}
	
	init{|numZones|
		var font, npanels = 3;
		
		npanels = numZones ? npanels;
		
		patfuncs = Array.newClear(npanels);
	
		window = Window("_.f(x)L._", Rect(50, 50, 800, 510)).alpha_(0.95).front;
		window.background = Color.black;
		window.view.background = HiliteGradient(Color.black, Color.grey(0.4), \v, 256, 0.5);

		font = Font("Lucida Grande", 10);
		
		panels = Array.newClear(npanels);
		
		mapFuncs = Array.newClear(npanels);
		
		zPanel = CompositeView(window, Rect(5, 5, 260, 55));
		
		zPanel.decorator = FlowLayout(zPanel.bounds, 5@5, 5@5);
		
		tctr = RoundButton(window, Rect(385, 10, 30, 20))
			.font_(font)
			.states_([["+", Color.grey(0.3), Color.black], ["-", Color.white, Color.black]])
			.action_({|btn|
				if (btn.value == 1) { timer.start } { timer.stop }
			});
		
		timer = TimeDisplay(window, Rect(420, 5, 180, 50), font: Font("Helvetica", 30));
		timer.view.background = Color.clear;
		timer.view.stringColor = Color.grey(0.6);
		
		npanels.do({|i|
			Button(zPanel, Rect(width: 590 / 8 - 5, height: 20))
				.font_(font)
				.states_([["zone " ++ i.asString, Color.grey(0.3), Color.new255(4, 50, 99)], 
					["zone " ++ i.asString, Color.new255(4, 50, 99), Color.new255(184, 134, 11)]])
				.action_({|btn|
					if (btn.value == 1) { 
						this.selectPanel(i, fx.zoneResetFunc);
						zPanel.children.do({|other|
							if (other != btn) {other.value = 0}
						});
						if (timer.isRunning.not)
						{
							tctr.value = 1;
							tctr.doAction;	
						}
					}
				});
			panels[i] = CompositeView(window, Rect(5, 65, 590, 430));
			StaticText(panels[i], Rect(0, 0, 490, 20))
				.font_(font)
				.stringColor_(Color.new255(28, 134, 238))
				.align_(\center)
				.string_("zone " ++ i.asString);
			panels[i].visible = false;
			
		});
		
		bPanel = CompositeView(window, Rect(240, 5, 110, 55));
		
		bPanel.decorator = FlowLayout(bPanel.bounds, 5@5, 5@5);
		
		npanels.do({|i|
			Button(bPanel, Rect(width: 30, height: 20))
				.font_(font)
				.states_([[i.asString, Color.green, Color.grey(0.2)], [i.asString, Color.grey(0.2), Color.green ]])
				.action_({|btn|
					if (btn.value == 1) {
						patfuncs[i] = patpath.pathMatch[i].load;
					}
					{
						patfuncs[i].value;
						patfuncs[i] = nil;
					}
				})
		});
		
		postwin = CompositeView(window, Rect(600, 5, 195, 490));
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
			Pen.strokeRect(Rect(5, 5, 590, 55));
			Pen.strokeRect(Rect(5, 65, 590, 430));
			Pen.strokeRect(Rect(600, 5, 195, 490));
		};
	}
	
	assignPanel{|index, assignFunc|
		mapFuncs[index] = assignFunc.value(panels[index], this);
	}
	
	selectPanel{|index, resetFunc|
		panels.do({|pnl, i| if (i != index) { pnl.visible = false } });
		panels[index].visible = true;
		resetFunc.value;
		mapFuncs[index].value;		
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
		
}

FxOpenGL{
	
	var fx, window, <glPanel, <ptPanel, <wrPanel;
	
	*new{|fx|
		^super.newCopyArgs(fx).makeWindow
	}
	
	makeWindow{

		var slider, label, button, gap = 5, font, weightSliders, settingValues, wspec, seedValues;
		var seedspec, poll, seed = 0, settingSliders, settingTexts;
		
		font = Font("Lucida Grande", 9);
	
		window = Window(":--: OGL :--:", Rect(100, 400, 870, 400), false).alpha_(0.95).front;
		window.background = Color.black;
		window.view.background = HiliteGradient(Color.black, Color.grey(0.4), \v, 256, 0.5);
		
		RoundButton(window, Rect(10, 10, 50, 20))
			.font_(font.copy.size_(10))
			.states_([
				["start f(x)", Color.grey(0.8), Color.grey(0.2)], 
				["quit f(x)", Color.green, Color.black]
			])
			.action_({|btn|
				if (btn.value == 1) { fx.startFx(true)} { fx.quitOpenGL }
			});

		RoundButton(window, Rect(10, 35, 50, 20))
			.font_(font.copy.size_(10))
			.states_([
				["freeze", Color.grey(0.8), Color.grey(0.2)], 
				["unfreeze", Color.green, Color.black]
			])
			.action_({|btn|
				if (btn.value == 1) { fx.sendMsg("freeze", 1) } { fx.sendMsg("freeze", 0) }
			});
		
		RoundButton(window, Rect(10, 60, 50, 20))
			.font_(font.copy.size_(10))
			.states_([["sync", Color.grey(0.8), Color.grey(0.2)]])
			.action_({
				settingSliders.do({|slider, i|
					var param;
					param = fx.glOrder[i];
					if (fx.visualdict.glSpecs[param].notNil)
					{
						slider.value = fx.visualdict.glSpecs[param].unmap(fx.visualdict.globals[param]);
					}
					{
						slider.value = fx.visualdict.globals[param];
					};
					settingTexts[i].string_(fx.visualdict.globals[param].round(0.001).asString)
				})
			});
		
		ptPanel = CompositeView(window,
			Rect(65, 5, window.bounds.width - 70, window.bounds.height / 2 - 10)
		);

		button = Rect(5, 5, ptPanel.bounds.width / 10 - gap, 20);
		slider = Rect(5, 5 + (button.height * 4) + (gap * 4), 
			ptPanel.bounds.width / 10 - (gap * 2) / 2, 90);
		
		fx.patchOrder.do({|patch, i|
			var fd, sd;
			RoundButton(ptPanel, button)
				.font_(font.copy.size_(10))
				.states_([
					[patch.asString, Color.grey(0.8), Color.grey(0.2)], 
					[patch.asString.toUpper, Color.green, Color.black]
				])
				.action_({|btn|
					if (btn.value == 1)
					{
						fx.activatePatch(patch, 0.0)
					}
					{
						fx.deactivatePatch(patch, 0.0)
					}
				});
			RoundButton(ptPanel, button.copy.top_(button.top + button.height + gap))
				.font_(font)
				.states_([
					["gray", Color.grey(0.2), Color.grey], 
					["blue", Color.grey(0.2), Color.blue], 
					["green", Color.grey(0.2), Color.green],
					["yellow", Color.grey(0.2), Color.yellow],
					["purple", Color.grey(0.2), Color(0.53, 0.0, 0.77)]
				])
				.action_({|btn|
					fx.sendPatchCmd(patch, \color, 0, btn.value.asFloat, 0)
				});
			RoundButton(ptPanel, button.copy.top_(button.top + (button.height * 2) + (gap * 2) ))
				.font_(font)
				.states_([
					["color map", Color.grey(0.8), Color.grey(0.2)], 
					["color map", Color.grey(0.2), Color.grey(0.8)]
				])
				.action_({|btn|
					fx.sendPatchCmd(patch, \colormap, 0, btn.value.asFloat, 0)
				});
			RoundButton(ptPanel, button.copy.top_(button.top + (button.height * 3) + (gap * 3) ))
				.font_(font)
				.states_([
					["alpha map", Color.grey(0.8), Color.grey(0.2)], 
					["alpha map", Color.grey(0.2), Color.grey(0.8)]
				])
				.action_({|btn|
					fx.sendPatchCmd(patch, \alphamap, 0, btn.value.asFloat, 0)
				});
			fd = StaticText(ptPanel, Rect(slider.left, slider.top + 10, slider.width, 20))
				.font_(font)
				.align_(\center)
				.string_("0")
				.stringColor_(Color.new255(28, 134, 238));
				
			SmoothSlider(ptPanel, slider)
				.action_({|sld|
					fd.string_(sld.value.round(0.01).asString);
					fx.sendPatchCmd(patch, \alphahi, 0, sld.value, 0)
				});

			sd = StaticText(ptPanel, Rect(slider.left + slider.width + gap, 
				slider.top + 10, slider.width, 20))
				.font_(font)
				.align_(\center)
				.string_("1")
				.stringColor_(Color.new255(28, 134, 238));
			SmoothSlider(ptPanel, slider.copy.left_(slider.left + slider.width + gap))
				.value_(1)
				.action_({|sld|
					sd.string_(sld.value.round(0.01).asString);
					fx.sendPatchCmd(patch, \param, 0, sld.value, 0)
				});

			button.left = button.left + button.width + gap;
			slider.left = slider.left + button.width + gap;
		});

		label = Rect(5, 5, 28, 15);
		slider = Rect(5, 5, 28, 180);
		
		glPanel = CompositeView(window, 
			Rect(5, 205, slider.width + 5 * fx.visualdict.globals.size, slider.height + 30)
		);
		
		settingValues = Array.fill(fx.glOrder.size);
		settingSliders = Array.newClear(fx.glOrder.size);
		settingTexts = Array.newClear(fx.glOrder.size);
					
		fx.glOrder.do({|param, i|
			var inval, val;
			if (fx.visualdict.glSpecs[param].notNil) {  
				inval = fx.visualdict.glSpecs[param].unmap(fx.visualdict.globals[param]);
				val = fx.visualdict.globals[param];
			}
			{
				inval = val = fx.visualdict.globals[param]
			};
			StaticText(glPanel, label)
				.align_(\center)
				.font_(font)
				.stringColor_(Color.new255(255, 140, 0))
				.string_(param.asString);
				
			settingTexts.put(i, StaticText(glPanel, label.copy.top_(100))
				.align_(\center)
				.font_(font)
				.stringColor_(Color.new255(28, 134, 238))
				.string_(val.round(0.001).asString)
			);
			settingValues.put(i, fx.visualdict.globals[param]);
			settingSliders.put(i, SmoothSlider(glPanel, slider)
				.value_(inval)
				.action_({|sld|
					if (fx.visualdict.glSpecs[param].notNil)
					{
						val = fx.visualdict.glSpecs[param].map(sld.value);
					}
					{
						val = sld.value;
					};
					settingTexts[i].string_(val.round(0.001).asString);
					settingValues.put(i, val);
					fx.sendSettings(0, *settingValues)
				})
			);
				
			label.left = label.left + label.width + gap;
			slider.left = slider.left + slider.width + gap;
		});
		
		wrPanel = CompositeView(window, 
			Rect( glPanel.bounds.left + glPanel.bounds.width + 10, 205, 
				window.bounds.width - (glPanel.bounds.left + glPanel.bounds.width + 10), 190)
		);
		
		RoundButton(wrPanel, Rect(5, 5, 90, 20))
			.font_(font)
			.states_([
				["world", Color.new255(28, 134, 238), Color.grey(0.2)]
			])
			.action_({				
				fx.sendReset(
					seed: seed,
					habitat: 0,
					radius: 1,
					left: seedValues@0,
					bottom: seedValues@1,
					front: seedValues@2,
					width: seedValues@3,
					height: seedValues@4,
					depth: seedValues@5,
					weights: weightSliders.collect({|item|
						wspec.map(item.value).asFloat
					})
				);
			});
		RoundButton(wrPanel, Rect(5, 30, 90, 20))
			.font_(font)
			.states_([
				["weights", Color.new255(28, 134, 238), Color.grey(0.2)]
			])
			.action_({
				fx.sendWeights( 
					weightSliders.collect({|item|
						wspec.map(item.value).asFloat
					})
				)			
			});
		
		seedValues = Array.newClear(6);
		seedValues.put(0, fx.visualdict.world.left);
		seedValues.put(1, fx.visualdict.world.bottom);
		seedValues.put(2, fx.visualdict.world.front);
		seedValues.put(3, fx.visualdict.world.width);
		seedValues.put(4, fx.visualdict.world.height);
		seedValues.put(5, fx.visualdict.world.depth);
		
		seedspec = ControlSpec(0, 15, \lin, 1);
		
		6.do({|i|
			var lbl;
			StaticText(wrPanel, Rect(90 / 6 * i + 5, 55, 90 / 6 - 3, 10))
				.align_(\center)
				.font_(font)
				.stringColor_(Color.new255(255, 140, 0))
				.string_(["L", "B", "F", "W", "H", "D"]@i);
			lbl = StaticText(wrPanel, Rect(90 / 6 * i + 5, 100, 90 / 6 - 3, 10))
				.align_(\center)
				.font_(font)
				.stringColor_(Color.new255(28, 134, 238))
				.string_(seedValues[i].asString);
			SmoothSlider(wrPanel, Rect(90 / 6 * i + 5, 65, 90 / 6 - 3, 70 ))
				.value_(seedspec.unmap(seedValues@i))
				.action_({|sld|
					seedValues.put(i, seedspec.map(sld.value));
					lbl.string_(seedValues[i].round(1).asString);
				});
		});	
		
		RoundButton(wrPanel, Rect(95, 95, 20, 20))
			.font_(font)
			.states_([["w", Color.green, Color.grey(0.3)], ["r", Color.blue, Color.grey(0.7)]])
			.action_({|btn| seed = btn.value });
		
		poll = PopUpMenu(wrPanel, Rect(5, 138, 60, 20))
			.font_(font)
			.stringColor_(Color.new255(28, 134, 238))
			.items_(["sides", "uni", "mesh", "core", "sheet", "xl uni"]);
			
		RoundButton(wrPanel, Rect(70, 138, 25, 20))				.font_(font)
			.states_([["--", Color.new255(28, 134, 238), Color.grey(0.2)]])
			.action_({ fx.sendPollIndices(fx.getPollIndices(poll.value)) });
		
		PopUpMenu(wrPanel, Rect(5, 160, 90, 20))
			.font_(font)
			.stringColor_(Color.new255(28, 134, 238))
			.items_(fx.weightPresets.keys(Array).sort)
			.value_(fx.weightPresets.keys(Array).sort.indexOf(\equal))
			.action_({|mnu|
				fx.weightPresets[mnu.items[mnu.value]].value.do({|val, i|
					weightSliders[i].value = wspec.unmap(val);
					weightSliders[i].doAction;
				})
			});
				
		slider = Rect(120, 5, 10, 180);
		
		weightSliders = Array.newClear(26);
		
		wspec = ControlSpec(0, 10, \lin, 1);
		
		26.do({|i|
			var lbl;
			lbl = StaticText(wrPanel, Rect(slider.left, slider.top, slider.width, 15))
				.align_(\center)
				.font_(font)
				.stringColor_(Color.new255(28, 134, 238))
				.string_("1");
			weightSliders.put(i, 
				SmoothSlider(wrPanel, slider)
					.step_(0.1)
					.value_(0.1)
					.action_({|sld| 
						lbl.string_(wspec.map(sld.value).round(1).asString) 
					})
			);
			slider.left = slider.left + slider.width + 3
		});
		
			
	}
	
}

TouchOSC{
	
	var addr, controls;
	
	*new{|addr|
		^super.newCopyArgs(addr)
	}
	
	addControl{|cmd, action|
		if (controls.isNil) { controls = () };
		controls[cmd] = OSCresponderNode(nil, cmd, action).add
	}
	
	removeControl{|cmd|
		controls[cmd].remove;
		controls[cmd] = nil;
	}
	
	replaceControl{|cmd, action|
		controls[cmd].action = action;
	}
	
	setControl{|cmd, value|
		addr.sendMsg(cmd, value)
	}
	
	clear{
		controls.keysValuesDo({|key| controls[key].remove; controls[key] = nil });
	}
	
}

