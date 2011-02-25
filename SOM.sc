SOMap{
	
	var <>trainDuration, <nodes, inputs, <size;
	var startLearningRate = 0.1, learningRate, <trainCount = 0, timeConst;
	
	*new{|trainDur, numNodes, numInputs, initLo, initHi|
		^super.newCopyArgs(trainDur).init(numNodes, numInputs, initLo, initHi)
	}
	
	init{|numNodes, numInputs, initLo, initHi|
		nodes = Array.fill(numNodes, {|i|
			Array.fill(numNodes, {|j| SONode(i@j, numInputs, initLo, initHi) })
		});
		inputs = numInputs;
		timeConst = trainDuration/log(nodes.size/2);
		learningRate = startLearningRate;
		size = numNodes;
	}
	
	findBMU{|inputVector|
		var best = 10, thenode;
		
		thenode = nodes[0][0];
		
		this.do({|node|
			var diff = (node.weights - inputVector).squared.sum.sqrt;
			if (diff < best) { best = diff; thenode = node }
		})
		
		^thenode
	}
	
	train{|inputVector|
		var bmu, nrad;
		if (trainCount < trainDuration)
		{
			bmu = this.findBMU(inputVector);
			nrad = (nodes.size / 2) * exp(trainCount/timeConst.neg);

			this.do({|node|			
				var dist, influence;
				dist = (bmu.coords.x-node.coords.x).squared + 
					(bmu.coords.y-node.coords.y).squared;
				if (dist < nrad.squared)
				{
					influence = exp(dist / (nrad.squared * 2).neg);
					node.adjustWeights(inputVector, learningRate, influence)
				}
			});
			
			learningRate = startLearningRate * exp(trainCount/trainDuration.neg);
			
			trainCount = trainCount + 1;
			
			^bmu
		}
		{
			"training completed".postln;
		}
	}
	
	do{|function|
		nodes.do({|row, i| 
			row.do({|node, j|
				function.value(node, i, j)
			})
		})
	}
	
}

SONode{
	
	var <coords, <>weights;
	
	*new{|coords, size, lo, hi|
		^super.newCopyArgs(coords).init(size, lo, hi)
	}
	
	init{|size, lo, hi|
		weights = Array.rand(size, lo, hi);
	}
		
	adjustWeights{|target, learningRate, influence|
		weights = weights + (target - weights * learningRate * influence)
	}
}