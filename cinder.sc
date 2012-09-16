CinderApp{
		
	var <screenX, <screenY, <fps, <scAddr, <ciAddr, <mode, <appPath;
	var args, oscPrefix = "/lambda/", patternLib, settings, symmetry;
	var <queryFunc, <world;

	*new{|screenX=800, screenY=600, fps=32, scAddr, ciAddr, mode=0, path, numPatterns=20|
		^super.newCopyArgs(screenX, screenY, fps, scAddr, ciAddr, mode, path).init(numPatterns);
	}
	
	init{|numPatterns|
		scAddr = scAddr ? NetAddr(NetAddr.localAddr.hostname, NetAddr.localAddr.port);
		ciAddr = ciAddr ? NetAddr("127.0.0.1", 7000);
		appPath = appPath ? "~/Development/lambda/xcode/build/Release/lambda.app";
		patternLib = Array.fill(numPatterns, {|i|
			(index: i, active: 0, alpha: 0.0, alphamap: 0, colormap: 0, r: 0.0, g: 0.0, b: 0.0)
		});
// 		rule = ('CONT', 'LIFE', 'GEN');
// 		interp = ( 'NONE': 0, 'LINEAR': 1, 'COSINE': 2 ); // interp count
		settings = ('rule': 0, 'add': 0.005, 'symmetry': 4, 'interp': [0, 1], 'background': (0.0 ! 3));
		world = (sizeX: 0, sizeY: 0, sizeZ: 0, vectorSize: 0);
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
		world.sizeX = sizeX;
		world.sizeY = sizeY;
		world.sizeZ = sizeZ;
		world.vectorSize = vectorSize;
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
//		this.setWeights(weights);		
	}
		
	setWeights{|weights|
		if (weights.isNil) {
			weights = (1.0 ! 26);
		};
		weights = weights.collect(_.asFloat);
		this.sendMsg("world/rule/weights", *weights)
	}	
	
	setAdd{|value|
		this.sendMsg("world/rule/add", value.asFloat)
	}
		
	initLife{|births, survivals|
		this.sendMsg("world/rule/init", 1);
		this.sendLifeRule(births, survivals)
	}
	
	sendPredefinedLifeRule{|name|
		var pre = Life.rules[name];
		this.sendLifeRule(pre[0], pre[1]);
	}
	
	sendLifeRule{|births, survivals|
		this.sendMsg("world/rule/births", *(births ? [2]));
		this.sendMsg("world/rule/survivals", *(survivals ? [2, 3]));
	}
	
	initGenerations{|births, survivals, states|
		this.sendMsg("world/rule/init", 2);
		this.sendGenRule(births, survivals, states)
	}
	
	sendPredefinedGenRule{|name|
		var pre = Generations.rules[name];
		this.sendGenRule(pre[0], pre[1], pre[2]);
	}
	
	sendGenRule{|births, survivals, states|
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
	
	moveCamera{|start, end, time, rate, donefunc|
	
		Tdef(\move, {
			
			var num, steps, incr, value;
			steps = time * rate;
			incr = end - start / steps;
			value = start;
			steps.do({
				this.setViewpoint(*value);
				value = value + incr;
				rate.reciprocal.wait
			});		
			donefunc.();
			
		}).play;

	}
	
	stopMove{
		Tdef(\move).stop
	}
	
	rotateCamera{
		Tdef(\rotate).play
	}
	
	stopRotation{
		Tdef(\rotate).stop
	}
	
	setCameraRotation{|rotation, lx, ly, lz, addx, addy, addz, msgrate=10|
				
		Tdef(\rotate, {
			loop({
				this.setViewpoint(rotation.x + addx, rotation.y + addy, rotation.z + addz, lx, ly, lz);
				msgrate.reciprocal.wait;
				rotation.update;
			})
		});		
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
	
	activateSwarm{|size, x, y, z, speed, cohesion, separation, alignment, center|
		this.sendMsg("boids/init", size, x, y, z, speed, cohesion, separation, alignment, center)
	}
	
	setSwarm{|speed, cohesion, separation, alignment, center|
		this.sendMsg("boids/set", speed, cohesion, separation, alignment, center)
	}
	
	killSwarm{
		this.sendMsg("boids/kill")
	}
	
	setBoidCam{|attachEye=false, lookAtCentroid=false|
		this.sendMsg("graphics/boidcam", attachEye.asInt, lookAtCentroid.asInt)
	}
	
	sendCodeLine{|line|
		this.sendMsg("livecode/codeline", line)
	}
	
	showCodePanel{ this.sendMsg("livecode/activate", 1) }

	hideCodePanel{ this.sendMsg("livecode/activate", 0) }
	
	setSymmetry{|sym|
		this.sendMsg("world/symmetry", sym.asInt ? 0)
	}
	
	quit{
		this.sendMsg("quit")
	}
	
	sendMsg{|cmd ... msg|
		ciAddr.sendMsg(oscPrefix++cmd, *msg)
	}
	
	changeOscPrefix{|prefix| oscPrefix = prefix }
		
}

Rotation{
	
	var <>rhoRate, <>rhoMin, <>rhoRange, <>thetaRate, <>phiRate;
	var spherical, rho;
	
	*new{|initRho, rhoRate, rhoMin, rhoRange, initTheta, thetaRate, initPhi, phiRate|
		^super.newCopyArgs(rhoRate, rhoMin, rhoRange, thetaRate, phiRate).init(initRho, initTheta, initPhi)
	}
	
	init{|initRho, initTheta, initPhi|
		spherical = Spherical(initRho, initTheta, initPhi);
		rho = 0;
	}
	
	update{
		rho = (rho + rhoRate).wrap(0, 2pi);
		spherical.rho = rhoMin+(sin(rho)*rhoRange);
		spherical.theta = (spherical.theta+thetaRate).wrap(0, 2pi);
		spherical.phi = (spherical.phi+phiRate).wrap(0, 2pi);
	}
	
	x{ ^spherical.x }	
	y{ ^spherical.y }	
	z{ ^spherical.z }		
	
}

QueryStates{

	*sides2D{|size, num|
		var arr = Array();
		var arrA, arrB;
		
		arrA = Array();
		arrB = Array();
		
		forBy(1, size-1, size/num, {|j|
			forBy(1, size-1, size/num, {|k|
				arr = arr.add([0, j, k])
			})
		});
		
		forBy(1, size-1, size/num, {|i|
			forBy(1, size-1, size/num, {|j|
				arrA = arrA.add([i, j, 0])
			})
		});
		
		forBy(1, size-1, size/num, {|i|
			forBy(1, size-1, size/num, {|j|
				arrB = arrB.add([i, j, size-1])
			})
		});
		
		arr = arr.addAll([arrA, arrB].lace(arrA.size + arrB.size) );
		
		forBy(1, size-1, size/num, {|j|
			forBy(1, size-1, size/num, {|k|
				arr = arr.add([size-1, j, k])
			})
		});
		
		^arr.collect({|arr| this.indicesAsInt(arr, size, size) })
		
	}
	
	*sheet2D{|size, index|
		var arr = Array.fill(size, {|x| Array.fill(size, {|y| [x, y, index] }) });
		^arr.collect({|arr| this.indicesAsInt(arr, size, size) })
	}
	
	*uniform3D{|size, num|
		var arr = Array();
		
		forBy(size/num/2, size-1, size/num, {|i|
			forBy(size/num/2, size-1, size/num, {|j|
				forBy(size/num/2, size-1, size/num, {|k|
					arr = arr.add([i, j, k])
				})
			})
		});	
		
		^arr.collect({|arr| this.indicesAsInt(arr, size, size) })
	}
	
	*core3D{|radius, size|
		var arr = Array();
		
		forBy(size/2-1-radius, size/2+radius, 1, {|i| 
			forBy(size/2-1-radius, size/2+radius, 1, {|j|
				forBy(size/2-1-radius, size/2+radius, 1, {|k| 
					arr=arr.add([i,j,k])
				})
			})
		});	
		
		^arr.collect({|arr| this.indicesAsInt(arr, size, size) })
	}
	
	*indicesAsInt{|coords, sizex, sizey|
		^coords[0]*sizex+coords[1]*sizey+coords[2]
	}

	
}