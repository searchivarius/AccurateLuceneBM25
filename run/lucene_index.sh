#/bin/bash
tr=`mktemp --tmpdir=run lucene_index.XXX`
echo "mvn compile exec:java -Dexec.mainClass=LuceneIndexer -Dexec.args='$@' " > $tr
. $tr
rm $tr
