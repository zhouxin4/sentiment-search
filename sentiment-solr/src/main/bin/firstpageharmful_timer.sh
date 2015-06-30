#!/bin/bash

echo "Starting ..."
bin/ctl.sh start firstPageHarmfulRun
echo "OneLoop Finished!"
while (true)
do
    sleep 30m
    bin/ctl.sh start firstPageHarmfulRun
	echo "OneLoop Finished!"
done
echo "Finishing ..."
