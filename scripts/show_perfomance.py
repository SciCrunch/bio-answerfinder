import csv

def read_csv(csv_file):
    with open(csv_file, 'r') as f:
        reader = csv.reader(f, delimiter=",")
        lines = []
        first = True
        for line in reader:
            if first:
                first = False
                continue
            lines.append(line)
        return lines

def calc_mrr(csv_file, rank_idx):
    data_lines = read_csv(csv_file) 
    sum = 0.0
    for idx,row in enumerate(data_lines):
        col_val = row[rank_idx]
        #print("{}:{}".format(idx, col_val))
        if not col_val == 'N/A':
            rank = int(col_val)
            sum += 1.0 / rank
    mrr = sum / len(data_lines)
    return mrr


def calc_precision_at_one(csv_file, rank_idx):
    data_lines = read_csv(csv_file) 
    sum = 0.0
    for idx,row in enumerate(data_lines):
        col_val = row[rank_idx]
        if not col_val == 'N/A':
            rank = int(col_val)
            if rank == 1:
                sum += 1.0
    pao = sum / len(data_lines)
    return pao



comparison_file = '../data/evaluation/bert_biobert_comparison.csv'
bert_mrr = calc_mrr(comparison_file,2)
print("BERT MRR:{}".format(bert_mrr))
biobert_mrr = calc_mrr(comparison_file,3)
print("BioBERT MRR:{}".format(biobert_mrr))
bert_pao = calc_precision_at_one(comparison_file, 2)
biobert_pao = calc_precision_at_one(comparison_file, 3)
print("BERT Precision@1:{}".format(bert_pao))
print("BioBERT Precision@1:{}".format(biobert_pao))


