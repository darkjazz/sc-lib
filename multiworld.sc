MultiWorld{
	var <dimensions, <nodes;
	
	*new{|dimensions|
		^super.newCopyArgs(dimensions).init
	}
	
	init{
		nodes = Array.fillND(dimensions, {|...indices| MultiWorldNode(indices) })
	}
}

MultiWorldNode{
	var <coords, states;
	
	*new{|coords|
		^super.newCopyArgs(coords).init
	}
	
	init{
		states = Array.fill(3, 0);
	}
	
	setState{|index, state|
		states[index] = state
	}
	
	stateAt{|index|
		^states[index]
	}
}