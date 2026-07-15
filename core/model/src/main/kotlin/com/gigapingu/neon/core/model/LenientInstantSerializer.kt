package com.gigapingu.neon.core.model

import java.time.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * ISO-8601 → [Instant], lenient: unparseable input decodes to null instead of
 * throwing (matches the Flutter models' DateTime.tryParse behaviour).
 * Apply to `Instant?` properties.
 */
@OptIn(ExperimentalSerializationApi::class)
object LenientInstantSerializer : KSerializer<Instant?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("neon.LenientInstant", PrimitiveKind.STRING).nullable

    override fun deserialize(decoder: Decoder): Instant? =
        runCatching { Instant.parse(decoder.decodeString()) }.getOrNull()

    override fun serialize(encoder: Encoder, value: Instant?) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value.toString())
    }
}
