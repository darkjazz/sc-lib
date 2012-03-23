EnvControl{

	*kr{|name = 'env', size = 8|
		^Control.names([name]).kr(Env.newClear(size).asArray);
	}
	
	*ir{|name = 'env', size = 8|
		^Control.names([name]).ir(Env.newClear(size).asArray);
	}
		
}

ArrayControl{
	
	*kr{|name, size = 8, fillFunc|
		^Control.names([name]).kr(Array.fill(size, fillFunc ? 0));
	}
		
	*ir{|name, size = 8, fillFunc|
		^Control.names([name]).ir(Array.fill(size, fillFunc ? 0));
	}
	
}