UGenGraphGEP{
	
	var populationSize, chromosomeSize, <ugenCollection, <population, <codeStrings;
	
	*new{|populationSize=40, chromosomeSize=16, ugenCollection|
		^super.newCopyArgs(populationSize, chromosomeSize, ugenCollection).init
	}
		
	init{
		if (ugenCollection.isNil) { 
			this.collectAllUGens;
			ugenCollection = ugenCollection.select(_.respondsTo('ar'));
		};
				
		population = Array.fill(populationSize, {
			[ugenCollection.choose.name] ++ Array.fill(chromosomeSize / 2 - 1, {
				[ugenCollection.choose.name, "d", "f"].choose
			}) ++ Array.fill(chromosomeSize / 2, { ["d", "f"].choose })
		})
	}
	
	collectAllUGens{|class|
		if (class.isNil) { 
			class = UGen; 
			ugenCollection = Array();
		};
		if (class.subclasses.notNil) {
			class.subclasses.do({|ugen| ugenCollection = ugenCollection.add(this.collectAllUGens(ugen)) })
		};
		^class
	}	
	
	collectCodeStrings{
		codeStrings = ();
		population.do({|chr|
			var str, class;
			str = "{" ++ chr.first.asString ++ ".ar(";
			class = ugenCollection.select({|cls| cls.name == chr.first }).first;
			str = str ++ this.translate(class, chr, 1) ++ ")}";
			codeStrings[str.identityHash.abs.asString.asSymbol] = str
		})
	}
		
	translate{|ugen, chr, index, depth = 1|
		var args, argEnvir, argNames, str = "";
		if (ugen.class.methods.notNil)
		{
			if (ugen.class.methods.select({|mth| mth.name == 'ar' }).size > 0) 
			{
				argNames = ugen.class.methods.select({|mth| mth.name == 'ar' }).first.argNames
			}
			{
				argNames = ugen.superclass.class.methods.select({|mth| mth.name == 'ar' }).first.argNames
			}
		}
		{
			argNames = ugen.superclass.class.methods.select({|mth| mth.name == 'ar' }).first.argNames
		};
		
		argNames = argNames.drop(1);
		args = chr[index..(index+argNames.size-1)];
		if (ugen.class.methods.notNil)
		{
			if (ugen.class.methods.select({|mth| mth.name == 'ar' }).size > 0)
			{
				argEnvir = ugen.class.methods.select({|mth| mth.name == 'ar' }).first.makeEnvirFromArgs;
			}
			{
				argEnvir = ugen.superclass.class.methods.select({|mth| mth.name == 'ar' }).first.makeEnvirFromArgs;
			}
		}
		{
			argEnvir = ugen.superclass.class.methods.select({|mth| mth.name == 'ar' }).first.makeEnvirFromArgs;
		};

		index = index+args.size;
		args.do({|item, i|
			if (item.isKindOf(Symbol)) {
				ugen = ugenCollection.select({|class| class.name == item }).first;
				str = str ++ argNames[i].asString ++ ": " ++ ugen.name.asString ++ ".ar(" ++ 
					this.translate(ugen, chr, index, depth + 1) ++ ")";
			}
			{
				if (item == "d") {
					str = str ++ argNames[i].asString ++ ": " ++ (argEnvir[argNames[i]] ? 0.0).asString;
				}
				{
					str = str ++ argNames[i].asString ++ ": " ++ (argEnvir[argNames[i]] ? 0.0 * rrand(0.1, 10.0)).asString;
				};
							
			};
			if (i < args.lastIndex) { str = str ++ ", " }
			
		});
		^str
		
	}
	
	evaluate{
		codeStrings.keysValuesDo({|key, string|
			string.compile.asSynthDef(name: key)
		})
	}
	
}