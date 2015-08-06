#!/bin/bash

echo "Starting insightHotKey ..."
while (true)
do
    bin/ctl.sh start insightHotKey
	echo "OneLoop Finished!"
    sleep 1800
done
echo "Finishing insightHotKey ..."
