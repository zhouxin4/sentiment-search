#!/bin/bash

echo "Starting relationCache..."
bin/ctl.sh start relationCache
echo "OneLoop Finished!"
while (true)
do
    sleep 3600
    bin/ctl.sh start relationCache
	echo "OneLoop Finished!"
done
echo "Finishing ..."
