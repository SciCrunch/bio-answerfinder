#!/bin/bash

# for Python 3 virtual environment with TensorFlow 1.12+
source $HOME/venv/bin/activate

export BERT_BASE_DIR=$HOME/data/bert_models/uncased_L-12_H-768_A-12
export DATA_DIR=$HOME/dev/java/bio-answerfinder/data/bioasq/bioasq_manual_100
export TRAINED_CLASSIFIER=/tmp/bioasq_output_baseline

python run_bioasq_classifier.py \
  --task_name=bioasq \
  --do_predict=true \
  --data_dir=$DATA_DIR \
  --vocab_file=$BERT_BASE_DIR/vocab.txt \
  --bert_config_file=$BERT_BASE_DIR/bert_config.json \
  --init_checkpoint=$TRAINED_CLASSIFIER \
  --max_seq_length=64 \
  --train_batch_size=16 \
  --learning_rate=2e-5 \
  --num_train_epochs=3.0 \
  --output_dir=/tmp/bioasq_predict_out/
