IPhoneConnect{
	
	var iwin, hasConnection, status, osctest;

	*new{
		^super.new.init
	}
	
	init{
		iwin = Window("iphone connect", Rect(500, 500, 200, 300))
			.background_(Color.grey(0.2)).front;
		hasConnection = false;
		Button(iwin, Rect(10, 10, 180, 180))
			.states_([
				["start", Color.grey(0.4), Color.grey(0.3)], 
				["stop", Color.green, Color.grey(0.8)]
			])
			.action_({|btn|
				if (btn.value == 1) {
					osctest = OSCFunc({
						hasConnection = true;
					}, '/accxyz');
					Tdef(\osctdef, {
						loop({
							if (hasConnection) {
								{
								status.string_("active");
								status.stringColor_(Color.green);
								}.defer;
								hasConnection = false;
							}
							{
								{
								status.string_("no connection");
								status.stringColor_(Color.red);
								}.defer
							};
							0.2.wait
						})
					}).play
					
				}
				{
					osctest.disable;
					osctest = nil;
					Tdef(\osctdef).clear;
					status.string_("inactive");
					status.stringColor_(Color.grey(0.8));
				}
			});
			
		StaticText(iwin, Rect(10, 200, 80, 30))
			.font_(Font("Inconsolata", 12))
			.string_("Status: ")
			.stringColor_(Color.grey(0.8));
			
		status = StaticText(iwin, Rect(60, 200, 80, 30))
			.font_(Font("Inconsolata", 12))
			.string_("inactive")
			.stringColor_(Color.grey(0.8));		
	}
	
}