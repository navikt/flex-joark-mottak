package no.nav.helse.flex.infrastructure.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import org.apache.kafka.common.serialization.Deserializer
import java.lang.Exception
import java.lang.IllegalStateException

class JfrKafkaDeserializer<T>(private val clazz: Class<T>) : Deserializer<T> {
    override fun configure(map: Map<String?, *>?, b: Boolean) {}
    override fun deserialize(s: String, bytes: ByteArray): T {
        return try {
            val readValues = ObjectMapper().readValue(bytes, String::class.java)
            Gson().fromJson(readValues, clazz)
        } catch (e: Exception) {
            e.printStackTrace()
            throw IllegalStateException("Failed while deserializing message", e)
        }
    }

    override fun close() {}
}
