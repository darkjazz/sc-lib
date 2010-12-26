FxNano{
	
	var fx, nano;

	*new{|fx|
		^super.newCopyArgs(fx).init
	}
	
	init{
		nano = NanoKONTROL();
		this.mapActions;
	}
	
	mapActions{
		var fxglobals, patchAlpha, patchActive;
		
		fxglobals = fx.oglGui.glPanel.children.select(_.isKindOf(SmoothSlider));
		
		nano.sliders[0].do({|nanoSlider, i|
			nanoSlider.action = {|slider|
				if (i > 2)
				{
					fxglobals[i+2].value = slider.value;
					fxglobals[i+2].doAction;
				}
				{
					fxglobals[i].value = slider.value;
					fxglobals[i].doAction;			
				}
			}
		});
		
		patchAlpha = fx.oglGui.ptPanel.children.select(_.isKindOf(SmoothSlider))
			.select({|it, i| i.even});
		
		nano.knobs[0].do({|nanoKnob, i|
			nanoKnob.action = {|knob|
				patchAlpha[i].value = knob.value;
				patchAlpha[i].doAction;
			}
		});
		
		patchActive = fx.oglGui.ptPanel.children.select(_.isKindOf(RoundButton))
			.select({|it, i| i%4 == 0 });
		
		nano.buttons[0][0..8].do({|nanoButton, i|
			nanoButton.action = {|button|
				patchActive[i].value = button.value;
				patchActive[i].doAction;
			}
		})
	}

}