#!/bin/bash


#	for i in `cat $1`;
#	do 
#		rsync words-sentiment.dic $i:~/solr-cloud/solr-4.8.0/solr/sentiment/mmseg4j_dic/words-sentiment.dic
#	done;
#	curl http://192.168.31.11:8983/solr/admin/collections?action=RELOAD&name=sentiment
#	sleep 5;

src=./dics/words-sentiment.dic
inotifywait -mrq --timefmt '%d/%m/%y %H:%M' --format '%T %w%f' \
    -e modify,delete,create,attrib ${src} \
    | while read x
do
		for i in `cat $1`
		do
            /usr/bin/rsync -avz --progress $src $i:~/solr-cloud/solr-4.8.0/solr/sentiment/mmseg4j_dic/words-sentiment.dic
        done;  
        echo "$(date "+%Y-%m-%dT%H:%M:%SZ"); $x was rsynced" | tee -a /tmp/rsync.log
done
