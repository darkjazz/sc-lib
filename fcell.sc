FCell{
	
	var <x, <y, <states, <>nhood, <>tag = 0;
	
	*new{|initState, x, y|
		^super.newCopyArgs(x, y).init(initState)
	}
	
	init{|initState|
		states = (initState ! 2);
	}
	
	initStates{|state|
		states = (state ! 2)
	}
	
	switchTag{
		if (tag == 0) { tag = 1 } { tag = 0 }
	}
	
}

FWorld{
	
	var <cells, <nhood, <rule, <size, <generation = 1, <>alive = 0, <>active = 0;
	var <initial;
	
	*new{|cells, nhood, rule|
		^super.newCopyArgs(cells, nhood, rule).init.setNeighborhood
	}
	
	init{
		size = cells.size;
		rule.world_(this);
		initial = Array.new;	
	}
	
	setNeighborhood{
		var count = 0, indi;
		
		nhood.indices.do({|it, i|
			nhood.indices.put(i, 
				[it[0]-(nhood.range+1), it[1]-(nhood.range+1)].wrap(0, cells.lastIndex)
			);
			active = active + 1
		});
			
		cells.do({|row, i|
			row.do({|cell, j|
				if (cell.states.first > rule.states[0], {
					alive = alive + 1;
					initial = initial.add([i, j, cell.states.first])
				});
				cell.nhood = Array.newClear(nhood.indices.size);
				nhood.indices.do({|it, ind|
					if (j == 0, {indi = 1}, {indi = 0});
					nhood.indices.put(ind, 
						[it[0]+indi, it[1]+1].wrap(0, cells.lastIndex));
					cell.nhood.put(ind, cells[nhood.indices[ind][0]][nhood.indices[ind][1]])
				})
			})
		});
		
	}
	
	*newRand2D{|size, weights, nhood, rule|
		^FWorld(
			Array.fill(size, {|i|
				Array.fill(size, {|j|
					FCell(rule.states.wchoose(weights), i, j)
				})
			}),
			nhood, 
			rule
		)
	}
	
	*newSetCells{|size, cellStates, nhood, rule|
	// cellStates is expected to be an array of arrays of [i, j, state]
	// only values other than state 0 are expected to be included
		var count = 0, wrld, obj;
		wrld = Array.fill(size, {|i|
				Array.fill(size, {|j|
					FCell(0, i, j)
				})
			});
		cellStates.do({|arr, i|
			wrld[arr[0]][arr[1]].initStates(arr[2])
		});
		^FWorld(wrld, nhood, rule);
		
	}
	
	next{|drawFunc|
		active = rule.next(drawFunc);
		generation = generation + 1;
	}	
	
}

