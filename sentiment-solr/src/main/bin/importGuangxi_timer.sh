#!/bin/bash

echo "Starting ..."
while (true)
do
    bin/ctl.sh start importSolrToGuangxi
    sleep 3600
done
echo "Finishing ..."
