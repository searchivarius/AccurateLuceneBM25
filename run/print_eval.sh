#!/bin/bash
trec_eval_path="$1"
N="$2"
qrels="$3"
pref="$4"
metr="$5"

if [ "$trec_eval_path" = "" ] ; then
  echo "Specify the first arg: a path to trec_eval"
  exit 1
fi
if [ ! -d "$trec_eval_path" ] ; then
  echo "Not a directory: $trec_eval_path"
  echo "Specify the first arg: a path to trec_eval"
  exit 1
fi
if [ "$N" = "" ] ; then
  echo "Specify the second arg: a number of iterations"
  exit 1
fi
if [ "$qrels" = "" ] ; then
  echo "Specify the third arg: QREL file"
  exit 1
fi
if [ ! -f "$qrels" ] ; then
  echo "Not a file: $qrels"
  echo "Specify the first arg: a path to trec_eval"
  exit 1
fi
if [ "$pref" = "" ] ; then
  echo "Specify the fourth arg: a prefix of the output file"
  exit 1
fi
if [ "$metr" = "" ] ; then
  metr="num_rel"
fi
echo "Using metric: $metr"
for ((i=1; i <= "$N"; ++i)) do 
  out_file="$pref.$i"
  if [ ! -f "$out_file" ] ; then
    echo "Not a file: $out_file"
    echo "Specify the proper args: a prefix of the output file and the number of iterations"
    exit 1
  fi
  echo "== Iter: $i ==="
  "${trec_eval_path}/trec_eval" "$qrels" "$out_file"  | fgrep "$metr"
  echo "==============="
done
