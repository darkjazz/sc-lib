Pgraphic : Pattern{
	var <>addr, <>path, <>pattern;
	
	*new{|addr, path, pattern|
		^super.newCopyArgs(addr, path, pattern)
	}
	
	storeArgs { ^[addr, path, pattern] }
	
	asStream{
		var 	addrStream = addr.asStream,
			pathStream = path.asStream,
			patternStream = pattern.asStream;
			
		^FuncStream({|inval|
			var values;
			if((patternStream.next(inval)).isNil){
				nil
			} {
				values = patternStream.next(inval);
				if (values.isKindOf(Collection).not) {
					values = values.bubble
				};
				addrStream.next(inval).sendMsg(pathStream.next(inval), *values)
			}
		})
	}
}
