Bio-AnswerFinder
================

A question answering system for biomedical literature.

## Prerequisites

* Java 1.8+
* Gradle
* An ElasticSearch Endpoint with indexed PubMED abstracts 


### Named entity databases
`LookupUtils` and `LookupUtils2`
* Drugs /usr/local/lt/drug_bank.db
* Diseases /usr/local/lt/diseases.db
* Gene names /usr/local/lt/hugo.db
* Scigraph ontology lookup
    * data/scigraph_lt/ontology-classes-with-labels-synonyms-parents.json
    * data/scigraph_lt/scr-classes-with-labels-synonyms-parents.json

### NominalizationService
/usr/local/lt/nominalization.db

### AcronymService
/usr/local/lt/acronyms.db

### Morphology database

`org.bio_answerfinder.nlp.morph.Lemmanizer`
and `org.bio_answerfinder.util.SRLUtils`

/usr/local/morph/morph.db

## Building

```bash
gradle clean war
```

Then deploy to Tomcat or any Java web app container.