Nhood{

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
		^Nhood(Array.fill(size, {|i| Array.fill(size, {|j| [i, j]})}).flatten(1), include)
			.numNeighbors_((range*2+1)**2-1)
			.range_(range)
			.cellIndex_( [(size/2).asInt, (size/2).asInt] )
	}
	
	*newNeumann{|range=1, include=false|
		var size;
		size = range*2+1;
		^Nhood(
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

Flife : Rule{

	classvar <rules;
	var <births, <survivals;
	
	*newPreset{|world, name|
		^Flife(world).births_(Flife.rules[name][0]).survivals_(Flife.rules[name][1]).name_(name)
	}
	
	init{
		births = Array.fill(9, 0);
		survivals = Array.fill(9, 0);
		states = [0, 1];
	}
	
	next{|drawFunc|
		world.cells.do({|col, i|
			col.do({|cell, j|
				var alive;
				cell.switchTag;
				alive = cell.nhood.select({|it, ind| 
					(if (it.tag == cell.tag) { it.states[1] } 
						{ it.states[0] } 
					) == 1
				}).size;
				cell.states[1] = cell.states[0];
				if (cell.states[0] == 1, 
					{
						cell.states[0] = survivals[alive]
					}, 
					{
						cell.states[0] = births[alive]
					}
				);
				drawFunc.value(cell)
			});
		});
	}
	
	births_{|bInds|
		bInds.do({|it|
			births.put(it, 1)
		})
	}
	
	survivals_{|sInds|
		sInds.do({|it|
			survivals.put(it, 1)
		})
	}
	
	*initClass{
		rules = IdentityDictionary[
			\twobytwo -> [[3,6], [1,2,5]],
			\life34 -> [[3,4], [3,4]],
			\amoeba -> [[3,5,7], [1,3,5,8]],
			\assimilation -> [[3,4,5], [4,5,6,7]],
			\coagulations -> [[3,7,8], [2,3,5,6,7,8]],
			\conwayslife -> [[3],[2,3]],
			\coral -> [[3],[4,5,6,7,8]],
			\dayandnight -> [[3,6,7,8],[3,4,6,7,8]],
			\diamoeba -> [[3,5,6,7,8],[5,6,7,8]],
			\flakes -> [[3],[0,1,2,3,4,5,6,7,8]],
			\gnarl -> [[1],[1]],
			\highlife -> [[3,6],[2,3]],
			\inverselife -> [[0,1,2,3,4,7,8],[2,3,4,6,7,8]],
			\longlife -> [[3,4,5],[5]],
			\maze -> [[3],[1,2,3,4,5]],
			\mazectric -> [[3],[1,2,3,4]],
			\move -> [[3,6,8],[2,4,5]],
			\pseudolife -> [[3,5,7],[2,3,8]],
			\replicator -> [[1,3,5,7],[1,3,5,7]],
			\seeds -> [[2],[]],
			\serviettes -> [[2,3,4], []],
			\stains -> [[3,6,7,8],[2,3,5,6,7,8]],
			\walledcities -> [[4,5,6,7,8],[2,3,4,5]]
		]

	}
}

Fdisplay{
	
	var <world, <>colors, cellSize, <>speed, <window;
	var rules, go, reset, fast, slow, task;
	
	*new{|world, colors, cellSize, speed|
		^super.newCopyArgs(world, colors, cellSize, speed).init
	}
	
	init{
		var font, items;
//		items = world.rule.family.rules.keys(Array).sort;
		font = Font("Helvetica", 9);
		window = Window(world.rule.family.asString, Rect(200, 200, world.cells.size * cellSize, 
			world.cells.size * cellSize + 20
		)).front;
		window.background = Color(0.7, 0.73, 0.75);
//		rules = SCPopUpMenu(window, Rect(10, 25, 90, 20))
//			.font_(font)
//			.items_(items)
//			.value_(items.indexOf(world.rule.name))
//			.action_({|me|
//				world.rule_(world.rule.family.newPreset(name:me.items[me.value]));
//				colors.do({|it, i|
//					if (i > 1, {
//						colors.put(i, it.alpha_(i+1/world.rule.states.size))
//					})
//				})
//			});

		go = Button(window, Rect(5, 0, 40, 15))
			.font_(font)
			.states_([[">", Color.grey, Color.black], ["o", Color.black, Color.grey]])
			.action_({|btn|
				if (btn.value == 0, {task.stop; task = nil}, {
					this.start	
				})
			});
		reset = Button(window, Rect(50, 0, 40, 15))
			.font_(font)
			.states_([[":.:", Color.grey, Color.black]])
			.action_({
				world.reset;
				window.refresh;
			});
		fast = Button(window, Rect(95, 0, 40, 15))
			.states_([["faster", Color.white, Color.black]])
			.font_(font)
			.action_({
				if (speed > 0.01, {speed = speed - 0.05})
			});
		slow = Button(window, Rect(140, 0, 40, 15))
			.states_([["slower", Color.white, Color.black]])
			.font_(font)
			.action_({
				if (speed < 1, {speed = speed + 0.05})
			});
			
		window.drawHook = {
			world.next({|cell|
				if (cell.states[0] == 1)
				{
					Pen.color = colors[cell.states[0]];
					Pen.fillOval(Rect(cell.x*cellSize, cell.y*cellSize+20, cellSize, cellSize))
				}
			});
		}	

	}
	
	start{
		task = Routine({
				inf.do({|i|
					window.refresh;
					if (world.active == 0, {
						window.refresh;
						go.value = 0;
						(speed*3).wait;
						task.yieldAndReset(0);
					});
					speed.wait
				});
			}).play(AppClock)	
	}
	
	
}