#!/usr/bin/env python
import sys

def Usage(err):
  if not err is None:
    print err
  print "Usage: <input prel file> <output qrel file> "
  sys.exit(1)

if len(sys.argv) != 3 : Usage(None)

inf = open(sys.argv[1], 'r')
outf = open(sys.argv[2], 'w')

for line in inf:
  line = line.strip()
  fields = line.split()
  outf.write("%s 0 %s %s\n" % (fields[0], fields[1], fields[2]));
