RunningStat{
	var <count = 0;
	var oldM, newM, oldS, newS;
	
	*new{
		^super.new
	}
	
	push{|val|
		count = count + 1;
		if (count == 1) {
			oldM = newM = val;
			oldS = 0.0;
		}
		{
			newM = val - oldM / count + oldM;
			newS = (val - oldM)*(val - newM) + oldS;
			
			oldM = newM;
			oldS = newS;
		}
	}
	
	reset{ count = 0 }
	
	mean{ ^if( count > 0 ) { newM } { 0.0 } }
	
	variance{ ^if ( count > 1 ) { newS / (count - 1) } { 0.0 } }
	
	stdDev{ ^this.variance.sqrt }
	
	asEvent{ ^(mean: this.mean, stdDev: this.stdDev) }
	
}

SegmentStat{
	
	var <start, <delta, <mfcc, <flat, <cent, <amps, <err, <diff;
	
	*new{|start, delta|
		^super.newCopyArgs(start, delta).init
	}
	
	init{
		mfcc = Array();
		flat = Array();
		cent = Array();
		amps = Array();
		err = Array();
	}
	
	addMFCC{|time, value|
		mfcc = mfcc.add([time, value])
	}

	addFlatness{|time, value|
		flat = flat.add([time, value])
	}

	addCentroid{|time, value|
		cent = cent.add([time, value])
	}

	addAmp{|time, value|
		amps = amps.add([time, value])
	}

	addError{|time, error|
		err = err.add([time, error])
	}
	
	duration{ 
		^(amps.last.first + delta - start)
	}
	
	size{ ^[mfcc.size, flat.size, cent.size, amps.size].minItem }
		
}
