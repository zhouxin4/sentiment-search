#!/bin/bash

echo "Starting relationCacheV2..."
bin/ctl.sh start relationCacheV2
echo "OneLoop Finished!"
while (true)
do
    sleep 1800
    bin/ctl.sh start relationCacheV2
	echo "OneLoop Finished!"
done
echo "Finishing ..."
