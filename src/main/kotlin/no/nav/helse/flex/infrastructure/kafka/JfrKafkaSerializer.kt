package no.nav.helse.flex.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;


public class JfrKafkaSerializer<T> implements Serializer<T> {
    @Override
    public void configure(final Map<String, ?> map, final boolean b) {}

    @Override
    public byte[] serialize(String s, T record) {
        try {
            return new ObjectMapper().writeValueAsBytes(new Gson().toJson(record));
        } catch (Exception e) {
            throw new IllegalStateException("Failed while serializing message", e);
        }
    }

    @Override
    public void close() {}
}
