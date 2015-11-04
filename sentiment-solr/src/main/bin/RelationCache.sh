#!/bin/bash

echo "Starting relationCache..."
bin/ctl.sh start relationCache
echo "OneLoop Finished!"
while (true)
do
    sleep 1800
    bin/ctl.sh start relationCache
	echo "OneLoop Finished!"
done
echo "Finishing ..."
