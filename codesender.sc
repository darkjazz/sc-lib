CodeSender{
	var graphics, documents, ctrStates;
	*new{|graphics|
		^super.newCopyArgs(graphics).init;
	}

	init{
		documents = Array();
		ctrStates = Array();
	}

	registerDocument{|document, prompt="@ "|
		documents = documents.add(document);
		ctrStates = ctrStates.add(false);

		document.keyDownAction = {|doc, char, mod, uni, key|
			if (uni == 65533) {
				ctrStates[ctrStates.lastIndex] = true
			}
		};

		document.keyUpAction = {|doc, char, mod, uni, key|
			var sendarray;
			if ((uni == 13).and(ctrStates[ctrStates.lastIndex])) {
				sendarray = document.selectedString.split(Char.nl);
				sendarray[0] = prompt ++ sendarray[0];
				sendarray.do({|str|
					graphics.sendCodeLine(str)
				});
				sendarray.postln
			};

			if (uni == 65533) {
				ctrStates[ctrStates.lastIndex] = false
			};
		}

	}

}