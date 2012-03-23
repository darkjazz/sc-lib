MikroGAPhenome : RedGAPhenome {
		
	*new{|genome|
		^super.new(genome)
	}
	
	*collectUGens{|class, ugenArray|
		if (class.isNil) { 
			class = UGen; 
			~ugenArray = Array();
		};
		if (class.subclasses.notNil and: {class != MultiOutUGen}) {
			class.subclasses.do({|ugen|
				~ugenArray = ~ugenArray.add(~collectUGens.value(ugen, ~ugenArray))
			})
		};
		^class		
	}
}