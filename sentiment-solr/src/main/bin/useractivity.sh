#!/bin/bash

echo "Starting userActivity ..."
while (true)
do
    bin/ctl.sh start userActivity
	echo "OneLoop Finished!"
    sleep 1800
done
echo "Finishing userActivity ..."
