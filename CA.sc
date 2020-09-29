Cell{
	var <>state;
	*new{arg state; ^super.newCopyArgs(state)}
}

OneDWorld[slot] : ArrayedCollection{

	*rand{arg size, stateArray, randRatio=0.5;

		var obj;
		obj = this.new(size);
		size.do({arg i;
				obj.add(Cell(stateArray.wchoose([randRatio, 1-randRatio])))
				});
		^obj
	}

	*single{arg size, stateArray, aliveInd;

		var obj, ind;
		obj = this.new(size);
		ind = aliveInd ?? (size/2).floor;
		size.do({arg i;
			if(i != ind, {obj.add(Cell(stateArray@0))}, {obj.add(Cell(stateArray@1))})
				});
		^obj
	}

	asStateArray{
		^Array.fill(this.size, {arg i;
			this.at(i).state
		})
	}

}

TwoDWorld {

	*new{|size|
		^Array.new(size)
	}

	*rand{|size, stateArray, weights|
		^Array.fill(size, {
			Array.fill(size, {Cell(stateArray.wchoose(weights))})
		});
	}

}

CA{

	var <world, <radius, recordHistory, <previous, wrapOptions, <history;

	*new{arg world, r=1, recordHistory=false;

		^super.newCopyArgs(world, r, recordHistory).init

	}

	init{
		wrapOptions = IdentityDictionary[
			\wrap -> {|i| previous.wrapAt(i).state},
			\fold -> {|i| previous.foldAt(i).state},
			\clip -> {|i| previous.clipAt(i).state}
		];
		history = Array.new;
	}

	next{|rule=90, wrap='wrap'|

		var binrule;
		if (world.isKindOf(OneDWorld).not, {^"CA world is not a OneDWorld".inform});
		previous = Array.fill(world.size, {|i| Cell(world.at(i).state)});
		binrule = rule.asFloat.decbin(2**(2*radius+1));

		world.do({arg item, i;
			item.state_(binrule.at(Array.fill(radius*2+1, {|j|
				wrapOptions.at(wrap).value(i+j-radius)}).bindec.asInt))
		})

	}

	next2{|limit|
//		game of life
//		2 black - stays same
//		3 black - black
//		any other - white

		if (world.at(0).at(0).isKindOf(Cell).not, {^"CA world is not a TwoDWorld".inform});
		previous = Array.fill(world.size, {|i|
			Array.fill(world.size, {|j| Cell(world.at(i).at(j).state)})
		});

		if (recordHistory, {
			history = history.add(
			 	Array.fill(world.size, {|i|
					Array.fill(world.size, {|j| Cell(world.at(i).at(j).state)})
				});
			);
		});

		world.do({|row, i|
			row.do({|it, j|
				var alive;
				alive = (Array.fill(3, {|indo|
					Array.fill(3, {|indi|
						previous.wrapAt(i+indo-1).wrapAt(j+indi-1).state
					})
				}).flat.occurencesOf(1)) - it.state;
				if (alive == 3, {it.state_(1)});
				if ((alive < 2).or(alive > limit), {it.state_(0)})
			})
		})

	}

	next3{|deaths, births|
//		game of life
//		2 black - stays same
//		3 black - black
//		any other - white

		if (world.at(0).at(0).isKindOf(Cell).not, {^"CA world is not a TwoDWorld".inform});
		previous = Array.fill(world.size, {|i|
			Array.fill(world.size, {|j| Cell(world.at(i).at(j).state)})
		});

		if (recordHistory, {
			history = history.add(
			 	Array.fill(world.size, {|i|
					Array.fill(world.size, {|j| Cell(world.at(i).at(j).state)})
				});
			);
		});

		world.do({|row, i|
			row.do({|it, j|
				var alive;
				alive = (Array.fill(3, {|indo|
					Array.fill(3, {|indi|
						previous.wrapAt(i+indo-1).wrapAt(j+indi-1).state
					})
				}).flat.occurencesOf(1)) - it.state;
				if (it.state == 1, {it.state_(deaths[alive])}, {it.state_(births[alive])});
			})
		})

	}

	nextCyclic{|states|
		previous = Array.fill(world.size, {|i|
			Array.fill(world.size, {|j| Cell(world[i][j].state)})
			});
		world.do({|row, i|
			row.do({|it, j|
				var nhood;
				nhood = Array.fill(3, {|indo|
					Array.fill(3, {|indi|
						previous.wrapAt(i+indo-1).wrapAt(j+indi-1)
					})
				}).flat;
				nhood.removeAt(4);
				nhood.do({|cell, ind|
					if (cell.state == states.wrapAt(previous[i][j].state - 1), {
						("changed neighbor # " ++ ind ++ " of cell at [" ++ i ++ "]" ++ "[" ++ j ++ "]").postln;
						("from " ++ cell.state).post;
						world[i][world[i][j]].state_(states.wrapAt(previous[i][j].state));
						("to " ++ states.wrapAt(previous[i][j].state)).postln;
						"".postln;

					})
				})
			})
		})
	}


	wrapNext{arg rule=90;

		var binrule, temp;
		temp = Array.fill(world.size, {arg i; world.at(i).state});
		binrule = rule.decbin;

		world.do({arg item, i;
			var count;
			count = 0;
			while({([temp.wrapAt(i-1), temp.wrapAt(i), temp.wrapAt(i+1)] != count.decbin(3).reverse)}, {
				count = count + 1
				});
			item.state_(binrule.at(count))

		})

	}

	foldNext{arg rule=90;

		var binrule, temp;
		temp = Array.fill(world.size, {arg i; world.at(i).state});
		binrule = rule.decbin;

		world.do({arg item, i;
			var count;
			count = 0;
			while({([temp.foldAt(i-1), temp.foldAt(i), temp.foldAt(i+1)] != count.decbin(3).reverse)}, {
				count = count + 1
				});
			item.state_(binrule.at(count))

		})

	}

	clipNext{arg rule=90;

		var binrule, temp;
		temp = Array.fill(world.size, {arg i; world.at(i).state});
		binrule = rule.decbin;

		world.do({arg item, i;
			var count;
			count = 0;
			while({([temp.clipAt(i-1), temp.clipAt(i), temp.clipAt(i+1)] != count.decbin(3).reverse)}, {
				count = count + 1
				});
			item.state_(binrule.at(count))

		})

	}

	nextAvg{arg k=1, mul=1.0, add=0.0;
		previous = world.asStateArray; //Array.fill(world.size, {arg i; world.at(i).state});
		world.do({arg item, i;
			var newState = 0, x, nh;
//			newState = (previous.wrapAt(i-1) + previous.wrapAt(i) + previous.wrapAt(i+1))/3;
			nh = Array.fill(k*2+1, {|j|
				previous.wrapAt(i+j-k)
			});
			newState = nh.sum/nh.size;
			x = newState*mul+add;
			if(x <= 1.0, {newState = x}, {newState = x - 1});
			item.state_(newState)
		})
	}

}
