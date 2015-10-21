#!/bin/bash

SRC=./dics/words-sentiment.dic
for i in `cat $1`;
do
        /usr/bin/rsync -avz --progress $SRC $i:~/solr-cloud/solr-4.8.0/solr/sentiment/mmseg4j_dic/words-sentiment.dic
done;
echo "$(date "+%Y-%m-%dT%H:%M:%SZ"); $SRC was rsynced" | tee -a rsync.log
curl "http://192.168.32.11:8983/solr/admin/collections?action=RELOAD&name=sentiment"
~                                                                                           