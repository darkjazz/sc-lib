FBoids{
	var <numboids, <dim, size, speed, <>cohesion, <>alignment, <>separation, <>center;
	var <boids, <ctr;
	
	*new{|numboids, dim=2, size, speed=0.9, cohesion=50.0, alignment=8.0, separation=30.0, center=100|
		^super.newCopyArgs(numboids, dim, size, speed, cohesion, alignment, separation, center).init
	}
	
	init{
		numboids = numboids ? 10;
		size = size ? (400.0 ! dim);
		boids = Array.fill(numboids, { FBoid({|i| size[i].rand} ! dim, {|i| (size[i]*0.01).rand } ! dim, dim) });
		ctr = RunningStat();
	}
	
	update{
		
		boids.do({|boid|
			boids.select({|other| boid != other  }).do({|other|
				this.applyForces(boid, other)
			});
			this.calc(boid);
			if (ctr.isKindOf(RunningStat)) { ctr.push(boid.pos) };
		});
		
	}
	
	setCenter{|centerpos|
		ctr = centerpos;
	}
	
	autoCenter{
		ctr = RunningStat();
	}
	
	calc{|boid|
		var centerpoint;
		
		if (ctr.isKindOf(RunningStat)) {  
			centerpoint = ctr.mean
		} {
			centerpoint = ctr
		};
		
		boid.vec = boid.vec * speed;
		
		boid.vec = boid.vec + ((boid.cohesion.mean - boid.pos) / cohesion);
		
		boid.vec = boid.vec + ((boid.alignment.mean - boid.vec) / alignment);
		
		boid.vec = boid.vec + boid.separation;
		
		boid.vec = boid.vec + ((centerpoint - boid.pos) / center);
				
		boid.pos = boid.pos + boid.vec;
		
//		boid.pos = boid.pos.wrap(0 ! 2, size);
		
		boid.init(dim)
	}
	
	applyForces{|boidA, boidB|
		
		boidA.cohesion.push(boidB.pos);
		
		boidA.alignment.push(boidB.vec);
		
		if (boidA.distance(boidB) < separation) {
			boidA.separation = boidA.separation - (boidB.pos - boidA.pos);
		}
		
	}
	
}

FBoid{
	
	var <>pos, <>vec;
	var <>cohesion, <>alignment, <>separation;
	
	*new{|pos, vec, dim|
		pos = pos ? Array.fill(dim, 0);
		vec = vec ? Array.fill(dim, 0);
		^super.newCopyArgs(pos, vec).init(dim)
	}
	
	init{|dim|
		separation = Array.fill(dim, 0);
		cohesion = RunningStat();
		alignment = RunningStat();
	}
	
	distance{|other|
		^sqrt((this.pos - other.pos).squared.sum)
	}

}