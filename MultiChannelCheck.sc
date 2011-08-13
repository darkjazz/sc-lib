MultiChannelCheck{

	classvar <standardSetups;
	
	var <layers, server, win, scope, gui, <scopebufs, synth, <numSpeakers, <outputs, <channels;
	var <>channelOut = 0, autorout;
	
	*new{|layers|
		^super.newCopyArgs(layers).init	
	}
	
	init{
		layers.class.postln;
		if((layers.isKindOf(Array).not).and(layers.isKindOf(MultiChannelLayer).not), 
			{"layers is expected to be an Array of MultiChannelLayers 
				or a single MultiChannelLayer.".warn}, {
			if (layers.isKindOf(MultiChannelLayer), {
				layers = layers.asArray
			});
			outputs = layers.collect({|it| it.outputs}).flat;
			numSpeakers = layers.collect({|it| it.speakersAzim.size}).sum;
			server = Server.internal;
			Server.default = server;
			if (numSpeakers > server.options.numOutputBusChannels)
			{
				server.options.numOutputBusChannels = numSpeakers
			};
			if (server.serverRunning.not, {
				server.waitForBoot({
					this.prepareServer
				})
			}, {
				this.prepareServer
			});
		});
	}
		
	prepareServer{
		scopebufs = Array.fill(numSpeakers, {|i|
			Buffer.alloc(server, 4096, 1, completionMessage: {|buf|
				SynthDef("scoper" ++ i.asString, {
					ScopeOut.ar(In.ar(outputs[i]), buf.bufnum)
				}).play(1, addAction: \addToTail)
			})
		});
		SynthDef("check", {
			var sig, outs, freqs, low, env;
			env = EnvGen.kr(Env.perc, Impulse.kr(0.5), timeScale: 0.2, levelScale: 0.3);
			outs = Control.names([\outs]).kr(Array.fill(outputs.size, 99));
			freqs = Array.geom(4, 40, 512**(1/4));
			sig = Mix(VarSaw.ar(freqs, mul: AmpCompA.kr(freqs)))
				* env;
			low = Mix(SinOsc.ar(Array.geom(6, 20, 2**(1/17)) * (1..6), pi, 0.2)) * env;
			sig = sig + CombC.ar(sig, 0.25, 0.25, 2);
			Out.ar(outs, sig + low)
		}).send(server);
		
		channels = Array.fill(numSpeakers, 99).put(channelOut, outputs[channelOut]);
		
		gui = MultiChannelCheckGui(this);
	}
	
	run{
		synth = Synth("check").setn("outs", channels);
	}
	
	autorun{|offset = 0|
		this.run;
		autorout = Routine({
			var seq;
			seq = Pseq(outputs, inf, offset).asStream;
			inf.do({
				layers.do({|layer, i|
					layer.outputs.do({|out, j|
						channels = Array.fill(numSpeakers, 99);
						channels.put(j, seq.next);
						synth.setn("outs", channels);
						gui.playbuttons[i][j].value = 1;
						3.98.wait;
						gui.playbuttons[i][j].value = 0;
					});
					if (i == (layers.size - 1), {gui.firstScreen}, {gui.screenForward})
				});
			})
		}).play(AppClock)
	}
	
	runChannel{|channel|
		if (synth.notNil, {this.stop});	
		channels = Array.fill(numSpeakers, 99).put(channel, outputs[channel]);
		this.run		
	}
	
	autostop{
		autorout.stop;
		synth.free;
		channels = Array.fill(numSpeakers, 99)
	}
	
	selectChannel{
		channels = Array.fill(numSpeakers, 99).put(channelOut, outputs[channelOut]);
		synth.setn("outs", channels)
	}
	
	stop{
		synth.free;
		synth = nil
	}
	
	*initClass{
		standardSetups = Dictionary[
			\quad -> ([-0.25, -0.75, 0.75, 0.25] * pi),
			\hexagon -> ([-0.167, -0.5, -0.833, 0.833, 0.5, 0.167] * pi),
			\octagon -> ([-0.125, -0.375, -0.625, -0.875, 0.875, 0.625, 0.375, 0.125] * pi)
		]
	}
		
}

MultiChannelLayer{
	
	var <name, <speakersAzim, <speakersElev, <outputs;
	
	*new{|name, speakersAzim, speakersElev, outputs|
		^super.newCopyArgs(name, speakersAzim, speakersElev, outputs)
	}
	
}

