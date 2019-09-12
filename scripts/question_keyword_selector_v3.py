import random

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
import utils
from glove_handler import GloveHandler

from keras_self_attention import SeqSelfAttention

np.random.seed(1337841)
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


def train_cv(data_map, db_file, recurrent=False):
    max_length = 40
    gv_dim = 100
    train_data = data_map['train_data']
    train_labels = data_map['train_labels']
    train_pos_data = data_map['train_pos_data']
    pos_idx_map = data_map['pos_idx_map']
    train_data, train_labels = utils.extract_data(train_data, train_labels,
                                                  max_length)
    train_pos = utils.prep_pos_data(train_pos_data, max_length, pos_idx_map)
    glove_handler = GloveHandler(db_file)
    train_X = prep_data(train_data, max_length, glove_handler)
    glove_handler.close()

    cv_idx_tuples = utils.prep_cv_sets(len(train_data), random, no_folds=8)
    precision_list = list()
    recall_list = list()
    f1_list = list()
    hist_list = list()
    for train_indices, dev_indices in cv_idx_tuples:
        common_set = set(train_indices).intersection(set(dev_indices))
        assert len(common_set) == 0
        print(dev_indices)
        print("# dev_indices:", len(dev_indices))
        cv_train_X = train_X[train_indices]
        print(cv_train_X.shape)
        cv_train_pos = train_pos[train_indices]
        cv_train_labels = train_labels[train_indices]
        cv_dev_X = train_X[dev_indices]
        cv_dev_pos = train_pos[dev_indices]
        cv_dev_labels = train_labels[dev_indices]
        cv_dev_data = [train_data[i] for i in dev_indices]
        pos_dim = len(pos_idx_map)
        if recurrent:
            model = build_lstm_model(pos_dim)
        else:
            model = build_model(pos_dim)
        if recurrent:
            # for recurrent networks
            cv_train_X = cv_train_X.reshape(len(train_indices),
                                            max_length, gv_dim)
            cv_dev_X = cv_dev_X.reshape(len(dev_indices), max_length, gv_dim)
            cv_train_pos = cv_train_pos.reshape(len(train_indices),
                                                max_length, pos_dim)
            cv_dev_pos = cv_dev_pos.reshape(len(dev_indices),
                                                max_length, pos_dim)
            print("train_X:", train_X.shape)
        history = model.fit([cv_train_X, cv_train_pos], cv_train_labels,
                            epochs=40,
                            batch_size=32,
                            validation_data=[[cv_dev_X, cv_dev_pos],
                                             cv_dev_labels])
        # print(history)
        hist_dict = history.history
        run_dict = {'loss': hist_dict['loss'], 
                    'val_loss': hist_dict['val_loss'],
                    'acc': hist_dict['acc'],
                    'val_acc': hist_dict['val_acc']}
        hist_list.append(run_dict)
        predictions = model.predict([cv_dev_X, cv_dev_pos])
        counter = Counter()
        for i in range(len(dev_indices)):
            show_performance(i, cv_dev_data, cv_dev_labels, predictions,
                             counter, verbose=False)

        no_correct = counter["no_correct"]
        no_fn = counter["no_fn"]
        no_fp = counter["no_fp"]
        precision = no_correct / float(no_correct + no_fp) * 100
        recall = no_correct / float(no_correct + no_fn) * 100
        f1 = 2 * precision * recall / (precision + recall)
        precision_list.append(precision)
        recall_list.append(recall)
        f1_list.append(f1)
        print("Overall P:{:.1f} R:{:.1f} F1:{:.1f}".format(precision,
                                                           recall, f1))
    print("P:{:.1f} ({:.1f})".format(np.mean(precision_list),
                                     np.std(precision_list)))
    print("R:{:.1f} ({:.1f})".format(np.mean(recall_list),
                                     np.std(recall_list)))
    print("F1:{:.1f} ({:.1f})".format(np.mean(f1_list),
                                      np.std(f1_list)))
    perf = {'P': precision_list, 'R': recall_list,
            'F1': f1_list}
    with open('/tmp/qks_v3_performance.json', 'w') as f:
        json.dump(perf, f)
    with open('/tmp/qks_v3_history.json', 'w') as f:
        json.dump(hist_list, f)

