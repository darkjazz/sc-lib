+ Int8Array{

	asFloatArray{
		var floatarray = Array();
		this.clump(4).do({|bytes|
			var word = 0;
			bytes.do({|byte, i|  word = word | ((byte & 0xff) << (i * 8)) });
			floatarray = floatarray.add(Float.from32Bits(word));
		});
		^floatarray
	}

}