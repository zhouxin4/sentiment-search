#!/bin/bash

echo "Starting originUpdate ..."
while (true)
do
    bin/ctl.sh start originUpdate
	echo "OneLoop Finished!"
    sleep 1800
done
echo "Finishing originUpdate ..."
