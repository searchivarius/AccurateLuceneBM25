#!/usr/bin/env python
import sys;

def Usage(err):
  if not err is None:
    print err
  print "Usage: <directory with uncompressed UQV100 files> <output query file> <output qrel file> "
  sys.exit(1)

if len(sys.argv) != 4 : Usage(None)

def read_qrels(fn):
  res = dict()
  f = open(fn, 'r')
  for line in  f:
    line = line.strip()
    if line == '' : continue
    (topicId, tmp, docId, rel)   = line.split()
    if not topicId in res:
      res[topicId] = []
    res[topicId].append('0 ' + docId + ' ' + rel)
  return res

uqv100dir = sys.argv[1]
qrels = read_qrels(uqv100dir + '/uqv100qrelsmedianlabels.txt')

fq = open(uqv100dir + '/uqv100queryvariationsandestimates.tsv')

topicIdField=-1
normQueryField=-1

topicQueries = dict()
hasQuery = set()

fqOut = open(sys.argv[2],'w')
fqQrel = open(sys.argv[3],'w')

f=True
for line in fq:
  line = line.strip()
  if line == '' : continue
  arr=line.split('\t')
  if f :
    for i in range(0, len(arr)):
      if arr[i] == 'TRECTopicNumber':
        topicIdField=i
      if arr[i] == 'NormQuery':
        normQueryField=i
    f=False
  else:
    if topicIdField == -1:
      raise Exception("Wrong format can't find field TRECTopicNumber")
    if normQueryField == -1:
      raise Exception("Wrong format can't find field NormQuery")
    tid=arr[topicIdField]
    q=arr[normQueryField]
    if not q in hasQuery:
      hasQuery.add(q)
      if not tid in topicQueries:
        topicQueries[tid]=0
      sub_tid=topicQueries[tid]+1
      topicQueries[tid]=sub_tid
      qid=str(10000*int(tid)+sub_tid)
      fqOut.write(qid +':1:'+q +'\n')
      for r in qrels[tid]:
        fqQrel.write(qid+' '+r+'\n')

