import sys

from flask import Flask, request, jsonify
import json

from dejavu import Dejavu

DEFAULT_CONFIG_FILE = "dejavu.cnf"


def init(configpath):
    """
    Load config from a JSON file
    """
    try:
        with open(configpath) as f:
            config = json.load(f)
    except IOError as err:
        print("Cannot open configuration: %s. Exiting" % (str(err)))
        sys.exit(1)

    # create a Dejavu instance
    return Dejavu(config)


app = Flask(__name__)


def decompress(compressed_client_data):
    import gzip, StringIO

    compressed_client_data = StringIO.StringIO(compressed_client_data) # it makes it look like a file because GzipFile takes a file
    decompressed_data = gzip.GzipFile(fileobj=compressed_client_data, mode='rb')

    client_data_json = json.loads(decompressed_data.read()) # dejavu.db.return_matches() won't work if it's not in JSON format

    return client_data_json

@app.route('/retrieve', methods=['POST'])
def retrieve():
    # in case of uncompressed data
    #client_data = request.get_json()

    client_data = decompress(request.data) # the app sends compressed data (no 'application/json' anymore!)

    matches = djv.db.return_matches(client_data)
    song = djv.align_matches(matches)

    song_name = song[Dejavu.SONG_NAME]
    confidence = song[Dejavu.CONFIDENCE]

    print(song_name, confidence)

    if confidence >= 50:
        return jsonify(song), 200
    else:
        return jsonify(""), 404


if __name__ == "__main__":
    djv = init(DEFAULT_CONFIG_FILE)
    app.run(host='0.0.0.0', port=8080, debug=True)
