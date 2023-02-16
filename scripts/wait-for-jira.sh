#!/bin/bash

maxWaitTimeSecs=600
index=1
result=0
echo "### Checking Jira is running"
while [ $result -eq 0 ]
do
  response=$(curl --max-time 2 http://localhost:2990/status 2> /dev/null)
  if [ "$response" == '{"state":"RUNNING"}' ]
  then
    result=1
  fi
  if [ $index -ge $maxWaitTimeSecs ]
  then
    echo "!!! JIRA NOT RUNNING AFTER $maxWaitTimeSecs SECONDS"
    exit 1
  fi
  sleep 1
  echo -"### WAITING FOR JIRA since $index Seconds"
  index=$((index+1))
done
echo "### JIRA IS UP"
sleep 5
