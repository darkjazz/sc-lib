Paths{
	*prefix{
		if (thisProcess.platform.name == 'linux')
		{
			^"/home/alo"
		}
		{
			^"/Users/alo"
		}
	}

	*gepdir{
		if (thisProcess.platform.name == 'linux')
		{
			^(Paths.prefix ++ "/data/gepdefs")
		}
		{
			^(Paths.prefix ++ "/Data/gep")
		}
	}
	
	*devdir{
		if (thisProcess.platform.name == 'linux')
		{
			^(Paths.prefix ++ "/development")
		}
		{
			^(Paths.prefix ++ "/Development")
		}		
	}

	*matrixdefs{
		if (thisProcess.platform.name == 'linux')
		{
			^(Paths.prefix ++ "/development/lambda/supercollider/sparsematrix/linux/sparsedefs.scd")
		}
		{
			^(Paths.prefix ++ "/Development/lambda/supercollider/sparsematrix/sparsedefs.scd")
		}
	}

	*skismdefs{
		if (thisProcess.platform.name == 'linux')
		{
			^(Paths.prefix ++ "/development/lambda/supercollider/sparsematrix/skismdefs.scd")
		}
		{
			^(Paths.prefix ++ "/Development/lambda/supercollider/sparsematrix/skismdefs.scd")
		}
	}

	*matrixbufs{
		^(Paths.prefix ++ "/sounds/sparsematrix/")
	}

	*gepArchDir{
		^(Paths.gepdir +/+ "data/")

	}

	*gepDefDir{
		^(Paths.gepdir +/+ "synthdefs/")
	}

	*gepMetaDir{
		^(Paths.gepdir +/+ "metadata/")
	}

	*eventLibDir{
		^(Paths.prefix +/+ "data/mikro/data")
	}

}