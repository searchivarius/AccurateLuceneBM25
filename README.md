Fixing Lucene's BM25 (and testing it using Yahoo! Answers and Stack Overflow collections)
=================


Main Prerequisites
-----------------------

1. Data
 1. Yahoo Answers! data set needs to be obtained [from Yahoo! Webscope](http://webscope.sandbox.yahoo.com/catalog.php?datatype=l);
 2. Stack Overflow data set can be freely downloaded: [we need only posts](https://archive.org/download/stackexchange/stackoverflow.com-Posts.7z). It needs to be subsequently converted to the Yahoo Answers! format
2. You need Java 7 and Maven;
3. [TREC trec_eval evaluation utility](http://trec.nist.gov/trec_eval/).

Indexing
-----------------------

To see the indexing options, type:
```
scripts/lucene_index.sh
```
In addition to an input file (can be gzipped or bzipped2). You will have to specify the output directory to store a *Lucene* index, an output file to store TREC-style QREL files.


Testing
-----------------------

To see querying options, type:
```
scripts/lucene_query.sh
```
It is possible to evaluate all questions, as well as randomly select a subset of questions. It is also possible to limit the number of query to execute. A sample invocation of lucene_query.sh:
```
scripts/lucene_query.sh -d ~/lucene/yahoo_answers_baseline/ -i /home/leo/TextCollect/YahooAnswers/Manner/manner-v2.0/manner.xml.bz2 -n 15 -o eval/out -prob 0.01 -bm25_k1 0.6 -bm25_b 0.25 -max_query_qty 10000 -s data/stopwords.txt
```
Note the **stopword** file!


The effectiveness can be evaluated using the above mentioned *trec_eval* utility and *QREL* files **produced during indexing**. An example of using *trec_eval*:
```
.../trec_eval qrels.txt output_of_lucene_query
```
We use the BM25 similarity function. The default parameter values are *k1=1.2* and *b=0.75*. These values are specified via parameters *bm25_k1* and *bm25_b*. 

Note on using Stanford NLP
-----------------------

One can use Stanford NLP to tokenize and lemmatize input. To activate Stanford NLP lemmatizer set the value of the constant UtilConst.USE_STANFORD in the file [UtilConst.java](src/main/java/UtilConst.java#L33) to **true**. The code will be recomiplied automatically if you use our scripts to index/query. This doesn't seem to improve performance, though, but processing is longer.
