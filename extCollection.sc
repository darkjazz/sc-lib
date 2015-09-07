+ Collection {

	selectIndices {| function |
		var res = this.class.new(this.size);
		this.do {|elem, i| if (function.value(elem, i)) { res = res.add(i) } }
		^res;
	}

}