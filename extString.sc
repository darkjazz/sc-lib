+String{

	urlEncode{
		^this	.replace(" ", "+")
				.replace("\n", "%0A")
				.replace("\t", "%09")
				.replace(":", "%3A")
				.replace("<", "%3C")
				.replace(">", "%3E")
				.replace("/", "%2F")
				.replace("#", "%23")
				.replace("?", "%3F")
				.replace("{", "%7B")
				.replace("}", "%7D")
				.replace(";", "%3B")
	}

	unixCmdGetAsync {|timeout=1|
		var pipe, lines, line, start;

		pipe = Pipe.new(this, "r");
		lines = "";
		line = pipe.getLine;
		start = SystemClock.seconds;
		Tdef('waitForResponse', {
			while({SystemClock.seconds - start > timeout},
			{
				lines = lines ++ line ++ "\n";
				line = pipe.getLine;
				0.001.wait;
			})
		}).play;
		pipe.close;

		^lines;
	}

	subStr { arg start=0, end;
	/*
		Returns a sub-string from start index to end index (0-based).
		Negative start indexes wrap around from the end.
		End indexes greater than string length and nil end indexes are forced to string length.
		Negative end indexes count backwards from the end of the string.
		If end index is greater than start index, the result is reversed
			"a string".subStr( -2 ).postln; // returns the last 2 chars
			"a string".subStr( 0, -1 ).postln; // snips the last char
			"a string".subStr( 1 ).postln; // snips the first char
			"a string".subStr( 2, 4 ).postln; // return chars from the middle
			"a string".subStr( 4, 2 ).postln; // return chars from the middle, backwards
	*/
		var len;
		len = this.size - 1;
		(start < 0).if({start = this.size + start});
		end = end ? len;
		(end > len).if({ end = len });
		(end < 0).if({ end = len + end });
		^( start <= end ).if({this.copyRange( start, end )},{ this.subStr( end, start ).reverse })
	}


}