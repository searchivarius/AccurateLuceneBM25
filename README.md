More accurate BM25 similarity for Lucene
=================
Improving the effectiveness Lucene's BM25 (and testing it using community QA and ClueWeb* collections). Please, see [my blog post for details](http://searchivarius.org/blog/accurate-bm25-similarity-lucene-follow). It works for early version of Lucene 6.x, e.g., 6.0. However, the later Lucene versions (I think starting from 6.3) changed internal API, so this similarity class will not work without changes.

Main Prerequisites
-----------------------

1. Data
 1. Yahoo Answers! data set needs to be obtained [from Yahoo! Webscope](http://webscope.sandbox.yahoo.com/catalog.php?datatype=l);
 2. Stack Overflow data set can be freely downloaded: [we need only posts](https://archive.org/download/stackexchange/stackoverflow.com-Posts.7z). It needs to be subsequently converted to the Yahoo Answers! format using the script ``scripts/convert_stack_overflow.sh``. The converted collection that I used is [also available here](https://s3.amazonaws.com/RemoteDisk/TextCollections/StackExchange/StackOverflow/PostsNoCode2016-04-28.xml.bz2). Note that I converted data without including any Stack Overflow code (exclusion of the code makes the retrieval task harder).
 3. ClueWeb09 & ClueWeb12. I use **Category B** only, which is a subset containing about 50 million documents. Unfortunately, these collections aren't freely available for download. For details on obtaining access to these collections, please refer to the official documents: [CluewWeb09](http://lemurproject.org/clueweb09/index.php#Obtaining), [ClueWeb12](http://lemurproject.org/clueweb12/index.php#Obtaining).
2. You need Java 7 and Maven;
3. To carry out evaluations you need R, Python, and Perl. Should you decide to use an old style evaluation scripts (not enabled by default), you will also need a C compiler. The evaluation script will download and compile [TREC trec_eval evaluation utility](http://trec.nist.gov/trec_eval/) on its own.

Indexing
-----------------------

The low-level indexing script is ``scripts/lucene_index.sh``. I have also implemented a wrapper script that I recommend using instead. To create indices and auxilliary files in subdirectories ``exper/compr`` (for Yahoo Answers! Comprehensive) and ``exper/stack`` (for Stack Oveflow), I used the following commands (you will need to specify location of input/output files on **your own computer**):
```
scripts/create_indices.sh ~/TextCollect/StackOverflow/PostsNoCode2016-04-28.xml.bz2 exper/stack yahoo_answers
scripts/create_indices.sh ~/TextCollect/YahooAnswers/Comprehensive/FullOct2007.xml.bz2 exper/compr yahoo_answers
```
Note that last argument, it specifies the type of input data. In the case of ClueWeb09 and ClueWeb09, the type of data set is **clueweb**. A sample indexing command (additional quotes are needed, because the input data set directory has a space in its full path):
```
scripts/create_indices.sh "\"/media/leo/Seagate Expansion Drive/ClueWeb12_B13/\"" exper/clueweb12 clueweb
```
Again, don't forget that you have to specify the location of input/output files on your computer!

Expert indexing 
------------------------

To see the indexing options of the low-level indexing script, type:
```
scripts/lucene_index.sh
```
In addition to an input file (which can be gzipped or bzipped2), you have to specify the output directory to store a *Lucene* index. For community QA data you can specify the location of an output file to store TREC-style QREL files.


Testing with community QA data sets
-----------------------

The low-level querying script is ``scripts/lucene_query.sh``, but I strongly recommend to use a wrapper ``scripts/run_eval_queries.sh`` that does almost all the evaluation work (except extracting average retrieval time). The following is an example of invoking the evaluation script:
```
scripts/run_eval_queries.sh ~/TextCollect/YahooAnswers/Comprehensive/FullOct2007.xml.bz2 yahoo_answers exper/compr/ 10000 5 1
```
We ask here to use the **first** 10000 questions. The search series is repeated 5 times. The value of the last argument tells the script to evaluate **effectiveness** as well as to compute p-values. Again, you need R, Perl, Python for this. You can hack an evaluation script and set the variable ``USE_OLD_STYLE_EVAL_FOR_YAHOO_ANSWERS`` to 1. In this case, you will also need a C compiler.

**Note 1:** the second argument is the type of data source. Use ``yahoo_answers`` for community QA collections. For ClueWeb09 and clueweb12 use ``trec_web``.

**Note 2:** the script will not re-run queries if output files already exist!. To re-run queries you need to manually delete file named ``trec_run``
from respective subdirectories.

The average retrieval times are saved to a log file. They can be extracted as follows:
```
grep 'on average' exper/compr/standard/query.log
```
To retrieve the list of timings for every run as a space-separated sequence, you can do the following:
```
grep 'on average' exper/compr/standard/query.log |awk '{printf("%s%s",t,$7);t=" "}END{print ""}'
```
**Note** that ``exper/compr`` in these examples should be replaced with your own top-level directory that you pass to the script ``scripts/create_indices.sh``.

Testing with ClueWeb09/12 data sets
-----------------------
Again, please, use the script ``scripts/lucene_query.sh``. However, an additional arguments: relevance judgements (the so-called QREL files) should be specified. For ClueWeb09 data, I have placed both judgements (QREL-files) and queries to this repo. Therefore, one can run evaluations as follows (don't forget to specify your own directories with ClueWeb09/12 indices instead of ``exper/clueweb09``):
```
scripts/run_eval_queries.sh eval_data/clueweb09_1MQ/queries_1MQ.txt trec_web exper/clueweb09 10000 1 1 eval_data/clueweb09_1MQ/qrels_1MQ.txt
```
For ClueWeb12 data sets, query files and QREL-files need to be generated. To do this, you first need to download and uncompress [UQV100 files](https://figshare.com/articles/_/3180694). Then, you can generate QREL-files and queries, e.g., as follows:
```
eval_data/uqv100/merge_uqv100.py  ~/TextCollect/uqv100/ eval_data/uqv100/uqv100_mult_queries.txt eval_data/uqv100/uqv100_mult_qrels.txt
```
Finally, you can use generated QREL and queries to run evaluation:
```
scripts/run_eval_queries.sh eval_data/uqv100/uqv100_mult_queries.txt trec_web exper/clueweb12/ 10000 1 1 eval_data/uqv100/uqv100_mult_qrels.txt 
```

Expert querying
-----------------------

To see options of the low-level querying script, type:
```
scripts/lucene_query.sh
```
It is possible to evaluate all questions, as well as randomly select a subset of questions. It is also possible to limit the number of queries to execute. A sample invocation of lucene_query.sh:
```
scripts/lucene_query.sh -d ~/lucene/yahoo_answers_baseline/ -i /home/leo/TextCollect/YahooAnswers/Manner/manner-v2.0/manner.xml.bz2 -source_type "yahoo_answers" -n 15 -o eval/out -prob 0.01 -bm25_k1 0.6 -bm25_b 0.25 -max_query_qty 10000 -s data/stopwords.txt
```
Note the **stopword** file!

The effectiveness can be evaluated using the above mentioned utility *trec_eval* and utilty *gdeval.pl* located in directory ``scripts``. To this end, you need *QREL* files **produced during indexing**. 

We use the BM25 similarity function. The default parameter values are *k1=1.2* and *b=0.75*. These values are specified via parameters *bm25_k1* and *bm25_b*. 

Note on using Stanford NLP
-----------------------

One can use Stanford NLP to tokenize and lemmatize input. To activate Stanford NLP lemmatizer set the value of the constant UtilConst.USE_STANFORD in the file [UtilConst.java](src/main/java/UtilConst.java#L33) to **true**. The code will be recomiplied automatically if you use our scripts to index/query. This doesn't seem to improve performance, though, but processing is **much** longer.
