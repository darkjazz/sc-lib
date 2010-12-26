CyclicWarp : Warp {
	
	map{arg value;
		^(value * 2).fold(0.0, 1.0) * spec.range + spec.minval
	}
	
	unmap{arg value;
		^(value - spec.minval / spec.range * 2).fold(0.0, 1.0)
	}
	
}