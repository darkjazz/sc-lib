// SC3 implementation of Craig Reynolds' Boid algorithm.
// Daniel Jones <dan@freq.co.uk>

BoidWorld {

	var <>boids, <dim, <>size, <ctry, <ctrx;

	*new { |count = 10, dim = 2, size|

		if (size.isNil) {
			size = Array.fill(dim, 400);
		}
		{
			size = Array.fill(dim, size);
		}
		^super.newCopyArgs(count, dim, size).init(count);
	}

	init { |count|
		boids = Array.new(count);
		count.do({
			boids.add(Boid(size.rand));
		});
	}

	move {|speed, cohesion=50.0, alignment=8.0, separation=30.0|
		ctry = 0; ctrx = 0;
		boids.do({ |boid|
			boid.move(this, speed, cohesion, alignment, separation);
			ctrx = ctrx + boid.pos[0];
			ctry = ctry + boid.pos[1];
		});
		ctrx = ctrx / boids.size;
		ctry = ctry / boids.size;
	}
}


Boid {

	var <>pos, <>vec;

	classvar <dim;
	classvar <>rules;

	*initClass {

		dim = 2;

		rules =
		(
			cohesion: { |self, env, coh|
				var	centre = Array.fill(dim, 0),
					vec = Array.fill(dim, 0),
					count = 0;

				env.boids.select(_ != self).do
				{ |boid|

					count = count + 1;
					centre = centre + boid.pos;
				};

				centre = centre / count;
				vec = centre - self.pos;
				(vec / coh); // was 50.0
			},

			alignment: { |self, env, alg|
				var	vec = Array.fill(dim, 0),
					count = 0;

				env.boids.select(_ != self).do
				{ |boid|

					count = count + 1;
					vec = vec + boid.vec;
				};

				vec = vec / count;
				vec = vec - self.vec;
				(vec / alg); // was 8.0
			},

			separation: { |self, env, sep|
				var vec = Array.fill(dim, 0);

				env.boids.select(_ != self).do
				{ |boid|

					if (self.distanceFrom(boid) < sep) // was 30
					{
						vec = vec - (boid.pos - self.pos);
					};
				};

				vec;
			},

			// keep boids in the centre of their environment
			centre: { |self, env|
				var vec = (env.size / 2.0) - self.pos;
				(vec / 100);
			}
		);

	}

	*new { |pos, vec|
		pos = pos ?? Array.fill(dim, 0);
		vec = vec ?? Array.fill(dim, 0);
		^super.newCopyArgs(pos, vec).init;

	}

	init {
		^this;
	}

	distanceFrom { |other|
		^sqrt((this.pos - other.pos).squared.sum);
	}

	move { |env, speed, cohesion, alignment, separation|

		var args;
		vec = vec * speed;

		args = (cohesion: cohesion, alignment: alignment, separation: separation);

		rules.keysValuesDo({ |name, rule|
			vec = vec + rule.value(this, env, args[name]);
		});

		pos = pos + vec;
		pos.do({|it, i|
			if ( it > env.size[i] ) { pos.put(i, env.size[i]) };
			if ( it < 0 ) { pos.put(i, 0) }
		});
	}
}
