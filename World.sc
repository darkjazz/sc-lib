XCell{
	
	var <>state, <x, <y, <>n, <history, <born = false;
	var <>id, <>bindex, <>value, <>lastValue;
	
	*new{|state, x, y|
		^super.newCopyArgs(state, x, y).init
	}
	
	init{
		history = Array.with(state)
	}
	
	next{|rule|
		history.put(0, state);
		state = rule[Array.with(
			n[0].history[n[0].history.lastIndex], 
			state, 
			n[1].state).bindec.asInt]
	}
	
	clear{
		history = Array.with(state);
	}
		
	hood{
		^{|i| n[i].state} ! n.size
	}
	
	setHistory{
		if (value.notNil) {lastValue = value};
		history.put(0, state)
	}
	
	occurencesOfState{|state|
		^n.select({|it| it.state == state}).size
	}
	
	averageState{
		^(n.collect({|it| it.state}).sum/n.size)
	}
	
	neighborState{|index|
		^n[index].state
	}
	
}

XWorld{

	var <>world, <habitat, <>rule, <initial, <groupSize, <nGroups, <size;
	var <data, <history, <>historySize, <>historyOn=false, <>alive = 0, <>active = 0;
	var <generation = 1, <>statesx, <>statesy;
	
	*new{|world, habitat, rule|
		^super.newCopyArgs(world, habitat, rule).init.setNeighborhood
	}
	
	init{
		nGroups = 1;
		groupSize = world.size;
		size = world.size;
		rule.world_(this);
		initial = Array.new;
	}
			
	setNeighborhood{
		var count = 0, indi;
		statesx = Array.fill(size, 0);
		statesy = Array.fill(size, 0);
		habitat.indices.do({|it, i|
			habitat.indices.put(i, 
				[it[0]-(habitat.range+1), it[1]-(habitat.range+1)].wrap(0, world.lastIndex)
			);
			active = active + 1
		});
		if (world[0].isKindOf(Cell), {
			world.do({|cell, i|
				cell.n = Array.with(world.wrapAt(i-1), world.wrapAt(i+1))
			});	
		},{
			world.do({|row, i|
				row.do({|cell, j|
					if (cell.state > rule.states[0], {
						alive = alive + 1;
						initial = initial.add([i, j, cell.state])
					});
					cell.n = Array.newClear(habitat.indices.size);
					habitat.indices.do({|it, ind|
						if (j == 0, {indi = 1}, {indi = 0});
						habitat.indices.put(ind, 
							[it[0]+indi, it[1]+1].wrap(0, world.lastIndex));
						cell.n.put(ind, world[habitat.indices[ind][0]][habitat.indices[ind][1]])
					});
					this.addToStateY(cell, j)
				});
				this.addToStateX(row, i)
			})			
		})
	}
	
	*newRand1D{|size, weights, habitat, rule|
		^XWorld(
			Array.fill(size, {|i|
				XCell(rule.states.wchoose(weights), i).setHistory
			}),
			habitat
		)
	}
	
	*newRand2D{|size, weights, habitat, rule|
		^XWorld(
			Array.fill(size, {|i|
				Array.fill(size, {|j|
					XCell(rule.states.wchoose(weights), i, j).setHistory
				})
			}),
			habitat, 
			rule
		)
	}
	
	// enables to define areas of live cells in certain areas
	// area is a Rect with dimensions equal or smaller than those of the world 
	*newRandInArea{|size, weights, habitat, area, rule|
		if (area.isKindOf(Rect), {
			if (area.left + area.width > (size-1), {area.left = 0; area.width = size-1});
			if (area.top + area.height > (size-1), {area.top = 0; area.height = size-1});
			^XWorld(
				Array.fill(size, {|i|
					Array.fill(size, {|j|
						if (((i >= area.left).and(j >= area.top))
								.and((i <= (area.left + area.width)).and(j <= (area.top + area.height))),
								{XCell(rule.states.wchoose(weights), i, j)},
								{XCell(rule.states[0], i, j)})
					})
				}),
				habitat,
				rule
			)
		}, {"Area has to be a Rect".inform; ^nil})	
	}
		
	*newSetCells{|size, cellStates, habitat, rule|
	// cellStates is expected to be an array of arrays of [i, j, state]
	// only values other than state 0 are expected to be included
		var count = 0, wrld, obj;
		wrld = Array.fill(size, {|i|
				Array.fill(size, {|j|
					XCell(rule.states[0], i, j).value_([-1, 1].choose)
				})
			});
		cellStates.do({|arr, i|
			wrld[arr[0]][arr[1]].state_(arr[2])
		});
		^XWorld(wrld, habitat, rule);
		
	}

	*newRand2DContinuous{|size, habitat, rule|
		^XWorld(
			Array.fill(size, {|i|
				Array.fill(size, {|j|
					XCell(rrand(0.0, 1.0), i, j).value_([-1, 1].choose).setHistory
				})
			}),
			habitat, 
			rule
		)
	}
	
	reset{
		world.do({|row, i|
			row.do({|cell, j|
				cell.state_(0)
			})
		});
		initial.do({|arr, i|
			world[arr[0]][arr[1]].state_(arr[2])
		})
	}
	
	next{
		statesx = Array.fill(size, 0);
		statesy = Array.fill(size, 0);
		active = rule.next;
		generation = generation + 1;
		if (historyOn, {this.group})
	}
	
	addToStateX{|row, index|
		statesx[index] = row.collect({|cell| if(cell.state > 0, {1}, {0})}).sum
	}
	
	addToStateY{|cell, index|
		statesy[index] = statesy[index] + if(cell.state > 0, {1}, {0})
	}
	
	averageState{
		var sum=0;
		world.do({|row, i|
			row.do({|cell, j|
				sum = sum + cell.state
			})
		});
		^sum/(world.size**2)
		
	}
	
	groupStates{|groupSize|
		var states, numGroups;
		numGroups = (world.size / groupSize).asInteger;
		numGroups.do({|i|
			numGroups.do({|j|
				var grp;
				groupSize.do({|inner|
					grp = grp.add(world[i+inner]
						.copyRange(j*groupSize, j*groupSize+(groupSize-1)))
				});
				states = states.add(grp.flat.collect(_.state))
			})
		});
		^states
	}
	
	groupCells{|groupSize|
		var cells, numGroups;
		numGroups = (world.size / groupSize).asInteger;
		numGroups.do({|i|
			numGroups.do({|j|
				var grp;
				groupSize.do({|inner|
					grp = grp.add(world[i+inner]
						.copyRange(j*groupSize, j*groupSize+(groupSize-1)))
				});
				cells = cells.add(grp.flat)
			})		
		});
		^cells
	}
	
	collect{
		^world.collect(_.collect(_.state)).flat
	}
	
}

