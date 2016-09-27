+Array{
	*primes{|lo = 1, hi = 20000|
		var next, primes;
		primes = Array();
		next = lo.nextPrime;
		if (lo.isPrime)
		{
			primes = primes.add(lo);
		}
		{
			primes = primes.add(next);
		};
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

	*primesNN{|num, hi|
		var next = hi.prevPrime;
		^Array.fill(num, {
			var now = next;
			next = (next - 1).prevPrime;
			now
		})
	}

	*primeSeries{|count, start, step|
		var next, coll;
		if (start.isPrime) {
			next = start;
		}
		{
			if (step > 0) {
				next = (start+1).nextPrime;
			}
			{
				next = (start-1).prevPrime;
			}
		};
		coll = Array.with(next);
		(count - 1).do({|i|
			if (step > 0)
			{ next = Array.primesN(step+1, next).last }
			{ next = Array.primesNN(step.neg-1, next).last };
			coll = coll.add(next);
		});
		^coll
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
