Creature{
	classvar <strategies;
	var <>score, <strategy, <cooperate, <>lastAction, memory;

	*initClass{
		strategies = (
			cheat: { false },
			sucker: { true },
			titat: {|opp| opp.lastAction ? true },
			grudger: {|opp, memory| memory[opp.strategy] },
			prober: {|opp, memory| if (opp.lastAction.notNil) {
				if (opp.lastAction) {[true, false].wchoose([0.8, 0.2])} 
				{false} 
				} { true }
				},
			random: { [true, false].choose },
			titfor2tats: {|opp, memory|
			
			}
		);
		
	}
	
	*new{|score, strategy|
		^super.newCopyArgs(score, strategy).init
	}
	
	init{
		memory = Event();
		strategies.keysDo({|key|
			memory.put(key, true)
		});
		if ((strategy == \titat).or(strategy == \grudger).or(strategy == \prober)) {
			cooperate = true
		}
		{
			cooperate = strategies[strategy].value;
		}
	}

	setBehaviour{|opponent|
		memory.put(opponent.strategy, opponent.lastAction ? true);
		lastAction = cooperate;
		cooperate = strategies[strategy].value(opponent, memory)
	}

	updateScore{|opponent|
		if ((cooperate.not).and(opponent.cooperate)) { score = score + 5.0 };
		if ((cooperate).and(opponent.cooperate)) { score = score + 3.0 };
		if ((cooperate.not).and(opponent.cooperate.not)) { score = score - 0.0 };
		if (cooperate.and(opponent.cooperate.not)) { score = score - 1.0 };
	}
	
}

Game{
	var <creatures, times = 200;

	*new{
		^super.new.init
	}
	
	init{
		creatures = Array();
		Creature.strategies.keysDo({|key|
			20.do({
				creatures = creatures.add(Creature(0.0, key))
			})
		});
	}
	
	run{
		creatures.do({|dude|
			creatures.do({|opponent|
				dude.lastAction = nil;
				opponent.lastAction = nil;
				times.do({
					dude.setBehaviour(opponent);
					opponent.setBehaviour(dude);
					dude.updateScore(opponent);
					opponent.updateScore(dude);
				})
			})
		})
	}

}
