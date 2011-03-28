Decoder{
	
	var isLocal, isUHJ, azimuth, elevation, <bus, <synth, isRunning = false;
	
	*new{|isLocal = true, isUHJ = false, speakersAzimuth, speakersElevation|
		^super.newCopyArgs(isLocal, isUHJ, speakersAzimuth, speakersElevation).init
	}
	
	init{
		bus = Bus.audio(Server.default, 4);
		azimuth = azimuth ? ([-0.25, -0.75, 0.75, 0.25] * pi);
		elevation = elevation ? (0 ! 4);
		this.addSynthDefs;
	}
	
	addSynthDefs{
		SynthDef(\bf2decode, {|bus, amp = 1|
			var w, x, y, z;
			#w, x, y, z = Limiter.ar(In.ar(bus, 4), 0.99);
			Out.ar( 0, BFDecode1.ar(w, x, y, z, azimuth, elevation ) * amp )
		}).add;
		
		SynthDef(\bfcollect, {|bus, amp = 1|
			Out.ar(0, Limiter.ar(In.ar(bus, 4), 0.99) * amp)
		}).add;
		
		SynthDef(\btoUHJ, {|bus, amp = 1|
			var w, x, y, z;
			#w, x, y, z = In.ar(bus, 4);
			#w, x, y, z = Limiter.ar([w, x, y, z], 0.99);
			Out.ar(0, B2UHJ.ar(w, x, y) * amp)
		}).add;
	}
	
	start{|target = 1, addAction=\addToHead|
		var defname;
		
		if (isRunning.not)
		{
			if (isUHJ)
			{ defname = \btoUHJ }
			{
				if (isLocal) { defname = \bf2decode } { defname = \bfcollect }
			};
			synth = Synth(defname, [\bus, bus], target, addAction);
			isRunning = true;
		}
		{
			"Decoder is already running!".inform
		}
	}
	
	set{|param, value|
		synth.set(param, value)
	}
	
	free{
		synth.free;
		isRunning = false
	}
	
}

