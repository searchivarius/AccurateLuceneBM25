Yahoo! Answers baseline based on Lucene
=================


Main Prerequisites
-----------------------

1. The data set needs to be obtained [from Yahoo! Webscope](http://webscope.sandbox.yahoo.com/catalog.php?datatype=l);
2. You need Java 7 and Maven;
3. [TREC trec_eval evaluation utility](http://trec.nist.gov/trec_eval/).

Indexing
-----------------------

To see the indexing options, type:
```
run/lucene_index.sh
```
In addition to an input file (can be gzipped or bzipped2). You will have to specify the output directory to store a *Lucene* index, an output file to store TREC-style QREL files.


Testing
-----------------------

To see querying options, type:
```
run/lucene_query.sh
```
It is possible to evaluate all questions, as well as randomly select a subset of question multiple times. The output produced in each sampling iteration is saved to a separate file. The effectiveness can be evaluated using the above mentioned *trec_eval* utility and *QREL* files **produced during indexing**. An example of using *trec_eval*:
```
.../trec_eval qrels.txt output_of_lucene_query
```
We use the BM25 similarity function. The default parameter values are *k1=1.2* and *b=0.75*. This parameters can be changed using parameters *bm25_k1* and *bm25_b*. In particular, better results are obtained for *k1=0.6* and *b=0.25*.

Note on using Stanford NLP
-----------------------

We use Stanford NLP to tokenize and lemmatize input. This can be switched off by changing the value of the constant UtilConst.USE_STANFORD in the file [UtilConst.java](src/main/UtilConst.java). The code will be recomiplied automatically if you use our scripts to index/query.
