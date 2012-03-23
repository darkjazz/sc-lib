PatternGui{
	
	var <patterns, order, <groups, <window, <>activeaction, <>ampsaction, <>dursaction, <>patternaction, <steps, <playbutton; 
	var <>groupsaction, text, activebuttons, <patternbuttons, ctrpnl, patpnl;

	*new{|patterns, order, groups, args|
		^super.newCopyArgs(patterns, order, groups).init(args);
	}
	
	init{|args|
		var grpstates;
		var font = Font("Courier", 10);
		args = args ? patterns.collect({ (active: 0, amp: 0, dur: 0.1, aspec: FaderWarp(), dspec: ControlSpec()) });
		steps = patterns.choose.size;
		window = Window("_o_o_o_o_o x o_o_o_o_o_", Rect(10, 500, steps*20+310, patterns.size*20+70))
			.background_(Color.grey(0.1)).alpha_(0.99).front;
		if (order.size != patterns.size) { order = nil };
		order = order ? patterns.keys(Array);
		groups = groups ? order.bubble;
		ctrpnl = CompositeView(window, window.view.bounds.resizeTo(window.view.bounds.width, 40));
		playbutton = RoundButton(ctrpnl, Rect(30, 5, 60, 25))
			.states_([[\play, Color.green, Color.grey(0.3)], [\stop, Color.red, Color.grey(0.5)]]);
		text = StaticText(ctrpnl, Rect(95, 5, 60, 25))
			.font_(font)
			.align_(\center)
			.stringColor_(Color.grey(0.6))
			.string_("0");
		groups.size.do({|i|
			Button(ctrpnl, Rect(i*30+160, 5, 20, 20))
				.font_(font)
				.states_([[(i+1).asString, Color.grey(0.5), Color.grey(0.2)], [(i+1).asString, Color.green, Color.grey(0.6)]])
				.value_(args[groups[i].first].active)
				.action_({|btn|
					groups[i].do({|name, j|
						activebuttons[name].value = btn.value;
						activebuttons[name].doAction;
					})
				})
		});
		patpnl = CompositeView(window, window.view.bounds.moveBy(0, 40));
		activebuttons = ();
		grpstates = Array.fill(groups.size, {|i|
			[(i+1).asString, Color.grey(0.5), Color.grey(0.2)]
		});
		patternbuttons = Array.fill(order.size, { Array.newClear(steps) });
		order.do({|key, i|
			StaticText(patpnl, Rect(5,i*20+5,20,20))
				.align_(\center)
				.font_(font)
				.stringColor_(Color.grey(0.7))
				.string_(key.asString);
			patterns[key].do({|onoff,j|
				patternbuttons[i][j] = Button(patpnl, Rect(j*20+30,i*20+5,20,20))
					.font_(font)
					.states_([["-", Color.grey(0.5), Color.grey(if(j%8==0){0.25}{0.3})],
						["o", Color.green, Color.grey(0.6)]])
					.value_(onoff)
					.action_({|btn|
						patternaction.(btn, key, j);
					})
			});
			activebuttons.put(key, 
				Button(patpnl, Rect(steps*20+30,i*20+5,30,20))
					.font_(font)
					.states_([[\off, Color.grey(0.5), Color.grey(0.2)], [\on, Color.green, Color.grey(0.6)]])
					.value_(args[key].active)
					.action_({|btn|
						activeaction.(btn, key)
					})
			);
			SmoothSlider(patpnl, Rect(steps*20+65,i*20+5,100,18))
				.font_(font)
				.string_(args[key].amp.round(0.01).asString)
				.stringColor_(Color.grey(0.5))
				.value_(args[key].aspec.unmap(args[key].amp))
				.action_({|slider|
					ampsaction.(slider, key)
				});
			SmoothSlider(patpnl, Rect(steps*20+170,i*20+5,100,18))
				.font_(font)
				.string_(args[key].dur.round(0.01).asString)
				.stringColor_(Color.grey(0.5))
				.value_(args[key].dspec.unmap(args[key].dur))
				.action_({|slider|
					dursaction.(slider, key)
				});
			
			Button(patpnl, Rect(steps*20+275, i*20+5, 20, 20))
				.font_(font)
				.states_(grpstates)
				.value_(this.findGroup(key))
				.action_({|btn|
					groups[(btn.value-1).wrap(0, groups.size-1)].remove(key);
					groups[btn.value] = groups[btn.value].add(key).sort
				})
		})
	}
	
	indicateEvent{|pat, col, dur=0.1|
		var row = order.indexOf(pat);
		patternbuttons[row][col].states = [ 
			["-", Color.grey(0.5), Color.grey(if(col%8==0){0.25}{0.3})],
			["o", Color.green, Color.grey(0.8)]
		];
		window.refresh;
		AppClock.sched(dur, {
			patternbuttons[row][col].states = [ 
				["-", Color.grey(0.5), Color.grey(if(col%8==0){0.25}{0.3})],
				["o", Color.green, Color.grey(0.6)]
			];
			window.refresh;
			nil
		})
	}
	
	findGroup{|name|
		if (groups.size == 1) {
			^0
		}
		{
			^groups.selectIndices({|grp| grp.includes(name) }).first
		}
	}
	
	setText{|string|
		text.string_(string.asString)
	}
	
}