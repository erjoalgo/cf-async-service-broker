#!/bin/bash -x

HOST=https://erjoalgo-broker.run.aws-usw02-pr.ice.predix.io
HOST=localhost:3000
HOST=http://erjoalgo.com:443
curl ${HOST}/v2/catalog || exit ${LINENO}

UUID=$(uuid) || exit ${LINENO}
curl -i -XPUT ${HOST}/v2/service_instances/${UUID}?accepts_incomplete=true  \
     -d '{"secs":3}' || exit ${LINENO}

exit
for _ in $(seq 9); do
    curl ${HOST}/v2/service_instances/${UUID}/last_operation || exit ${LINENO}
    curl -XPUT ${HOST}/v2/service_instances/${UUID}/service_bindings/$(uuid) || exit ${LINENO}
    sleep 1
done

sleep 1


curl -XDELETE ${HOST}/v2/service_instances/${UUID} || exit ${LINENO}

curl ${HOST}/v2/service_instances/${UUID}/last_operation || exit ${LINENO}
