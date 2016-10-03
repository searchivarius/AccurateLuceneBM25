This directory contains 100 queries and respective relevance judgements from ClueWeb12 WebTrack 2013-2014 task. 
They were obtained from the following NIST pages: [2013](http://trec.nist.gov/data/web2013.html), [2014](http://trec.nist.gov/data/web2014.html).
Note that we combined files containing **all** judgments, which were deduplicated (merging judgments relevant to all sub-topics) as follows:
```
cat qrels_Web_orig.txt |awk '{print $1" 0 "$3" "$4}'|sort|awk '{if (pq!=$1 || pd!=$3) print $0;pq=$1;pd=$3}' > qrels_Web_dedup.txt
```
Also note that this set of queries is quite small and we don't get statistically significant differences.
