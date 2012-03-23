/*
 *  fxpublic.sc
 *  Fx
 *
 *  Created by tehis on 23/09/2010.
 *  
 *	This file is part of Fx.
 *
 *	Fx is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 
 *	Fx is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 
 *	You should have received a copy of the GNU General Public License
 *	along with Fx.  If not, see <http://www.gnu.org/licenses/>. 
 *
 */

FxPublicOptions{
	var <>fxip = "127.0.0.1";
	var <>fxport = 7770;
	var <>fxcmd = '/fx';
	var <>fxapp = "/Fx.app";
	var <>sendcmd = "/fx";
}

FxPublic{
	classvar <visuals, <actions;

	var <opts;
	var <vaddr, glResponder, glFuncs, stResponder, stFuncs, trResponder, trFuncs; 
	var <invalues, <visualdict;
	var <glOrder, <patchOrder, <oglGui, <weightPresets, <perfGui;
	
	*initClass{
		visuals = (
			horizons: 0,
			axial: 1,
			grid: 2,
			mesh: 3
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
			scale: 10		
		)	
	}
	
	*new{|appPath|
		^super.new.init(appPath);
	}

	init{|appPath|
		opts = FxPublicOptions();
		opts.fxapp = (appPath ? "") ++ opts.fxapp;
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
			frame: 4
		);	
		
		visualdict.glSpecs = (
			add: ControlSpec(0.001, 0.999, \cos),
			transx: ControlSpec(-6.0, 6.0, \lin),
			transy: ControlSpec(-6.0, 6.0, \lin), 
			transz: ControlSpec(-64, 4, \lin, 1),
			angle: ControlSpec(-1.0, 1.0, \lin),
			frame: ControlSpec(1, 12, \lin, 1)
		);
		
		glOrder = [\alpha, \clear, \add, \transx, \transy, \transz, \angle, \rotX, \rotY, \rotZ, 
			\frame];
		patchOrder = [\grid, \horizons, \axial, \mesh];
		
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
			left: 4,
			bottom: 4,
			front: 7,
			width: 8,
			height: 8,
			depth: 1,
			weights: (1.0 ! 26)
		);
		
		weightPresets = (
			equal: (1 ! 26),
			neumann: [1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 3, 1, 3, 3, 1, 3, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1],
			cross: [1, 3, 1, 1, 3, 1, 1, 3, 1, 3, 3, 3, 3, 3, 3, 3, 3, 1, 3, 1, 1, 3, 1, 1, 3, 1],
			symmetry: { Array.rand(13, 1, 5).mirror2 },
			rand: { Array.rand(26, 1, 5) }
		);
		
		glFuncs = (); stFuncs = (); trFuncs = ();
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
			invalues.states = ms[1].asFloatArray;
			stFuncs.do({|func| func.value(invalues) });
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
	
	activatePatch{|patch, time|
		this.sendPatchCmd(patch, \active, 0.0, 1.0, time);
	}

	deactivatePatch{|patch, time|
		this.sendPatchCmd(patch, \active, 1.0, 0.0, time);
	}
	
	setGlobals{|alpha, clear, add, transx, transy, transz, angle, rotX, rotY, rotZ, frame|
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
									angle, rotX, rotY, rotZ, frame, doneAction|
		
		if (time > 0)
		{
			Routine({
				var times, steps, values;
				times = time * opts.msgRate;
				values = Array.fill(visualdict.globals.size, {|i| visualdict.globals[glOrder@i] });
				steps = [alpha, clear, add, transx, transy, transz, angle, rotX, rotY, rotZ, frame] -
					values / times;
				times.do({
					this.sendMsg("settings", *values.flat);
					values = values + steps;
					opts.msgRate.reciprocal.wait
				});
				opts.msgRate.reciprocal.wait;
				this.setGlobals(alpha, clear, add, transx, transy, transz, angle, rotX, rotY, rotZ, 
					frame);
				doneAction.value(this);
			}).play
		}
		{
			this.sendMsg("settings", *[alpha, clear, add, transx, transy, transz, 
				angle, rotX, rotY, rotZ, frame]);
			this.setGlobals(alpha, clear, add, transx, transy, transz, angle, rotX, rotY, rotZ, frame);
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
			visualdict.globals[\frame]			
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
		this.sendMsg("poll", *indices)
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
				
			}
		);
		
		^arr.flat
		
	}

	quitOpenGL{
		this.sendMsg("quit", 0)
	}
	
	makeOglGui{
		oglGui = FxOGLGui(this)
	}
	
	startFx{
		("open" + opts.fxapp).unixCmd;
	}
	
	wait{|numCycles=1|
		(numCycles/24).wait	
	}
	
	

}

