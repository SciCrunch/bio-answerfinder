import random
import numpy as np
import collections
from collections import defaultdict

from keras.layers import Activation, Dense, Input, Subtract, LSTM, Flatten, Dropout
from keras.models import Model

from keras_self_attention import SeqSelfAttention

random.seed(1337841)

def load_data(rank_data_file):
    kw1_fvs = None
    kw2_fvs = None
    num_questions, fv_dim = None, None
    qid_list = list()
    labels = list()
    kw1_idx_list = list()
    kw2_idx_list = list()
    idx = 0
    with open(rank_data_file) as f:
        for i, line in enumerate(f):
            if i == 0:
                tokens = line.split(' ')
                num_questions = int(tokens[0])
                fv_dim = int(tokens[1])
                kw1_fvs = np.ndarray((num_questions, fv_dim),
                                     dtype='float32')
                kw2_fvs = np.ndarray((num_questions, fv_dim),
                                     dtype='float32')
            else:
                tokens = line.split(' ')
                qid_list.append(int(tokens[0]))
                labels.append(int(tokens[1]))
                kw1_idx_list.append(int(tokens[2]))
                kw2_idx_list.append(int(tokens[3]))
                kw1_fvs[idx] = [float(x) for x in tokens[4:fv_dim+4]]
                kw2_fvs[idx] = [float(x) for x in tokens[fv_dim+4:]]
                idx += 1
    return (qid_list, labels, kw1_idx_list, kw2_idx_list, kw1_fvs, kw2_fvs)


def build_rnn_model(max_length=40, gv_dim=100):
    h_1 = LSTM(128, dropout=0.27, recurrent_dropout=0.068,
               input_shape=(max_length, gv_dim),
               return_sequences=True)
    h_2 = SeqSelfAttention(attention_activation='sigmoid')
    h_3 = Flatten()
    ro = Dense(1)

    # first keyword
    fkw_inp = Input(shape=(max_length,gv_dim), dtype='float32')
    h_1_fw = h_1(fkw_inp)
    h_2_fw = h_2(h_1_fw)
    h_3_fw = h_3(h_2_fw)
    fkw_score = ro(h_3_fw)

    # second keyword
    skw_inp = Input(shape=(max_length,gv_dim), dtype='float32')
    h_1_sw = h_1(skw_inp)
    h_2_sw = h_2(h_1_sw)
    h_3_sw = h_3(h_2_sw)
    skw_score = ro(h_3_sw)
    diff = Subtract()([fkw_score, skw_score])

    rank_prob = Activation("sigmoid")(diff)

    model = Model(inputs=[fkw_inp, skw_inp], outputs=rank_prob)
    model.compile(optimizer="adam", loss="binary_crossentropy")
    model.summary()
    return model


def build_rnn_only_model(max_length=40, gv_dim=100):
    h_1 = LSTM(128, dropout=0.27, recurrent_dropout=0.068,
               input_shape=(max_length, gv_dim),
               return_sequences=True)
    h_2 = Flatten()
    ro = Dense(1)

    # first keyword
    fkw_inp = Input(shape=(max_length,gv_dim), dtype='float32')
    h_1_fw = h_1(fkw_inp)
    h_2_fw = h_2(h_1_fw)
    fkw_score = ro(h_2_fw)

    # second keyword
    skw_inp = Input(shape=(max_length,gv_dim), dtype='float32')
    h_1_sw = h_1(skw_inp)
    h_2_sw = h_2(h_1_sw)
    skw_score = ro(h_2_sw)
    diff = Subtract()([fkw_score, skw_score])

    rank_prob = Activation("sigmoid")(diff)

    model = Model(inputs=[fkw_inp, skw_inp], outputs=rank_prob)
    model.compile(optimizer="adam", loss="binary_crossentropy")
    model.summary()
    return model


