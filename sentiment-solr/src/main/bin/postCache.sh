#!/bin/bash

echo "Starting postCache..."
while (true)
do
    bin/ctl.sh start postCache
	echo "OneLoop Finished!"
    sleep 3600
done
echo "Finishing ..."