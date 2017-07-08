SlideShow{
	var paths, width, height, title, <window, <index = 0, <>bookmarks, current;
	*new{|paths, width, height, title="..::.."|
		^super.newCopyArgs(paths, width, height, title)
	}

	makeWindow{|func|
		window = Window(title, Rect(0, 0, width, height), border: true).front;
		window.view.backgroundImage_(Image(paths[index]).setSize(width, height, \keepAspectRatio));
		// RoundButton(window, Rect(1400, 5, 40, 20))
		// .states_([["x", Color.red, Color.clear]])
		// .action_({ window.close });

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

	setPage{|number| index = number }

	goToBookmark{|name|
		if (bookmarks.notNil)
		{
			index = bookmarks[name];
			this.makeWindow
		}
	}

	next{
		if (index < paths.size)
		{
		 	index = index + 1;
			window.view.backgroundImage_(Image(paths[index]).width_(width).height_(height))
		}
	}

	prev{
		if (index > 0)
		{
		 	index = index - 1;
			window.view.backgroundImage_(Image(paths[index]))
		}
	}

	closeWindow{
		window.close
	}

}