AFConvert{

	classvar <path = "afconvert";

	var inputPath, outputPath, options;

	*new{|inputPath, outputPath, options|
		^super.newCopyArgs(inputPath, outputPath, options)
	}
	
	convert{|action|
		options = options ? "-f AIFF -d BEI24@44100";
		(this.class.path + options + inputPath.quote + outputPath.quote).unixCmdThen({
			Post << (inputPath.quote + "has been converted to" + outputPath.quote) << Char.nl;
			action.value
		}, 0.1)
	}

}