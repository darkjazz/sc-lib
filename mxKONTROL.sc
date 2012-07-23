MxKONTROL{

	var mx, nano, win, view, updater, openTracks, assignedTracks, assignitems; 
	var font, guiItems;

	*new{|mx, nano|
		^super.newCopyArgs(mx, nano).init
	}
	
	init{
		
		nano = nano ? NanoKONTROL();

		win = Window("...KTR-mxiiiixm-RTK...", Rect(100, 200, 200, 200)).front;
		win.view.background_(HiliteGradient(Color.grey(0.15), Color.grey(0.4), \v, 256, 0.5));
		
		font = Font("Andale Mono", 10);
		
		view = CompositeView(win, win.view.bounds);
		
		win.onClose = { updater.stop; updater.clear };
		
		assignedTracks = (nil ! 4);
		assignitems = ["..", "1:2", "3:4", "5:6", "7:8"];
		
		openTracks = [];
			
		updater = Routine({
			loop({
				if (mx.tracks.notNil) {
					var previousState;
					previousState = openTracks.collect(_.id);
					openTracks = mx.tracks;
					assignedTracks.select({|id| openTracks.includes(id).not }).do({|id|
						assignedTracks[assignedTracks.indexOf(id)] = nil
					});
					openTracks.do({|track, i|
						if ((assignedTracks.includes(track.id).not).and(assignedTracks.indexOf(nil).notNil)) {
							assignedTracks.put(assignedTracks.indexOf(nil), track.id)
						}
					});
					if (previousState != openTracks.collect(_.id)) {
						{ this.drawTracks }.defer;
						this.mapnano;
					}
				};
				2.wait
			})
		}).play;
				
	}
	
	drawTracks{
		if (guiItems.notNil) {
			guiItems.do({|ev|
				ev.values.do(_.remove)
			})
		};
		guiItems = openTracks.collect({|track, i|
			var aindex, text, menu;
			text = StaticText(view, Rect(10, i * 30 + 10, 140, 25))
				.align_(\left)
				.font_(font)
				.string_(track.id.asString.padLeft(2, "0") ++ " " ++ track.name.keep(16))
				.stringColor_(Color.green)
				.background_(Color.grey(0.2));
			if (assignedTracks.indexOf(track.id).isNil) {
				aindex = 0;				
			}
			{
				aindex = assignedTracks.indexOf(track.id) + 1
			};
			menu = PopUpMenu(view, Rect(150, i * 30 + 10, 40, 25))
				.items_(assignitems)
				.font_(font)
				.stringColor_(Color.grey(0.8))
				.value_(aindex)
				.action_({|menu|
					var remove;
					if (menu.value > 0) {
						remove = guiItems.select({|it| (it.menu != menu) and: {it.menu.value == menu.value} });
						if ( remove.notEmpty ) {
							remove.first.menu.value = 0;
						};
						assignedTracks[menu.value-1] = track.id;
						this.mapnano;
					}
				});
			(text: text, menu: menu)
		})
	}
	
	mapnano{
		assignedTracks.collect({|id| openTracks.select({|it| it.id == id }).first }).do({|track, i|
			nano.sliders[0][i*2].action = {|slider| track.ampslider.value = slider.value; track.ampslider.doAction };
			nano.sliders[0][i*2+1].action = {|slider| track.cueslider.value = slider.value; track.cueslider.doAction };
			nano.knobs[0][i*2].action = {|knob| track.warpslider.value = knob.value; track.warpslider.doAction };
			nano.buttons[0][i*2+15].action = {|button| track.play.value = button.value; track.play.doAction  };
		})
	}
	
}