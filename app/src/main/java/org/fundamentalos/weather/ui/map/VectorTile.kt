package org.fundamentalos.weather.ui.map

/** One layer of a vector tile: its features, in tile coordinates that run 0..[extent] across. */
class MvtLayer(val name: String, val extent: Int, val features: List<MvtFeature>)

/**
 * A vector-tile feature. [type] is 1 for points, 2 for lines and 3 for polygons; [parts] holds
 * each line, ring or run of points as x0, y0, x1, y1, …
 */
class MvtFeature(val type: Int, val tags: Map<String, Any>, val parts: List<FloatArray>)

/**
 * Reads the Mapbox Vector Tile format (protobuf, spec 2.1) with nothing but a byte cursor. The
 * format is small enough that a decoder is shorter than a protobuf dependency's manifest: a tile
 * is layers; a layer is a name, a key table, a value table and features; a feature is tag
 * indexes plus a run of zig-zag delta-encoded drawing commands.
 */
object MvtDecoder {
    /** Decodes the layers named in [wanted], or all of them when it is null. */
    fun decode(bytes: ByteArray, wanted: Set<String>? = null): Map<String, MvtLayer> {
        val layers = LinkedHashMap<String, MvtLayer>()
        val reader = Reader(bytes, 0, bytes.size)
        while (reader.hasMore()) {
            val tag = reader.varint().toInt()
            if (tag ushr 3 == 3 && tag and 7 == 2) {
                val length = reader.varint().toInt()
                val start = reader.pos
                decodeLayer(bytes, start, start + length, wanted)?.let { layers[it.name] = it }
                reader.pos = start + length
            } else {
                reader.skip(tag and 7)
            }
        }
        return layers
    }

    private fun decodeLayer(bytes: ByteArray, start: Int, end: Int, wanted: Set<String>?): MvtLayer? {
        var name = ""
        var extent = 4096
        val keys = ArrayList<String>()
        val values = ArrayList<Any>()
        val featureSpans = ArrayList<Int>()
        val reader = Reader(bytes, start, end)
        while (reader.hasMore()) {
            val tag = reader.varint().toInt()
            val wire = tag and 7
            when (tag ushr 3) {
                1 -> {
                    name = reader.string()
                    // A layer nobody asked for is left undecoded.
                    if (wanted != null && name !in wanted) return null
                }
                2 -> {
                    val length = reader.varint().toInt()
                    featureSpans.add(reader.pos)
                    featureSpans.add(reader.pos + length)
                    reader.pos += length
                }
                3 -> keys.add(reader.string())
                4 -> {
                    val length = reader.varint().toInt()
                    values.add(decodeValue(bytes, reader.pos, reader.pos + length))
                    reader.pos += length
                }
                5 -> extent = reader.varint().toInt()
                else -> reader.skip(wire)
            }
        }
        val features = ArrayList<MvtFeature>(featureSpans.size / 2)
        for (i in featureSpans.indices step 2) {
            decodeFeature(bytes, featureSpans[i], featureSpans[i + 1], keys, values)?.let(features::add)
        }
        return MvtLayer(name, extent, features)
    }

    private fun decodeValue(bytes: ByteArray, start: Int, end: Int): Any {
        val reader = Reader(bytes, start, end)
        while (reader.hasMore()) {
            val tag = reader.varint().toInt()
            when (tag ushr 3) {
                1 -> return reader.string()
                2 -> return Float.fromBits(reader.fixed32())
                3 -> return Double.fromBits(reader.fixed64())
                4, 5 -> return reader.varint()
                6 -> return zigzag(reader.varint())
                7 -> return reader.varint() != 0L
                else -> reader.skip(tag and 7)
            }
        }
        return ""
    }