MultiChannelCheckGui{
	
	var channelcheck, win, scope, currentlayer, scopesize = 80, scopes, <fwd, <bck;
	var scopeIndex = 0, index = 0, txts, fwsp, bksp, <playbuttons;
	
	*new{|channelcheck|
		^super.newCopyArgs(channelcheck).make
	}
	
	make{
		currentlayer = if(channelcheck.layers.isKindOf(MultiChannelLayer), {channelcheck.layers}, 
			{channelcheck.layers[index]});

		win = SCWindow(":: " + currentlayer.name + " ::", Rect(20, 200, 700, 700)).front;
		win.onClose_({
				Server.internal.freeAll
			});
		win.view.background_(HiliteGradient(Color.black, Color.new255(45, 70, 82), steps: 256));
		SCButton(win, Rect(300, 325, 100, 50))
			.states_([["....", Color.new255(45, 70, 82), Color.grey(0.7)], 
				[".::.", Color.grey(0.7), Color.new255(45, 70, 82)]])
			.action_({|btn|
				if (btn.value == 1, {
					channelcheck.autorun(
						channelcheck.outputs.indexOf(currentlayer.outputs[0]));
					fwd.enabled_(false);
					bck.enabled_(false);

				}, {
					channelcheck.autostop;
					fwd.enabled_(true);
					bck.enabled_(true);
				})
			});
		fwd = SCButton(win, Rect(65, 10, 50, 25))
			.states_([[">>", Color.new255(45, 70, 82), Color.grey(0.7)]])
			.visible_(channelcheck.layers.size > 1)
			.action_({|btn|
				this.screenForward;
				if (index == channelcheck.layers.lastIndex, {btn.visible = false});
			});
		bck = SCButton(win, Rect(10, 10, 50, 25))
			.states_([["<<", Color.new255(45, 70, 82), Color.grey(0.7)]])
			.visible_(false)
			.action_({|btn|
				this.screenBack;
				if (index == 0, {btn.visible = false});
			});
		
//		bksp = SCButton(win, Rect(300, 300, 50, 20))
//			.states_([["<", Color.new255(51, 80, 88), Color.new255(72, 209, 204)]])
//			.visible_(false)
//			.action_({|btn|
//				channelcheck.channelOut = channelcheck.channelOut - 1;
//				if (channelcheck.channelOut == 0, {btn.visible = false});
//				channelcheck.selectChannel
//			});
//			
//		fwsp = SCButton(win, Rect(350, 300, 50, 20))
//			.states_([[">", Color.new255(51, 80, 88), Color.new255(72, 209, 204)]])
//			.visible_(false)
//			.action_({|btn|
//				channelcheck.channelOut = channelcheck.channelOut + 1;
//				if (channelcheck.channelOut == (channelcheck.outputs.size - 1), 
//					{btn.visible = false});
//				channelcheck.selectChannel			
//			});
		
//		SCButton(win, Rect(300, 275, 25, 20))
//			.states_([
//				["..", Color.new255(51, 80, 88), Color.new255(72, 209, 204)], 
//				["o", Color.new255(72, 209, 204), Color.new255(51, 80, 88)]
//			])
//			.action_({|btn|
//				if (btn.value == 1, {
//					autorun = true;
//					bksp.visible_(false);
//					fwsp.visible_(false)
//				}, {
//					autorun = false
//				})
//			});
			
		scopes = Array.newClear(channelcheck.layers.size);
		txts = Array.newClear(channelcheck.layers.size);
		playbuttons = Array.newClear(channelcheck.layers.size);
		
		this.drawScopes;
							
	}
	
	screenForward{
		this.setVisibility(false);
		bck.visible = true;
		index = index + 1;
		currentlayer = channelcheck.layers[index];
		win.name = ":: " + currentlayer.name + " ::";
		this.setVisibility(true);	
	}
	
	screenBack{
		this.setVisibility(false);
		fwd.visible = true;
		index = index - 1;
		currentlayer = channelcheck.layers[index];
		win.name = ":: " + currentlayer.name + " ::";
		this.setVisibility(true);			
	}
	
	firstScreen{
		this.setVisibility(false);
		bck.visible = false;
		fwd.visible = true;
		index = 0;		
		currentlayer = channelcheck.layers[0];
		win.name = currentlayer.name;
		this.setVisibility(true);
	}
	
	drawScopes{
		channelcheck.layers.do({|layer, i|
			var sc, tx, pb, coords;
			coords = Array.fill(layer.speakersAzim.size, {|j|
				Polar(win.bounds.width/2 - (scopesize * 2), layer.speakersAzim[j] - 0.5pi)
			});			
			coords.do({|polar, j|
				var point, scope, lbl, pbtn;
				point = polar.asPoint;
				scope = SCScope(win, 
					Rect(
						win.bounds.height / 2 + (point.x - 40), 
						win.bounds.height / 2 + (point.y - 40), 
						scopesize, scopesize))
					.background_(HiliteGradient(Color.grey(0.5), Color.clear, \v, 64, 0.66))
					.bufnum_(channelcheck.scopebufs[layer.outputs[j]].bufnum)
					.waveColors_([Color.grey(0.8)])
					.visible_(i == 0)
					.yZoom_(6);
//				lbl = SCStaticText(win, 
//					Rect(
//						win.bounds.height / 2 + (point.x - 40), 
//						win.bounds.height / 2 + (point.y - 40) + scopesize,
//						scopesize, 20
//						)
//				).string_("channel :" + layer.outputs[j]).visible_(i == 0).align_(\center);
				pbtn = SCButton(win, Rect(
						win.bounds.height / 2 + (point.x - 40), 
						win.bounds.height / 2 + (point.y - 40) + scopesize + 2,
						scopesize, 20				
				)).states_([
					["channel :" + layer.outputs[j], 
						Color.new255(45, 70, 82), Color.grey(0.7)], 
					["channel :" + layer.outputs[j],
						Color.grey(0.7), Color.new255(45, 70, 82)]
				]).action_({|btn|
					if (btn.value == 1, {
						playbuttons[i].do({|pbt, indj|
							if ((indj != j).and(pbt.value == 1), {
								pbt.value_(0)
							})
						});
						channelcheck.runChannel(channelcheck.outputs.indexOf(layer.outputs[j]));

					}, {
						channelcheck.stop;
					})
				}).visible_(i == 0);
				sc = sc.add(scope);
//				tx = tx.add(lbl);
				pb = pb.add(pbtn);
			});
			scopes.put(i, sc);
//			txts.put(i, tx);
			playbuttons.put(i, pb)
		});
	}
	
	setVisibility{|visible|
		scopes[channelcheck.layers.indexOf(currentlayer)].do({|it|
			it.visible_(visible)
		});
//		txts[channelcheck.layers.indexOf(currentlayer)].do({|it|
//			it.visible_(visible)
//		});
		playbuttons[channelcheck.layers.indexOf(currentlayer)].do({|it|
			it.visible_(visible)
		});		
	
	}
		
}