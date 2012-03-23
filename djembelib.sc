// Thanks to Thorfinn for his hard work transforming the patterns into arrays!!!

DjembeLib{
	
	classvar <patterns;
		
	*initClass{
		patterns = (
			kokou: [
				[1,0,1,1,0,0,1,0,1,0,1,1,0,0,1,0],
				#[lo,mi,mi,hi],
				[1,0,0,1,0,1,1,0,1,0,1,1,1,0,1,0],
				#[lo,hi,lo,hi,lo,mi,mi,mi,hi],
				[1,1,0,1,1,1,1,0,1,1,0,1,1,1,1,0],
				#[mi,mi,hi,mi,mi,hi,mi,mi,hi,mi,mi,hi],
				[1,0,0,1,0,0,1,0,1,0,0,0,0,0,1,0],
				#[mi,lo,mi,mi,mi],
				[1,0,0,1,0,0,1,0,1,0,1,0,1,0,1,0]
			],
			
			soli: [
				[1,0,1,1,0,0,1,0,1,1,0,0],
				#[hi,mi,hi,hi,mi,hi],
				[1,0,1,0,1,1,1,0,1,1,0,1],
				#[lo,hi,mi,mi,hi,hi,lo,hi],
				[1,0,1,1,0,1,1,1,1,0,1,0],
				#[lo,hi,hi,hi,mi,mi,hi,hi],
				[1,0,0,0,1,0,1,0,1,1,0,0],
				#[lo,mi,mi,lo,lo],
				[1,0,1,0,1,0,1,0,0,0,1,0]
			],
			
			raboday: [
				[1,0,0,1,1,0,1,0,1,0,0,1,1,0,1,0],
				#[lo,lo,lo,hi,lo,lo,lo,hi],
				[1,1,0,0,1,0,0,0,1,0,1,0,1,0,0,0],
				#[mi,mi,lo,mi,mi,lo],
				[1,0,1,1,0,1,1,0,1,0,1,1,0,1,1,0],
				#[mi,mi,mi,mi,mi,mi,mi,mi,mi,mi],
				[1,0,0,0,1,0,0,0,1,0,0,0,1,0,0,0],
				#[lo,mi,lo,mi],
				[1,0,0,1,0,0,1,0,1,0,0,1,0,0,1,0]
			],
			
			macrou: [
				[1,0,1,1,1,0,1,0,1,1,1,1,1,0,1,0],
				#[lo,mi,hi,lo,mi,lo,mi,mi,hi,lo,mi],
				[1,0,1,1,0,1,1,0,1,0,1,1,0,1,1,0,1,0,1,0,1,1,0,0,1,0,0,0,1,0,0,0],
				#[mi,mi,mi,mi,hi,hi,mi,mi,mi,mi,hi,hi],
				[1,0,1,1,0,1,1,0,1,0,1,0,1,1,1,0],
				#[hi,mi,hi,mi,mi,mi,hi,mi,mi,mi],
				[1,0,0,1,1,0,0,0,1,0,0,0,1,0,0,0],
				#[lo,lo,lo,mi,mi],
				[1,0,0,1,0,0,1,0,0,0,1,0,1,0,0,0]
			],
			
			yole: [
				[1,1,0,1,1,0,1,1,1,0,1,0,1,0,1,1],
				#[lo,hi,hi,lo,mi,mi,lo,hi,lo,mi,mi],
				[1,0,0,1,0,1,1,0,1,0,0,0,1,1,0,0],
				#[mi,lo,hi,lo,hi,mi,mi],
				[1,0,1,1,0,1,1,1,1,0,1,1,0,1,1,1],
				#[hi,hi,hi,hi,mi,mi,hi,hi,hi,hi,mi,mi],
				[1,0,0,0,1,0,0,0,1,0,1,0,1,0,0,0],
				#[lo,mi,lo,lo,mi],
				[0,0,1,0,0,0,1,0,0,0,1,0,0,0,1,0]
			],
			
			tiriba: [
				[1,0,1,1,1,1,1,1,1,1,1,0],
				#[lo,hi,lo,mi,mi,lo,hi,hi,lo,mi],
				[1,0,1,1,0,0,1,0,1,1,0,0],
				#[hi,mi,mi,hi,mi,mi],
				[1,0,1,0,1,1,0,0,0,0,1,1],
				#[lo,lo,mi,mi,mi,mi],
				[1,0,0,0,1,1,0,1,0,1,0,0],
				#[lo,lo,lo,mi,lo],
				[1,0,1,0,1,1,0,1,0,1,0,1]
			],
			
			kpanilogo: [
				[1,1,0,1,1,0,1,1,1,0,1,1,1,1,1,0],
				#[mi,mi,hi,lo,hi,hi,mi,hi,hi,lo,mi,mi],
				[1,1,1,1,1,0,1,1,1,0,1,0,1,0,1,1],
				#[mi,mi,mi,mi,lo,hi,hi,mi,mi,lo,hi,hi],
				[0,0,1,1,0,0,1,1,0,0,1,1,0,0,1,1],
				#[hi,hi,mi,mi,hi,hi,mi,mi],
				[1,0,0,0,1,0,0,0,1,0,1,0,1,0,0,0],
				#[lo,mi,lo,lo,mi],
				[1,0,0,1,0,0,1,0,0,0,1,0,1,0,0,0]
			],
			
			kakilambe: [
				[1,0,1,1,1,0,1,1,0,1,1,0],
				#[lo,mi,mi,hi,lo,mi,mi,hi],
				[1,0,1,0,1,0,0,1,0,1,1,0,1,0,0,0,1,0,0,1,0,1,1,0],
				#[lo,lo,lo,hi,mi,hi,lo,hi,mi,mi,hi],
				[1,0,1,1,0,1,1,0,1,1,0,1],
				#[lo,lo,lo,mi,mi,lo,mi,mi,mi],
				[1,0,1,0,1,0,0,1,0,1,0,0,1,0,0,0,1,0,0,1,0,1,0,0],
				#[lo,mi,lo,lo,mi],
				[1,0,1,0,1,0,1,1,0,1,1,0]
			],
			
			cassa: [
				[1,0,1,1,0,0,1,1,1,0,1,1,1,0,1,1],
				#[mi,hi,hi,hi,mi,mi,hi,hi,lo,hi,mi],
				[1,0,0,1,1,0,1,1,1,0,0,1,1,0,1,1],
				#[hi,hi,hi,mi,mi,hi,hi,hi,mi,mi],
				[0,0,1,1,0,0,1,0,0,0,1,1,0,0,1,0],
				#[mi,mi,hi,mi,mi,hi],
				[1,0,0,1,1,0,0,0,1,0,1,0,0,0,0,0,1,0,1,0,0,0,0,0,1,0,1,0,0,0,0,0],
				#[mi,lo,lo,mi,mi,mi,mi,mi,mi],
				[1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0]
			],
			
			basikolo: [
				[1,1,1,0,1,0,0,1,0,1,1,0],
				#[lo,hi,mi,hi,hi,lo,hi],
				[1,0,0,0,1,1,1,0,0,1,1,1],
				#[hi,mi,mi,hi,hi,mi,mi],
				[1,0,1,1,0,0,1,1,1,1,0,0],
				#[hi,mi,mi,hi,mi,mi,hi],
				[1,0,0,1,1,0,1,0,0,1,0,0],
				#[lo,mi,lo,lo,mi],
				[1,0,1,1,1,0,1,1,0,1,0,1]
			],
			
			sorsornet: [
				[1,1,1,1,1,1,1,1,1,1,1,1],
				#[hi,hi,hi,hi,mi,mi,hi,hi,hi,hi,mi,mi],
				[1,0,1,1,0,0,1,0,1,1,0,0],
				#[hi,mi,hi,hi,mi,hi],
				[1,1,1,0,1,1,0,0,1,1,1,1,1,1,1,0,1,1,0,0,1,0,1,0],
				#[lo,lo,lo,mi,mi,lo,lo,mi,mi,lo,lo,lo,mi,mi,hi,mi],
				[1,1,1,0,1,0,0,0,1,0,1,0],
				#[lo,lo,lo,mi,lo,mi],
				[1,1,1,0,1,0,1,0,1,0,1,0]
			],
			
			koukou: [
				[1,0,0,1,1,0,1,1,1,0,0,1,1,0,1,1],
				#[hi,hi,hi,mi,mi,hi,hi,hi,mi,mi],
				[1,0,1,1,0,1,1,1,1,0,1,1,0,1,1,1],
				#[hi,hi,hi,hi,mi,mi,hi,hi,hi,hi,mi,mi],
				[1,0,0,1,1,0,0,0,1,1,0,1,1,0,1,0],
				#[lo,mi,mi,lo,mi,mi,mi,mi],
				[1,0,0,1,0,0,1,0,1,0,1,0,1,0,1,0],
				#[lo,mi,mi,mi,lo,lo,lo],
				[1,0,0,1,0,0,1,0,1,0,0,1,0,0,1,0 ]
			],
			
			foret: [
				[1,0,1,0,1,1,1,0,1,1,0,1],
				#[lo,hi,hi,hi,lo,hi,hi,hi ],
				[1,0,0,1,1,1,1,1,1,1,1,1],
				#[hi,hi,mi,mi,hi,hi,hi,hi,mi,mi],
				[1,0,1,1,0,0,1,0,1,1,0,0],
				#[hi,mi,hi,hi,mi,hi],
				[1,0,0,1,0,0,1,0,0,0,1,0],
				#[lo,lo,lo,mi],
				[1,0,1,1,0,1,1,0,1,0,1,0]
			],
			
			liberte: [
				[1,1,1,0,1,1,0,1,1,1,0,0],
				#[lo,hi,hi,mi,mi,hi,hi,hi],
				[1,0,0,1,0,0,1,0,0,1,1,1],
				#[lo,lo,lo,lo,mi,mi],
				[1,0,1,1,0,0,1,0,1,1,0,0 ],
				#[hi,mi,hi,hi,mi,hi],
				[1,0,0,0,1,1,0,1,0,1,0,0],
				#[lo,mi,mi,lo,lo],
				[1,0,1,0,1,1,0,1,0,1,0,1 ]
			],
			
			djakandi: [
				[1,0,0,1,0,1,1,0,1,1,0,1],
				#[lo,hi,lo,lo,mi,mi,lo],
				[1,0,1,1,0,1,1,0,1,0,0,1],
				#[hi,hi,mi,mi,hi,hi,lo],
				[1,0,0,1,0,0,1,0,0,0,0,1,1,0,0,1,0,1,0,0,0,0,0,1],
				#[lo,mi,lo,lo,lo,mi,mi,lo],
				[1,0,0,1,0,0,1,0,0,0,0,1,1,0,0,1,0,1,0,0,0,0,0,1],
				#[lo,mi,lo,lo,lo,mi,mi,lo],
				[1,0,1,0,0,1,0,0,1,1,0,0]
			],
			
			rumba: [
				[1,0,0,1,1,0,0,0,1,0,1,0,1,0,0,0],
				#[lo,mi,lo,lo,hi,lo],
				[1,0,0,0,0,0,1,1,1,0,1,0,0,0,1,1],
				#[lo,mi,mi,lo,lo,mi,mi],
				[1,0,0,0,1,0,1,0,1,0,0,0,1,0,1,0],
				#[lo,mi,mi,lo,mi,mi],
				[1,0,0,1,0,0,1,0,0,0,1,0,1,0,1,0],
				#[lo,lo,mi,lo,lo,mi],
				[1,0,1,1,1,0,1,0,1,0,1,0,1,0,1,0 ]
			],
			
			sokou: [
				[1,0,1,1,1,1,1,0,1,1,1,1],
				#[hi,hi,hi,mi,mi,hi,hi,hi,mi,mi],
				[1,0,1,1,0,0,1,0,1,1,0,0],
				#[hi,mi,hi,hi,mi,hi],
				[1,1,0,0,1,0,1,1,0,0,1,0],
				#[mi,mi,lo,mi,mi,lo],
				[1,0,1,0,0,0,1,1,0,1,1,0,1,0,1,0,1,0,1,1,0,1,1,0],
				#[lo,mi,lo,lo,lo,lo,lo,mi,mi,mi,mi,lo,lo],
				[1,0,1,0,1,0,1,1,0,1,1,0 ]
			],
			
			mandiani: [
				[1,0,0,1,1,0,0,1,1,1,1,1],
				#[hi,hi,hi,lo,hi,hi,mi,mi],
				[1,0,1,1,0,0,1,0,1,1,0,0],
				#[hi,mi,hi,hi,mi,hi],
				[0,0,1,0,1,1,0,0,1,0,1,1],
				#[hi,mi,hi,hi,mi,hi],
				[1,0,1,0,0,1,0,0,1,0,1,0],
				#[lo,lo,mi,lo,lo],
				[1,0,1,1,0,1,1,0,1,0,1,0]
			],
			
			doudoumba: [
				[1,0,0,1,1,1,1,0,0,1,1,1],
				#[hi,hi,mi,mi,hi,hi,mi,mi],
				[1,1,0,0,0,0,1,1,1,0,1,0],
				#[mi,mi,mi,mi,lo,lo],
				[0,0,1,0,1,1,0,0,1,0,1,1],
				#[hi,mi,hi,hi,mi,hi],
				[1,0,1,1,0,1,0,1,1,0,1,0,1,0,1,0,0,1,0,1,0,0,0,0],
				#[mi,lo,lo,lo,lo,lo,lo,lo,mi,mi,mi],
				[1,0,1,1,0,1,0,1,1,0,1,0]
			],
			
			diansa: [
				[1,0,0,1,1,0,1,1,1,0,0,1,1,0,1,1],
				#[hi,hi,hi,mi,mi,hi,hi,hi,mi,mi],
				[1,1,1,1,0,0,1,1,1,1,1,1,0,0,1,1],
				#[mi,mi,hi,hi,hi,hi,mi,mi,hi,hi,hi,hi],
				[1,1,0,1,1,0,1,1,1,0,1,0,1,1,1,1],
				#[hi,hi,hi,hi,mi,mi,hi,lo,hi,lo,mi,mi],
				[1,0,0,1,0,0,1,0,0,0,1,0,0,0,1,0],
				#[lo,lo,lo,mi,lo],
				[1,0,1,1,0,1,1,0,1,0,1,0,1,0,1,0]
			]
			
		)
	}
	
	*convert{|name, quant=1, dropEmpty=true|
		var s1, s2, s3, s4, p1, p2, p3, p4, p5, patterns, pat, patsize;
		pat = this.patterns[name];
		patterns = Array.newClear(12);
		s1 = Pseq(pat[1], 2).asStream;
		p1 = pat[0].collect({|seq| if (seq == 1) { s1.next.switch('lo', 1, 'mi', 2, 'hi', 3) } { 0 } });
		s2 = Pseq(pat[3], 2).asStream;
		p2 = pat[2].collect({|seq| if (seq == 1) { s2.next.switch('lo', 1, 'mi', 2, 'hi', 3) } { 0 } });
		s3 = Pseq(pat[5], 2).asStream;
		p3 = pat[4].collect({|seq| if (seq == 1) { s3.next.switch('lo', 1, 'mi', 2, 'hi', 3) } { 0 } });
		s4 = Pseq(pat[7], 2).asStream;
		p4 = pat[6].collect({|seq| if (seq == 1) { s4.next.switch('lo', 1, 'mi', 2, 'hi', 3) } { 0 } });
		p5 = pat[8];
		
		patsize = [p1, p2, p3, p4, p5].collect(_.size).maxItem;
		
		if (quant > 1) {
			p1 = ([p1] ++ (0 ! patsize).dup(quant-1)).lace(patsize*quant);
			p2 = ([p2] ++ (0 ! patsize).dup(quant-1)).lace(patsize*quant);
			p3 = ([p3] ++ (0 ! patsize).dup(quant-1)).lace(patsize*quant);
			p4 = ([p4] ++ (0 ! patsize).dup(quant-1)).lace(patsize*quant);
			p5 = ([p5] ++ (0 ! patsize).dup(quant-1)).lace(patsize*quant);
		};
				
		patterns[0] = p1.collect({|seq| if (seq == 1) { 1 } { 0 } });
		patterns[1] = p1.collect({|seq| if (seq == 2) { 1 } { 0 } });
		patterns[2] = p1.collect({|seq| if (seq == 3) { 1 } { 0 } });
	
		patterns[3] = p2.collect({|seq| if (seq == 1) { 1 } { 0 } });
		patterns[4] = p2.collect({|seq| if (seq == 2) { 1 } { 0 } });
		patterns[5] = p2.collect({|seq| if (seq == 3) { 1 } { 0 } });
	
		patterns[6] = p3.collect({|seq| if (seq == 1) { 1 } { 0 } });
		patterns[7] = p3.collect({|seq| if (seq == 2) { 1 } { 0 } });
		patterns[8] = p3.collect({|seq| if (seq == 3) { 1 } { 0 } });
		
		patterns[9] = p4.collect({|seq| if (seq == 1) { 1 } { 0 } });
		patterns[10] = p4.collect({|seq| if (seq == 2) { 1 } { 0 } });
	
		patterns[11] = p5;
		
		if (quant == 1)
		{
			patterns = patterns.collect({|pat, i|
				pat.dup((patsize/pat.size).asInt).flat
			});
		};
				
		if (dropEmpty) { patterns = patterns.select({|seq| seq.sum > 0 }) };
				
		^patterns
		
	}
	
	*convertAll{|quant=1, dropEmpty=true|
		^patterns.collect({|pat, name| this.convert(name, quant, dropEmpty) })
	}
	
	*makeSparse{|name, quant|
		^SparsePattern(this.convert(name, quant))
	}
	
	*makeAllSparse{|quant|
		^this.convertAll(quant).collect(SparsePattern(_)).collect(_.makeSparse).collect(_.patterns)
	}
	
}