def build_model(no_inputs):
    # based on https://github.com/airalcorn2/RankNet/blob/master/ranknet.py
    h_1 = Dense(128, activation='relu')
    d_1 = Dropout(0.27)
    h_2 = Dense(128, activation='relu')
    d_2 = Dropout(0.02)
    h_3 = Dense(64, activation='relu')
    d_3 = Dropout(0.29)
    ro = Dense(1)

    # first keyword
    fkw_inp = Input(shape=(no_inputs,), dtype="float32")
    h_1_fw = h_1(fkw_inp)
    h_1_fw = d_1(h_1_fw)
    h_2_fw = h_2(h_1_fw)
    h_2_fw = d_2(h_2_fw)
    h_3_fw = h_3(h_2_fw)
    h_3_fw = d_3(h_3_fw)
    fkw_score = ro(h_3_fw)

    # second keyword
    skw_inp = Input(shape=(no_inputs,), dtype="float32")
    h_1_sw = h_1(skw_inp)
    h_1_sw = d_1(h_1_sw)
    h_2_sw = h_2(h_1_sw)
    h_2_sw = d_2(h_2_sw)
    h_3_sw = h_3(h_2_sw)
    h_3_sw = d_3(h_3_sw)
    skw_score = ro(h_3_sw)

    diff = Subtract()([fkw_score, skw_score])

    rank_prob = Activation("sigmoid")(diff)

    model = Model(inputs=[fkw_inp, skw_inp], outputs=rank_prob)
    model.compile(optimizer="adam", loss="binary_crossentropy")
    return model


def predict(model, pair_vecs):
    # PairVec = collections.namedtuple('PairVec', 'qid label idx1 idx2 fv1 fv2')
    p = pair_vecs[0]
    num_recs = len(pair_vecs)
    no_dims = len(p.fv1.shape)
    print("shape:", p.fv1.shape)
    if no_dims == 1:
        fv_dim = len(p.fv1)
        kw1_fvs = np.ndarray((num_recs, fv_dim), dtype='float32')
        kw2_fvs = np.ndarray((num_recs, fv_dim), dtype='float32')
    else:
        kw1_fvs = np.ndarray((num_recs, p.fv1.shape[0], p.fv1.shape[1]), dtype='float32')
        kw2_fvs = np.ndarray((num_recs, p.fv1.shape[0], p.fv1.shape[1]), dtype='float32')

    qid_set = set()
    for i, pv in enumerate(pair_vecs):
        kw1_fvs[i] = pv.fv1
        kw2_fvs[i] = pv.fv2
        qid_set.add(pv.qid)
    predictions = model.predict([kw1_fvs, kw2_fvs])
    for qid in qid_set:
        show_performance(qid, pair_vecs, predictions)


def show_performance(qid, pair_vecs, predictions):
    q_pv_pred_list = list()
    for i, pv in enumerate(pair_vecs):
        if pv.qid == qid:
            q_pv_pred_list.append((pv, predictions[i]))
    po_map = defaultdict(list)  # partial order map for each keyword (first keyword idx)
    pod_map = defaultdict(list)  # partial order (decreasing) map for each keyword (first keyword idx)
    for pv, prob in q_pv_pred_list:
        if prob > 0.5:
            po_map[pv.idx1].append(pv.idx2)
        elif prob < 0.5:
            pod_map[pv.idx1].append(pv.idx2)
    print("partial ordering for keywords in qid:{} # of pairs:{}".format(qid, len(q_pv_pred_list)))
    print(po_map)
    print(to_rank(po_map))
    # print(pod_map)
    print('-'*40)


def _get_lowest_matching_idx(low_ranks, ranks):
    min_idx = len(ranks)
    for lr in low_ranks:
        try:
            idx = ranks.index(lr)
            min_idx = min(idx, min_idx)
        except:
            ranks.append(lr)
    return min_idx


def to_rank(po_map):
    ''' from highest to lowest rank of importance for keywords '''
    po_tuple_list = sorted(list(po_map.items()), key=lambda t: len(t[1]), reverse=True)
    ranks = list()
    for i, (pivot, low_ranks) in enumerate(po_tuple_list):
        if i == 0:
            ranks.append(pivot)
            ranks.extend(low_ranks)
        else:
            if pivot not in ranks:
                lmidx = _get_lowest_matching_idx(low_ranks, ranks)
                ranks.insert(lmidx, pivot)
            else:
                ranks.remove(pivot)
                lmidx = _get_lowest_matching_idx(low_ranks, ranks)
                ranks.insert(lmidx, pivot)
    return ranks


