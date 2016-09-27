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

	*dataDir{
		if (thisProcess.platform.name == 'linux')
		{
			^(Paths.prefix ++ "/data")
		}
		{
			^(Paths.prefix ++ "/Data")
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
			^(Paths.prefix ++ "/dev")
		}
	}

	*matrixdefs{
		if (thisProcess.platform.name == 'linux')
		{
			^(Paths.devdir ++ "/lambda/supercollider/sparsematrix/linux/sparsedefs.scd")
		}
		{
			^(Paths.devdir ++ "/lambda/supercollider/sparsematrix/sparsedefs.scd")
		}
	}

	*skismdefs{
		if (thisProcess.platform.name == 'linux')
		{
			^(Paths.devdir ++ "/lambda/supercollider/sparsematrix/skismdefs.scd")
		}
		{
			^(Paths.devdir ++ "/lambda/supercollider/sparsematrix/skismdefs.scd")
		}
	}

	*matrixbufs{
		^(Paths.prefix ++ "/snd/sparsematrix/")
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

	*soundDir{
		^(Paths.prefix +/+ "sounds")
	}

}