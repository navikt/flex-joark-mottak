#!/bin/bash
pod=$1
configFile=~/.config/aiven.conf

echo config: $configFile

context=$(kubectl config current-context)
credstore=~/.config/kafka/$context/creds

echo context: $context

[ -d $credstore ] || mkdir -p $credstore
[ -d ~/.config ] || mkdir ~/.config
rm -f $configFile

kubectl exec $pod -c flex-joark-mottak -- sh -c 'cat $KAFKA_TRUSTSTORE_PATH' > $credstore/kafka.client.truststore.jks
kubectl exec $pod -c flex-joark-mottak -- sh -c 'cat $KAFKA_KEYSTORE_PATH' > $credstore/kafka.client.keystore.jks
truststorePassword=$(kubectl exec $pod -c flex-joark-mottak -- sh -c 'echo $KAFKA_CREDSTORE_PASSWORD')
echo "bootstrap.servers="$(kubectl exec $pod -c flex-joark-mottak -- sh -c 'echo $KAFKA_BROKERS') >> $configFile
echo "security.protocol=SSL" >> $configFile
echo "ssl.truststore.location=$credstore/kafka.client.truststore.jks" >> $configFile
echo "ssl.keystore.location=$credstore/kafka.client.keystore.jks" >> $configFile
echo "ssl.truststore.password=$truststorePassword" >> $configFile
echo "ssl.key.password=$truststorePassword" >> $configFile
echo "ssl.keystore.password=$truststorePassword" >> $configFile
echo "ssl.endpoint.identification.algorithm=" >> $configFile
