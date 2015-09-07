+ Number{

	roundFreq{|octavediv=24, ref=440|
		^(2**(round(log2(this/ref)*octavediv)/octavediv)*ref)
	}

}