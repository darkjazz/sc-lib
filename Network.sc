Network{

	var <connectionLimit, <nodeLimit, <groups, <nodes, nextID = 0, recentActiveTargets; 
	
	*newClear{
		
		^super.new;
		
	}
	
	*new{|connLimit, nodeLimit|
		
		^super.newCopyArgs(connLimit, nodeLimit).init
	
	}
	
	init{

		nodes = Array();
		
		this.createNode;

	}
	
	addNodes{|nodeCollection|
		
		nodes = nodes.addAll(nodeCollection.asArray)
		
	}
	
	addGroup{|aGroup|
	
		groups = groups.add(aGroup)
	
	}

	createNode{|gate = 1, state = 0|
	
		var newnode, target;
		
		newnode = NetNode(nextID = nextID + 1, gate, state);
			
		nodes = nodes.add(newnode);
		
		target = this.wchooseFittest;
	
		this.createLink(newnode, target);
		
		this.createLink(this.chooseCloseSource(target), newnode);
	
	}
	
	createLink{|source, target|
	
		if (target.inputCount < connectionLimit)
		{
			
			source.linkToTarget(target)
			
		}
	
	}
	
	removeNode{
	
		var aNode;

		aNode = nodes.remove(nodes.choose);

		nodes.do({|node, i|
			
			node.links.removeAll(node.links.select({|link| link.target == aNode }));
			
			if (node.links.size == 0) { this.createLink(node, this.wchooseFittest) }
			
		})
	}
	
	reconnect{
		
		var aSource = this.wchooseFittest;
		
		this.createLink(aSource, this.wchooseWeakest);
		
	}
	
	choose{ ^nodes.choose }
	
	collect{|function| ^nodes.collect(function) }

	select{|function| ^nodes.select(function) }
	
	wchooseFittest{
	
		^nodes.wchoose(nodes.collect(_.score).normalizeSum)
	
	}
	
	wchooseWeakest{
	
		var weakNodes, min;
		
		min = nodes.collect(_.score).minItem;

		weakNodes = nodes.select({|node| node.score == min });
						
		^weakNodes.choose 
		
	}
	
	chooseCloseSource{|target| 
		
		var links = nodes.select({|node| node.links.isEmpty });
		
		if (links.isEmpty)
		{
		
			links = target.links.collect(_.target)
			
		}
		
		^links.choose
				
	}
	
	checkActivity{
		
		var activeLinks = 0, activeNodes = 0;
		
		nodes.do({|node| activeLinks = activeLinks + node.links.select(_.active).size });
		
		activeNodes = nodes.select(_.active);
		
		if ((activeLinks + activeNodes.size) == 0) { 
		
			this.wchooseFittest.force;
		
			//nodes.do(_.decrementFitness(1));
			
			recentActiveTargets.do(_.decrementFitness(1))
		
		}
		{
		
			//nodes.do(_.incrementFitness(1));
		
			activeNodes.do(_.incrementFitness(3))
		
		}
		
	}
	
	updateStates{
	
//		nodes.do({|node, i|
//			
//			if (node.links[0].active)
//			{
//				
//				node.links.do({|link| link.target.incrementState; link.deactivate;  })
//				
//			}
//			
//		});
			
		nodes.do({|node, i|

			if (node.links.size > 0)
			{

				if (node.links[0].active)
				{
					
					node.links.do({|link| link.target.incrementState; link.deactivate;  })
					
				}
					
			};
		
			if (node.active)
			{
				
				node.links.do(_.activate);
				
				node.state = 0;
				
			};
		
			if (node.state >= node.gate) {  node.activate; }
		
		});
		
		recentActiveTargets = Array();
		
		nodes.do({|node| 
		
			node.gate = max(1, node.inputCount);
			
			if (node.links.size > 0) 
			{
				
				if (node.links[0].active)
				{
				
					recentActiveTargets = recentActiveTargets.addAll(
					
						node.links.collect(_.target)
					
					)
				
				}
				
			} 
		
		})
				
	}
	
	nextNodeID{
		
		if (nodes.isNil) { ^0 } { ^(nodes.last.id + 1) }
		
	}
		
}

NetGroup{
	
	var <nodes;
	
	*new{|nodes|
	
		^super.newCopyArgs(nodes)
	
	}
	
}

NetNode{
	
	var <id, <>gate, <>state, <links, <fitness = 0, <inputCount = 0, activated = false; 
	
	var	<activity = 0;
	
	*new{|id, gate = 1, state = 0| ^super.newCopyArgs(id, gate, state).init }
	
	init{ links = Array(); }

	linkToTarget{|aTarget|
				
		aTarget = aTarget ? this; 
		
		aTarget.addInput;
		
		if (links.select({|lnk| lnk.target == aTarget }).size == 0)
		{
			links = links.add(NetLink(aTarget))
		}
	}
	
	active{ ^(state == -1) }
	
	force{ state = -1; activated = true; }
	
	activate{ state = -1; activity = activity + 1 }
	
	activated{ var value; value = activated; activated = false; ^value }
	
	incrementState{ state = state + 1; }
	
	incrementFitness{|value = 1| fitness = fitness + value }

	decrementFitness{|value = 1| fitness = max(0, fitness - value) }
	
	fitness_{|aValue| fitness = max(0, aValue) }
	
	score{ ^(fitness + activity + inputCount + links.size) }
	
	addInput{ inputCount = inputCount + 1 }
	
	removeInput{ inputCount = inputCount - 1 }

}

NetLink{
	
	var <target, <active = false;
	
	*new{|target|
	
		^super.newCopyArgs(target)
	
	}
	
	changeTarget{|newTarget|
		
		target = newTarget;
		
	}
	
	activate{ active = true }
	
	deactivate{ active = false }
}                                                                                                                                                                             