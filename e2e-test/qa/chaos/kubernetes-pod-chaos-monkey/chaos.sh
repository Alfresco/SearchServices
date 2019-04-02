#!/bin/bash
# Randomly delete pods in a Kubernetes namespace.
set -ex

while true
do
	  kubectl \
	    --namespace "${NAMESPACE}" \
	    -o 'jsonpath={.items[*].metadata.name}' \
	    get pods | \
	      tr " " "\n" | \
	      shuf | \
	      head -n ${KILL_NR} |
	      xargs -t --no-run-if-empty \
	        kubectl --namespace "${NAMESPACE}" delete pod
  sleep "${DELAY_SEC}"
done
