TimeDisplay {
	var <>view, wn, rout, <>hrstr="00", <>minstr="00", <>secstr="00", <>timestr, <>tsec=0, stop=1;
	var font, begin;
	*new{|win, rect, start=0, font|
		var new, r, w;
		r = rect ? Rect(10, 10, 100, 40);
		w = if (win.isNil, {SCWindow(":time:", Rect(50, 300, 120, 60)).front}, {win});
		^super.new.init(w, r, start, font);
	}
	
	init{|win, rect, start, font|
		wn = win;
		font = font ? Font("Helvetica", 10);
		view = SCStaticText(win, rect);
		view.background_(Color.black);
		view.align_(\center);
		view.font_(font);
		view.stringColor_(Color.green);
		timestr = hrstr++":"++minstr++":"++secstr;
		view.string_(timestr);
	}
	
	start{|start = 0|
		stop = 1;
		begin = SystemClock.seconds.round(1.0);
	 	rout = Routine({
				var hr, min, sec, mstr, sstr, hstr;
				AppClock.sched(0.0, {
					tsec = SystemClock.seconds.round(1.0) + start - begin;
					hr = (tsec/3600).floor;
					min = (tsec/60%60).floor;
    					sec = tsec%60;
					if (sec < 10, {sstr = "0"++sec.asString}, {sstr = sec.asString});
					if (min < 10, {mstr = "0"++min.asString}, {mstr = min.asString});
					if (hr < 10, {hstr = "0"++hr.asString}, {hstr = hr.asString});
					timestr = hstr++":"++mstr++":"++sstr;
					view.string_(timestr);
					stop
				});

			}).play;
	}
	
	stop{
		stop = nil;
		rout.stop;
		rout = nil;
	}
		
	background_{|color|
		view.background_(color)
	}
	
	stringColor_{|color|
		view.stringColor_(color)
	}
	
	font{|newFont|
		view.font = newFont
	}
	
	close{
		wn.close	
	}

}