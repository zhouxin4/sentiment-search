#!/bin/bash

echo "Starting negativeRecordsRun ..."
while (true)
do
    bin/ctl.sh start negativeRecordsRun
	echo "OneLoop Finished!"
    sleep 1800
done
echo "Finishing negativeRecordsRun ..."
