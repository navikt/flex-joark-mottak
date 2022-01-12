#!/bin/bash
pod=$1
configFile=~/.config/aiven.conf

echo $configFile

context=$(kubectx config current-context)
credstore=~/.config/kafka/$context/creds

[ -d $credstore ] || mkdir -p $credstore
[ -d ~/.config ] || mkdir ~/.config
rm -f $configFile

kubectl cp $pod:$(kubectl exec $pod -- sh -c 'readlink -f $KAFKA_TRUSTSTORE_PATH') $credstore/kafka.client.truststore.jks
kubectl cp $pod:$(kubectl exec $pod -- sh -c 'readlink -f $KAFKA_KEYSTORE_PATH') $credstore/kafka.client.keystore.jks
truststorePassword=$(kubectl exec $pod -- sh -c 'echo $KAFKA_CREDSTORE_PASSWORD')
echo "bootstrap.servers="$(kubectl exec -n isa $pod -- sh -c 'echo $KAFKA_BROKERS') >> $configFile
echo "security.protocol=ssl" >> $configFile
echo "ssl.truststore.location=$credstore/kafka.client.truststore.jks" >> $configFile
echo "ssl.keystore.location=$credstore/kafka.client.keystore.jks" >> $configFile
echo "ssl.truststore.password=$truststorePassword" >> $configFile
echo "ssl.key.password=$truststorePassword" >> $configFile
echo "ssl.keystore.password=$truststorePassword" >> $configFile
echo "ssl.endpoint.identification.algorithm=" >> $configFile
