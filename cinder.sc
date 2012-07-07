CinderApp{
		
	var <screenX, <screenY, <fps, <scAddr, <ciAddr, <mode, <appPath;
	var args, oscPrefix = "/lambda/", patternLib, settings, symmetry;
	var <queryFunc;

	*new{|screenX=800, screenY=600, fps=32, scAddr, ciAddr, mode=0, path, numPatterns=20|
		^super.newCopyArgs(screenX, screenY, fps, scAddr, ciAddr, mode, path).init(numPatterns);
	}
	
	init{|numPatterns|
		scAddr = scAddr ? NetAddr(NetAddr.localAddr.hostname, NetAddr.localAddr.port);
		ciAddr = ciAddr ? NetAddr("127.0.0.1", 7000);
		appPath = appPath ? "~/Development/lambda/xcode/build/Release/lambdaApp.app";
		patternLib = Array.fill(numPatterns, {|i|
			(index: i, active: 0, alpha: 0.0, alphamap: 0, colormap: 0, r: 0.0, g: 0.0, b: 0.0)
		});
// 		rule = ('CONT', 'LIFE', 'GEN');
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
	
	initWorld{|sizeX, sizeY, sizeZ, vectorSize|
		this.sendMsg("world/init", sizeX.asInt, sizeY.asInt, sizeZ.asInt, vectorSize.asInt)
	}
	
	resetWorld{|left, bottom, front, width, height, depth, random=false, includeAllStates=false, aliveRatio|
		if (random) {
			this.sendMsg("world/reset/rand", left, bottom, front, width, height, depth, aliveRatio, includeAllStates)
		}
		{
			this.sendMsg("world/reset/wirecube", left, bottom, front, width, height, depth)
		}
	}
		
	initContinuous{|weights|
		this.sendMsg("world/rule/init", 0);
		this.setWeights(weights)
	}
	
	setWeights{|weights|
		this.sendMsg("world/rule/weights", *(weights ? (1.0 ! 26)))
	}	
	
	setAdd{|value|
		this.sendMsg("world/rule/add", value.asFloat)
	}
	
	initLife{|births, survivals|
		this.sendMsg("world/rule/init", 1);
		this.sendMsg("world/rule/births", *(births ? [2]));
		this.sendMsg("world/rule/survivals", *(survivals ? [2, 3]));
	}
	
	initGenerations{|births, survivals, states|
		this.sendMsg("world/rule/init", 2);
		this.sendMsg("world/rule/births", *(births ? [2]));
		this.sendMsg("world/rule/survivals", *(survivals ? [2, 3]));
		this.sendMsg("world/rule/states", states ? 6);
	}
		
	setPattern{|index, active, alpha, alphamap, colormap, red, green, blue|
		patternLib[index].active = active ? patternLib[index].active;
		patternLib[index].alpha = alpha ? patternLib[index].alpha;
		patternLib[index].alphamap = alphamap ? patternLib[index].alphamap;
		patternLib[index].colormap = colormap ? patternLib[index].colormap;
		patternLib[index].r = red ? patternLib[index].r;
		patternLib[index].g = green ? patternLib[index].g;
		patternLib[index].b = blue ? patternLib[index].b;
		
		this.sendPattern(index);
	}
	
	setPatternArg{|index, argname, value, send=true|
		patternLib[index][argname] = value;
		if (send) { this.sendPattern(index) }
	}
	
	sendPattern{|index|
		this.sendMsg("graphics/pattern", index.asInt, *this.collectPatternArgs(index))
	}
	
	sendSOMVector{|vector|
		vector = vector.collect(_.asFloat);
		this.sendMsg("world/somvector", *vector)
	}
		
	collectPatternArgs{|index|
		var types;
		types = #[i,f,i,i,f,f,f];
		^(#[active, alpha, colormap, alphamap, r, g, b]).collect({|name, i|
			var value;
			value = patternLib[index][name];
			if (types[i] == \i) {
				value.asInt
			}
			{
				value.asFloat
			}
		})
	}
	
	setInterpolation{|type, length|
		this.sendMsg("world/interpl", type.asInt, length.asInt)
	}
	
	rotate{|rotx, roty, rotz, angle|
		this.sendMsg("graphics/rotate", rotx.asFloat, roty.asFloat, rotz.asFloat, angle.asFloat)
	}
	
	setViewpoint{|eyex, eyey, eyez, ctrx, ctry, ctrz|
		this.sendMsg("graphics/view", eyex.asFloat, eyey.asFloat, eyez.asFloat, 
			ctrx.asFloat, ctry.asFloat, ctrz.asFloat)
	}
	
	setBackground{|red, green, blue|
		this.sendMsg("graphics/background", red.asFloat, green.asFloat, blue.asFloat)
	}
	
	queryStates{|indexArray, oscFunc|
		this.sendMsg("world/query/states", *indexArray);
		queryFunc = OSCFunc({|ms|
			oscFunc.(ms)
		}, "/lambda/world/states")
	}
	
	stopQuery{
		queryFunc.disable;
		queryFunc = nil;
		this.sendMsg("world/query/stop");
	}
	
	quit{
		this.sendMsg("quit")
	}
	
	sendMsg{|cmd ... msg|
		ciAddr.sendMsg(oscPrefix++cmd, *msg)
	}
	
	changeOscPrefix{|prefix| oscPrefix = prefix }
		
}