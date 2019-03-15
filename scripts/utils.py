
import re
import csv
from collections import  Counter
import numpy as np


def load_glove(glove_text_file, gv_dim=100, strip_phrases=True):
    term_idx_map = {}
    count = 0
    with open(glove_text_file) as f:
        for line in f:
            if strip_phrases:
                idx = line.index(' ')
                term = line[0:idx]
                if term.find('_') == -1:
                    count += 1
            else:
                count += 1
    max_rank = count + 1
    print("max_rank:", max_rank)
    glove_vecs = np.ndarray((max_rank+1, gv_dim), dtype='float32')
    i = 1
    with open(glove_text_file) as f:
        for line in f:
            tokens = line.split(' ')
            term = tokens[0]
            if strip_phrases:
                if term.find('_') == -1:
                    glove_vecs[i] = [float(x) for x in tokens[1:]]
                    term_idx_map[term] = i
                    i += 1
            else:
                glove_vecs[i] = [float(x) for x in tokens[1:]]
                term_idx_map[term] = i
                i += 1
            if (i % 1000) == 0:
                print("\rGlove vectors read so far:{}".format(i), end='')
    print()
    return glove_vecs, term_idx_map


def load_answer_ranking_data(ar_data_file):
    labels = list()
    questions = list()
    candidates = list()
    first = True
    with open(ar_data_file) as f:
        reader = csv.reader(f, delimiter="\t")
        for line in reader:
            if first:
                first = False
                continue
            questions.append(line[1])
            candidates.append(line[2])
            labels.append(int(line[0]))
    return questions, candidates, labels

def load_answer_ranking_data_ns(ar_data_file, max_neg_size=5):
    labels = list()
    questions = list()
    candidates = list()
    first = True
    cur_q = None
    neg_count = 0
    with open(ar_data_file) as f:
        reader = csv.reader(f, delimiter="\t")
        for line in reader:
            if first:
                first = False
                continue
            label = int(line[0])
            question = line[1]
            candidate = line[2]
            if not cur_q or cur_q != question:
                cur_q = question
                neg_count = 0
            if label == 1:
                questions.append(question)
                candidates.append(candidate)
                labels.append(label)
            else:
                if neg_count <= max_neg_size:
                    if np.random.random_sample() < 0.1:
                        questions.append(question)
                        candidates.append(candidate)
                        labels.append(label)
                        neg_count += 1

    return questions, candidates, labels

def load_answer_ranking_data_ns2(ar_data_file, max_neg_size=5):
    labels = list()
    questions = list()
    candidates = list()
    first = True
    cur_q = None
    neg_count = 0
    samples = list()
    neg_added = False
    with open(ar_data_file) as f:
        reader = csv.reader(f, delimiter="\t")
        for line in reader:
            if first:
                first = False
                continue
            label = int(line[0])
            question = line[1]
            candidate = line[2]
            if not cur_q or cur_q != question:
                if not neg_added and len(samples) > max_neg_size:
                    negs = np.random.choice(samples, max_neg_size, replace=False)
                    for neg in negs:
                        questions.append(cur_q)
                        candidates.append(neg)
                        labels.append(0)
                neg_added = False
                cur_q = question
                samples = list()
            if label == 1:
                questions.append(question)
                candidates.append(candidate)
                labels.append(label)
                if not neg_added and len(samples) > max_neg_size:
                    neg_added = True
                    negs = np.random.choice(samples, max_neg_size, replace=False)
                    for neg in negs:
                        questions.append(question)
                        candidates.append(neg)
                        labels.append(0)

            else:
                samples.append(candidate)
    if not neg_added:
        negs = np.random.choice(samples, max_neg_size, replace=False)
        for neg in negs:
            questions.append(cur_q)
            candidates.append(neg)
            labels.append(0)


    return questions, candidates, labels

def prep_embedding_vocab(questions, candidates, max_tokens=30, max_vocab_size=10000):
    counter = Counter()
    cur_question = None
    for question, candidates in zip(questions, candidates):
        if not cur_question or cur_question != question:
            q_toks = question.split()
            for token in q_toks:
                counter[token.lower()] += 1
        c_toks = candidates.split()
        for token in c_toks:
            counter[token.lower()] += 1
    vocab = {}
    words = counter.most_common(max_vocab_size)
    for i, pair in enumerate(words):
        vocab[pair[0]] = i + 1
    return vocab

