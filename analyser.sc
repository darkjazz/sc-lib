Atsa{
	
	classvar atsapath = "/Users/alo/ats/atsc/atsa";
	
	var <>source, <>target, <>start, <>duration, <>lowestFreq, <>highestFreq, <>freqDev;
	var <>windowCycles, <>windowType, <>hopSize, <>lowestMag, <>trackLen, <>minSegLen, <>minGapLen;
	var  <>smrThreshold, <>minSegSMR, <>lastPeakContrib, <>smrContrib, <>fileType;
	
	*new{|source, target, start, duration, windowType = 1, hopSize = 0.25|
		^super.newCopyArgs(source, target, start, duration)
			.windowType_(windowType).hopSize_(hopSize)
	}
	
	compileFlags{
		var flags;
		
		flags = String();
		
		if (start.notNil) { flags = flags + "-b" + start.asString };
		
		if (duration.notNil) { flags = flags + "-e" + duration.asString };
		
		if (lowestFreq.notNil) { flags = flags + "-l" + lowestFreq.asString };
		
		if (highestFreq.notNil) { flags = flags + "-H" + highestFreq.asString };
		
		if (freqDev.notNil) { flags = flags + "-d" + freqDev };
		
		if (windowCycles.notNil) { flags = flags + "-c" + windowCycles };
		
		if (windowType.notNil) { flags = flags + "-w" + windowType };

		if (hopSize.notNil) { flags = flags + "-h" + hopSize };

		if (lowestMag.notNil) { flags = flags + "-m" + lowestMag };

		if (trackLen.notNil) { flags = flags + "-t" + trackLen };

		if (minSegLen.notNil) { flags = flags + "-s" + minSegLen };

		if (minGapLen.notNil) { flags = flags + "-g" + minGapLen };

		if (smrThreshold.notNil) { flags = flags + "-T" + smrThreshold };

		if (lastPeakContrib.notNil) { flags = flags + "-P" + lastPeakContrib };

		if (smrContrib.notNil) { flags = flags + "-M" + smrContrib };

		if (fileType.notNil) { flags = flags + "-F" + fileType };
		
		if (flags.size > 0) {  flags = "\"" ++ flags.trim ++ "\"" };
		
		^flags

	}
	
	run{|doneAction|
		(atsapath + source + target + this.compileFlags).unixCmdThen({
			("Writing" + target + "finished.").inform;
			doneAction.value(this)
		})
	}
	
}

BufferToAts{
	
	classvar <atsapath = "/Users/alo/ats/atsc/atsa", tempdir = "/Users/alo/ats/atsfiles/";
	var buffer, atsfile, tempPath;
	
	*new{|buffer|
		^super.newCopyArgs(buffer)
	}
	
	convert{|action|
		
		fork({
			var tempPath;
			if (buffer.path.notNil) 	
				{ tempPath = tempdir ++ buffer.path.basename } 
				{ tempPath = tempdir ++ "b2a_" ++ Date.getDate.stamp };
			buffer.write(tempPath);
			buffer.server.sync;
			(atsapath + tempPath + (tempPath ++ "ats")).unixCmdThen({
				atsfile = AtsFile((tempPath ++ "ats"), buffer.server).loadToBuffer;
				buffer.server.sync;
				action.value(atsfile);
				1.wait;
				this.deleteFiles(tempPath);
			});
		})
	}
	
	deleteFiles{|path|
		("rm " ++ path).unixCmd;
		("rm " ++ path ++ "ats").unixCmd;
	}
}
