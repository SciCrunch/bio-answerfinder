import random
import json
from os.path import expanduser
from collections import Counter
import numpy as np

from keras.models import Sequential
from keras.layers import Dense, Dropout, Flatten
from keras.layers import LSTM
from keras import layers
# from keras.layers import Bidirectional, Dropout
from keras.optimizers import SGD
from glove_handler import GloveHandler
import utils


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

def train_cv(data_map, db_file, recurrent=False, convNet=False):
    max_length = 40
    gv_dim = 100
    train_data = data_map['train_data']
    train_labels = data_map['train_labels']
    train_data, train_labels = utils.extract_data(train_data, train_labels,
                                                  max_length)
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
        assert not common_set
        print(dev_indices)
        print("# dev_indices:", len(dev_indices))
        cv_train_X = train_X[train_indices]
        print(cv_train_X.shape)
        cv_train_labels = train_labels[train_indices]
        cv_dev_X = train_X[dev_indices]
        cv_dev_labels = train_labels[dev_indices]
        cv_dev_data = [train_data[i] for i in dev_indices]
        if recurrent:
            model = build_lstm_model()
        elif convNet:
            model = build_convnet_model()
        else:
            model = build_model()
        if recurrent or convNet:
            # for recurrent networks
            cv_train_X = cv_train_X.reshape(len(train_indices),
                                            max_length, gv_dim)
            cv_dev_X = cv_dev_X.reshape(len(dev_indices), max_length, gv_dim)
            print("train_X:", train_X.shape)
        history = model.fit(cv_train_X, cv_train_labels,
                            epochs=40,
                            batch_size=32,
                            validation_data=[cv_dev_X, cv_dev_labels])
        hist_dict = history.history
        run_dict = {'loss': hist_dict['loss'], 
                    'val_loss': hist_dict['val_loss'],
                    'acc': hist_dict['acc'],
                    'val_acc': hist_dict['val_acc']}
        hist_list.append(run_dict)
        # print(history)
        predictions = model.predict(cv_dev_X)
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
    with open('/tmp/qks_v2_performance.json', 'w') as f:
        json.dump(perf, f)
    with open('/tmp/qks_v2_history.json', 'w') as f:
        json.dump(hist_list, f)


def train_full(data_map, db_file, recurrent=False):
    max_length = 40
    gv_dim = 100
    train_data = data_map['train_data']
    train_labels = data_map['train_labels']
    train_data, train_labels = utils.extract_data(train_data, train_labels,
                                                  max_length)
    glove_handler = GloveHandler(db_file)
    train_X = prep_data(train_data, max_length, glove_handler)
    if recurrent:
        train_X = train_X.reshape(len(train_data), max_length, gv_dim)
    print("train_X:", train_X.shape)
    glove_handler.close()
    if recurrent:
        model = build_lstm_model()
    else:
        model = build_model()
    history = model.fit(train_X, train_labels,
                        epochs=40, batch_size=32)
    model_path = "/tmp/qsc_glove_model.h5"
    model.save_weights(model_path)
    print(history.history)
    print("saved model to {}".format(model_path))


def train(data_map, db_file, recurrent=True):
    max_length = 40
    gv_dim = 100
    dev_data = data_map['dev_data']
    train_data = data_map['train_data']
    dev_labels = data_map['dev_labels']
    train_labels = data_map['train_labels']

    dev_data, dev_labels = utils.extract_data(dev_data, dev_labels, max_length)
    train_data, train_labels = utils.extract_data(train_data, train_labels,
                                                  max_length)
    glove_handler = GloveHandler(db_file)
    train_X = prep_data(train_data, max_length, glove_handler)
    dev_X = prep_data(dev_data, max_length, glove_handler)
    if recurrent:
        # for recurrent networks
        train_X = train_X.reshape(len(train_data), max_length, gv_dim)
        dev_X = dev_X.reshape(len(dev_data), max_length, gv_dim)

    # train_X = train_X.reshape(len(train_data), gv_dim, max_length)
    # dev_X = dev_X.reshape(len(dev_data), gv_dim, max_length)
    # train_X = np.swapaxes(train_X, 1, 2)
    # dev_X = np.swapaxes(dev_X, 1, 2)

    print("train_X:", train_X.shape)
    glove_handler.close()

    if recurrent:
        model = build_lstm_model()
    else:
        model = build_model()
    history = model.fit(train_X, train_labels,
                        epochs=40,
                        batch_size=32,
                        validation_data=[dev_X, dev_labels])
    # model.save_weights("/tmp/qsc_glove_model.h5")
    print(history.history)

    predictions = model.predict(dev_X)
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


def build_model(gv_dim=100, max_length=40):
    model = Sequential()
    # build model
    model.add(Dense(max_length, activation='relu',
                    input_shape=(gv_dim * max_length,)))
    model.add(Dropout(0.2))
    model.add(Dense(max_length, activation='sigmoid'))
    model.summary()

    # sgd = SGD(lr=0.01, decay=1e-6, momentum=0.9, nesterov=True)
    # model.compile(loss='binary_crossentropy', optimizer=sgd,
    #              metrics=['acc'])
    model.compile(loss='binary_crossentropy', optimizer="rmsprop",
                  metrics=['acc'])
    #model.compile(loss='binary_crossentropy', optimizer="adam",
    #              metrics=['acc'])
    return model


def build_lstm_model(gv_dim=100, max_length=40):
    model = Sequential()
    # model.add(Bidirectional(LSTM(max_length, return_sequences=True)))
    # model.add(LSTM(max_length, return_sequences=True,
    #               input_shape=(max_length, gv_dim)))
    model.add(LSTM(max_length, dropout=0.2,
                   recurrent_dropout=0.2, 
                   return_sequences=True,
                   input_shape=(max_length, gv_dim)))
    model.add(Flatten())
    model.add(Dense(max_length, activation='sigmoid'))
    # model.add(Dropout(0.1))
    model.summary()

    model.compile(loss='binary_crossentropy', optimizer="rmsprop",
                  metrics=['acc'])
    return model

def build_convnet_model(gv_dim=100, max_length=40):
    model = Sequential()
    model.add(layers.Conv1D(max_length, 3, activation='relu',
                        input_shape=(max_length, gv_dim)))
    model.add(layers.MaxPooling1D(2))
    model.add(layers.Conv1D(max_length, 3, activation='relu'))
    # model.add(layers.GlobalMaxPooling1D())
    model.add(Flatten())
    model.add(Dense(max_length, activation='sigmoid'))
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
    test_file = data_dir + "/qsc_set_test.txt"

    db_file = home + "/medline_glove_v2.db"
    train_data, train_labels = utils.load_qsc_data(train_file)
    test_data, test_labels = utils.load_qsc_data(test_file)

    data_map = {'dev_data': test_data, 'dev_labels': test_labels,
                'train_data': train_data,
                'train_labels': train_labels}
    # train(data_map, db_file, recurrent=True)
    # train_cv(data_map, db_file, recurrent=False, convNet=True)
    train_full(data_map, db_file, recurrent=True)


if __name__ == '__main__':
    main()
