#!/bin/bash
JIRA_PID=$1
maxWaitTimeSecs=100
index=1
result=0
echo "### Checking Jira shutdown"
kill $JIRA_PID
result=`if kill -0 $JIRA_PID 2>/dev/null; then echo 0; else  echo 1; fi`
while [ $result -eq 0 ]
do
  result=`if kill -0 $JIRA_PID 2>/dev/null; then echo 0; else  echo 1; fi`
  if [ $index -ge $maxWaitTimeSecs ]
  then
    echo "!!! JIRA STILL RUNNING AFTER $maxWaitTimeSecs SECONDS"
    exit 1
  fi
  sleep 1
  echo -"### WAITING FOR JIRA KILL since $index Seconds"
  index=$((index+1))
done
echo "### JIRA IS DOWN"