def load_pair_vectors(rank_data_file):
    qid_list, labels, kw1_idx_list, kw2_idx_list, kw1_fvs, kw2_fvs = load_data(rank_data_file)
    PairVec = collections.namedtuple('PairVec', 'qid label idx1 idx2 fv1 fv2')
    num_recs = len(kw1_idx_list)
    pair_vecs = list()
    for i in range(num_recs):
        pair_vecs.append(PairVec(qid_list[i], labels[i], kw1_idx_list[i],
              kw2_idx_list[i], kw1_fvs[i], kw2_fvs[i]))
    return pair_vecs

def load_pair_vectors_rnn(rank_data_file, max_length=40, gv_dim=100):
    qid_list, labels, kw1_idx_list, kw2_idx_list, kw1_fvs, kw2_fvs = load_data(rank_data_file)
    kw1_fvs = kw1_fvs.reshape(len(labels), max_length, gv_dim)
    kw2_fvs = kw2_fvs.reshape(len(labels), max_length, gv_dim)
    PairVec = collections.namedtuple('PairVec', 'qid label idx1 idx2 fv1 fv2')
    num_recs = len(kw1_idx_list)
    pair_vecs = list()
    for i in range(num_recs):
        pair_vecs.append(PairVec(qid_list[i], labels[i], kw1_idx_list[i],
              kw2_idx_list[i], kw1_fvs[i], kw2_fvs[i]))
    return pair_vecs

def test(rank_data_file):
    model_path = "/tmp/qk_ranknet_model.h5"
    pair_vecs = load_pair_vectors(rank_data_file)
    no_inputs = len(pair_vecs[0].fv1)
    model = build_model(no_inputs)
    model.load_weights(model_path)
    print("loaded model:{}".format(model_path))
    model._make_predict_function()
    predict(model, pair_vecs)
    predict2(rank_data_file, model, rnn=False)


def test_rnn(rank_data_file, use_attention = True):
    if use_attention:
        model_path = "/tmp/qk_ranknet_rnn_model.h5"
    else:
        model_path = "/tmp/qk_ranknet_rnn_only_model.h5"

    pair_vecs = load_pair_vectors_rnn(rank_data_file)
    no_inputs = len(pair_vecs[0].fv1)
    if use_attention:
        model = build_rnn_model()
    else:
        model = build_rnn_only_model()

    print("loading model:{}".format(model_path))
    model.load_weights(model_path)
    print("loaded model:{}".format(model_path))
    model._make_predict_function()
    predict(model, pair_vecs)
    predict2(rank_data_file, model, rnn=True)


def predict2(rank_data_file, model, rnn=False):
    qid_list, labels, kw1_idx_list, kw2_idx_list, kw1_fvs, kw2_fvs = load_data(rank_data_file)
    gv_dim = 100
    max_length = 40
    if rnn:
        kw1_fvs = kw1_fvs.reshape(len(labels), max_length, gv_dim)
        kw2_fvs = kw2_fvs.reshape(len(labels), max_length, gv_dim)

    predictions = model.predict([ kw1_fvs, kw2_fvs])
    show_accuracy(labels, predictions)


def train_cv(rank_data_file):
    from utils import  prep_cv_sets
    qid_list, labels, kw1_idx_list, kw2_idx_list, kw1_fvs, kw2_fvs = load_data(rank_data_file)

    uniq_qid_list = list(set(qid_list))
    uniq_qid_list.sort()

    cv_idx_tuples = prep_cv_sets(len(uniq_qid_list), random, no_folds=8)

    no_inputs = kw1_fvs.shape[1]
    num_epochs = 20
    model = build_model(no_inputs)

    accuracy_list = list()
    for train_indices, dev_indices in cv_idx_tuples:
        train_cv_qid_set = set( [ uniq_qid_list[i] for i in train_indices] )
        train_cv_labels, train_cv_kw1_fvs, train_cv_kw2_fvs = prep_cv_data(train_cv_qid_set,
                                                                           qid_list, labels,
                                                                           kw1_fvs, kw2_fvs)
        dev_cv_qid_set = set([uniq_qid_list[i] for i in dev_indices] )
        dev_cv_labels, dev_cv_kw1_fvs, dev_cv_kw2_fvs = prep_cv_data(dev_cv_qid_set,
                                                                     qid_list, labels,
                                                                     kw1_fvs, kw2_fvs)
        history = model.fit([train_cv_kw1_fvs, train_cv_kw2_fvs], train_cv_labels,
                            batch_size=32, epochs=num_epochs, verbose=1)

        predictions = model.predict([dev_cv_kw1_fvs, dev_cv_kw2_fvs])
        accuracy = show_accuracy(dev_cv_labels, predictions)
        accuracy_list.append(accuracy)

    print("Accuracy:{:.3f} ({:.3f})".format(np.mean(accuracy_list),
                                     np.std(accuracy_list)))



