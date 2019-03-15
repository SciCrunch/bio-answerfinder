import json

import matplotlib.pyplot as plt
import numpy as np

def load_from_json(json_file):
    with open(json_file) as f:
        data = json.load(f)
    list_len = len(data[0]['acc'])
    acc_mat = np.ndarray((len(data), list_len), dtype='float32')
    val_acc_mat = np.ndarray((len(data), list_len), dtype='float32')
    for i, hist in enumerate(data):
        acc_mat[i] = np.asarray(hist['acc'])
        val_acc_mat[i] = np.asarray(hist['val_acc'])
    return (np.mean(acc_mat, 0)* 100, np.mean(val_acc_mat, 0) * 100)



mean_acc, mean_val_acc = load_from_json('/tmp/qks_v3_history.json')

epochs = range(1, len(mean_acc) + 1)

plt.plot(epochs, mean_acc, 'ko', label='Mean Training Accuracy')
plt.plot(epochs, mean_val_acc, 'k', label='Mean Validation Accuracy')
plt.title("Average Training and Validation Accuracy")
plt.xlabel('Epochs')
plt.ylabel('Accuracy(%)')
plt.legend()

plt.show()


