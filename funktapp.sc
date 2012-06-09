FunktApp{
		
	var <screenX, <screenY, <fps, <scAddr, <ciAddr, <mode, <appPath;
	var args, oscPrefix = "/funkt/", patternLib, settings, symmetry;

	*new{|screenX=800, screenY=600, fps=32, scAddr, ciAddr, mode=0, path, numPatterns=20|
		^super.newCopyArgs(screenX, screenY, fps, scAddr, ciAddr, mode, path).init(numPatterns);
	}
	
	init{|numPatterns|
		scAddr = scAddr ? NetAddr(NetAddr.localAddr.hostname, NetAddr.localAddr.port);
		ciAddr = ciAddr ? NetAddr("127.0.0.1", 7000);
		appPath = appPath ? "~/Development/funkt/cinder/xcode/build/Release/funktApp.app";
		patternLib = Array.fill(numPatterns, {|i|
			(index: i, active: 0, alpha: 0.0, alphamap: 0, colormap: 0)
		});
//		symmetry = ('ZERO': 0, 'AX': 1, 'AY': 2, 'DIAG': 3, 'QUAD': 4);
// 		interp = ( 'NONE': 0, 'LINEAR': 1, 'COSINE': 2 ); // interp count
		settings = ('rule': 0, 'add': 0.005, 'symmetry': 4, 'interp': [0, 1], 'background': (0.0 ! 3));
	}
		
	makeArgumentString{
		var str = "";
		
		args = (
			'-screenx': screenX, 
			'-screeny': screenY, 
			'-fps': fps, 
			'-remote': scAddr.hostname, 
			'-outport': scAddr.port, 
			'-inport': ciAddr.port, 
			'-wmode': mode 
		);		

		args.keysValuesDo({|key, val|
			str = (str + key.asString + val.asString)
		});
		
		^("--args" ++ str)
	}
	
	open{|includeArgs=true|
		if (includeArgs) {
			("open" + appPath + this.makeArgumentString).unixCmd
		}
		{
			("open" + appPath).unixCmd
		}
	}
	
	start{|argMode|
		mode = argMode ? 0;
		this.open;
	}
	
	initWorld{|sizeX, sizeY, vectorSize, tdur, lrate|
		this.sendMsg("init", sizeX, sizeY, vectorSize, tdur, lrate)
	}
	
	setPattern{|index, active, alpha, alphamap, colormap|
		patternLib[index].active = active ? patternLib[index].active;
		patternLib[index].alpha = alpha ? patternLib[index].alpha;
		patternLib[index].alphamap = alphamap ? patternLib[index].alphamap;
		patternLib[index].colormap = colormap ? patternLib[index].colormap;
		this.sendPattern(index);
	}
	
	setPatternArg{|index, argname, value, send=true|
		patternLib[index][argname] = value;
		if (send) { this.sendPattern(index) }
	}
	
	sendPattern{|index|
		this.sendMsg("pattern", index, *this.collectPatternArgs(index))
	}
	
	sendPresetRule{|name|
		var rule;
		rule = Generations.rules[name];
		this.sendMsg("births", *rule[0]);
		this.sendMsg("survivals", *rule[1]);
		this.sendMsg("states", *rule[2]);
	}
	
	changeSetting{|name, value, send=true|
		settings[name] = value;
		if (send) { this.sendSetting(name) }
	}
		
	sendSetting{|name|
		if (settings[name].isKindOf(Array)) {
			this.sendMsg(name.asString, *settings[name])
		}
		{
			this.sendMsg(name.asString, settings[name])
		}
	}
	
	sendWeights{| ... weights|
		this.sendMsg("weights", *weights)
	}
	
	collectPatternArgs{|index|
		^(#[active, alpha, alphamap, colormap]).collect(patternLib[index][_])
	}
	
	quit{
		this.sendMsg("quit")
	}
	
	sendMsg{|cmd ... msg|
		ciAddr.sendMsg(oscPrefix++cmd, *msg)
	}
	
	changeOscPrefix{|prefix| oscPrefix = prefix }
	
}