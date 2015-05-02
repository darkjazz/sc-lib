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
		
}