package no.nav.helse.flex.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class JfrKafkaDeserializer<T> implements Deserializer<T> {
    private final Class<T> clazz;

    public JfrKafkaDeserializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }

    @Override
    public T deserialize(String s, byte[] bytes) {
        try {
            String readValues = new ObjectMapper().readValue(bytes, String.class);
            return new Gson().fromJson(readValues, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Failed while deserializing message", e);
        }
    }

    @Override
    public void close() {

    }
}
