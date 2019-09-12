import csv


def eval_perf(csv_file, method_column):
    first = True
    no_questions = 0
    sum = 0.0
    sum2 = 0.0
    with open(csv_file) as f:
        reader = csv.reader(f)
        for line in reader:
            if first:
                first = False
                continue
            no_questions += 1
            col_val = line[method_column].strip()
            if col_val.isdigit():
                # print(col_val)
                rank = int(col_val) + 1
                sum += 1.0 / rank
                if rank == 1:
                    sum2 += 1.0
    mrr = sum / no_questions
    prec_at_one = sum2 / no_questions
    print("MRR: {} Prec@1:{}".format(mrr, prec_at_one))
    return (mrr, prec_at_one)


csv_file = '../data/rank_annotations/question_method_ranks_annotation_table.csv'
print("Baseline")
eval_perf(csv_file, 1)
print('-'*40)
print("Method 1")
eval_perf(csv_file, 2)
print('-'*40)
print("Method 2")
eval_perf(csv_file, 3)
print('-'*40)
print("Method 3")
eval_perf(csv_file, 4)
print('-'*40)
print("Method 4")
eval_perf(csv_file, 5)
print('-'*40)
print("Method 5")
eval_perf(csv_file, 6)
print('-'*40)
print("Method 6")
eval_perf(csv_file, 7)
print('-'*40)


