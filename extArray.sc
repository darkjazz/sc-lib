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
��� removeDups {
��� ��� var result;
��� ��� result = this.species.new(this.size);
��� ��� this.do({ arg item;
��� ��� ��� result.includes(item).not.if({ result.add(item) });
��� ��� });
��� ��� ^result
��� }
}
