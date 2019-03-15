import random

# use CPU (for Keras)
import os
os.environ["CUDA_DEVICE_ORDER"] = "PCI_BUS_ID"   # see issue #152
os.environ["CUDA_VISIBLE_DEVICES"] = ""

import utils
import json
from os.path import expanduser
from collections import Counter

from keras.models import Sequential
from keras.layers import Dense, Dropout, Flatten
from keras.layers import LSTM
from keras.models import Model
from keras import layers
from keras import Input
# from keras.layers import Bidirectional, Dropout
from keras.optimizers import SGD
import numpy as np
from glove_handler import GloveHandler

from flask import Flask
from flask import request

random.seed(1337841)


def prep_data(data, max_length, glove_handler, gv_dim=100):
    Xs = np.zeros((len(data), max_length * gv_dim), dtype='float32')
    for i, question in enumerate(data):
        tokens = question.split()
        for j, token in enumerate(tokens):
            offset = j * gv_dim
            vec = glove_handler.get_glove_vec(token)
            if vec:
                Xs[i, offset:offset+gv_dim] = vec
            else:
                vec = glove_handler.get_glove_vec('unk1')
                Xs[i, offset:offset+gv_dim] = vec
    return Xs


class Predictor:
    def __init__(self, db_file, model_path, pos_idx_map_json_file, recurrent=True):
        self.max_length = 40
        self.gv_dim = 100
        self.glove_handler = GloveHandler(db_file)
        self.recurrent = recurrent
        # with open(pos_idx_map_json_file, 'r') as f:
        #    self.pos_idx_map = json.load(f)
        if recurrent:
            # self.model = self.build_lstm_model(len(self.pos_idx_map))
            self.model = self.build_lstm_model()
        else:
            self.model = self.build_model_v3(len(self.pos_idx_map))
        self.model.load_weights(model_path)
        print("loaded model:{}".format(model_path))
        self.model._make_predict_function()

    def predict(self, question, pos_list):
        pred_X = prep_data([question], self.max_length, self.glove_handler)
        if self.recurrent:
            pred_X = pred_X.reshape(1, self.max_length, self.gv_dim)
        print("pred_X.shape:", pred_X.shape)
        y_pred = self.model.predict(pred_X)
        json_str = json.dumps(y_pred[0].tolist(), separators=(',', ': ') )
        return  json_str

    def predict_v3(self, question, pos_list):
        pred_pos = utils.prep_pos_data([pos_list], self.max_length, self.pos_idx_map)
        pred_X = prep_data([question], self.max_length, self.glove_handler)
        if self.recurrent:
            pred_X = pred_X.reshape(1, self.max_length, self.gv_dim)
        print("pred_X.shape:", pred_X.shape)
        print('pred_pos:', pred_pos.shape)
        y_pred = self.model.predict([pred_X, pred_pos])
        json_str = json.dumps(y_pred[0].tolist(), separators=(',', ': ') )
        return  json_str


    def close(self):
        if self.glove_handler:
            self.glove_handler.close()


    def build_model_v3(self, pos_dim, gv_dim=100, max_length=40):
        question_input = Input((gv_dim * max_length,), dtype='float32')
        pos_input = Input((pos_dim * max_length,), dtype='float32')
        q_layer = layers.Dense(max_length, activation='relu')(question_input)
        pos_layer = layers.Dense(10, activation='relu')(pos_input)
        concatenated = layers.concatenate([q_layer, pos_layer], axis=-1)
        out = layers.Dense(max_length, activation='sigmoid')(concatenated)
        model = Model([question_input, pos_input], out)
        #model.summary()
        sgd = SGD(lr=0.01, decay=1e-6, momentum=0.9, nesterov=True)
        model.compile(loss='binary_crossentropy', optimizer=sgd, metrics=['acc'])
        return model


    def build_lstm_model_v3(self, pos_dim, gv_dim=100, max_length=40):
        question_input = Input((max_length, gv_dim), dtype='float32')
        pos_input = Input((pos_dim * max_length,), dtype='float32')
        q_layer = layers.LSTM(max_length, return_sequences=True)(question_input)
        flattened = layers.Flatten()(q_layer)
        pos_layer = layers.Dense(10, activation='relu')(pos_input)
        concatenated = layers.concatenate([flattened, pos_layer], axis=-1)
        out = layers.Dense(max_length, activation='sigmoid')(concatenated)
        model = Model([question_input, pos_input], out)
        #model.summary()
        model.compile(loss='binary_crossentropy', optimizer="rmsprop",
                    metrics=['acc'])
        return model

    def build_lstm_model(self, gv_dim=100, max_length=40):
        model = Sequential()
        model.add(LSTM(max_length, dropout=0.2,
                    recurrent_dropout=0.2, 
                    return_sequences=True,
                    input_shape=(max_length, gv_dim)))
        model.add(Flatten())
        model.add(Dense(max_length, activation='sigmoid'))

        model.compile(loss='binary_crossentropy', optimizer="rmsprop",
                metrics=['acc'])
        return model

def setup():
    home = expanduser("~")
    db_file = home + "/medline_glove_v2.db"
    model_path = "models/qsc_glove_model.h5"
    pos_idx_map_json_file = "/tmp/qsc_pos_idx_map.json"
    predictor = Predictor(db_file, model_path, pos_idx_map_json_file)
    return predictor


def cleanup():
    print("cleanup...")
    if predictor:
        predictor.close()

app = Flask(__name__)

predictor = setup()

@app.route('/qks', methods=['POST'])
def select_search_keywords():
    question = request.form['question']
    pos_tags = request.form['pos_tags']
    print("question:{}\npos_tags:{}".format(question, pos_tags))
    pos_list = pos_tags.split()
    json_res = predictor.predict(question, pos_list)
    return json_res


def main():
    try:
        question = "List signaling molecules ( ligands ) that interact with the receptor EGFR ?"
        pos_list = "VB VBG NNS -LRB- NNS -RRB- IN NN IN DT NN NN .".split()
        print(predictor.predict(question, pos_list))
    finally:
        predictor.close()
        predictor = None

import atexit
atexit.register(cleanup)


if __name__ == '__main__':
    #main()
    app.run()