DjembePattern{
	
	var <name, <def;
	var <beatpat1, <beatseq1, <beatpat2, <beatseq2, <beatpat3, <beatseq3;
	var <basspat, <bassseq, <bellpat, args, player;
	
	*new{|name|
		^super.newCopyArgs(name).init
	}
	
	init{
		var bpat1, bseq1, bpat2, bseq2, bpat3, bseq3, bspat, bsseq, blpat;
		#bpat1, bseq1, bpat2, bseq2, bpat3, bseq3, bspat, bsseq, blpat = DjembeLib.patterns[name];
		def = (
			beatpat1: Pseq(bpat1, inf).asStream,
			beatseq1: Pseq(bseq1, inf).asStream,
			beatpat2: Pseq(bpat2, inf).asStream,
			beatseq2: Pseq(bseq2, inf).asStream,
			beatpat3: Pseq(bpat3, inf).asStream,
			beatseq3: Pseq(bseq3, inf).asStream,
			basspat: Pseq(bspat, inf).asStream,
			bassseq: Pseq(bsseq, inf).asStream,
			bellpat: Pseq(blpat, inf).asStream		
		);
	}
	
	join{|joinName|
		var bpat1, bseq1, bpat2, bseq2, bpat3, bseq3, bspat, bsseq, blpat;
		var jbpat1, jbseq1, jbpat2, jbseq2, jbpat3, jbseq3, jbspat, jbsseq, jblpat;
		#bpat1, bseq1, bpat2, bseq2, bpat3, bseq3, bspat, bsseq, blpat = DjembeLib.patterns[name];
		#jbpat1, jbseq1, jbpat2, jbseq2, jbpat3, jbseq3, jbspat, jbsseq, jblpat = DjembeLib.patterns[joinName];
		def = (
			beatpat1: Pseq(bpat1++jbpat1, inf).asStream,
			beatseq1: Pseq(bseq1++jbseq1, inf).asStream,
			beatpat2: Pseq(bpat2++jbpat2, inf).asStream,
			beatseq2: Pseq(bseq2++jbseq2, inf).asStream,
			beatpat3: Pseq(bpat3++jbpat3, inf).asStream,
			beatseq3: Pseq(bseq3++jbseq3, inf).asStream,
			basspat: Pseq(bspat++jbspat, inf).asStream,
			bassseq: Pseq(bsseq++jbsseq, inf).asStream,
			bellpat: Pseq(blpat++jblpat, inf).asStream		
		);
	}
	
	next{
		^def.collect(_.next)
	}

	play{|name, server|

		var scale;
		
		scale = Array.geom(40, 1.1574e-05 * (2**21), 2**(1/5)).clump(5);
		
		args = (
		
			bell: (
				args: (\am: 0.2, \fr: scale[4][2], \dr: 0.001, \ph: 0.5pi, \pn: 0, 
					\rt: 0.5, \ra: 0.01, \at: 0.01, \re: 0.99, \cu: 4, \du: 0.005)
			),
			
			bass: (
				args: (\am: 0.03, \nm: 20, \wd: 0.001, \dr: 0.001, \pn: 0, \ra: 0.1, 
					\du: 0.005),
				lo: (\fr: scale[2][2], \nf: scale[6][0], \at: 0.01, \re: 0.99, \cu: 4),
				mi: (\fr: scale[1][4], \nf: scale[7][4], \at: 0.1, \re: 0.9, \cu: -4)
			),
			
			beat: (
				args: (\am: 0.05, \dr: 0.1, \pn: 0, \rt: 1, \ra: 0.1, \du: 0.5),
				lo: (\cf: scale[1][4], \mf: scale[1][0], \pf: scale[1][2], 
					\fi: 50, \ip: 25, \at: 0.001, \re: 0.999, \cu: 4),
			 	mi: (\cf: scale[2][4], \mf: scale[2][0], \pf: scale[2][2], 
			 		\fi: 75, \ip: 50, \at: 0.2, \re: 0.8, \cu: -2),
			 	hi: (\cf: scale[4][4], \mf: scale[4][0], \pf: scale[4][2], 
			 		\fi: 100, \ip: 75, \at: 0.1, \re: 0.9, \cu: 4)
			)		
		);
		
		Routine({
			
			SynthDef(\bell, {|ou, am, fr, dr, ph, pn, rt, ra, at, re, cu, du|
				var sig,frqs;
				frqs = Array.geom(5,1,4**(1/5));
				sig = SinOsc.ar(fr,ph,am)*EnvGen.kr(Env.perc(at,re,curve:cu),timeScale:dr);
				sig = sig + Klank.ar(`[fr*frqs,frqs.reciprocal,dr*frqs.reciprocal],Reverb.ar(sig*ra,rt));
				Line.kr(dur: du, doneAction: 2);
				Out.ar(ou,BFEncode1.ar(sig,pn));
			}).add;
			
			SynthDef(\beat, {|ou, am, cf, mf, pf, dr, fi, ip, pn, rt, ra, at, re, cu, du|
				var sig, car;
				car = SinOsc.ar(mf,SinOsc.ar(pf,0,ip),fi);
				sig = SinOsc.ar(cf+car)*EnvGen.kr(Env.perc(at,re,curve:cu),timeScale:dr);
				sig = sig + Reverb.ar(sig*ra,rt);
				Line.kr(dur: du, doneAction: 2);
				Out.ar(ou,BFEncode1.ar(sig,pn))
			}).add;
			
			SynthDef(\bass, {|ou, fr, nf, am, nm, wd, dr, pn, rt, ra, at, re, cu, du|
				var sig, frqs;
				frqs = Array.geom(5,1,19/16);
				sig = VarSaw.ar(fr+LFNoise0.ar(nf,nm),0,wd)
					*EnvGen.kr(Env.perc(at,re,curve:cu),timeScale:dr);
				sig = sig+Klank.ar(`[fr*frqs,frqs.reciprocal,dr*frqs.reciprocal],sig*ra);
				Line.kr(dur: du, doneAction: 2);
				Out.ar(ou,BFEncode1.ar(sig,pn))
			}).add;		
			
			server = server ? Server.default;
			server.sync;
			
			player = Ppar(
				Pbind(\instrument, \beat, *(args.beat.args)),
				Pbind(\instrument, \beat, *(args.beat.args)),
				Pbind(\instrument, \beat, *(args.beat.args)),
				Pbind(\instrument, \bass, *(args.bass.args)),
				Pbind(\instrument, \bell, *(args.bell.args))
			)
				
		}).play
	}	
	
}

