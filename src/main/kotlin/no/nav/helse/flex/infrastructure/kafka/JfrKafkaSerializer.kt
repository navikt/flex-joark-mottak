package no.nav.helse.flex.infrastructure.kafka

import com.google.gson.Gson
import no.nav.helse.flex.objectMapper
import org.apache.kafka.common.serialization.Serializer
import java.lang.Exception
import java.lang.IllegalStateException

class JfrKafkaSerializer<T> : Serializer<T> {
    override fun configure(map: Map<String?, *>?, b: Boolean) {}
    override fun serialize(s: String, record: T): ByteArray {
        return try {
            objectMapper.writeValueAsBytes(Gson().toJson(record))
        } catch (e: Exception) {
            throw IllegalStateException("Failed while serializing message", e)
        }
    }

    override fun close() {}
}
