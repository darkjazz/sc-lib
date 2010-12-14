AtkRotateXYZ{

	*ar{|w, x, y, z, xAng, yAng, zAng|
		#w, x, y, z = AtkTilt.ar(w, x, y, z, xAng );
		#w, x, y, z = AtkTumble.ar(w, x, y, z, yAng );
		^AtkRotate.ar(w, x, y, z, zAng );
	}

}

AtkZoom{
	
	*ar{|w, x, y, z, xZoom, yZoom, zZoom|
		#w, x, y, z = AtkZoomX.ar(w, x, y, z, xZoom );
		#w, x, y, z = AtkZoomY.ar(w, x, y, z, yZoom );
		^AtkZoomZ.ar(w, x, y, z, zZoom );
	}

}

AtkPush{
	
	*ar{|w, x, y, z, xPush, yPush, zPush|
		#w, x, y, z = AtkPushX.ar(w, x, y, z, xPush );
		#w, x, y, z = AtkPushY.ar(w, x, y, z, yPush );
		^AtkPushZ.ar(w, x, y, z, zPush );
	}

}

AtkSquish{
	
	*ar{|w, x, y, z, xSquish, ySquish, zSquish|
		#w, x, y, z = AtkPushX.ar(w, x, y, z, xSquish );
		#w, x, y, z = AtkPushY.ar(w, x, y, z, ySquish );
		^AtkPushZ.ar(w, x, y, z, zSquish );
	}
}

AtkFocus{

	*ar{|w, x, y, z, xFocus, yFocus, zFocus|
		#w, x, y, z = AtkFocusX.ar(w, x, y, z, xFocus );
		#w, x, y, z = AtkFocusY.ar(w, x, y, z, yFocus );
		^AtkFocusZ.ar(w, x, y, z, zFocus );
	}

}

AtkPress{

	*ar{|w, x, y, z, xPress, yPress, zPress|
		#w, x, y, z = AtkFocusX.ar(w, x, y, z, xPress );
		#w, x, y, z = AtkFocusY.ar(w, x, y, z, yPress );
		^AtkFocusZ.ar(w, x, y, z, zPress );
	}

}

AtkDominate{

	*ar{|w, x, y, z, xDominate, yDominate, zDominate|
		#w, x, y, z = AtkDominateX.ar(w, x, y, z, xDominate );
		#w, x, y, z = AtkDominateY.ar(w, x, y, z, yDominate );
		^AtkDominateZ.ar(w, x, y, z, zDominate );
	}

}