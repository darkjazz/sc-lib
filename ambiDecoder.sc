AmbiDecoder{

	var <setup, <order, <isBformat, <bus, <synth, <numChannels, <decoder;

	*new{|setup='perflab_3o', order=3, isBformat=false|
		^super.newCopyArgs(setup, order, isBformat).init
	}

	init{
		this.makeSynthDef;
	}

	makeSynthDef{
		{
			case
			{ order == 1 } {
				numChannels = 4;
				decoder = DecodeAmbi1O;
			}
			{ order == 2 } {
				numChannels = 9;
				decoder = DecodeAmbi2O;
			}
			{ order == 3 } {
				numChannels = 16;
				decoder = DecodeAmbi3O;
			};
			bus = Bus.audio(Server.default, numChannels);
			SynthDef('ambi-decoder', {|in, amp|
				var input, dec;
				input = In.ar(in, numChannels) * amp;
				Out.ar(0, decoder.ar(input, setup))
			}).add
		}.fork
	}

	*load_config{|path, name|
		if (File.exists(path)) {
			var json, text, file, matrix_size;
			file = File(path, "r");
			text = file.readAllString;
			file.close;
			json = text.parseJson;
			matrix_size = json["Decoder"]["Matrix"][0].size;
			if (matrix_size == 4) {
				AmbiDecoder.add_1o_config(json, name)
			}
			{
				AmbiDecoder.add_3o_config(json, name)
			}

		}
		{
			"File does not exist".warn
		}
	}

	*add_1o_config{|json, name|
		DecodeAmbi1O.setups[name] = json["Decoder"]["Matrix"].collect({|row| row.collect(_.asFloat) });
		DecodeAmbi1O.positions[name] = [
			json["LoudspeakerLayout"]["Loudspeakers"].collect({|speaker|
				if (speaker["IsImaginary"] == "false") { speaker["Elevation"].asFloat }
			}).select(_.notNil),
			json["LoudspeakerLayout"]["Loudspeakers"].collect({|speaker|
				if (speaker["IsImaginary"] == "false") { speaker["Azimuth"].asFloat }
			}).select(_.notNil)
		]

	}

	*add_3o_config{|json, name|
		DecodeAmbi3O.setups[name] = json["Decoder"]["Matrix"].collect({|row| row.collect(_.asFloat) });
		DecodeAmbi3O.positions[name] = [
			json["LoudspeakerLayout"]["Loudspeakers"].collect({|speaker|
				if (speaker["IsImaginary"] == "false") { speaker["Elevation"].asFloat }
			}).select(_.notNil),
			json["LoudspeakerLayout"]["Loudspeakers"].collect({|speaker|
				if (speaker["IsImaginary"] == "false") { speaker["Azimuth"].asFloat }
			}).select(_.notNil)
		]
	}

}