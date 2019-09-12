import os
from os.path import expanduser
import numpy as np
import json

from flask import Flask
from flask import request

from centroid_aknn_utils import CentroidKNN


def setup():
    home = expanduser("~")
    index_file = home + '/data/pubmed/centroid_idx.dat'
    pmid_map_file = home + '/data/pubmed/pmid_map.p'
    retriever = CentroidKNN(index_file, pmid_map_file)
    return retriever

app = Flask(__name__)

retriever = setup()

@app.route('/aknn', methods=['POST'])
def find_closest():
    data_str = request.form['data']
    data = json.loads(data_str)
    K = data['k']
    v = data['query']
    query_vec = np.array(v, dtype='float32')
    pmids = retriever.query(query_vec, k=K)
    print(pmids)
    json_str = json.dumps(pmids)
    return json_str

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5551)