def prep_embedding_data(questions, candidates, vocab, max_tokens=30, max_vocab_size=10000):
    Xa = np.zeros((len(questions), max_tokens ), dtype="int32")
    Xb = np.zeros((len(candidates), max_tokens), dtype="int32")
    for i, text in enumerate(questions):
        tokens = text.split()
        for j, token in enumerate(tokens):
            if j >= max_tokens:
                break
            token = token.lower()
            if token in vocab:
                Xa[i, j] = vocab[token]
    for i, text in enumerate(candidates):
        tokens = text.split()
        for j, token in enumerate(tokens):
            if j >= max_tokens:
                break
            token = token.lower()
            if token in vocab:
                Xb[i,j] = vocab[token]
    return Xa, Xb




def load_qsc_data(qsc_data_file):
    labels = []
    data = []
    with open(qsc_data_file) as f:
        i = 1
        for line in f:
            if i % 2 == 1:
                data.append(line.strip())
            else:
                labels.append([int(x) for x in re.split(r'\s+', line.strip())])
            i += 1
    return data, labels


def load_qsc_pos_data(qsc_pos_data_file):
    pos_data = []
    data = []
    pos_set = set()
    with open(qsc_pos_data_file) as f:
        i = 1
        for line in f:
            line = line.strip()
            if i % 2 == 1:
                data.append(line)
            else:
                pos_tags = [x for x in re.split(r'\s+', line)]
                pos_data.append(pos_tags)
                for pos in pos_tags:
                    pos_set.add(pos)
            i += 1
    pos_set.add("<UNK>")
    pos_idx_map = {k: i for i, k in enumerate(sorted(pos_set))}
    return pos_data, data, pos_idx_map


def prep_pos_data(pos_data, max_length, pos_idx_map):
    max_pos_tags = len(pos_idx_map)
    Xs = np.zeros((len(pos_data), max_length * max_pos_tags), dtype='float32')
    for i, pos_tags in enumerate(pos_data):
        for j, pos in enumerate(pos_tags):
            offset = j * max_pos_tags
            if pos in pos_idx_map:
                Xs[i, offset + pos_idx_map[pos]] = 1.0
            else:
                Xs[i, offset + pos_idx_map['<UNK>']] = 1.0
    return Xs


def extract_data(data, labels, max_length):
    label_matrix = np.zeros((len(labels), max_length), dtype='int32')
    for i, label in enumerate(labels):
        print(label)
        for j, col_label in enumerate(label):
            label_matrix[i, j] = col_label
    return data, label_matrix


def _expand_tokens(tokens, term_idx_map):
    unkwn_map = {}
    idx = 1
    for i in range(len(tokens)):
        tok = tokens[i]
        if tok not in term_idx_map:
            if tok not in unkwn_map:
                lab = 'unk' + str(idx)
                unkwn_map[tok] = lab
                idx += 1
                tokens[i] = lab
            else:
                tokens[i] = unkwn_map[tok]


def prep_word_id_matrix(data, term_idx_map, max_length=40):
    Xs = np.zeros((len(data), max_length), dtype='int32')
    no_vocab_count = 0
    vocab_count = 0
    unknown_vocabs = set()
    for i, question in enumerate(data):
        tokens = question.split(' ')
        _expand_tokens(tokens, term_idx_map)
        words = []
        while len(words) <= max_length and tokens:
            word = tokens.pop(0)
            if word in term_idx_map:
                words.append(word)
                vocab_count += 1
            else:
                no_vocab_count += 1
                unknown_vocabs.add(word)
                raise ValueError(
                    "Unexpected missing term id for {}".format(word))
        for j, term in enumerate(words):
            Xs[i, j] = term_idx_map[term]

    print('vocab count:' + str(vocab_count))
    print('no_vocab_count:' + str(no_vocab_count))
    print("---------------")
    print(", ".join(str(e) for e in unknown_vocabs))
    return Xs


def prep_cv_sets(num_data, rnd, no_folds=4):
    cv_tuples = list()
    fold_size = num_data // no_folds
    index_list = list(range(0, num_data))
    rnd.shuffle(index_list)
    for i in range(no_folds):
        offset = i * fold_size
        if i == 0:
            dev_indices = list(index_list[:fold_size])
            train_indices = list(index_list[fold_size:])
        elif i == (no_folds - 1):
            dev_indices = list(index_list[offset:])
            train_indices = list(index_list[:offset])
        else:
            dev_indices = list(index_list[offset:offset + fold_size])
            train_indices = list(index_list[:offset])
            train_indices += list(index_list[offset+fold_size:])
        cv_tuples.append((train_indices, dev_indices))
    return cv_tuples
