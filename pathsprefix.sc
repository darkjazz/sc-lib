Paths{
	*prefix{

		if (thisProcess.platform.isKindOf(LinuxPlatform))
		{
			^"/home/alo"
		}
		{
			^"/Users/alo"
		}

	}

	*gepdefs{

		if (thisProcess.platform.isKindOf(LinuxPlatform))
		{
			^(Paths.prefix ++ "/data/gepdefs/")
		}
		{
			^(Paths.prefix ++ "/Data/gep/")
		}

	}

	*matrixdefs{

		if (thisProcess.platform.isKindOf(LinuxPlatform))
		{
			^(Paths.prefix ++ "/development/lambda/supercollider/sparsematrix/linux/sparsedefs.scd")
		}
		{
			^(Paths.prefix ++ "/Development/lambda/supercollider/sparsematrix/sparsedefs.scd")
		}


	}

	*skismdefs{

		if (thisProcess.platform.isKindOf(LinuxPlatform))
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
		^(Paths.gepdefs ++ "/data/")

	}

	*gepDefDir{
		^(Paths.gepdefs ++ "/synthdefs/")
	}

	*gepMetaDir{
		^(Paths.gepdefs ++ "/synthdefs/")
	}

}