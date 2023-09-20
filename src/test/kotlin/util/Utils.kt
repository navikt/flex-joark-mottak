package util

import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.avro.reflect.ReflectData
import kotlin.reflect.full.declaredMemberProperties

fun fromKClassToGenericRecord(obj: Any): GenericRecord {
    val schema = ReflectData.get().getSchema(obj.javaClass)
    val record = GenericData.Record(schema)
    obj.javaClass.kotlin.declaredMemberProperties.forEach { prop ->
        record.put(prop.name, prop.get(obj))
    }
    return record
}
