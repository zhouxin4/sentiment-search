#!/bin/bash

echo "Starting relationCache..."
while (true)
do
    bin/ctl.sh start relationCache
	echo "OneLoop Finished!"
    sleep 1800
done
echo "Finishing ..."
