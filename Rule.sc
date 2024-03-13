Rule {

	classvar <families;
	var <>world, <>name, <states;

	*new{|world|
		^super.newCopyArgs(world).init.states_(2)
	}

	family{
		^this.class
	}

	states_{|size|
		states = (0..size-1)
	}

	init{}

	*initClass{
		families = IdentityDictionary[

		];
	}
}

Life : Rule {

	classvar <rules;
	var <births, <survivals;

	*newPreset{|world, name|
		^Life(world).births_(Life.rules[name][0]).survivals_(Life.rules[name][1]).name_(name)
	}

	init{
		births = Array.fill(9, 0);
		survivals = Array.fill(9, 0);
		states = [0, 1];
//		family = \life;
	}

	next{
		var worldAlive=0;
		world.alive = 0;
		world.world.do({|row, i|
			row.do({|cell, j|
				cell.setHistory
			})
		});
		world.world.do({|row, i|
			row.do({|cell, j|
				var alive;
				alive = cell.n.select({|it, ind| it.history[0] == 1}).size;
				if (cell.history[0] == 1,
					{cell.state_(survivals[alive])}, {cell.state_(births[alive])});
				if (cell.state > 0, {world.alive = world.alive + 1});
				if (cell.state != cell.history[0], {worldAlive = worldAlive + 1});
				world.addToStateY(cell, j)
			});
			world.addToStateX(row, i)
		});
		^worldAlive
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

Vote : Rule{

	classvar <rules;
	var <life;

	*newPreset{|world, name|
		^Vote(world).life_(Vote.rules[name]).name_(name)
	}

	init{
		life = Array.fill(10, 0);
		states = [0, 1];
	}

	next{
		var worldAlive = 0;
		world.alive = 0;
		world.world.do({|row, i|
			row.do({|cell, j|
				cell.setHistory
			})
		});
		world.world.do({|row, i|
			row.do({|cell, j|
				var alive;
				alive = cell.n.select({|it, ind| it.history[0] == 1}).size + cell.history[0];
				cell.state_(life[alive]);
				if (cell.state > 0, {world.alive = world.alive + 1});
				if (cell.state != cell.history[0], {worldAlive = worldAlive + 1});
				world.addToStateY(cell, j)
			});
			world.addToStateX(row, i)
		});
		^worldAlive
	}

	life_{|inds|
		inds.do({|it|
			life.put(it, 1)
		})
	}

	*initClass{
		rules = IdentityDictionary[
			\fredkin -> [1,3,5,7,9],
			\feux -> [1,3,5,8],
			\vote45 -> [4,6,7,8,9],
			\vote -> [5,6,7,8,9]
		]
	}

}

LargerThanLife : Rule {

	var <>births, <>survivals;

	init{
		births = Range(3, 0);
		survivals = Range(3, 1);
	}

	next{
		var worldAlive = 0;
		world.alive = 0;
		world.world.do({|row, i|
			row.do({|cell, j|
				cell.setHistory
			})
		});
		world.world.do({|row, i|
			row.do({|cell, j|
				var alive;
				alive = cell.n.select({|it, ind| it.history[0] == 1}).size;
				if (cell.history[0] > 1,
					{if (cell.history[0] < states.size,
						{cell.state_(cell.history[0] + 1)}, {cell.state_(0)})},
					{if (cell.history[0] == 0,
						{cell.state_(if (births.includes(alive), {1}, {0}))},
						{if (survivals.includes(alive).not, {
								if (cell.history[0] < states.size,
									{cell.state_(cell.history[0] + 1)}, {cell.state_(0)})
							}, {cell.state_(1)})
						})
					}
				);
				if (cell.state > 0, {world.alive = world.alive + 1});
				if (cell.state != cell.history[0], {worldAlive = worldAlive + 1})			})
		});
		^worldAlive
	}

}

WeightedLife : Rule {

	classvar <rules;
	var <>weights, <>survivals, <>births;

	init{
		survivals = Array.new;
		births = Array.new;
	}

	*newPreset{|world, name|
		^WeightedLife(world)
			.weights_(rules[name][0])
			.survivals_(rules[name][1])
			.births_(rules[name][2])
			.states_(rules[name][3])
			.name_(name)
	}

	*newFromGenerations{|world, name|
		var rule;
		rule = Generations.rules[name];
		^WeightedLife(world)

	}

	*newFromLife{|world, name|
		var rule;
		rule = Life.rules[name];
		^WeightedLife(world)
	}

	next{
		var worldAlive = 0;
		world.alive = 0;
		world.world.do({|row, i|
			row.do({|cell, j|
				cell.setHistory
			})
		});
		world.world.do({|row, i|
			row.do({|cell, j|
				var alive=0;
				if (states.size-1 < 3, {
					cell.n.do({|it, ind|
						if (it.history[0] == 1, {alive = alive + weights[ind]})
					});
					if (cell.history[0] == 0,
						{
							if (births.includes(alive), {cell.state_(1)})
						},
						{
							if (survivals.includes(alive).not, {cell.state_(0)})
					})
				},{
					if (cell.history[0] > 1,
						{
							if (cell.history[0] < states.size,
								{cell.state_(cell.history[0] + 1)}, {cell.state_(0)})},
						{
							cell.n.do({|it, ind|
								if (it.history[0] == 1, {alive = alive + weights[ind]})
							});
							if (cell.history[0] == 0,
							{
								if (births.includes(alive), {cell.state_(1)})
							},
							{
								if (survivals.includes(alive).not, {
									if (cell.history[0] < states.size,
										{cell.state_(cell.history[0] + 1)}, {cell.state_(0)})
								})
							})
						}
					)
				});
				if (cell.state > 0, {world.alive = world.alive + 1});
				if (cell.state != cell.history[0], {worldAlive = worldAlive + 1});				world.addToStateY(cell, j)
			});
			world.addToStateX(row, i)
		});
		^worldAlive
	}

	*initClass{
		rules = IdentityDictionary[
			\bensrule -> [[3,2,3,2,0,2,3,2,3], [3,5,8], [4,6,8], 2],
			\border -> [[1,1,1,1,9,1,1,1,1], (10..16), (1..8), 2],
			\bricks -> [[5,2,5,2,0,2,5,2,5], [10,12,14,16], [7,13], 3],
			\bustle -> [[2,1,2,1,0,1,2,1,2], [2,4,5,7], [3], 4],
			\career -> [[1,2,1,1,0,1,1,1,1], [2,3], [3], 2],
			\cloud54 -> [[1,1,9,1,0,9,1,9,9], [2,3,9,10,19,27], [3,10,27], 2],
			\cloud75 -> [[1,1,9,1,0,9,1,9,9],
						[2,3,4,10,11,13,18,21,22,27,29,30,31,36,37,38,39,40],
						[3,10,27], 2],
			\conway -> [[5,1,5,1,0,1,5,1,5], [2,3,6,7,10,11,15], [3,7,11,15], 2],
			\conwaymm -> [[5,1,5,1,0,1,5,1,5], [3,6,7,10,11,15], [3,7,11,15], 2],
			\conwaypp -> [[5,1,5,1,0,1,5,1,5], [2,3,6,7,10,11,15,20], [3,7,11,15], 2],
			\conwayp1 -> [[5,1,5,1,0,1,5,1,5], [2,3,6,7,10,12,15], [3,7,11,15], 2],
			\conwayp2 -> [[5,1,5,1,0,1,5,1,5], [2,3,7,10,11,12,13,15], [3,7,11,15], 2],
			\crossporpoises -> [[1,4,1,4,0,4,1,4,0], [2,3,6,7,8,9,10,12,13], [5], 2],
			\cyclish -> [[0,1,0,1,0,1,0,1,0], [2], [1,2,3], 7],
			\cyclones -> [[1,1,0,1,0,1,0,1,1], [2,4,5], [2,3,4,5], 5],
			\dragon -> [[5,1,5,1,0,1,5,1,5], [1,2,7,8,12,15,18,20], [7,11,12,13,20], 2],
			\emergence -> [[1,8,1,1,0,1,8,8,1], [2,3,4,10,11,16,17,24,25], [3,4,9,24], 2],
			\fireflies -> [[1,5,1,5,10,5,1,5,1], [1,10], [6,11,12,21], 9],
			\fleas -> [[5,1,5,1,0,1,5,1,5], [2,3,6,7,10,11,15,20], [2,11], 2],
			\fleas2 -> [[5,1,5,1,0,1,5,1,5], [1,2,5,7,10,11], [2,4,8,11], 2],
			\nofleas2 -> [[5,1,5,1,0,1,5,1,5], [2,3,5,6,7,10,11], [2,4,8,11], 2],
			\froggyhex -> [[4,1,0,1,0,4,0,4,1], [1,6,8], [5,6], 2],
			\frostM -> [[1,1,1,1,0,1,1,1,1], [], [1], 25],
			\frostN -> [[0,1,0,1,0,1,0,1,0], [], [1], 25],
			\gnats -> [[9,1,9,1,0,1,9,1,9], [1,2,11,19], [11,19], 2],
			\hexparity -> [[1,1,0,1,0,1,0,1,1], [2,4,6], [1,3,5], 2],
			\hexruleb2o -> [[1,2,0,32,0,4,0,16,8], [5,7,10,11,13,14,15,17,19,20,21,22,23,25,26,27,28,29,30,34,35,37,38,39,40,41,42,43,44,45,46,49,50,51,52,53,54,56,57,58,60], [3,6,12,24,33,48], 2],
			\hextenders -> [[1,1,0,1,0,1,0,1,1], [1,3,4,5], [2,3], 10],
			\hexinverse -> [[4,1,0,1,0,4,0,4,1], [2,3,6,7,8,9,11,12,13,14,15], [5,10,11,14,15], 2],
			\hglass -> [[0,2,0,8,1,16,0,4,0], [1,2,3,11,21,25,29,30,31], [1,2,3,11,21,25,29,30,31], 2],
			\hogs -> [[0,2,3,3,0,2,2,3,0], [2,3,4,6], [5,6], 2],
			\jitters -> [[-1,-1,5,5,0,5,5,-1,-1], [4,14], [1,4,9], 2],
			\lemmings -> [[1,2,1,2,0,2,1,3,1], [3,4,5,6], [4], 2],
			\linguini -> [[9,1,9,1,0,1,9,1,9], [2,3,4,9,10,11,19,20], [11,18], 2],
			\madness -> [[2,3,2,3,0,3,2,3,2], [8,10,12,14], [5,8,13], 2],
			\mazemakers -> [[4,4,1,4,0,4,1,4,1], [2,3,6,7,8,9,10,12,13], [5], 2],
			\midgedn -> [[2,2,2,1,0,1,2,1,2], [0,2,3], [4,5,6], 9],
			\midges -> [[1,2,1,2,3,2,1,2,1], [3,5,6], [4,5,6], 4],
			\mikesants -> [[0,1,1,1,0,0,1,1,1], [4,5], [2,5,6], 2],
			\mosquito -> [[-1,-1,5,5,0,5,5,-1,-1], [3,4,8,9], [2,3,9], 2],
			\mosquito2 -> [[-1,-1,5,5,0,5,5,-1,-1], [3,4,8,9], [3,6,9,18], 2],
			\navaho -> [[4,1,4,5,7,5,4,1,4], [8,9,11], [2,5], 12],
			\nocturne -> [[1,1,0,1,0,1,0,1,1], [1,6], [2,3,4], 4],
			\parity -> [[0,1,0,1,0,1,0,1,0], [1,3,5], [1,3,5], 2],
			\pictures -> [[0,1,0,1,0,1,0,1,0], [1,2,3], [2,3,4], 2],
			\picturesh -> [[0,1,0,1,0,1,0,1,0], [1,2,3], [2,3,4], 9],
			\pinwheels -> [[1,1,0,1,0,1,0,1,1], [2], [2,3], 7],
			\pipefleas -> [[5,1,5,1,0,1,5,1,5], [3,4,7,11,12,13,14,15,17,18,20,22,23,25], [6,10], 3],
			\prehogs -> [[2,3,0,3,0,2,0,2,3], [3,4,6], [5,6], 2],
			\puttputt -> [[9,1,9,1,0,1,9,1,9], [1,2,3,4,9,18,27,36,40], [2,4,11,18,19,36,40], 2],
			\semigration -> [[5,1,5,1,0,0,5,0,5], [2,3,6,7,10,11,12], [7,11,12,16], 2],
			\simple -> [[5,1,5,1,0,1,5,1,5], [1,5], [2,10], 2],
			\simplehex -> [[1,1,0,1,0,0,0,0,0], [1,2,3], [2,4], 2],
			\simpleinverse -> [[5,1,5,1,0,1,5,1,5], [1,4,5,8,9,12,13,16,17,18,19,20,21,23,24],
			[2,9,10,13,14,17,18,21,22,24], 2],
			\simpleinversefire -> [[5,1,5,1,0,1,5,1,5], [1,5,9,13,17,18,19,21,23,24],
			[2,4,8,9,10,12,13,14,16,17,18,20,21,22,24], 2],
			\stampede -> [[1,3,0,3,0,3,1,3,0], [4,6,9,10], [4,7], 8],
			\starburst -> [[1,2,1,2,0,2,1,2,1], [2,4,6], [4], 2],
			\starbursts2 -> [[1,2,1,2,0,2,1,2,1], [2,4,5,6], [4], 2],
			\stream -> [[0,1,0,1,0,4,0,4,1], [1,3,4,7,8,9,10,11], [5,7,8,10,11], 2],
			\updown1 -> [[1,1,1,4,0,4,4,4,4], [2,4,5,6,9,12,16,20], [3,6,9,12], 2],
			\updown2 -> [[1,5,1,1,0,1,5,5,5], [2,3,7,10,11,12,15], [3,7,11,15], 2],
			\upstream -> [[1,3,0,4,0,4,1,3,0], [4,6,9,10], [4,7], 10],
			\vineyard -> [[1,4,1,4,0,4,0,4,0], [2,6,8,9,10,12,13], [15], 2],
			\vineyard2 -> [[1,4,1,4,0,4,0,4,0], [2,8,9,10,12,13], [5], 2],
			\weevils -> [[3,3,1,1,0,1,1,3,3], [1,2,3,4], [5,6,14], 2],
			\weightedbrain -> [[2,3,2,3,-5,3,2,3,2], [3,4,7], [4,5,8], 3],
			\ychromo -> [[1,1,0,1,0,1,0,1,1], [2], [2], 3],
			\zippers -> [[1,0,1,0,0,4,1,4,1], [2,3,6,7,8,9,10,12,13], [5], 2]
		]
	}

}

Generations : Rule {

	classvar <rules;
	var <births, <survivals;

	*newPreset{|world, name|
		^Generations(world)
			.births_(Generations.rules[name][0])
			.survivals_(Generations.rules[name][1])
			.states_(Generations.rules[name][2])
			.name_(name)
	}

	init{
		births = Array.fill(9, 0);
		survivals = Array.fill(9, 0);
//		family = \generations
	}

	next{
		var worldAlive = 0;
		world.alive = 0;
		world.world.do({|row, i|
			row.do({|cell, j|
				cell.setHistory
			})
		});
		world.world.do({|row, i|
			row.do({|cell, j|
				var alive;
				alive = cell.n.select({|it, ind| it.history[0] == 1}).size;
				if (cell.history[0] > 1,
					{
						if (cell.history[0] < states.size,
						{
							cell.state_(cell.history[0] + 1)
						},
						{
							cell.state_(0)
						})
					},
					{
						if (cell.history[0] == 0,
						{
							cell.state_(births[alive])
						},
						{
							if (survivals[alive] == 0,
							{
								if (cell.history[0] < states.size,
								{
									cell.state_(cell.history[0] + 1)
								},
								{
									cell.state_(0)
								})
							},
							{
								cell.state_(1)
							})
						})
					}
				);
				if (cell.state == 1, {world.alive = world.alive + 1});
				if ((cell.state != cell.history[0]).and(cell.state > 0),
					{worldAlive = worldAlive + 1});
				world.addToStateY(cell, j)
			});
			world.addToStateX(row, i)
		});
		^worldAlive
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
			\banners -> [[3,4,5,7],[2,3,6,7],5],
			\belzhab -> [[2,3],[2,3],8],
			\belzhabsedi -> [[2,3],[1,4,5,6,7,8],8],
			\bloomerang -> [[3,4,6,7,8],[2,3,4],24],
			\bombers -> [[2,4],[3,4,5],25],
			\brain6 -> [[2,4,6],[6],3],
			\briansbrain -> [[2],[],3],
			\burst -> [[3,4,6,8],[0,2,3,5,6,7,8],9],
			\burstII -> [[3,4,6,8],[2,3,5,6,7,8],9],
			\caterpillars -> [[3,7,8],[1,2,4,5,6,7],4],
			\chenille -> [[2,4,5,6,7],[0,5,6,7,8],6],
			\circuitgenesis -> [[1,2,3,4],[2,3,4,5],8],
			\cooties -> [[2],[2,3],8],
			\ebbandflow -> [[3,6],[0,1,2,4,7,8],18],
			\ebbandflowII -> [[3,7],[0,1,2,4,6,8],18],
			\faders -> [[2],[2],25],
			\fireworks -> [[1,3],[2],21],
			\flamingstarbow -> [[2,3],[3,4,7],8],
			\frogs -> [[3,4],[1,2],3],
			\frost -> [[1],[],25],
			\frozenspirals -> [[2,3],[3,5,6],6],
			\glisserati -> [[2,4,5,6,7,8],[0,3,5,6,7,8],7],
			\glissergy -> [[2,4,5,6,7,8],[0,3,5,6,7,8],5],
			// \lambda0 -> [[4,6,8],[3,5,7,9],8],
			// \lambda1 -> [[4,6,8],[3,5,7,9],16],
			// \lambda2 -> [[3,4,5],[3,4,5],17],
			// \lambda3 -> [[4,5],[0,1,2,3,4,5],15],
			// \lambda4 -> [[4,6],[2,3],9],
			\lava -> [[4,5,6,7,8],[1,2,3,4,5],8],
			\lines -> [[4,5,8],[0,1,2,3,4,5],3],
			\livingonedge -> [[3],[3,4,5],6],
			\meteorguns -> [[3],[0,1,2,4,5,6,7,8],8],
			\nova -> [[2,4,7,8],[4,5,6,7,8],25],
			\orthogo -> [[2],[3],4],
			\prairiefire -> [[3,4],[3,4,5],6],
			\rainzha -> [[2,3],[2],8],
			\rake -> [[2,6,7,8],[3,4,6,7],6],
			\sedimental -> [[2,5,6,7,8],[4,5,6,7,8],4],
			\snake -> [[2,5],[0,3,4,6,7],6],
			\softfreeze -> [[3,8],[1,3,4,5,8],6],
			\spirals -> [[2,3,4],[2],5],
			\starwars -> [[2],[3,4,5],4],
			\sticks -> [[2],[3,4,5,6],6],
			\swirl -> [[3,4],[2,3],8],
			\thrillgrill -> [[3,4],[1,2,3,4],48],
			\transers -> [[2,6],[3,4,5],5],
			\transersII -> [[2,6], [0,3,4,5],6],
			\wanderers -> [[3,4,6,7,8],[3,4,5],5],
			\worms -> [[2,5],[3,4,6,7],6],
			\xtasy -> [[2,3,5,6],[1,4,5,6],16]
		]
	}

}

Cyclic : Rule {

	classvar <rules;
	var <>gate, <>gh;

	*newPreset{|world, name|
		^Cyclic(world)
			.gate_(rules[name][0])
			.gh_(rules[name][3])
			.states_(rules[name][1])
			.name_(name)
	}

	init{
		gate = 1;
		gh = false
	}

	next{
		var worldAlive = 0;
		world.alive = 0;
		world.world.do({|row, i|
			row.do({|cell, j|
				cell.setHistory
			})
		});
		world.world.do({|row, i|
			row.do({|cell, j|
				if (gh, {
					if (cell.history[0] == 0, {
						if (cell.n.select({|it, i| it.history[0] == 1}).size >= gate,
							{cell.state_(1)})
						},
						{cell.state_(states.wrapAt(cell.history[0] + 1))
					})

				},
				{
					if (
						cell.n.select({|it, i| it.history[0] ==
							states.wrapAt(cell.history[0] + 1)}).size >= gate,
						{cell.state = states.wrapAt(cell.history[0] + 1)})

				});
				if (cell.state > 0, {world.alive = world.alive + 1});
				if (cell.state != cell.history[0], {worldAlive = worldAlive + 1});
				world.addToStateY(cell, j)
			});
			world.addToStateX(row, i)
		});
		^worldAlive
	}

	*initClass{
		rules = IdentityDictionary[
			\three13 -> [3, 3, {Habitat.newMoore}, false],
			\threecolor -> [11, 3, {Habitat.newMoore(2)}, false],
			\amoeba -> [10, 2, {Habitat.newNeumann(3)}, false],
			\blackvswhite -> [23, 2, {Habitat.newNeumann(5)}, false],
			\cca -> [1, 14, {Habitat.newNeumann}, false],
			\ccaMoore -> [1, 14, {Habitat.newMoore}, false],
			\cubism -> [5, 2, {Habitat.newNeumann(2)}, false],
			\cyclicspirals -> [5, 8, {Habitat.newMoore(3)}, false],
			\fossildebris -> [9, 4, {Habitat.newMoore(2)}, false],
			\ghmacaroni -> [4, 5, {Habitat.newMoore(2)}, true],
			\ghmulti -> [15, 6, {Habitat.newMoore(5)}, true],
			\ghmix -> [10, 8, {Habitat.newMoore(5)}, true],
			\ghweak -> [9, 7, {Habitat.newMoore(4)}, true],
			\gh -> [5, 8, {Habitat.newMoore(3)}, true],
			\imperfect -> [2, 4, {Habitat.newMoore}, false],
			\lavalamp -> [10, 3, {Habitat.newMoore}, false],
			\maps -> [3, 5, {Habitat.newNeumann(2)}, false],
			\perfect -> [3, 4, {Habitat.newMoore}, false],
			\squarish -> [2, 6, {Habitat.newNeumann(2)}, false],
			\squarishM -> [2, 6, {Habitat.newMoore}, false],
			\stripes -> [4, 5, {Habitat.newNeumann(23)}, false],
			\turbulent -> [5, 8, {Habitat.newMoore(2)}, false]
		]
	}
}

GeneralBinary : Rule {

}

NeumannBinary : Rule {

}

RulesTable : Rule {

}

Continuous : Rule {

	classvar <rules;
	var <>add, <>mul, <>weights, <>wrap;

	*new{|world, add = 0, mul = 1, weights, wrap = true|
		^super.new(world).set(add, mul, weights, wrap)
	}

	set{|pAdd, pMul, pWeights, pWrap|
		add = pAdd;
		mul = pMul;
		wrap = pWrap;
		weights = pWeights.normalizeSum;
		name = \continuous;
	}

	next{
		var worldAlive = 0;
		world.alive = 0;
		world.world.do({|row, i|
			row.do({|cell, j|
				cell.setHistory
			})
		});
		world.world.do({|row, i|
			row.do({|cell, j|
				var avg, dir = 0;
				avg = cell.n.collect({|it, ind|
					it.history[0] * weights[ind]
				}).sum / weights.sum;
				if (wrap)
				{
					if (cell.state > 0.5)
						{ avg = avg + (add * cell.state) }
						{ avg = avg - (add * cell.state) }
				}
				{
					avg = avg * mul + add
				};
				cell.state_(avg.wrap(0.0, 1.0));
			})
		})
	}

	*initClass{
		rules = IdentityDictionary[
			\continuous -> 0
		]
	}

}

Continuous2 : Rule {

	classvar <rules;
	var <>weights, <>wrap;

	*new{|world, wrap = true|
		^super.new(world).init(wrap)
	}

	init{|pWrap|
		wrap = pWrap;
		name = \continuous;
	}

	next{
		var worldAlive = 0;
		world.alive = 0;
		world.world.do({|row, i|
			row.do({|cell, j|
				cell.setHistory
			})
		});
		world.world.do({|row, i|
			row.do({|cell, j|
				var avg, add, dir = 0;
				avg = cell.n.collect({|it, ind|
					it.history[0] * weights[ind]
				}).sum / weights.sum;
				add = (cell.n.collect(_.lastValue).sum + cell.value / (cell.n.size + 1))
					.fold(0.0, 1.0);
				if (wrap)
				{
					if (cell.state > 0.5)
						{ avg = avg + (add * cell.state) }
						{ avg = avg - (add * cell.state) }
				}
				{
					avg = avg + add
				};
				cell.value = add;
				cell.state_(avg.wrap(0.0, 1.0));
			})
		})
	}

	*initClass{
		rules = IdentityDictionary[
			\continuous -> 0
		]
	}

}

LimSeries : Rule {

	classvar <rules;
	var <>add, <>lim, <>func;

	*new{|world, add = 0, lim = 10, funcName='zeta'|
		^super.new(world).init(add, lim, funcName)
	}

	init{|pAdd, pLim, funcName|
		add = pAdd;
		lim = pLim;
		name = \limseries;
		func = LimSeries.rules[funcName];
	}

	next{
		var worldAlive = 0;
		world.alive = 0;
		world.world.do({|row, i|
			row.do({|cell, j|
				cell.setHistory
			})
		});
		world.world.do({|row, i|
			row.do({|cell, j|
				var avg, dir = 0;
				avg = cell.n.collect({|it, ind|
					func.(it.history[0].reciprocal, lim)
				}).mean;
				avg = avg + add;
				cell.state_(avg.wrap(0.0, 1.0));
			})
		})
	}

	*initClass{
		rules = IdentityDictionary[
			\zeta -> {|zp, lim| (1..lim).collect({|num| num.pow(zp.neg) }).sum },
			\zetaPrimes -> {|zp, lim| Array.primesN(lim, 1).collect({|num| num.pow(zp.neg) }).sum },
			\fib -> {|zp, lim| lim.fib.collect({|num| num.pow(zp.neg) }).sum }
		]
	}


}