def train_full(data_map, db_file, recurrent=False):
    max_length = 40
    gv_dim = 100
    dev_data = data_map['dev_data']
    train_data = data_map['train_data']
    train_labels = data_map['train_labels']
    train_pos_data = data_map['train_pos_data']
    pos_idx_map = data_map['pos_idx_map']
    with open('/tmp/qsc_pos_idx_map.json', 'w') as f:
        json.dump(pos_idx_map, f)

    train_data, train_labels = utils.extract_data(train_data, train_labels,
                                                  max_length)
    train_pos = utils.prep_pos_data(train_pos_data, max_length, pos_idx_map)
    glove_handler = GloveHandler(db_file)
    train_X = prep_data(train_data, max_length, glove_handler)
    if recurrent:
        # for recurrent networks
        train_X = train_X.reshape(len(train_data), max_length, gv_dim)
        train_pos = train_pos.reshape(len(train_data), max_length, len(pos_idx_map))

    print("train_X:", train_X.shape)
    glove_handler.close()

    if recurrent:
        model = build_lstm_model(len(pos_idx_map))
    else:
        model = build_model(len(pos_idx_map))
    history = model.fit([train_X, train_pos], train_labels,
                        epochs=40,
                        batch_size=32)
    model_path = "/tmp/qsc_glove_mi_model.h5"
    model.save_weights(model_path)
    print(history.history)
    print("saved model to {}".format(model_path))


def train(data_map, db_file, recurrent=False):
    max_length = 40
    gv_dim = 100
    dev_data = data_map['dev_data']
    train_data = data_map['train_data']
    dev_labels = data_map['dev_labels']
    train_labels = data_map['train_labels']
    dev_pos_data = data_map['dev_pos_data']
    train_pos_data = data_map['train_pos_data']
    pos_idx_map = data_map['pos_idx_map']
    with open('/tmp/qsc_pos_idx_map.json', 'w') as f:
        json.dump(pos_idx_map, f)

    dev_data, dev_labels = utils.extract_data(dev_data, dev_labels, max_length)
    train_data, train_labels = utils.extract_data(train_data, train_labels,
                                                  max_length)
    train_pos = utils.prep_pos_data(train_pos_data, max_length, pos_idx_map)
    dev_pos = utils.prep_pos_data(dev_pos_data, max_length, pos_idx_map)
    glove_handler = GloveHandler(db_file)
    train_X = prep_data(train_data, max_length, glove_handler)
    dev_X = prep_data(dev_data, max_length, glove_handler)
    if recurrent:
        # for recurrent networks
        train_X = train_X.reshape(len(train_data), max_length, gv_dim)
        dev_X = dev_X.reshape(len(dev_data), max_length, gv_dim)

    print("train_X:", train_X.shape)
    glove_handler.close()

    if recurrent:
        model = build_lstm_model(len(pos_idx_map))
    else:
        model = build_model(len(pos_idx_map))
    history = model.fit([train_X, train_pos], train_labels,
                        epochs=40,
                        batch_size=32,
                        validation_data=[[dev_X, dev_pos], dev_labels])
    model.save_weights("/tmp/qsc_glove_model.h5")
    print(history.history)

    predictions = model.predict([dev_X, dev_pos])
    counter = Counter()
    for i in range(len(dev_data)):
        show_performance(i, dev_data, dev_labels, predictions, counter)

    no_correct = counter["no_correct"]
    no_fn = counter["no_fn"]
    no_fp = counter["no_fp"]
    precision = no_correct / float(no_correct + no_fp) * 100
    recall = no_correct / float(no_correct + no_fn) * 100
    f1 = 2 * precision * recall / (precision + recall)
    print("Overall P:{:.1f} R:{:.1f} F1:{:.1f}".format(precision, recall, f1))


def build_model(pos_dim, gv_dim=100, max_length=40):
    question_input = Input((gv_dim * max_length,), dtype='float32')
    pos_input = Input((pos_dim * max_length,), dtype='float32')
    q_layer = layers.Dense(max_length, activation='relu')(question_input)
    q_layer = layers.Dropout(0.2)(q_layer)
    pos_layer = layers.Dense(10, activation='relu')(pos_input)
    # pos_layer = layers.Dropout(0.1)(pos_layer)
    concatenated = layers.concatenate([q_layer, pos_layer],
                                      axis=-1)
    out = layers.Dense(max_length, activation='sigmoid')(concatenated)
    model = Model([question_input, pos_input], out)
    model.summary()

    model.compile(loss='binary_crossentropy', optimizer="rmsprop",
                  metrics=['acc'])
    return model


