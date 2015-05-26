#/bin/bash
tr=`mktemp --tmpdir=run lucene_query.XXX`
echo "mvn compile exec:java -Dexec.mainClass=LuceneQuery -Dexec.args='$@' " > $tr
. $tr
rm $tr
