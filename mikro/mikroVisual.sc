MikroVisual{
	
	var <width, <height, <sizeX, <sizeY, <frameRate, <remoteAddr, <vectorSize, <trainDur, <lRate;
	var debugMode;
	
	var oscPrefix = "/mikro/";
	var appPath = "/Users/alo/Development/mikro/visual/build/Release/mikro";
	
	*new{|width, height, sizeX, sizeY, frameRate, remoteAddr, vectorSize, trainDur, lRate|
		^super.newCopyArgs(width, height, sizeX, sizeY, frameRate, remoteAddr, 
			vectorSize, trainDur, lRate).init
	}
	
	init{
		remoteAddr = remoteAddr ? NetAddr("127.0.0.1", 7770);
	}
	
	start{|mode = 1|
		debugMode = mode;
		(appPath + this.makeArgumentString).unixCmd;
	}
	
	quit{ this.sendMsg("quit", 0) }
	
	sendMsg{|cmd ... msg| remoteAddr.sendMsg(oscPrefix ++ cmd, *msg) }
	
	sendWeights{| ... weights| this.sendMsg("weights", *weights) }
	
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