FxOGLGui{
	
	var fx, window, <glPanel, <ptPanel, <wrPanel, <wrSPanel;
	
	*new{|fx|
		^super.newCopyArgs(fx).makeWindow
	}
	
	makeWindow{

		var slider, label, button, gap = 5, font, weightSliders, settingValues, wspec, seedValues;
		var seedspec, poll;
		
		font = Font("Lucida Grande", 9);
	
		window = Window(":--: OGL :--:", Rect(100, 400, 820, 400)).alpha_(0.95).front;
		window.background = Color.black;
		window.view.background = HiliteGradient(Color.black, Color.grey(0.4), \v, 256, 0.5);
		
		ptPanel = CompositeView(window,
			Rect(5, 5, window.bounds.width - 10, window.bounds.height / 2 - 10)
		);

		button = Rect(5, 5, ptPanel.bounds.width / 10 - gap, 20);
		slider = Rect(5, 5 + (button.height * 4) + (gap * 4), 
			ptPanel.bounds.width / 10 - (gap * 2) / 2, 90);
		
		fx.patchOrder.do({|patch, i|
			var fd, sd;
			Button(ptPanel, button)
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
			Button(ptPanel, button.copy.top_(button.top + button.height + gap))
				.font_(font)
				.states_([
					["gray", Color.grey(0.2), Color.grey], 
					["blue", Color.grey(0.2), Color.blue], 
					["green", Color.grey(0.2), Color.green]
				])
				.action_({|btn|
					fx.sendPatchCmd(patch, \color, 0, btn.value.asFloat, 0)
				});
			Button(ptPanel, button.copy.top_(button.top + (button.height * 2) + (gap * 2) ))
				.font_(font)
				.states_([
					["color map", Color.grey(0.8), Color.grey(0.2)], 
					["color map", Color.grey(0.2), Color.grey(0.8)]
				])
				.action_({|btn|
					fx.sendPatchCmd(patch, \colormap, 0, btn.value.asFloat, 0)
				});
			Button(ptPanel, button.copy.top_(button.top + (button.height * 3) + (gap * 3) ))
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
				
			Slider(ptPanel, slider)
				.action_({|sld|
					fd.string_(sld.value.round(0.01).asString);
					fx.sendPatchCmd(patch, \alphahi, 0, sld.value, 0)
				});

			sd = StaticText(ptPanel, Rect(slider.left + slider.width + gap, 
				slider.top + 10, slider.width, 20))
				.font_(font)
				.align_(\center)
				.string_("0")
				.stringColor_(Color.new255(28, 134, 238));
			Slider(ptPanel, slider.copy.left_(slider.left + slider.width + gap))
				.action_({|sld|
					sd.string_(sld.value.round(0.01).asString);
					fx.sendPatchCmd(patch, \scale, 0, sld.value, 0)
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
					
		fx.glOrder.do({|param, i|
			var dsp, inval, val;
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
			dsp = StaticText(glPanel, label.copy.top_(100))
				.align_(\center)
				.font_(font)
				.stringColor_(Color.new255(28, 134, 238))
				.string_(val.round(0.001).asString);
			settingValues.put(i, fx.visualdict.globals[param]);
			Slider(glPanel, slider)
				.value_(inval)
				.action_({|sld|
					if (fx.visualdict.glSpecs[param].notNil)
					{
						val = fx.visualdict.glSpecs[param].map(sld.value);
					}
					{
						val = sld.value;
					};
					dsp.string_(val.round(0.001).asString);
					settingValues.put(i, val);
					fx.sendSettings(0, *settingValues)
				});
				
			label.left = label.left + label.width + gap;
			slider.left = slider.left + slider.width + gap;
		});
		
		wrPanel = CompositeView(window, 
			Rect( glPanel.bounds.left + glPanel.bounds.width + 20, 5, 100, 190)
		);
		
		Button(wrPanel, Rect(5, 5, 90, 20))
			.font_(font)
			.states_([
				["world", Color.new255(28, 134, 238), Color.grey(0.2)]
			])
			.action_({				
				fx.sendReset(
					seed: 0,
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
		Button(wrPanel, Rect(5, 30, 90, 20))
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
			Slider(wrPanel, Rect(90 / 6 * i + 5, 65, 90 / 6 - 3, 70 ))
				.value_(seedspec.unmap(seedValues@i))
				.action_({|sld|
					seedValues.put(i, seedspec.map(sld.value));
					lbl.string_(seedValues[i].round(1).asString);
				});
		});	
		
		poll = PopUpMenu(wrPanel, Rect(5, 138, 60, 20))
			.font_(font)
			.stringColor_(Color.new255(28, 134, 238))
			.items_(["sides", "uni", "mesh"]);
			
		Button(wrPanel, Rect(70, 138, 25, 20))					.font_(font)
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
					window.refresh;
				})
			});
				
		slider = Rect(5, 5, 10, 180);
		
		weightSliders = Array.newClear(26);
		
		wspec = ControlSpec(0, 10, \lin, 1);
		
		wrSPanel =  CompositeView(window, 
			Rect(glPanel.bounds.left + glPanel.bounds.width + 20, 205, 
				slider.width + 5 * fx.visualdict.globals.size, slider.height + 30)
		);
		
		26.do({|i|
			var lbl;
			lbl = StaticText(wrSPanel, Rect(slider.left, slider.top, slider.width, 15))
				.align_(\center)
				.font_(font)
				.stringColor_(Color.new255(28, 134, 238))
				.string_("1");
			weightSliders.put(i, 
				Slider(wrSPanel, slider)
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
