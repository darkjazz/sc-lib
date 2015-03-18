+UGEP {
	
	draw{
		var win, height;
		if (colors.isNil) {
			colors = methods.collect({ Color( *{1.0.rand.round(0.1)} ! 3 ) })
		};
		if (names.isNil)
		{
			names = methods.collect({|ugen| ugen.name.asString.keep(4) });
		};
		height = this.chromosomes.size * 10 + 
			(this.chromosomes.last.generation * 10);
		win = Window("GES population", Rect(100, 100, 450, 800))
			.background_(Color.grey(0.1)).front;
		win.drawFunc = {
			var currentGen = 0;
			var posy = 10;
			this.chromosomes.do({|chrom, y|
				var cols, ugens;
				ugens = chrom.code.select({|codon| codon.isKindOf(Class) });
				cols = ugens.collect({|ugen| colors[methods.indexOf(ugen)] });
				if (currentGen == chrom.generation) 
				{ posy = posy + 10; } 
				{ posy = posy + 20; currentGen = chrom.generation};
				Pen.font = Font("Inconsolata", 8);
				cols.do({|color, x|
					Pen.fillColor = Color.grey(0.8);
					Pen.stringInRect(y.asString.padLeft(2), Rect(10, posy, 10, 7));
					color.set;
					Pen.fillRect(Rect(x*12+20, posy, 12, 7));
				})
			})	
		};
	}
	
	drawOrder{|indices|

		var win, height;
		if (colors.isNil) {
			colors = methods.collect({ Color( *{1.0.rand.round(0.1)} ! 3 ) })
		};
		if (names.isNil)
		{
			names = methods.collect({|ugen| ugen.name.asString.keep(4) });
		};
		height = this.chromosomes.size * 10 + 40;
		win = Window("GES population", Rect(100, 100, 450, 800))
			.background_(Color.grey(0.1)).front;
		win.drawFunc = {
			var currentGen = 0;
			var posy = 10;
			indices.do({|ind, y|
				var chrom, cols, ugens;
				chrom = this.chromosomes[ind];
				ugens = chrom.code.select({|codon| codon.isKindOf(Class) });
				cols = ugens.collect({|ugen| colors[methods.indexOf(ugen)] });
				if (currentGen == chrom.generation) 
				{ posy = posy + 10; } 
				{ posy = posy + 20; currentGen = chrom.generation};
				Pen.font = Font("Inconsolata", 8);
				cols.do({|color, x|
					Pen.fillColor = Color.grey(0.8);
					Pen.stringInRect(y.asString.padLeft(2), Rect(10, posy, 10, 7));
					color.set;
					Pen.fillRect(Rect(x*12+20, posy, 12, 7));
				})
			})	
		};
			
	}
	
	drawCompare{|indexA, indexB|
		var ucodeA, ucodeB, maxLen, similarity, chromA, chromB;
		if (colors.isNil) {
			colors = methods.collect({ Color( *{1.0.rand.round(0.1)} ! 3 ) })
		};
		if (names.isNil)
		{
			names = methods.collect({|ugen| ugen.name.asString.keep(4) });
		};		
		chromA = this.chromosomes[indexA];
		chromB = this.chromosomes[indexB];
		#ucodeA, ucodeB = [chromA, chromB].collect({|chrm| 
			chrm.code.select({|codon| codon.isKindOf(Class) }) 
		});
		maxLen = max(ucodeA.size, ucodeB.size);
		similarity = (this.calculateSimilarity(indexA, indexB)*100).round(1);
		Window("GES - compare", Rect(100, 100, 1000, 200)).background_(Color.grey(0.1)).front
		.drawFunc_({
			var size;
			size = (960 / maxLen).floor;
			[ucodeA, ucodeB].do({|code, y|
				Pen.font = Font("Inconsolata", 10);
				code.do({|ugen, x|
					var rect, index, color;
					index = methods.indexOf(ugen);
					color = colors[index];
					color.set;
					rect = Rect(x*size+20, y*(size + 10)+20, size, size);
					Pen.fillOval(rect);
					Color.grey(0.7).set;
					Pen.strokeOval(rect);
					Pen.fillColor = Color.white;
					Pen.stringCenteredIn(names[index], rect);
				})			
			});
			Pen.font = Font("Lato Regular", 14);
			Pen.stringInRect("Similarity: " ++ similarity.asString ++ "%", 
				Rect(20, 120, 200, 100));
		});			
	}
	
	calculateSimilarity{|indexA, indexB|
		var chromA, chromB;
		var sameCount = 0, len; 
		chromA = this.chromosomes[indexA];
		chromB = this.chromosomes[indexB];
		len = min(chromA.code.size, chromB.code.size);
		len.do({|i|
			if (chromA.code[i] == chromB.code[i]) 
			{
				sameCount = sameCount + 1
			}
		});
		^(sameCount / max(chromA.code.size, chromB.code.size))
	}
	
	drawSimilarityMatrix{|size=5|
		var win;
		var winsize;
		similarityMatrix = [];
		this.chromosomes.do({|chromA, i|
			var array;
			array = this.chromosomes.collect({|chromB, j|
				this.calculateSimilarity(i, j)
			});
			similarityMatrix = similarityMatrix.add(array)
		});
		winsize = size*similarityMatrix.size + 20;
		win = Window("similarity matrix", 
			Rect(100, 100, winsize, winsize)).background_(Color.grey(0.1)).front;
		win.drawFunc = {
			similarityMatrix.do({|row, y|
				row.do({|val, x|
					Color.grey(val).set;
					Pen.fillRect(Rect(x*size+10, y*size+10, size, size))
				})
			})
		};	
	}
}

