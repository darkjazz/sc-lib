FoaRotate3D{
	*ar{|w, x, y, z, xng, yng, zng|
		#w, x, y, z = FoaTilt.ar(w, x, y, z, xng);
		#w, x, y, z = FoaTumble.ar(w, x, y, z, yng);
		^FoaRotate.ar(w, x, y, z, zng);
	}
}

FoaDirect3D{
	*ar{|w, x, y, z, xng, yng, zng|
		#w, x, y, z = FoaDirectX.ar(w, x, y, z, xng);
		#w, x, y, z = FoaDirectY.ar(w, x, y, z, yng);
		^FoaDirectZ.ar(w, x, y, z, zng);
	}	
}

FoaFocus3D{
	*ar{|w, x, y, z, xng, yng, zng|
		#w, x, y, z = FoaFocusX.ar(w, x, y, z, xng);
		#w, x, y, z = FoaFocusY.ar(w, x, y, z, yng);
		^FoaFocusZ.ar(w, x, y, z, zng);
	}	
}

FoaPush3D{
	*ar{|w, x, y, z, xng, yng, zng|
		#w, x, y, z = FoaPushX.ar(w, x, y, z, xng);
		#w, x, y, z = FoaPushY.ar(w, x, y, z, yng);
		^FoaPushZ.ar(w, x, y, z, zng);
	}	
}

FoaPress3D{
	*ar{|w, x, y, z, xng, yng, zng|
		#w, x, y, z = FoaPressX.ar(w, x, y, z, xng);
		#w, x, y, z = FoaPressY.ar(w, x, y, z, yng);
		^FoaPressZ.ar(w, x, y, z, zng);
	}	
}

FoaZoom3D{
	*ar{|w, x, y, z, xng, yng, zng|
		#w, x, y, z = FoaZoomX.ar(w, x, y, z, xng);
		#w, x, y, z = FoaZoomY.ar(w, x, y, z, yng);
		^FoaZoomZ.ar(w, x, y, z, zng);
	}	
}

FoaDominate3D{
	*ar{|w, x, y, z, xng, yng, zng|
		#w, x, y, z = FoaDominateX.ar(w, x, y, z, xng);
		#w, x, y, z = FoaDominateY.ar(w, x, y, z, yng);
		^FoaDominateZ.ar(w, x, y, z, zng);
	}		
}

