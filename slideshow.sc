SlideShow{
	var paths, <window, index = 0;
	*new{|paths|
		^super.newCopyArgs(paths)
	}
		
	makeWindow{|func|
		window = Window("granular synthesis", Rect(0, 0, 1440, 900), border: false).front.fullScreen;
		window.view.backgroundImage_(Image(paths[index]), 10);
		RoundButton(window, Rect(1400, 5, 40, 20))
			.states_([["x", Color.red, Color.clear]])
			.action_({ window.close });
	
		window.view.keyDownAction_({|vw, ch, md, uni, key|
			key.switch(
				49, { this.next },
				51, { this.prev },
				124, { this.next },
				123, { this.prev }
			)
		});
		
		func.(window)
	}
	
	next{
		if (index < paths.size)
		{
		 	index = index + 1;
		 	window.view.backgroundImage_(Image(paths[index]), 10)
		}		
	}
	
	prev{
		if (index > 0)
		{
		 	index = index - 1;
		 	window.view.backgroundImage_(Image(paths[index]), 10)
		}
	}
	
	closeWindow{
		window.close
	}
	
}