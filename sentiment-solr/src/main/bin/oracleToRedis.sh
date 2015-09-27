#!/bin/bash

echo "Starting oracleToRedis ..."
while (true)
do
    bin/ctl.sh start oracleToRedis
	echo "OneLoop Finished!"
    sleep 3600
done
echo "Finishing oracleToRedis ..."
