+UGEP {

	draw{
		var win, height, width;
		if (colors.isNil) {
			colors = methods.collect({ Color( *{1.0.rand.round(0.1)} ! 3 ) })
		};
		if (names.isNil)
		{
			names = methods.collect({|ugen| ugen.name.asString.keep(4) });
		};
		width = this.chromosomes.collect({|chrom|
			chrom.code.select({|codon| codon.isKindOf(Class) }).size
		}).maxItem + numgenes * 12 + 30;
		height = this.chromosomes.size - 2 * 10 +
			(this.chromosomes.last.generation * 10) + 40;
		if (height > 800) { height = 800 };
		win = Window("GES population", Rect(100, 100, width, height))
			.background_(Color.grey(0.1)).front;
		win.drawFunc = {
			var currentGen = 0;
			var posy = 10;
			this.chromosomes.drop(2).do({|chrom, y|
				var cols, ugens;
				ugens = chrom.code.clump(genesize).collect({|gene|
					gene.select({|codon| codon.isKindOf(Class) })
				});
				cols = [];
				ugens.do({|gene|
					cols = cols.addAll(gene.collect({|ugen| colors[methods.indexOf(ugen)] }));
					cols = cols.add(Color.yellow);
				});
				if (currentGen == chrom.generation)
				{ posy = posy + 10; }
				{ posy = posy + 20; currentGen = chrom.generation};
				Pen.font = Font("Inconsolata", 8);
				Pen.fillColor = Color.grey(0.8);
				Pen.stringInRect(y.asString.padLeft(2), Rect(10, posy, 10, 7));
				cols.keep(cols.size-1).do({|color, x|
					color.set;
					if (color == Color.yellow) {
						Pen.fillRect(Rect(x*12+20 + 3, posy + 2, 6, 3))
					}
					{
						Pen.fillRect(Rect(x*12+20, posy, 12, 7))
					};
				})
			})
		};
	}

	drawScroll{
		var win, scroll, view, viewheight, width, size, ugens, genes;
		if (colors.isNil) {
			colors = methods.collect({ Color( *{1.0.rand.round(0.1)} ! 3 ) })
		};
		if (names.isNil)
		{
			names = methods.collect({|ugen| ugen.name.asString.keep(4) });
		};
		ugens = this.chromosomes.collect(_.code).collect(_.select(_.isKindOf(Class)));
		size = 960 / ugens.collect(_.size).maxItem;
		genes = this.chromosomes.collect({|chrm|
			chrm.code.clump(genesize).collect({|gene|
				gene.select({|codon| codon.isKindOf(Class) }).size
			})
		});
		viewheight = this.chromosomes.size - 2 * size +
		(this.chromosomes.last.generation * size) + 40;
		win = Window("GES", Rect(100, 100,
			1000, 600)).front;
		scroll = ScrollView(win, Rect(0, 0, win.bounds.width, win.bounds.height));
		view = UserView(scroll, Rect(0, 0, win.bounds.width, viewheight))
		.background_(Color.grey(0.0));
		view.drawFunc = {
			ugens.do({|uarr, y|
				var posy, sum = 0;
				posy = y*(size + 10)+20;
				Pen.font = Font("Inconsolata", 12);
				uarr.do({|ugen, x|
					var rect, index, color;
					index = methods.indexOf(ugen);
					color = colors[index];
					color.set;
					rect = Rect(x*size+20, posy, size, size);
					Pen.fillOval(rect);
					Color.grey(0.7).set;
					Pen.strokeOval(rect);
					Pen.fillColor = Color.white;
					Pen.stringCenteredIn(names[index], rect);
				});
				genes[y].keep(genes[y].size-1).do({|gsize, x|
					var rect;
					Color.yellow.set;
					sum = sum + gsize;
					rect = Rect(
						sum*size+20-(size*0.33/2),
						y*(size+10)+(size*0.4)+20,
						size*0.33, size*0.2);
					Pen.fillRect(rect);
					Color.black.set;
					Pen.strokeRect(rect);
				});
				Pen.font = Font("Inconsolata", 10);
				Pen.fillColor = Color.grey(0.8);
				Pen.stringInRect(y.asString.padLeft(2), Rect(10, posy, 10, 7));
			})
		};
	}

	drawOrder{|indices|

		var win, height, width;
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
		var ucodeA, ucodeB, genesA, genesB, maxLen, similarity, chromA, chromB;
		if (colors.isNil) {
			colors = methods.collect({ Color( *{1.0.rand.round(0.1)} ! 3 ) })
		};
		if (names.isNil)
		{
			names = methods.collect({|ugen| ugen.name.asString.keep(4) });
		};
		indexA = indexA + 2;
		indexB = indexB + 2;
		chromA = this.chromosomes[indexA];
		chromB = this.chromosomes[indexB];
		#ucodeA, ucodeB = [chromA, chromB].collect({|chrm|
			chrm.code.select({|codon| codon.isKindOf(Class) })
		});
		#genesA, genesB = [chromA, chromB].collect({|chrm|
			chrm.code.clump(genesize).collect({|gene|
				gene.select({|codon| codon.isKindOf(Class) }).size
			})
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
			[genesA, genesB].do({|gene, y|
				var sum = 0;
				gene.keep(gene.size-1).do({|gsize, x|
					var rect;
					Color.yellow.set;
					sum = sum + gsize;
					rect = Rect(sum*size+20-(size*0.33/2), y*(size+10)+(size*0.4)+20,
						size*0.33, size*0.2);
					Pen.fillRect(rect);
					Color.black.set;
					Pen.strokeRect(rect);
				})
			});
			Pen.font = Font("Lato Regular", 14);
			Pen.stringInRect("Similarity: " ++ similarity.asString ++ "%",
				Rect(20, 160, 200, 100));
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

	drawDistanceMatrix{|size=5|
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
		win = Window("distance matrix",
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

	drawUGenTree{|index, ugens|
		this.chromosomes[index+2].asUgenExpressionTree.draw(ugens, this.colors)
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
			.background_(Color.grey(0.0)).front;
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