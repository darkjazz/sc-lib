CinderApp{

	var <screenX, <screenY, <fps, <scAddr, <ciAddr, <mode, <appPath, <fullScreen;
	var args, oscPrefix = "/lambda/", <patternLib, <boidPatternLib, settings, symmetry;
	var <queryFunc, <world, <oscdefs;

	*new{|screenX=800, screenY=600, fps=32, scAddr, ciAddr, mode=0, path, fullScreen=false, numPatterns=40, numBoidPatterns=7|
		^super.newCopyArgs(screenX, screenY, fps, scAddr, ciAddr, mode, path, fullScreen).init(numPatterns, numBoidPatterns);
	}

	init{|numPatterns, numBoidPatterns|
		scAddr = scAddr ? NetAddr(NetAddr.localAddr.hostname, NetAddr.localAddr.port);
		ciAddr = ciAddr ? NetAddr("127.0.0.1", 7000);
		appPath = appPath ? "/Applications/lambda.app";
		patternLib = Array.fill(numPatterns, {|i|
			(index: i, active: 0, alpha: 0.0, alphamap: 0, colormap: 0, r: 0.0, g: 0.0, b: 0.0)
		});
		boidPatternLib = Array.fill(numBoidPatterns, {|i|
			(index: i, active: 0, mapIndex: 0)
		});
// 		rule = ('CONT', 'LIFE', 'GEN');
// 		interp = ( 'NONE': 0, 'LINEAR': 1, 'COSINE': 2 ); // interp count
		settings = ('rule': 0, 'add': 0.005, 'symmetry': 4, 'interp': [0, 1], 'background': (0.0 ! 3));
		world = (sizeX: 0, sizeY: 0, sizeZ: 0, vectorSize: 0);
		oscdefs = [];
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
			'-wmode': mode,
			'-full': fullScreen.asInteger
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

	openNix{|includeArgs=true|
		var argstr = "";
		if (includeArgs) { argstr = this.makeArgumentString };
		(appPath + argstr).unixCmd
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
		this.sendMsg("world/init", sizeX.asInteger, sizeY.asInteger, sizeZ.asInteger, vectorSize.asInteger)
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

	sendPredefined3DRule{|name|
		var pre = Generations.rules[name];
		this.sendGenRule(pre[0]*2, pre[1]*2, pre[2]);
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

	fadeInPattern{|index, time, alpha|
		Routine({
			var steps, incr, value = 0.0;
			steps = time / 0.1;
			incr = alpha / steps;
			patternLib[index].active = 1;
			patternLib[index].alpha = 0.0;
			steps.do({
				this.sendPattern(index);
				value = value + incr;
				patternLib[index].alpha = value;
				0.1.wait;
			});
			patternLib[index].alpha = alpha;
			this.sendPattern(index);
			Post << "Pattern " << index << " fade in complete" << Char.nl;
		}).play
	}

	fadeOutPattern{|index, time|
		Routine({
			var steps, incr, value;
			steps = time / 0.1;
			incr = patternLib[index].alpha / steps;
			value = patternLib[index].alpha;
			steps.do({
				this.sendPattern(index);
				value = value - incr;
				patternLib[index].alpha = value;
				0.1.wait;
			});
			patternLib[index].alpha = 0.0;
			patternLib[index].active = 0;
			this.sendPattern(index);
			Post << "Pattern " << index << " fade out complete" << Char.nl;
		}).play
	}

	xfadePatterns{|fadeInIndex, fadeInAlpha, fadeOutIndex, time|
		Routine({
			var steps, incrIn, incrOut, valueIn = 0.0, valueOut;
			steps = time / 0.1;
			incrIn = fadeInAlpha / steps;
			incrOut = patternLib[fadeOutIndex].alpha / steps;
			patternLib[fadeInIndex].active = 1;
			patternLib[fadeInIndex].alpha = 0.0;
			valueOut = patternLib[fadeOutIndex].alpha;
			steps.do({
				this.sendPattern(fadeInIndex);
				valueIn = valueIn + incrIn;
				patternLib[fadeInIndex].alpha = valueIn;
				0.05.wait;
				this.sendPattern(fadeOutIndex);
				valueOut = valueOut - incrOut;
				patternLib[fadeOutIndex].alpha = valueOut;
				0.05.wait;
			});
			patternLib[fadeInIndex].alpha = fadeInAlpha;
			this.sendPattern(fadeInIndex);
			0.05.wait;
			patternLib[fadeOutIndex].alpha = 0.0;
			patternLib[fadeOutIndex].active = 0;
			this.sendPattern(fadeOutIndex);

			Post << "Xfade from " << fadeOutIndex << " to " << fadeInIndex << " complete" << Char.nl;
		}).play

	}

	setPatternArg{|index, argname, value, send=true|
		patternLib[index][argname] = value;
		if (send) { this.sendPattern(index) }
	}

	sendPattern{|index|
		this.sendMsg("graphics/pattern", index.asInteger, *this.collectPatternArgs(index))
	}

	sendBoidPattern{|index, active, mapIndex|
		boidPatternLib[index].active = active;
		boidPatternLib[index].mapIndex = mapIndex;
		this.sendMsg("graphics/boidpattern", index.asInteger, active.asInteger, mapIndex.asInteger)
	}

	sendSOMVector{|vector|
		vector = vector.collect(_.asFloat);
		this.sendMsg("world/somvector", *vector)
	}

	// support an older version of the graphics app messaging
	sendWeights{| ... vector|
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
				value.asInteger
			}
			{
				value.asFloat
			}
		})
	}

	setInterpolation{|type, length|
		this.sendMsg("world/interpl", type.asInteger, length.asInteger)
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
		var key = 'states';
		this.sendMsg("world/query/states", *indexArray);
		OSCdef(key, {|ms|
			oscFunc.(ms)
		}, "/lambda/world/states");
		oscdefs = oscdefs.add(key);
	}

	queryGenStates{|indexArray, oscFunc|
		var key = 'states';
		this.sendMsg("world/query/states", *indexArray);
		OSCdef(key, {|ms|
			oscFunc.(ms)
		}, "/lambda/world/faderstates");
		oscdefs = oscdefs.add(key);
	}

	queryAliveStates{|oscFunc|
		var key = 'alive';
		this.sendMsg("world/query/alive");
		OSCdef(key, {|ms|
			oscFunc.(ms)
		}, "/lambda/world/alive");
		oscdefs = oscdefs.add(key);
	}

	queryCoordsByState{|minState=1, maxState=1, oscFunc|
		var key = 'coords';
		this.sendMsg("world/query/coords", minState, maxState);
		OSCdef(key, {|ms|
			oscFunc.(ms)
		}, "/lambda/world/coords");
		oscdefs = oscdefs.add(key);
	}

	unpackCoords{|ms|
		var coords;
		coords = ms.drop(1).clump(2).collect({|tup|
			var enc, state, x, y, z;
			enc = tup[0];
			state = tup[1];
			x = (enc/pow(~size, 2)).asInteger;
			y = (enc/~size).asInteger%~size;
			z = enc%~size;
			('x': x, 'y': y, 'z': z, 'state': state)
		});
		^coords
	}

	stopQuery{
		queryFunc.disable;
		queryFunc = nil;
		this.sendMsg("world/query/stop");
	}

	clearQueryDef{|key|
		OSCdef(key).clear;
		OSCdef(key).free;
	}

	putStatesFunction{|id, func|

	}

	removeStatesFunction{|id|

	}

	removeBmuFunction{|id|

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
		this.sendMsg("graphics/boidcam", attachEye.asInteger, lookAtCentroid.asInteger)
	}

	sendCodeLine{|line|
		this.sendMsg("livecode/codeline", line)
	}

	sendCodeTitle{|title|
		this.sendMsg("livecode/codetitle", title.asString)
	}

	showCodePanel{ this.sendMsg("livecode/activate", 1) }

	hideCodePanel{ this.sendMsg("livecode/activate", 0) }

	mapCodePanel{ this.sendMsg("livecode/map", 1) }

	unmapCodePanel{ this.sendMsg("livecode/map", 0) }

	setCodePanelFont{|name, size|
		this.sendMsg("livecode/font", name.asString, size.asInteger)
	}

	setCodePanelColor{|red, green, blue|
		this.sendMsg("livecode/fontcolor", red.asFloat, green.asFloat, blue.asFloat)
	}

	mapImage{ this.sendMsg("livecode/mapimage", 1) }

	unmapImage{ this.sendMsg("livecode/mapimage", 0) }

	setCodePanelFade{|value|
		this.sendMsg("livecode/fadeTime", value.asInteger)
	}

	setSymmetry{|sym|
		this.sendMsg("world/symmetry", sym.asInteger ? 0)
	}

	quit{
		this.sendMsg("quit")
	}

	sendMsg{|cmd ... msg|
		ciAddr.sendMsg(oscPrefix++cmd, *msg)
	}

	changeOscPrefix{|prefix| oscPrefix = prefix }

	setFrameRate{|value|
		fps = value;
		this.sendMsg("framerate", fps.asFloat)
	}

	postActivePatterns{
		this.patternLib.select({|pat| pat.active == 1 }).collect(_.index)
	}
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
		rho = (rho + rhoRate).wrap(-pi, pi);
		spherical.rho = rhoMin+(sin(rho)*rhoRange);
		spherical.theta = (spherical.theta+thetaRate).wrap(-pi, pi);
		spherical.phi = (spherical.phi+phiRate).wrap(-pi, pi);
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
		^(coords[0]*sizex+coords[1]*sizey+coords[2]).asInteger
	}


}