+Integer{
	nearestPrime{|preferNext=true|
		var next, prev;
		if (this.isPrime) { ^this }
		{
			next = abs(this - this.nextPrime);
			prev = abs(this - this.prevPrime);
			if (prev == next) { if (preferNext) { ^this.nextPrime } { ^this.prevPrime } } {
				if (prev > next) { ^this.nextPrime } { ^this.prevPrime }
			}
		}
	}
}

+Float{
	nearestPrime{|preferNext=true|
		^this.asInt.nearestPrime(preferNext)
	}
}