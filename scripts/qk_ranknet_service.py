import os
from os.path import expanduser
import numpy as np
import json
from collections import defaultdict, namedtuple

# use CPU (for Keras)
os.environ["CUDA_DEVICE_ORDER"] = "PCI_BUS_ID"   # see issue #152
os.environ["CUDA_VISIBLE_DEVICES"] = ""


from flask import Flask
from flask import request

from qk_ranknet import build_model, build_rnn_model, to_rank

from keras.utils import plot_model


class Predictor(object):
    def __init__(self, model_path, no_inputs):
        # self.model = build_model(no_inputs)
        self.model = build_rnn_model()
        self.model.load_weights(model_path)
        print("loaded model:{}".format(model_path))
        self.model._make_predict_function()
        plot_model(self.model, show_shapes=True,
                   to_file='rank_lstm_sa_model.png')

    def predict(self, pair_vecs):
        p = pair_vecs[0]
        num_recs = len(pair_vecs)
        max_length = 40
        gv_dim = 100
        fv_dim = len(p.fv1)
        kw1_fvs = np.ndarray((num_recs, fv_dim), dtype='float32')
        kw2_fvs = np.ndarray((num_recs, fv_dim), dtype='float32')
        qid_set = set()
        for i, pv in enumerate(pair_vecs):
            kw1_fvs[i] = pv.fv1
            kw2_fvs[i] = pv.fv2
            qid_set.add(pv.qid)
        assert len(qid_set) == 1
        # for RNN (self-attention) model
        kw1_fvs = kw1_fvs.reshape(num_recs, max_length, gv_dim)
        kw2_fvs = kw2_fvs.reshape(num_recs, max_length, gv_dim)

        predictions = self.model.predict([kw1_fvs, kw2_fvs])
        qid = qid_set.pop()
        return self._find_rank_order(qid, pair_vecs, predictions)

    def _find_rank_order(self, qid, pair_vecs, predictions):
        print(predictions)
        q_pv_pred_list = list()
        for i, pv in enumerate(pair_vecs):
            if pv.qid == qid:
                q_pv_pred_list.append((pv, predictions[i]))
        po_map = defaultdict(list)
        for pv, prob in q_pv_pred_list:
            if prob > 0.5:
                po_map[pv.idx1].append(pv.idx2)
        return to_rank(po_map)


def setup():
    model_path = "models/qk_ranknet_rnn_model.h5"
    predictor = Predictor(model_path, 4000)
    return predictor


app = Flask(__name__)

predictor = setup()


@app.route('/qk_rank', methods=['POST'])
def rank_keywords():
    data_str = request.form['data']
    pairs_raw = json.loads(data_str)['pairs']
    # print(pairs_raw)
    pair_vecs = list()
    PairVec = namedtuple('PairVec', 'qid label idx1 idx2 fv1 fv2')
    for p in pairs_raw:
        fv1 = [float(x) for x in p['fv1']]
        fv2 = [float(x) for x in p['fv2']]
        pv = PairVec(p['qid'], p['label'], p['locIdx1'], p['locIdx2'],
                     fv1, fv2)
        pair_vecs.append(pv)
    # print("pair_vecs (size):{}".format(len(pair_vecs)))
    ranks = predictor.predict(pair_vecs)
    print(ranks)
    json_str = json.dumps(ranks)
    return json_str


if __name__ == '__main__':
    app.run(port=5020)



