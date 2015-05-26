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
It is possible to evaluate all questions, as well as randomly select a subset of question multiple times. The output produced in each sampling iteration is saved to a separate file. A sample invokation of lucene_query.sh:
```
run/lucene_query.sh -d ~/lucene/yahoo_answers_baseline/ -i /home/leo/TextCollect/YahooAnswers/Manner/manner-v2.0/manner.xml.bz2 -n 15 -o eval/out -prob 0.01 -bm25_k1 0.6 -bm25_b 0.25 -sample_qty 2 -s data/stopwords.txt
```
Note the **stopword** file!


The effectiveness can be evaluated using the above mentioned *trec_eval* utility and *QREL* files **produced during indexing**. An example of using *trec_eval*:
```
.../trec_eval qrels.txt output_of_lucene_query
```
We use the BM25 similarity function. The default parameter values are *k1=1.2* and *b=0.75*. This parameters can be changed using parameters *bm25_k1* and *bm25_b*. In particular, better results are obtained for *k1=0.6* and *b=0.25*.

Note on using Stanford NLP
-----------------------

One can use Stanford NLP to tokenize and lemmatize input. To activate Stanford NLP lemmatizer set the value of the constant UtilConst.USE_STANFORD in the file [UtilConst.java](src/main/UtilConst.java) to **true**. The code will be recomiplied automatically if you use our scripts to index/query. This doesn't seem to improve performance, though, but processing is longer.