SparsePattern{
	
	var original, <patterns, <subpatterns;
	
	*new{|original|
		^super.newCopyArgs(original)
	}
		
	makeSparse{|startFirst=false|
		var beatsum, order, copy, rotate = 0;
		
		order = Pseq(original.collect(_.sum)
			.collect({|count, i| (count:count,index:i) })
			.sort({|a, b| a['count'] < b['count'] })
			.collect(_.index),
			1
		).asStream;
		
		copy = Array.fill(original.first.size, {
			(0 ! original.size)
		});
		
		beatsum = original.flop.collect(_.sum);
		
		(1..beatsum.maxItem).do({|num|
			beatsum.selectIndices({|sum| sum == num }).do({|ind|
				var slot;
				slot = order.next;
				if (slot.notNil) {  
					copy[ind][slot] = 1;
				}
			})
		});
		
		if (startFirst) { rotate = copy.selectIndices({|a| a.first == 1 }).first };
		
		patterns = Array.newClear(copy.flop.size);
		
		copy.flop.do({|seq, i|
			patterns[i] = seq.rotate(rotate.neg);
		});
		
	}
	
	makeSubPatterns{|numPatterns=3|
		subpatterns = Array();
		numPatterns.do({|i|
			subpatterns = subpatterns.add(
				SparsePattern(original - ([patterns] ++ subpatterns).sum).makeSparse.patterns
			);
		});
	}
	
}