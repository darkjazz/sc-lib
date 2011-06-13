MikroGraphics{
	
	var <width, <height, <sizeX, <sizeY, <frameRate, <remoteAddr, <vectorSize, <trainDur, <lRate;
	var debugMode, <bmu, bmuResponder, bmuResponderFunctions, statesResponder, statesResponderFunctions;
	var <patternLib, <numPatterns = 9, <settings, <states;
	var settingOrder;
	
	var oscPrefix = "/mikro/";
	var appPath = "/Users/alo/Development/mikro/visual/build/Release/mikro";
	
	*new{|width, height, sizeX, sizeY, frameRate, remoteAddr, vectorSize, trainDur, lRate|
		^super.newCopyArgs(width, height, sizeX, sizeY, frameRate, remoteAddr, 
			vectorSize, trainDur, lRate).init
	}
	
	init{
		remoteAddr = remoteAddr ? NetAddr("127.0.0.1", 7770);
		bmu = (x: 0, y: 0, state: 0, vector: (0!vectorSize) );
				
		bmuResponderFunctions = Event();
		
		statesResponderFunctions = Event();
		
		bmuResponder = OSCresponderNode(nil, '/mikro/bmu', {|ti, re, ms| 
			var values;
			if (ms.isKindOf(ArrayedCollection)) {
				if (ms[1].isKindOf(Int8Array)) {
					values = ms[1].asFloatArray;
					bmu.x = values[0].asInt;
					bmu.y = values[1].asInt;
					bmu.state = values[2];
					bmu.vector = values[3..(vectorSize+2)];
					bmuResponderFunctions.do(_.value(bmu))
				}
			}
		}).add;
		
		statesResponder = OSCresponderNode(nil, '/mikro/states', {|ti, re, ms|
			if (ms.isKindOf(ArrayedCollection)) {
				if (ms[1].isKindOf(Int8Array)) {
					states = ms[1].asFloatArray;
					statesResponderFunctions.do(_.value(states))
				}
			}
		}).add;
		
		patternLib = Array.fill(numPatterns, {
			(active: 0.0, alpha: 0.0)
		});
		
		settings = (
			add: 0.995,
			bgred: 0,
			bggreen: 0,
			bgblue: 0,
			transx: 0,
			transy: 0,
			transz: 0
		);
		
		settingOrder = [\add, \bgred, \bggreen, \bgblue, \transx, \transy, \transz];
				
	}
		
	putBmuFunction{|key, func|
		bmuResponderFunctions[key] = func;
	}
	
	removeBmuFunction{|key|
		bmuResponderFunctions[key] = nil;
	}
	
	putStatesFunction{|key, func|
		statesResponderFunctions[key] = func
	}
	
	removeStatesFunction{|key|
		statesResponderFunctions[key] = nil
	}
	
	clear{
		bmuResponder.remove;
		bmuResponderFunctions.clear;
		statesResponder.remove;
		statesResponderFunctions.clear;
	}
	
	start{|mode = 1|
		debugMode = mode;
		(appPath + this.makeArgumentString).unixCmd;
	}
	
	quit{ this.sendMsg("quit", 0) }
	
	sendMsg{|cmd ... msg| remoteAddr.sendMsg(oscPrefix ++ cmd, *msg) }
	
	sendWeights{| ... weights| this.sendMsg("weights", *weights) }
	
	sendPattern{|index, active, alpha| this.sendMsg("pattern", index, active, alpha) }
	
	sendSettings{
		this.sendMsg("settings", *this.collectSettings )
	}
	
	collectSettings{ ^settingOrder.collect({|key| settings[key] }) }
	
	transformSettings{|endValues, time|
		Routine({
			var steps, wait, incr;
			steps = time * frameRate;
			incr = endValues - this.collectSettings / steps;
			steps.do({
				settingOrder.do({|key, i|
					settings[key] = settings[key] + incr[i];
				});
				this.sendSettings;
				wait.wait;
			})
		}).play
	}
	
	sendSetting{|key, value|  
		settings[key] = value;
		this.sendSettings;
	}
	
	fadeInPattern{|index, maxAlpha, time|
		Routine({
			var steps, wait, incr;
			this.sendPattern(index, 1.0, 0.0);
			patternLib[index].active = 1.0;
			steps = time * frameRate;
			wait = frameRate.reciprocal;
			incr = maxAlpha / steps;
			wait.wait;
			steps.do({
				patternLib[index].alpha = patternLib[index].alpha + incr;
				this.sendPattern(index, 1.0, patternLib[index].alpha);
				wait.wait;
			})
			
		}).play
	}
	
	fadeOutPattern{|index, time|
		Routine({
			var steps, wait, incr;
			steps = time * frameRate;
			wait = frameRate.reciprocal;
			incr = (patternLib[index].alpha / steps).neg;
			wait.wait;
			steps.do({
				patternLib[index].alpha = patternLib[index].alpha + incr;
				this.sendPattern(index, 1.0, patternLib[index].alpha);
				wait.wait;
			});
			this.sendPattern(index, 0.0, 0.0);
			
		}).play		
	}
	
	makeArgumentString{
		var str = "";

		if (debugMode.notNil) { str = str + "-d" + debugMode.asString };
		if (width.notNil) { str = str + "-w" + width.asString };
		if (height.notNil) { str = str + "-h" + height.asString };
		if (frameRate.notNil) { str = str + "-f" + frameRate.asString };
		if (sizeX.notNil) { str = str + "-x" + sizeX.asString };
		if (sizeY.notNil) { str = str + "-y" + sizeY.asString };
		str = str + "-i" + NetAddr.localAddr.hostname;
		str = str + "-p" + NetAddr.localAddr.port;
		if (vectorSize.notNil) { str = str + "-s" + vectorSize.asString };
		if (trainDur.notNil) { str = str + "-t" + trainDur.asString };
		if (lRate.notNil) { str = str + "-l" + lRate.asString };
		
		^str
	}
	
}