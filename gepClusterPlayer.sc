ClusterPlayer{

	const dbname = "ges_ld_00";
	var <date, <player, <defnames, <nclusters, <docs, <kmeans, <coefs, <assignments;
	var <nearest, <clusters, <guictr, <win, <limits, <loader;

	*new{|date|
		^super.newCopyArgs(date).init
	}

	init{
		player = JGepPlayer(FoaDecoder(decoderType: 'stereo'), CinderApp(), dbname);
		nearest = ();
		clusters = ();
		guictr = ();
		this.loadDocs;
	}

	loadDocs{
		var result;
		loader = JsonLDLoader(dbname);
		result = loader.getIDsByDateRange(date);
		defnames = result.collect({|thing| thing['value'].first });
		loader.loadDocumentsByDefNames(defnames, {
			"Finished loading!".postln;
			docs = loader.results;
			this.makeClusters;
		})
	}

	makeClusters{
		nclusters = docs.size.sqrt.asInt + 1;
		kmeans = KMeans(nclusters);
		coefs = docs.collect({|event|
			event['stats']['ges:mfcc']['ges:mean'] ++ event['stats']['ges:mfcc']['ges:std_dev']
		});
		coefs.do({|coef| kmeans.add(coef) });
		kmeans.update;
		assignments = coefs.collect({|coef|
			(cluster: kmeans.classify(coef), distance: 0);
		});
		nclusters.do({|num|
			clusters[num] = []
		});
		this.orderBySimilarity;
		"Clusters ready!".postln;
	}

	orderBySimilarity{
		assignments.keysValuesDo({|key, val|
			var sim = this.cosineSimilarity(coefs[key], kmeans.centroids[val['cluster']]);
			val['distance'] = sim;
			clusters[val['cluster']] = clusters[val['cluster']].add( (name: key, distance: sim) );
			if (nearest.includesKey(val)) {
				Post << key << ": " << val << " -- " << sim << Char.nl;
				if (sim < nearest[val])
				{
					nearest[val] = key
				}
			}
			{
				nearest[val] = key
			}
		});

		clusters.keysValuesDo({|key, val|
			val.sort({|a, b| a['distance'] > b['distance'] })
		});

	}

	startPlayer{
		player.decoder.start;
		player.start(#[zoom,push]);
		AppClock.sched(1.0, {
			player.setFoa('zoom', 1.0);
			player.setFoa('push', 1.0);
			nil
		});

	}

	loadPlayerData{
		player.getDefNamesByDateRange(date);
		AppClock.sched(1.0, {
			player.defnames.do({|name, i|
				{
					player.loadData(i)
				}.try({
					Post << "No data for " << i << ": " << name << Char.nl
				})
			})
		});
	}

	playCluster{|cluster|
		Tdef('play', {
			clusters[cluster].do({|synth|
				var index = player.defnames.indexOf(synth['name']);
				if (player.data[index].notNil) {
					Post << synth['name'] << ": " << synth['distance'] << Char.nl;
					player.play(index, index, ['zoom', 'push'].choose, 0.25pi.rand);
					0.4.wait;
					player.set(index, 0.5);
					rrand(2.0, 4.0).round(0.125).wait;
					SystemClock.sched(0.4, {player.free(index);nil})
				}
			});
			AppClock.sched(0.5, { guictr['btn'].value = 0 })
		}).play;
	}

	stopCluster{
		Tdef('play').clear;
		player.synths.keys(Array).do({|index|
			player.free(index)
		})
	}

	getLimits{
		limits = ('dim0': ('lo': 1.0, 'hi': 0.0), 'dim1': ('lo': 1.0, 'hi': 0.0));

		coefs.do({|coef|
			var min, max;
			min = coef[0];
			max = coef[0];
			if (min < limits.dim0.lo) { limits.dim0.lo = min };
			if (max > limits.dim0.hi) { limits.dim0.hi = max };
			min = coef[1];
			max = coef[1];
			if (min < limits.dim1.lo) { limits.dim1.lo = min };
			if (max > limits.dim1.hi) { limits.dim1.hi = max };
		});

	}

	makeGui{|size=500|
		var clusterColors = nclusters.collect({|cluster|
			Color.hsv(  0.999 / nclusters * cluster, rrand(0.7, 1.0), rrand(0.7, 1.0) )
		});
		this.getLimits;
		win = Window("..kmeans..", Rect(100, 100, size, size)).front;
		win.view.background_(Color.grey(0.2));
		guictr['mnu'] = PopUpMenu(win, Rect(10, 10, 75, 20));
		guictr['mnu'].items_(["all - " ++ defnames.size.asString] ++ (0..nclusters-1).collect({|cluster|
			cluster.asString ++ " - " ++ clusters[cluster].size.asString
		}));
		guictr['mnu'].action_({|menu|
			win.refresh
		});
		guictr['mnu'].font_(Font("Helvetica", 8));
		guictr['btn'] = Button(win, Rect(90, 10, 50, 20));
		guictr['btn'].states_([[">", Color.black, Color.grey],["[]", Color.black, Color.green]]);
		guictr['btn'].action = {|btn|
			if (btn.value == 1) {
				this.playCluster(guictr['mnu'].value - 1)
			}
			{
				this.stopCluster;
			}
		};
		win.drawFunc = {
			coefs.keysValuesDo({|key, coef|
				var color, cluster = assignments[key]['cluster'];
				color = clusterColors[cluster];
				if ((guictr['mnu'].value != 0).and(guictr['mnu'].value - 1 != cluster)) {
					color.alpha = 0.1;
				} {
					color.alpha = 1.0;
				};
				color.set;
				Pen.fillOval(Rect(
					coef[0].linlin(limits.dim0.lo, limits.dim0.hi, 0.0, 1.0) * (size-10),
					coef[1].linlin(limits.dim1.lo, limits.dim1.hi, 0.0, 1.0) * (size-10),
					5, 5
				));
			});

			kmeans.centroids.do({|cent, i|
				clusterColors[i].set;
				Pen.fillOval(Rect(
					cent[0].linlin(limits.dim0.lo, limits.dim0.hi, 0.0, 1.0) * (size-10),
					cent[1].linlin(limits.dim1.lo, limits.dim1.hi, 0.0, 1.0) * (size-10),
					10, 10
				));
				Color.black.set;
				Pen.strokeOval(Rect(
					cent[0].linlin(limits.dim0.lo, limits.dim0.hi, 0.0, 1.0) * (size-10),
					cent[1].linlin(limits.dim1.lo, limits.dim1.hi, 0.0, 1.0) * (size-10),
					10, 10
				));
			});
		}
	}

	cosineSimilarity{|arrA, arrB|
		^(arrA*arrB).sum / (arrA.squared.sum.sqrt * arrB.squared.sum.sqrt)
	}

}