PenDisplay{

	var <>world, <>rule, <colors, <cellSize, <>speed, <window;
	var genCount, ruleFamily, cells, go, <reset, task, world, count=0;
	var ruleBox, rclass, keys, <>group, <>numValues=1, <>showValues=false, <currentValues;
	var fast, slow, rules;
	var <>maxGens = inf, numAlive, display, statesx, statesy, <>drawFunc;
	
	*new{|world, rule, colors, cellSize=5, speed=0.2, window, bgc|
		^super.newCopyArgs(world, rule, colors, cellSize, speed, window).init(bgc)
	}
	
	init{|background|
		var font, items;
		items = rule.family.rules.keys(Array).sort;
		font = Font("Verdana", 10);
		if (window.isNil, {
		window = SCWindow(rule.family.asString, 
			Rect(200, 200, world.size*cellSize + 250, world.sizey*cellSize+200)).front;
//		window.view.background = Color.new(0.7, 0.73, 0.75 );
		window.view.background = background ? Color.black;
		});
		rules = SCPopUpMenu(window, Rect(10, 25, 90, 20))
			.font_(font)
			.items_(items)
			.value_(items.indexOf(rule.name))
			.action_({|me|
				rule = rule.family.newPreset(name:me.items[me.value]);
				rule.world_(world);
				world.rule_(rule);
				colors.do({|it, i|
					if (i > 1, {
						colors.put(i, it.alpha_(i+1/rule.states.size))
					})
				})
			});
		SCStaticText(window, Rect(10, 55, 55, 15))
			.font_(font)
			.string_("generation: ");
//		rclass = rule.class;
		genCount = SCStaticText(window, Rect(10, 70, 55, 15))
			.font_(font)
			.align_(\center)
			.string_("0");
		go = SCButton(window, Rect(105, 25, 40, 20))
			.states_([[">", Color.grey, Color.black], ["o", Color.black, Color.grey]])
			.action_({|btn|
				if (btn.value == 0, {task.stop; task = nil}, {
					this.start	
				})
			});
		reset = SCButton(window, Rect(150, 25, 40, 20))
			.states_([[":.:", Color.grey, Color.black]])
			.action_({
				world.reset;
				window.refresh;
				count = 0;
//				this.renewDisplay;
			});
		SCStaticText(window, Rect(10, 90, 55, 15))
			.font_(Font("Verdana", 10))
			.string_("population: ");
		numAlive = SCStaticText(window, Rect(10, 105, 55, 15))
			.align_(\center)
			.font_(font)
			.string_(world.alive);
//		keys = rclass.rules.keys(Array).sort;
//		ruleBox = SCPopUpMenu(window, Rect(160, 25, 100, 20))
//			.items_(keys)
//			.action_({|it|
//				rule = rclass.newPreset(world, rclass.rules[keys[it.value]]);
//			});
		fast = SCButton(window, Rect(200, 25, 40, 20))
			.states_([["faster", Color.white, Color.black]])
			.font_(Font("Verdana", 9))
			.action_({
				if (speed > 0.01, {speed = speed - 0.05})
			});
		slow = SCButton(window, Rect(245, 25, 40, 20))
			.states_([["slower", Color.white, Color.black]])
			.font_(Font("Verdana", 9))
			.action_({
				if (speed < 1, {speed = speed + 0.05})
			});
		
		display = SCCompositeView(window, Rect(80, 50, cellSize*world.size, cellSize*world.size));
		
		drawFunc = {|display, cell, i, j|
			
			display.colors[cell.state].set;
			
			Pen.strokeRect(
				Rect(i * display.cellSize + 80, j * display.cellSize + 50, display.cellSize, display.cellSize)
			);
			Color.grey(cell.state.reciprocal).set;
			Pen.fillRect(
				Rect(i * display.cellSize + 80 + (display.cellSize * 0.25), 
					j * display.cellSize + 50 + (display.cellSize * 0.25), 
					display.cellSize * 0.5, display.cellSize * 0.5)
			);
			
		};
		
		window.drawHook = {
			var sp;
			world.world.do({|row, i|
				row.do({|cell, j|
					if (cell.state > 0, {
						
						drawFunc.value(this, cell, i, j)

					/*		
						
						if (rule.isKindOf(Continuous).or(rule.isKindOf(Continuous2))) 
						{
							
							if (cell.state > 0.001)
							{
								7.do({
									Pen.strokeColor = Color.grey(cell.state 
										* rrand(0.7, 1.3), cell.state);
									Pen.moveTo(
										Point(
											i * cellSize + 80 + cellSize.rand,
											j * cellSize + 50 + cellSize.rand
										)
									);
									Pen.lineTo(
										Point(
											i * cellSize + 80 + cellSize.rand,
											j * cellSize + 50 + cellSize.rand
										)
									);
//									Pen.translate(cellSize.rand, cellSize.rand);
									Pen.stroke
									
								})
							};
					*/
						/*
							Pen.color = Color.black;
							(cell.state * 10).do({
								Pen.fillOval(Rect(
									i * cellSize + 80 + cellSize.rand,
									j * cellSize + 50 + cellSize.rand,
									2, 2))
							});
						*/	
						/*
							Pen.color = Color.grey(cell.state, cell.state.fold(0.0, 0.3));
							Pen.fillOval(
								Rect(
									i * cellSize + 80, 
									j * cellSize + 50, 
									cellSize, cellSize)
							);
							Pen.color = Color.grey(1 - cell.state, (1 - cell.state).fold(0.0, 0.3));
							Pen.fillOval(
								Rect(
									i * cellSize + 80 + (cellSize / 4),
									j * cellSize + 50 + (cellSize / 4),
									cellSize / 2, cellSize / 2
								)
							);		
					*/
						
						
						/*
							sp = [0.4, 0.6, \exp].asSpec;
							Pen.color = Color.grey(sp.map(1 - cell.state), cell.state);
							Pen.strokeOval(Rect(
								i * cellSize + 80 + (cell.state * 2.8).rand2, 
								j * cellSize + 50 + (cell.state * 2.8).rand2, 
								cellSize * cell.state * 1.4, cellSize * cell.state * 1.4));
							Pen.color = Color.grey(sp.map( cell.state), cell.state);
							Pen.fillOval(Rect(
								(i * cellSize + 80) + (cellSize * (1 - cell.state) / 2), 
								(j * cellSize + 50) + (cellSize * (1 - cell.state) / 2), 
								cellSize * cell.state, cellSize * cell.state ));
						*/
						
						/*
							Pen.moveTo(i * cellSize + 80 @ j * cellSize + 50);
							Pen.color = Color.grey(cell.state);
							Pen.rotate(i * world.size + j * pi);
							SCPen.quadCurveTo(
								(i * cellSize + 80 + (cellSize / 2))@(j * cellSize + 50 + (cellSize / 2)),
								x1@y1
								//x2 @ y2
							);
							Pen.stroke;
						*/	
						/*
						
							Pen.color = Color.grey(cell.state, 1 - cell.state);
							Pen.strokeRect(
								Rect(
									i * cellSize + 80, 
									j * cellSize + 50, 
									cellSize * cell.state, 
									cellSize * cell.state)
								);
						
							Pen.color = Color.grey(1 - cell.state, 1 - cell.state);
							Pen.fillRect(
								Rect(
									i * cellSize + 80 + (cellSize / 4),
									j * cellSize + 50 + (cellSize / 4),
									cellSize / 2, cellSize / 2
								)
							);											
						
							Pen.color = Color.grey(cell.state);
							Pen.fillRect(
								Rect(i * cellSize + 80, j * cellSize + 50, cellSize, cellSize)
							);
							
																	Pen.color = Color.grey(1 - cell.state, 1 - cell.state);
							Pen.strokeRect(
								Rect(
									i * cellSize + 80 + cellSize - (cellSize * cell.state),
									j * cellSize + 50 + cellSize - (cellSize * cell.state),
									cellSize * 4 * cell.state,
									cellSize * 4 * cell.state
								)
							);
						*/
						
							/*
							Pen.color = Color.grey(1 - cell.state, 1 - cell.state);
							Pen.width = cell.state * cellSize;
							Pen.line(
								Point(
									i * cellSize + 80,
									j * cellSize + 50 + (cellSize / 2)
								),
								Point(
									i * cellSize + 80 + cellSize,
									j * cellSize + 50 + (cellSize / 2)
								)
							);
							Pen.stroke;
						
						
							Pen.color = Color.grey(cell.state, cell.state);
							Pen.width = 1 - cell.state * cellSize;
							Pen.line(
								Point(
									i * cellSize + 80 + (cellSize / 2),
									j * cellSize + 50
								),
								Point(
									i * cellSize + 80 + (cellSize / 2),
									j * cellSize + 50 + cellSize
								)
							);
							Pen.stroke;
						
							*/
						
						
						/*
							(cell.state * 5).asInteger.do({
								Pen.color = Color.grey(cell.state * rrand(0.9, 1.1), cell.state.wrap(0.0, 0.5));
								Pen.strokeRect(Rect(
									rrand(i * cellSize + 80, i * cellSize + 80 + cellSize), 
									rrand(j * cellSize + 50, j * cellSize + 50 + cellSize),
									rrand(1, 5), rrand(1, 5) ));
							})
						*/
						/*
							Pen.color = Color.new(cell.state, cell.state, cell.state, cell.state);
							Pen.addAnnularWedge(
								Point(i * cellSize + 80 + (cellSize / 4), j * cellSize + 50 + (cellSize / 4)),
								(cell.state * cellSize / 2).rand, cell.state * cellSize, 2pi.rand, 2pi.rand
							);
							Pen.perform([\stroke, \fill].choose)
						
						}
						{
							colors[cell.state].set;
						Pen.width = (cell.state/2).ceil;
							Pen.beginPath;
							Pen.moveTo((cell.x * cellSize + (cellSize) + 80)@
								(cell.y * cellSize + (cellSize) + 50));
							[
							Pen.addAnnularWedge(
								Point(cell.x * cellSize + 80, cell.y * cellSize + 50), 
								(cell.state.asFloat.rand*0.5).ceil, 
								(cell.state*cellSize.reciprocal).ceil, 
								([0, rule.states.size-1].asSpec.unmap(cell.state)).rand * 2 * pi, 
								([0, rule.states.size-1].asSpec.unmap(cell.state)).rand * 2 * pi ** 1.068),
							Pen.addWedge(
								Point(cell.x * cellSize + 80, cell.y * cellSize + 50), 
								(cell.state.asFloat.rand*0.5).ceil, 
								([0, rule.states.size-1].asSpec.unmap(cell.state)).rand * 2 * pi, 
								([0, rule.states.size-1].asSpec.unmap(cell.state)).rand * 2 * pi ** 1.068)
	//						Pen.lineTo(
	//							(cell.x * cellSize + 
	//								(cell.occurencesOfState(cell.state).rand2 - world.rule.states.size) 
	//									+ 80 + 10.rand2)@
	//							(cell.y * cellSize + (cell.averageState.rand2) + 50 + 10.rand2)
	//						),
	//						Pen.addRect(
	//							Rect(i * cellSize + 80, j * cellSize + 50,
	//								cellSize.rand, cellSize.rand)
	//						)
							
							].wrapAt(cell.state);
//							Pen.perform([\stroke, \fill].wchoose([0.7, 0.3]));
							Pen.strokeRect(
								Rect(i * cellSize + 80, j * cellSize + 50, cellSize, cellSize)
							);
							Color.grey(cell.state.reciprocal).set;
							Pen.fillRect(
								Rect(i * cellSize + 80 + (cellSize * 0.25), 
									j * cellSize + 50 + (cellSize * 0.25), 
									cellSize * 0.5, cellSize * 0.5)
							);
						}
						
						*/
					})
					
				})
			});

//			world.statesx.do({|sum, i|
//				Color.black.set;
//				Pen.strokeRect(
//					Rect(i * cellSize + 80, world.size * cellSize + 50, cellSize, sum)
//				)
//			});
//		
//			world.statesy.do({|sum, i|
//				Color.black.set;
//				Pen.strokeRect(
//					Rect(world.size * cellSize + 80, i * cellSize + 50, sum, cellSize))
//			})
		};		
	
		group = world.world.size;
		currentValues = [];
		
	}
	
	renewDisplay{
		cells.do({|row, i|
			row.do({|cell, j|
				cell.background_(colors[world.world[i][j].state])
			})
		})	
	}
	
	next{
		cells.do({|row, i|
			row.do({|cell, j|
				cell.background_(colors[world.world[i][j].state])
			})
		});
		count = count + 1;
		genCount.string_(count.asString);
		numAlive.string_(world.alive);
		world.next;
	}
		
	start{
		task = Routine({
				maxGens.do({|i|
					this.next;
					window.refresh;
					if (world.active == 0, {
						this.next;
						window.refresh;
						go.value = 0;
						(speed*3).wait;
						task.yieldAndReset(0);
					});
					speed.wait
				});
			}).play(AppClock)	
	}
	
	refresh{
		window.refresh
	}



}