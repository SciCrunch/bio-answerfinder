#!/bin/bash

# for Python 3 virtual environment with TensorFlow 1.12+
source $HOME/venv/bin/activate
export BERT_BASE_DIR=$HOME/data/biobert_v1.1_pubmed
export DATA_DIR=$HOME/dev/java/bio-answerfinder/data/bioasq/bioasq_manual_100
export OUT_DIR=/tmp/bioasq_output_biobert/

python run_bioasq_classifier.py \
  --task_name=bioasq \
  --do_train=true \
  --do_eval=true \
  --data_dir=$DATA_DIR \
  --vocab_file=$BERT_BASE_DIR/vocab.txt \
  --bert_config_file=$BERT_BASE_DIR/bert_config.json \
  --init_checkpoint=$BERT_BASE_DIR/model.ckpt-1000000 \
  --max_seq_length=64 \
  --train_batch_size=16 \
  --learning_rate=2e-5 \
  --num_train_epochs=3.0 \
  --output_dir=$OUT_DIR
