#!/bin/bash

echo "Starting ..."
bin/ctl.sh start firstPageRun
echo "OneLoop Finished!"
while (true)
do
    sleep 1800
    bin/ctl.sh start firstPageRun
	echo "OneLoop Finished!"
done
echo "Finishing ..."
