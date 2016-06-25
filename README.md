More accurate BM25 similarity for Lucene
=================
Improving the effectiveness Lucene's BM25 (and testing it using Yahoo! Answers and Stack Overflow collections). Please, see [my blog post for details](http://searchivarius.org/blog/accurate-bm25-similarity-lucene).

Main Prerequisites
-----------------------

1. Data
 1. Yahoo Answers! data set needs to be obtained [from Yahoo! Webscope](http://webscope.sandbox.yahoo.com/catalog.php?datatype=l);
 2. Stack Overflow data set can be freely downloaded: [we need only posts](https://archive.org/download/stackexchange/stackoverflow.com-Posts.7z). It needs to be subsequently converted to the Yahoo Answers! format using the script ``scripts/convert_stack_overflow.sh``. The converted collection that I used is [also available here](https://s3.amazonaws.com/RemoteDisk/TextCollections/StackExchange/StackOverflow/PostsNoCode2016-04-28.xml.bz2). Note that I converted without include any code (which makes the retrieval task harder).
2. You need Java 7 and Maven;
3. To carry out evaluations you need R, Python, and a C compiler. The evaluation script will download and compile [TREC trec_eval evaluation utility](http://trec.nist.gov/trec_eval/).

Indexing
-----------------------

The low-level indexing script is ``scripts/lucene_index.sh``. I have also implemented a wrapper script that I recommend using instead. To create indices and auxilliary files in subdirectories ``exper/compr`` (for Yahoo Answers! Comprehensive) and ``exper/stack`` (for Stack Oveflow), I used the following commands (you will need to specify location of input files on **your own computer**, which is the first argument of ``scripts/create_indices``):
```
scripts/create_indices.sh ~/TextCollect/StackOverflow/PostsNoCode2016-04-28.xml.bz2 exper/stack
scripts/create_indices.sh ~/TextCollect/YahooAnswers/Comprehensive/FullOct2007.xml.bz2 exper/compr
```


To see the indexing options of the low-level indexing script, type:
```
scripts/lucene_index.sh
```
In addition to an input file (which can be gzipped or bzipped2), you have to specify the output directory to store a *Lucene* index, and an output file to store TREC-style QREL files.


Testing
-----------------------

The low-level querying script is ``scripts/lucene_query.sh``. I have also implemented a wrapper ``scripts/run_eval_queries.sh`` that does almost all the evaluation work (except extracting average retrieval time). The following is an example of invoking the evaluation script:
```
scripts/run_eval_queries.sh ~/TextCollect/YahooAnswers/Comprehensive/FullOct2007.xml.bz2 exper/compr/ 1000 6 1
```
We ask here to use the **first** 1000 questions. The search series is repeated 6 times. The value of the last argument tells the script to evaluate **effectiveness** as well as to compute p-values (again, you need R, Python, and C compiler for this). 
The average retrieval times are saved to a log file. They can be extracted as follows:
```
grep 'on average' exper/compr/standard/query.log
```
To retrieve the list of timings for every run as a space-separated sequence, you can do the following:
```
grep 'on average' exper/compr/standard/query.log |awk '{printf("%s%s",t,$7);t=" "}'
```
**Note** that ``exper/compr`` in these examples should be replaced with your own top-level directory that you pass to the script ``scripts/create_indices.sh``.

To see options of the low-level querying script, type:
```
scripts/lucene_query.sh
```
It is possible to evaluate all questions, as well as randomly select a subset of questions. It is also possible to limit the number of queries to execute. A sample invocation of lucene_query.sh:
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
