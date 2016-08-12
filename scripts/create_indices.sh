#/bin/bash
export MAVEN_OPTS="-Xms8192m -server"
input=$1
if [ "$input" = "" ] ; then
  echo "Specify the input file as the (1st argument)"
  exit 1
fi
output=$2
if [ "$output" = "" ] ; then
  echo "Specify the top-level directory for indices of two types (2d argument)"
  exit 1
fi
source_type=$3
if [ "$source_type" = "" ] ; then
  echo "Specify the type of the source, e.g., yahoo_answers (3d argument)"
  exit 1
fi

for type in standard fixed ; do
  INDEX_DIR="$output/$type/index"
  mkdir -p "$INDEX_DIR"
  if [ "$?" != "0" ] ; then
    echo "Cannot create directory '$INDEX_DIR'"
    exit 1
  fi
  RUN_DIR="$output/$type/runs"
  mkdir -p "$RUN_DIR"
  if [ "$?" != "0" ] ; then
    echo "Cannot create directory '$RUN_DIR'"
    exit 1
  fi

  flag=""
  if [ "$type" = "standard" ] ; then
    echo "Creating the index using the standard Lucene similarity"
  else
    echo "Creating the index using the fixed Lucene similarity"
    flag=" -bm25fixed "
  fi

  scripts/lucene_index.sh -i "$input" -o "$INDEX_DIR" $flag -r "$RUN_DIR/qrels.txt" -source_type "$source_type"

  if [ "$?" != "0" ] ; then
    exit 1
  fi

done


