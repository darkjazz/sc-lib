+Array{
	*primes{|lo = 1, hi = 20000|
		var next, primes;
		primes = Array();
		next = lo.nextPrime;
		while ({next < hi},
		{
			next = (next + 1).nextPrime;
			if (next < hi) { primes = primes.add(next) }
		});
		^primes
	}
	
	*primesN{|num = 5, lo = 1|
		var next = lo.nextPrime;
		^Array.fill(num, {  
			var now = next;
			next = (next + 1).nextPrime;
			now
		})
	}
}

+ Collection {
ÊÊÊ removeDups {
ÊÊÊ ÊÊÊ var result;
ÊÊÊ ÊÊÊ result = this.species.new(this.size);
ÊÊÊ ÊÊÊ this.do({ arg item;
ÊÊÊ ÊÊÊ ÊÊÊ result.includes(item).not.if({ result.add(item) });
ÊÊÊ ÊÊÊ });
ÊÊÊ ÊÊÊ ^result
ÊÊÊ }
}