def prep_cv_data(cv_qid_set, qid_list, labels, kw1_fvs, kw2_fvs):
    indices = [i for i, qid in enumerate(qid_list) if qid in cv_qid_set]
    cv_labels =  [labels[i]  for i in indices]
    cv_kw1_fvs = [kw1_fvs[i] for i in indices]
    cv_kw2_fvs = [kw2_fvs[i] for i in indices]
    return (cv_labels, cv_kw1_fvs, cv_kw2_fvs)


def show_accuracy(true_labels, predictions):
    no_correct, no_fp, no_fn = 0, 0, 0
    for i, gslabel in enumerate(true_labels):
        print("label:{} prediction:{}".format(gslabel, predictions[i]))
        if predictions[i] >= 0.5:
            if gslabel == 1:
                no_correct += 1
            else:
                no_fp += 1
        else:
            if gslabel == 1:
                no_fn += 1
            else:
                no_correct += 1
    accuracy =  no_correct  / float(len(true_labels))
    print("Accuracy: {:.3f}".format(accuracy))
    return accuracy



def train(rank_data_file):
    qid_list, labels, kw1_idx_list, kw2_idx_list, kw1_fvs, kw2_fvs = load_data(rank_data_file)

    no_inputs = kw1_fvs.shape[1]
    model = build_model(no_inputs)

    num_epochs = 20

    history = model.fit([kw1_fvs, kw2_fvs], labels, batch_size=16,
                        epochs=num_epochs, verbose=1)
    model_path = "/tmp/qk_ranknet_model.h5"
    model.save_weights(model_path)
    print(history.history)
    print("saved model to {}".format(model_path))

def train_rnn(rank_data_file, use_attention = True):
    qid_list, labels, kw1_idx_list, kw2_idx_list, kw1_fvs, kw2_fvs = load_data(rank_data_file)
    gv_dim = 100
    max_length = 40
    kw1_fvs = kw1_fvs.reshape(len(labels), max_length, gv_dim)
    kw2_fvs = kw2_fvs.reshape(len(labels), max_length, gv_dim)
    print(kw1_fvs.shape)
    if use_attention:
        model = build_rnn_model()
    else:
        model = build_rnn_only_model()

    num_epochs = 20
    history = model.fit([kw1_fvs, kw2_fvs], labels, batch_size=32,
                        epochs=num_epochs, verbose=1)
    if use_attention:
        model_path = "/tmp/qk_ranknet_rnn_model.h5"
    else:
        model_path = "/tmp/qk_ranknet_rnn_only_model.h5"

    model.save_weights(model_path)
    print(history.history)
    print("saved model to {}".format(model_path))


def main():
    from os.path import expanduser
    home = expanduser("~")
    data_dir = home + "/dev/java/bio-answerfinder/data/rank_test/pair_data"
    rank_data_file = data_dir + "/train_ranking.txt"
    rank_test_file = data_dir + "/test_ranking.txt"
    # train(rank_data_file)
    #test(rank_test_file)
    train_rnn(rank_data_file, use_attention=True)
    #train_rnn(rank_data_file, use_attention=False)
    #train_rnn(rank_data_file, use_attention=False)
    #test_rnn(rank_test_file, use_attention=False)
    #train_cv(rank_data_file)


if __name__ == '__main__':
    main()