def build_lstm_model(pos_dim, gv_dim=100, max_length=40):
    question_input = Input((max_length, gv_dim), dtype='float32')
    pos_input = Input((max_length, pos_dim), dtype='float32')
    q_layer = layers.LSTM(max_length, return_sequences=True,
                          dropout=0.2, 
                          recurrent_dropout=0.2)(question_input)
    flattened = layers.Flatten()(q_layer)
    #pos_layer = layers.Dense(10, activation='relu')(pos_input)
    pos_layer = layers.LSTM(10, return_sequences=True)(pos_input)
    # pos_layer = SeqSelfAttention(attention_activation='sigmoid')(pos_layer)
    pos_layer = layers.Flatten()(pos_layer)
    concatenated = layers.concatenate([flattened, pos_layer],
                                      axis=-1)
    out = layers.Dense(max_length, activation='sigmoid')(concatenated)
    model = Model([question_input, pos_input], out)
    model.summary()

    model.compile(loss='binary_crossentropy', optimizer="rmsprop",
                  metrics=['acc'])
    return model


def show_performance(i, data, true_labels, predictions, counter, verbose=True):
    question, true_labels = data[i], true_labels[i]
    predictions = predictions[i]
    tokens = question.split(' ')
    no_correct = 0
    no_fp = 0
    no_fn = 0
    gs_correct = 0
    results = []
    if verbose:
        print(question)
    for i, token in enumerate(tokens):
        if predictions[i] >= 0.5:
            if true_labels[i] == 1:
                no_correct += 1
                counter["no_correct"] += 1
                gs_correct += 1
                results.append(token)
            else:
                no_fp += 1
                counter["no_fp"] += 1
                results.append('*' + token)
        else:
            if true_labels[i] == 1:
                gs_correct += 1
                no_fn += 1
                counter["no_fn"] += 1
                results.append('[' + token + ']')
    precision = no_correct / float(no_correct + no_fp) if (no_correct + no_fp) > 0 else 0
    recall = no_correct / float(no_correct + no_fn) if (no_correct + no_fn) > 0 else 0
    f1 = 0
    if precision > 0 and recall > 0:
        f1 = 2 * precision * recall / (precision + recall)
    if verbose:
        print(" ".join(results))
        print("P:{:.1f} R:{:.1f} F1:{:.1f}".format(precision, recall, f1))
        print("------------------------------------")


def main():

    home = expanduser("~")
    data_dir = home + "/dev/java/bnerkit/data/bioasq/bioasq_manual_100/qsc"
    train_file = data_dir + "/qsc_set_train.txt"
    pos_file = data_dir + "/qsc_set_pos_tags_train.txt"
    test_file = data_dir + "/qsc_set_test.txt"
    test_pos_file = data_dir + "/qsc_set_pos_tags_test.txt"
    
    data_dir = home + "/dev/java/bio-answerfinder/data/rank_test"
    train_file = data_dir + "/rank_train_data.dat"
    pos_file = data_dir + "/rank_train_pos_data.dat"
    test_file = data_dir + "/rank_test_data.dat"
    test_pos_file = data_dir + "/rank_test_pos_data.dat"


    db_file = home + "/medline_glove_v2.db"
    data, labels = utils.load_qsc_data(train_file)
    pos_data, _, pos_idx_map = utils.load_qsc_pos_data(pos_file)

    test_data, test_labels = utils.load_qsc_data(test_file)
    test_pos_data, _, _ = utils.load_qsc_pos_data(test_pos_file)

    train_data = data
    train_labels = labels
    train_pos_data = pos_data
    data_map = {'dev_data': test_data, 'dev_labels': test_labels,
                'dev_pos_data': test_pos_data, 'train_data': train_data,
                'train_labels': train_labels,
                'train_pos_data': train_pos_data,
                'pos_idx_map': pos_idx_map}

    # train(data_map, db_file, recurrent=True)
    #train_cv(data_map, db_file, recurrent=True)
    train_full(data_map, db_file, recurrent=True)


if __name__ == '__main__':
    main()
