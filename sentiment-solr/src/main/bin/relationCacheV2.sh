#!/bin/bash

echo "Starting relationCacheV2..."
while (true)
do
    bin/ctl.sh start relationCacheV2
	echo "OneLoop Finished!"
    sleep 1800
done
echo "Finishing ..."
