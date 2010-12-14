FxMrmr : Mrmr {

	var <>actions;
	
	*new { |host,port,name|
		^super.new(host,port,name).initialize
	}
	
	initialize{
		actions = List();
	}
		
	addControl { |type,gridCols,gridRows,posCol,posRow,colSpan(1),rowSpan(1),title("_"),style(1),
		responseFunction|
		/*@
		desc: Adds an element (control) to the interface.  For more information on the available controls, go to mrmr noisepages website interface commands page.  A more detailed example can be found at the end of this document.
		type: the kind of control. Can be one of the following: [\pushbutton, \togglebutton, \tactilezone, \slider, \textview, \textinputview, \titleview, \accelerometer, \webview]
		gridCols: number of grid columns for layout
		gridRows: number of grid rows for layout
		posCol: the column position of the element
		posRow: the row position of the element
		colSpan: the number of columns spanned by the element
		rowSpan: the number of rows spanned by the element
		title: the title associated with the element
		style: the style of the element
		@*/
		var messageNames, controlTypes, controlName;
		title = title.replace(" ","_");
		messageNames = List();
		controlTypes = List();
		type.switch(
			\pushbutton, {
				messageNames.add("pushbutton");
				controlTypes.add("PB");
				controlName = "pushbutton";
			},
			\togglebutton, {
				messageNames.add("pushbutton");
				controlTypes.add("PB");
				controlName = "togglebutton";
			},
			\tactilezone, {
				messageNames.addAll(["tactilezoneX","tactilezoneY","tactilezoneTouchDown"]);
				controlTypes.addAll(["TX","TY","TB"]);
				controlName = "tactilezone";
			},
			\slider, {
				messageNames.add("slider/horizontal");
				controlTypes.add("S");
				controlName = "slider";
			},
			\textview, {
				controlName = "textview";
			},
			\textinputview, {
				messageNames.add("textinput");
				controlTypes.add("Txt");
				controlName = "textinputview";
			},
			\titleview, {
				controlName = "titleview";
			},
			\accelerometer, {
				style.switch(
					1, {
						messageNames.addAll(["accelerometerX","accelerometerY","accelerometerZ"]);
						controlTypes.addAll(["AX","AY","AZ"]);
					},
					2, {
						messageNames.addAll(["accelerometer/direction","accelerometer/force"]);
						controlTypes.addAll(["AD","AF"]);
					},
					3, {
						messageNames.addAll(["accelerometer/angle", "accelerometer/force"]);
						controlTypes.addAll(["AA","AF"]);
					},
					{("undefined style for accelerometer:"+style).postln; ^this;}
				);
				controlName = "accelerometer";
			},
			\webview, {
				controlName = "webview";
			},
			{("did not recognize control type:"+type).postln; ^this;}
		);
		messageNames.do( { |item, i|
			this.interfaceMessages.add("/mrmr/"
			                           ++item++"/"
			                           ++this.interfaceIndex++"/"
			                           ++this.iPhoneName);
		});
		controlTypes.do( { |item, i|
			this.interfaceLabels.add((item++"("++this.interfaceIndex++")").asSymbol);
		});
		this.interfaceCommands.add("/mrmrIB"+controlName+"nil 0.2"+gridCols+gridRows
		                                                         +posCol  +posRow
		                                                         +colSpan +rowSpan
		                                                         +title+style
		                                                         +"\n");
		if (responseFunction.notNil) { this.actions.add(responseFunction) };
		this.interfaceIndex = this.interfaceIndex+1;
	}

	resetInterface {
		/*@
		desc: Resets the internal representation of the interface. <br><b>Be sure to run this before creating a new interface!</b>
		@*/
		this.interfaceCommands = List();
		this.interfaceMessages = List();
		this.interfaceLabels = List();
		this.actions = List();
		this.interfaceIndex = 0;
	}
	
	
	setupInterface { |debug(false)|
		/*@
		desc: Sends the current form of the interface to the device and begins listening for messages returned by the device.
		debug: wether or not to also post the values returned by the device
		@*/
		this.removeResponders;
		this.clearAll;
		this.connect;
		this.interfaceCommands.do( {|item, i|
			this.net.sendRaw(item);
		});
		this.disconnect;
		this.interfaceMessages.do( {|item, i|
			this.busses.add(Bus.control(Server.default));
			this.responders.add(
				OSCresponderNode(nil,item, {|time, responder, msg|
					if(debug,{[this.interfaceLabels[i],msg[1]].postln;});
					this.busses[i].set(msg[1]);
					this.actions[i].value(msg[1]);
				}).add
			);
		});
	}
	
	changeBank{|index|
		this.connect;
		this.net.sendRaw("/mrmrIB mrmr_changeBank:("++index.asString++")\n");
	}

	setBankLimit{|max|
		this.connect;
		this.net.sendRaw("/mrmrIB mrmr_setNumberOfBanksVisibe:"++max.asString++"\n");
	}	
}