+UGenExpressionTree{
	
	draw{|methods, funcColors|
		var tree, win, view, dict, layers, size;
		colors = funcColors;
		ugens = methods;
		if (colors.isNil) {
			colors = methods.collect({ Color( *{1.0.rand.round(0.1)} ! 3 ) })
		};
		if (names.isNil)
		{
			names = ugens.collect({|ugen| ugen.name.asString.keep(4) });
		};
		drawdict = [];
		size = 34;
		this.addNodes(root, 0);
		layers = maxDepth.collect({|y| 
			drawdict.select({|ev| ev.depth == y }).size 
		});
		win = Window("tree", Rect(100, 100, 800, 600))
			.background_(Color.grey(0.1)).front;
		view = UserView(win, Rect(0, 0, win.view.bounds.width, win.view.bounds.height));
		view.clearOnRefresh = false;
		view.drawFunc = {
			var linrect;
			maxDepth.do({|y|
				var step, nextstep, childno = 0;
				step = view.bounds.width - 40 / layers[y];
				if (y < layers.lastIndex)
				{
					nextstep = view.bounds.width - 40 / layers[y + 1];
				};
				drawdict.select({|ev|
					ev.depth == y
				}).do({|ev, x|
					var color, rect, index, from, to;
					if (ev.depth == 0)
					{
						Pen.color = Color.grey(0.7);
						from = Point(
							x*(size+(step-size))+(step*0.5) + (size*0.5), 
							y*110+100 + (size*0.5)
						);
						to = Point(view.bounds.width*0.5, 10 + (size*0.5));
						Pen.line(from, to);
						Pen.perform(\stroke);
					};
					if (ev.children > 0)
					{
						Pen.color = Color.grey(0.7);
						ev.children.do({|cx|
							from = Point(
								x*(size+(step-size))+(step*0.5) + (size*0.5), 
								y*110+100 + (size*0.5)
							);
							to = Point(
								childno*(size+(nextstep-size))+(nextstep*0.5)+(size*0.5),
								(y+1)*110+100+(size*0.5)
							);
							Pen.line(from, to);
							childno = childno + 1;
						});
						Pen.perform(\stroke);
					};
					index = ugens.indexOf(ev.ugen);
					rect = Rect(x*(size+(step-size))+(step*0.5), y*110+100, size, size);
					colors[index].set;
					Pen.fillOval(rect);
					Color.grey(0.7).set;
					Pen.strokeOval(rect);		
					Pen.font = Font("Inconsolata", 10);
					Pen.fillColor = Color.white;
					Pen.stringCenteredIn(names[index], rect);
				})
			});
			linrect = Rect(view.bounds.width*0.5-(size*0.5), 10, size, size);
			Color.black.set;
			Pen.fillOval(linrect);
			Color.grey(0.7).set;
			Pen.strokeOval(linrect);
			Pen.fillColor = Color.white;
			Pen.stringCenteredIn("*", linrect);
			
		}
	}
	
	addNodes{|node, depth|
		if (node.nodes.notNil)
		{
			node.nodes.do({|subnode|
				if(subnode.isFunction) { 
					var numins = subnode.nodes.select({|it| it.isFunction });
					drawdict = drawdict.add(
						('ugen': subnode.value, 'depth': depth, 'children': numins.size)
					);
//					Post << (($\t)!depth).toString << subnode.value << Char.nl;
					this.addNodes(subnode, depth+1)
				}
			})
		}
	}
}