Habitat{

	var <indices, includeCell;
	var <numNeighbors, <range, <cellIndex, includeCell=false;
	
	*new{|indices, includeCell=false|
		^super.newCopyArgs(indices, includeCell).init
	}
	
	init{
		if (includeCell.not, {this.excludeCell})
	}
	
	*newMoore{|range=1, include=false|
		var size;
		size = range*2+1;
		^Habitat(Array.fill(size, {|i| Array.fill(size, {|j| [i, j]})}).flatten(1), include)
			.numNeighbors_((range*2+1)**2-1)
			.range_(range)
			.cellIndex_( [(size/2).asInt, (size/2).asInt] )
	}
	
	*newNeumann{|range=1, include=false|
		var size;
		size = range*2+1;
		^Habitat(
			Array.fill(size, {|i| 
				Array.fill(size, {|j| 
					if ((i == range).or(j == range), {[i, j]})
				})
			}).flatten(1).select({|it| it.notNil}), include)
		.numNeighbors_(4*range)
		.range_(range)
		.cellIndex_( [(size/2).asInt, (size/2).asInt] )	}

	numNeighbors_{|num|
		numNeighbors = num
	}
	
	range_{|value|
		range = value
	}
	
	cellIndex_{|index|
		cellIndex = index
	}
	
	excludeCell{
		indices.removeAt((indices.size/2).asInt)
	}

}

XSeeds{

	*point{|x, y|
		^[[x, y, 1]]
	}
		
	*fillRect{|left, top, width, length| 
		^({|i|
			({|j| [left + i, top + j, 1] } ! length)
		} ! width).flatten(1)
	}
	
	*rect{|left, top, width, length| 
		^({|i|
			({|j| 
				if ((i == 0).or(j == 0).or(i == (width - 1)).or(j == (length - 1)), {
					[left + i, top + j, 1]
				}) 
			} ! length)
		} ! width).flatten(1).select({|it| it.notNil})	
	}
	
	*checker{|left, top, width, length| 
		^({|i|
			({|j| [left + i, top + j, 1] } ! length)
		} ! width).flatten(1).select({|item, ind| 
			if (width.odd, {ind.even}, {ind.odd})
		})
	}

	*randRectCont{|left, top, width, length| 
		^({|i|
			({|j| [left + i, top + j, 1.0.rand] } ! length)
		} ! width).flatten(1)
	}

}
