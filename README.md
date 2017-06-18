# CompCorpus

An assortment of things to create parsed, annotated sentences with comparison information from a bunch of sources (IMS data, JDPA, Jindal and Liu).



## Basic packages

Packages for the annotation workflow (see below section Usage):

- `de.uni_stuttgart.ims.corpus.comparatives`:
   Extract comparison data and convert it to our format (step 1).

- `de.uni_stuttgart.ims.corpus.normalize`:
   Normalize annotations and sentences (step 2).
   
- `de.uni_stuttgart.ims.corpus.srlannotation`:
   Add labels to the parsed sentences (step 4).

- `de.uni_stuttgart.ims.corpus.folds`:
   Create folds for crossvalidation (step 5).


General packages:

- `de.uni_stuttgart.ims.corpus.compannotation`:
   Classes to represent an annotation for a sentence, a comparison, and a token.
   
- `de.uni_stuttgart.ims.corpus.util`:
   Assorted useful stuff, e.g., options reader, exceptions.
   


## Stuff from other people

- [CompBase](https://github.com/WiltrudKessler/CompBase): 
   Basic data structures, in-/output and just general helpful stuff for my project.
- [Stanford CoreNLP](http://stanfordnlp.github.io/CoreNLP/) aka `stanford-corenlp-3.2.0.jar`:
   For sentence splitting (see step 1 "Data extraction").
- [Stanford Named Entity Recognizer](http://stanfordnlp.github.io/CoreNLP/) aka `stanford-ner.jar`:
   For Named Entity Recognition
   (only necessary if you want to normalize them, see step 2 "Normalization")
- [MATE Parser](https://code.google.com/archive/p/mate-tools/)
   Dependency parser (see step 3 "Parsing").
- [JDom](http://www.jdom.org/) aka `jdom-2.0.4.jar`:
   XML parser
   (only necessary if you want to extract the JDPA data, see step 1 "Data extraction, JDPA").
- [Guava](https://github.com/google/guava) aka `guava-14.0-rc1.jar`:
   Basic stuff for files
   (only necessary if you want to extract the JDPA data, see step 1 "Data extraction, JDPA")
   

## Data annotation pipeline

Most of the following steps require a configuration file. Here is an example file:

```bash
# Sentences from the corpus in plain text, one sentence per line
plaintextSentencesFilename=cameras.sentences.txt
# Annotation from the corpus in IMS format
plaintextAnnotationsFilename=cameras.annotations.txt
# Normalized sentences from the corpus in plain text, one sentence per line
plaintextSentencesNormFilename=cameras.sentencesn.txt
# Normalized annotation from the corpus in IMS format
plaintextAnnotationsNormFilename=cameras.annotationsn.txt
# Options for normalization
normOptions=-splitPred

# Parsed sentences in CoNLL format
parsedSentencesFilename=cameras.parsed.txt
# Parsed sentences with pred-arg annotations in CoNLL format
srlAnnotatedSentencesFilename=cameras.comparisons.txt
# Argument markers to be used in annotation
useArgumentMarker=three
# Order of entities
annOption=surf
```


### Step 1: Data extraction

Extract all comparisons from the original data sets and convert data to the IMS comparison format. There are four different converters: JDPA, Jindal-Liu, USAGE and IMS (does some filtering of annotations, if wanted).

All converters use the following values from the configuration file:
- `plaintextSentencesFilename`: output file with the sentences for comparisons in the format of one sentence per line.
- `plaintextAnnotationsFilename`: output file with the annotations for comparisons in IMS format.



#### For the JDPA corpus

This converts the comparisons from the [JDPA corpus](http://verbs.colorado.edu/jdpacorpus/) into our format.

```bash
java -cp bin:../lib/jdom-2.0.4.jar:../lib/guava-14.0-rc1.jar:../lib/stanford-corenlp-3.2.0.jar:../NLPBase/bin de.uni_stuttgart.ims.corpus.comparatives.jdpa.JDPAConverter $CONFIGFILE
```

In addition to the above, the program uses the following values from the configuration file:
- `jdpaCorpusPaths`: Path for corpus directory, this directory should have two subfolders, 'annotation' and 'txt', several directories can be combined with ':' 
- `htmlMentionsOutputFilename`: Optional file for debug output.



#### For the Jindal and Liu data

This converts the [Jindal and Liu comparison data](http://www.cs.uic.edu/~liub/FBS/data.tar.gz) into our format.

```bash
java -cp bin:../lib/stanford-corenlp-3.2.0.jar:../NLPBase/bin de.uni_stuttgart.ims.corpus.comparatives.liu.LiuConverter $CONFIGFILE
```

In addition to the above, the program uses the following values from the configuration file:
- `inputCorpusFilename`: location of the file with the J&L annotations.


#### For the USAGE data

This converts the comparisons from the [USAGE fine-grained sentiment corpus](http://romanklinger.de/usagecorpus/) into our format.


```bash
java -cp bin:../NLPBase/bin:../lib/stanford-corenlp-3.2.0.jar de.uni_stuttgart.ims.corpus.comparatives.usage.USAGEExtractor $CONFIGFILE
```

In addition to the above, the program uses the following values from the configuration file:
- `inputCorpusFilename`: Input file for review texts (`.txt`).
- `inputAnnotationsFilenameSpans`: Input file for span annotations (`.csv`).
- `inputAnnotationsFilenameRelations`: Input file for relation annotations (`.rel`).


#### For IMS data

This step may be useful to combine sentences and annotations or to filter out non-comparisons, but usually this should not be necessary.

```bash
java -cp bin:../NLPBase/bin de.uni_stuttgart.ims.corpus.comparatives.ims.IMSExtractor $CONFIGFILE
```

In addition to the above, the program uses the following values from the configuration file:
- `inputSentencesFilename`: location of the file with the sentences.
- `inputAnnotationsFilename`: location of the file with the IMS annotations.




### Step 2: Normalization

```bash
java -cp bin:../NLPBase/bin:../lib/stanford-ner.jar:../lib/stanford-corenlp-3.2.0.jar de.uni_stuttgart.ims.corpus.normalize.NormalizeSentence $CONFIGFILE
```
The program uses the following values from the configuration file:
- `plaintextSentencesFilename`: input file with the sentences for comparisons in the format of one sentence per line.
- `plaintextAnnotationsFilename`: input file with the annotations for comparisons in IMS format.
- `plaintextSentencesNormFilename`: output file with the normalized sentences for comparisons in the format of one sentence per line.
- `plaintextAnnotationsNormFilename`: output file with the normalized annotations for comparisons in IMS format.
- `normOptions`: Further normalization options
   - `-ner`: recognize Named Entities and combine them into a single token.
   - `-adj`: for cases like "more comfortable", annotate the adjective ("comfortable") as the predicate instead of the default of annotating the function word ("more").
   - `-splitPred`: split multiword predicates into predicate and scale annotations.


### Step 3: Parsing

Use the MATE parser:

```bash
anna-eng $INPUTFILE $OUTPUTFILE
```

The following arguments are expected:
- `$INPUTFILE`: a file with one sentence per line, tokenized (as is produced by the data extraction or the normalization step).
- `OUTPUTFILE`: a file with dependency parse information in the CoNLL format.


### Step 4: Annotate the parsed file

```bash
java -cp bin:../NLPBase/bin de.uni_stuttgart.ims.corpus.srlannotation.CreateSRLTrainingData $CONFIGFILE
```

The program uses the following values from the configuration file:

- `parsedSentencesFilename`: input file with parsed sentences in CoNLL format.
- `plaintextAnnotationsNormFilename`: input file with the annotations for comparisons in IMS format.
- `srlAnnotatedSentencesFilename`: output file with parsed sentences in CoNLL format annotated with the information from the comparisons.
- `useArgumentMarker`: how to mark the arguments in the output file. There are the following options
   - `one`: everything is just "argument" A0 without distinction. <br/>
        (entity1, entity2, aspect, scale).
   - `two`: two classes, entities A1 and aspects A0. <br/>
         (entity1, entity2), (aspect, scale).
   - `thra`: leave aspects A0 and scale A3 separated, but combine both entities A1. <br/>
      (entity1, entity2), (aspect), (scale).
   - `thre`: leave entities one A1 and two A2 separated, but combine both aspect and scale A0. <br/>
      (entity1), (entity2), (aspect, scale).
   - `all`: use all four possible labels, entity one A1, entity two A2, aspect A0 and scale A3 <br/>
      (entity1), (entity2), (aspect), (scale).
- `annOption`: Order of annotation for entities by
   - `pref`: preference, A1 is the preferred, A2 the non-preferred entity.
   - `surf`: surface order, A1 is the first, A2 the second entity.


### Step 5: Create folds for crossvalidation (optional)

```bash
java -cp bin:../NLPBase/bin de.uni_stuttgart.ims.corpus.folds.CreateMixedFolds $INPUTFILE $FOLDS
```
The following arguments are expected:
- `$INPUTFILE`: a file with dependency parse information and annotated comparison information in the CoNLL format.
- `$FOLDS`: number of folds to create

The output will be `$FOLDS` files with the names `$INPUTFILE.1` to `$INPUTFILE.$FOLDS`.




## Licence

(c) Wiltrud Kessler

This code is distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported license
[http://creativecommons.org/licenses/by-nc-sa/3.0/](http://creativecommons.org/licenses/by-nc-sa/3.0/)
