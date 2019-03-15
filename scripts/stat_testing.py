import json
import numpy as np
from scipy import stats

def load_from_json(json_file):
    with open(json_file) as f:
        data = json.load(f)
    return  np.asarray(data['F1'])

def load_coverage(json_file):
    with open(json_file) as f:
        data = json.load(f)
    cf_list = list()
    for rec in data:
        cf_list.append(rec['coverageFrac'])
    return np.asarray(cf_list)


sample1 =  load_from_json('results/qks_v2_dnn_performance.json')
sample2 =  load_from_json('results/qks_v2_lstm_performance.json')
sample3 =  load_from_json('results/qks_v3_lstm_performance.json')
sample4 =  load_from_json('results/qks_v3_dnn_performance.json')

#sample5 = load_from_json('/tmp/qks_v2_performance_rmsprop.json')
#sample6 = load_from_json('/tmp/qks_v2_performance.json')

sample7 =  load_from_json('results/qks_v2_cnn_performance.json')
sample8 = load_from_json('/results/qks_v3_lstm_lstm_performance.json')
# print( stats.ttest_ind(sample1, sample2))

#print( stats.ttest_ind(sample2, sample3))
#print( stats.ttest_ind(sample1, sample4))

# print(stats.ttest_ind(sample1, sample7))

print(stats.ttest_ind(sample2, sample8))


#cov_base = load_coverage('results/coverage_details_baseline.json')
#cov_filter = load_coverage('results/coverage_details_lstm_filter.json')

#print(stats.ttest_ind(cov_base, cov_filter))

