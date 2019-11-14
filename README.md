Bio-AnswerFinder
================

A question answering system for biomedical literature.

## Prerequisites

* Java 1.8+
* Gradle
* An ElasticSearch Endpoint with indexed PubMED abstracts 


The datasets generated during and/or analysed during the current study are available 
in the Zenodo repository.
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.2597595.svg)](https://doi.org/10.5281/zenodo.2597595)


This includes learned GloVE vectors, vocabulary, named entity databases, nominalization, acronym and morphology databases.

The system is configured by a configuration file. An example configuration file is provided in the project 
`src/main/resources/bio-answerfinder.properties.example`.

The code is written in Java (1.8+) and uses Gradle to build the project.


### Named entity databases
`LookupUtils` and `LookupUtils2`


*  Drugs /usr/local/lt/drug_bank.db
*  Diseases /usr/local/lt/diseases.db
*  Gene names /usr/local/lt/hugo.db
*  Scigraph ontology lookup
    *  data/scigraph_lt/ontology-classes-with-labels-synonyms-parents.json
    *  data/scigraph_lt/scr-classes-with-labels-synonyms-parents.json

### NominalizationService
```
/usr/local/lt/nominalization.db
```
### AcronymService
```
/usr/local/lt/acronyms.db
```

### Morphology database

`org.bio_answerfinder.nlp.morph.Lemmanizer`
and `org.bio_answerfinder.util.SRLUtils`

```
/usr/local/morph/morph.db
```

## Scripts
Python 3 code for the keyword selection classifier and BERT fine-tuning is in the `scripts` directory of the project.
You need Tensorflow (1.12+) and Keras installed into an Python virtual environment. 
You need to clone Google BERT github project 

```
git clone https://github.com/google-research/bert
```

After that, follow instructions on BERT github site to download BERTBase model `uncased_L-12_H-768_A-12`.
Similarly, download BioBERT model from `https://github.com/naver/biobert-pretrained/releases/tag/v1.1-pubmed`.

Copy `run_bioasq_classifier.py`, `train_bioasq.sh`, `train_bioasq.sh`, `train_biobert_bioasq.sh`, `predict_biobert_bioasq.sh` scripts to Google BERT clone directory and update the environment variables 
in the driver Bash shell scripts to match your project and model 
installation directories.

## Annotated Datasets

All the annotated datasets generated during this study is under the project directory `data`.

### Query Keyword Selection Classifier Data
To be used by the Python 3 script `scripts/question_keyword_selector_v2.py`

*  data/bioasq/bioasq_manual_100/qsc/qsc_set_train.txt
*  data/bioasq/bioasq_manual_100/qsc/qsc_set_test.txt
*  data/bioasq/bioasq_manual_100/qsc/qsc_set_pos_tags_train.txt
*  data/bioasq/bioasq_manual_100/qsc/qsc_set_pos_tags_test.txt

## BERT reranker training/testing data

Note that there is no development data. Both dev.tsv and test.tsv are same. The file dev.tsv is there for the Google BERT code.

*  data/bioasq/bioasq_manual_100/train.tsv
*  data/bioasq/bioasq_manual_100/dev.tsv
*  data/bioasq/bioasq_manual_100/test.tsv

## BioAnswerFinder unsupervised (wRWMD reranking) annotated results

*  data/bioasq/bioasq_manual_100/qaengine1/question_results_wmd_defn_focus.txt

## BERT Reranker Blind Evaluation

*  data/evaluation/annotator_1_bert_rank.csv
*  data/evaluation/annotator_1_rwmd_rank.csv
*  data/evaluation/annotator_2_bert_rank.csv
*  data/evaluation/annotator_2_rwmd_rank.csv
*  data/evaluation/annotator_3_bert_rank.csv
*  data/evaluation/annotator_3_rwmd_rank.csv
*  data/evaluation/rwmd_question_answer_candidates.csv
*  data/evaluation/bert_question_answer_candidates.csv


## BioBERT and BERT Reranker Comparison
To be used by the Python 3 script `scripts/show_perfomance.py`

*  data/evaluation/bert_biobert_comparison.csv
*  data/evaluation/biobert_perf_records.json

## Datasets for AKTS workshop paper 

Ibrahim Burak Ozyurt, Jeffrey S. Grethe. Iterative Document Retrieval via Deep Learning Approaches for Biomedical Question Answering 
 in 15th International Conference on eScience (2019). (doi: 10.1109/eScience.2019.00072 )

* data/rank_test (training/testing datasets for NN models)
* data/rank_annotations (curator rankings for each method tested)

## Exact answers for factoid question evaluation

* data/evaluation/bert_factoid_test_perf.csv
* data/evaluation/rank_test_factoid_perf.csv



Due PubMed license restrictions, Pubmed abstracts ElasticSearch index cannot be provided. i
It can be generated using our ETL system Foundry available in GitHub `https://github.com/biocaddie/Foundry-ES`. 

## Building

```bash
gradle clean war
```

Then deploy to Tomcat or any Java web app container.


For any questions, please contact iozyurt@ucsd.edu.




