Paths{
	*prefix{
		if (thisProcess.platform.name == 'linux')
		{
			^"/home/alo"
		}
		{
			^"/Users/kurivari"
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
			if ((currentEnvironment['ambiOrder'].notNil).and(currentEnvironment['ambiOrder'] > 1)) {
				^(Paths.devdir ++ "/lambda/supercollider/sparsematrix/sparsedefs_hoa.scd")
			}
			{
				^(Paths.devdir ++ "/lambda/supercollider/sparsematrix/sparsedefs.scd")
			}
		}
	}

	*skismdefs{
		if (thisProcess.platform.name == 'linux')
		{
			^(Paths.devdir ++ "/lambda/supercollider/sparsematrix/skismdefs.scd")
		}
		{
			if ((currentEnvironment['ambiOrder'].notNil).and(currentEnvironment['ambiOrder'] > 1)) {
				^(Paths.devdir ++ "/lambda/supercollider/sparsematrix/skismdefs_hoa.scd")
			}
			{
				^(Paths.devdir ++ "/lambda/supercollider/sparsematrix/skismdefs.scd")
			}
		}
	}

	*matrixDir{
		^(Paths.devdir ++ "/lambda/supercollider/sparsematrix")
	}

	*noisefunkDir{
		^(Paths.devdir ++ "/lambda/supercollider/noisefunk")
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
		^(Paths.prefix +/+ "snd")
	}

}