    private fun decodeFeature(
        bytes: ByteArray, start: Int, end: Int, keys: List<String>, values: List<Any>,
    ): MvtFeature? {
        var type = 0
        var tagIndexes = LongArray(0)
        var commands = LongArray(0)
        val reader = Reader(bytes, start, end)
        while (reader.hasMore()) {
            val tag = reader.varint().toInt()
            val wire = tag and 7
            when (tag ushr 3) {
                2 -> tagIndexes = reader.packed(wire)
                3 -> type = reader.varint().toInt()
                4 -> commands = reader.packed(wire)
                else -> reader.skip(wire)
            }
        }
        if (type == 0) return null
        val tags = HashMap<String, Any>(tagIndexes.size / 2)
        for (i in 0 until tagIndexes.size - 1 step 2) {
            val key = keys.getOrNull(tagIndexes[i].toInt()) ?: continue
            val value = values.getOrNull(tagIndexes[i + 1].toInt()) ?: continue
            tags[key] = value
        }
        return MvtFeature(type, tags, decodeGeometry(commands, type))
    }

    /**
     * Turns the command stream into parts. A point feature is one part with every point in it;
     * lines and rings each start at a MoveTo. The cursor carries across commands.
     */
    private fun decodeGeometry(commands: LongArray, type: Int): List<FloatArray> {
        val parts = ArrayList<FloatArray>()
        var current = FloatList()
        var x = 0L
        var y = 0L
        var i = 0
        fun flush() {
            if (current.size >= 2) parts.add(current.toArray())
            current = FloatList()
        }
        while (i < commands.size) {
            val command = commands[i++]
            val id = (command and 7).toInt()
            val count = (command ushr 3).toInt()
            when (id) {
                1 -> repeat(count) {
                    if (i + 1 >= commands.size) return@repeat
                    x += zigzag(commands[i++])
                    y += zigzag(commands[i++])
                    if (type != 1) flush()
                    current.add(x.toFloat(), y.toFloat())
                }
                2 -> repeat(count) {
                    if (i + 1 >= commands.size) return@repeat
                    x += zigzag(commands[i++])
                    y += zigzag(commands[i++])
                    current.add(x.toFloat(), y.toFloat())
                }
                // ClosePath: the ring is closed when it is drawn.
                7 -> Unit
                else -> return parts
            }
        }
        flush()
        return parts
    }

    private fun zigzag(value: Long): Long = (value ushr 1) xor -(value and 1)

    private class FloatList {
        private var data = FloatArray(32)
        var size = 0
            private set

        fun add(x: Float, y: Float) {
            if (size + 2 > data.size) data = data.copyOf(data.size * 2)
            data[size++] = x
            data[size++] = y
        }

        fun toArray(): FloatArray = data.copyOf(size)
    }

    private class Reader(private val bytes: ByteArray, var pos: Int, private val end: Int) {
        fun hasMore(): Boolean = pos < end

        fun varint(): Long {
            var result = 0L
            var shift = 0
            while (pos < end) {
                val byte = bytes[pos++].toInt()
                result = result or ((byte and 0x7F).toLong() shl shift)
                if (byte and 0x80 == 0) return result
                shift += 7
                if (shift > 63) break
            }
            return result
        }

        fun fixed32(): Int {
            var result = 0
            for (i in 0 until 4) result = result or ((bytes[pos++].toInt() and 0xFF) shl (8 * i))
            return result
        }

        fun fixed64(): Long {
            var result = 0L
            for (i in 0 until 8) result = result or ((bytes[pos++].toLong() and 0xFF) shl (8 * i))
            return result
        }

        fun string(): String {
            val length = varint().toInt()
            val value = String(bytes, pos, length, Charsets.UTF_8)
            pos += length
            return value
        }

        /** A packed repeated varint field, or a single one if the writer did not pack it. */
        fun packed(wire: Int): LongArray {
            if (wire != 2) return longArrayOf(varint())
            val length = varint().toInt()
            val stop = pos + length
            val out = ArrayList<Long>(length)
            while (pos < stop) out.add(varint())
            return out.toLongArray()
        }

        fun skip(wire: Int) {
            when (wire) {
                0 -> varint()
                1 -> pos += 8
                2 -> pos += varint().toInt()
                5 -> pos += 4
                else -> pos = end
            }
        }
    }
}
