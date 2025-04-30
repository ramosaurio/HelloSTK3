package com.brownfields.github.hellostk3;

import javacard.framework.Util;

/**
 * JsonUtil class
 *
 * Provides utility methods for building JSON structures in a byte array format,
 * optimized for JavaCard environments with limited resources.
 */
public class JsonUtil {

    // JSON formatting characters
    private static final byte OPEN_BRACE = '{';
    private static final byte CLOSE_BRACE = '}';
    private static final byte DOUBLE_QUOTE = '"';
    private static final byte COLON = ':';
    private static final byte COMMA = ',';

    /**
     * Builds a JSON object into the provided output buffer.
     *
     * @param keys Array of JSON key byte arrays
     * @param values Array of JSON value byte arrays
     * @param valueOffsets Offsets inside each value array
     * @param valueLengths Lengths of each value
     * @param outBuffer Buffer where the JSON object will be written
     * @param outOffset Starting offset in the output buffer
     * @return New offset after writing the JSON
     */
    public static short buildJson(byte[] flatKeys, short[] keyOffsets, short[] keyLengths,
                                  byte[] flatValues, short[] valueOffsets, short[] valueLengths,
                                  byte[] outBuffer, short outOffset) {
        outOffset = startObject(outBuffer, outOffset);

        for (short i = 0; i < keyOffsets.length; i++) {
            outOffset = addKeyValue(
                    outBuffer, outOffset,
                    flatKeys, keyOffsets[i], keyLengths[i],
                    flatValues, valueOffsets[i], valueLengths[i]
            );

            if (i != (short)(keyOffsets.length - 1)) {
                outOffset = addComma(outBuffer, outOffset);
            }
        }

        outOffset = endObject(outBuffer, outOffset);
        return outOffset;
    }

    /**
     * Starts a new JSON object by adding '{' to the buffer.
     *
     * @param buffer Output buffer
     * @param offset Current offset
     * @return New offset after writing
     */
    private static short startObject(byte[] buffer, short offset) {
        buffer[offset++] = OPEN_BRACE;
        return offset;
    }

    /**
     * Adds a key-value pair to the JSON object.
     *
     * @param buffer Output buffer
     * @param offset Current offset
     * @param key Key as a byte array
     * @param value Value as a byte array
     * @param valueOffset Offset within the value array
     * @param valueLength Length of the value to copy
     * @return New offset after writing
     */
    private static short addKeyValue(byte[] buffer, short offset,
                                     byte[] flatKeys, short keyOffset, short keyLength,
                                     byte[] flatValues, short valueOffset, short valueLength) {
        buffer[offset++] = DOUBLE_QUOTE;
        offset = Util.arrayCopyNonAtomic(flatKeys, keyOffset, buffer, offset, keyLength);
        buffer[offset++] = DOUBLE_QUOTE;
        buffer[offset++] = COLON;
        buffer[offset++] = DOUBLE_QUOTE;
        offset = Util.arrayCopyNonAtomic(flatValues, valueOffset, buffer, offset, valueLength);
        buffer[offset++] = DOUBLE_QUOTE;
        return offset;
    }

    /**
     * Adds a comma separator between JSON entries.
     *
     * @param buffer Output buffer
     * @param offset Current offset
     * @return New offset after writing
     */
    private static short addComma(byte[] buffer, short offset) {
        buffer[offset++] = COMMA;
        return offset;
    }

    /**
     * Ends a JSON object by adding '}' to the buffer.
     *
     * @param buffer Output buffer
     * @param offset Current offset
     * @return New offset after writing
     */
    private static short endObject(byte[] buffer, short offset) {
        buffer[offset++] = CLOSE_BRACE;
        return offset;
    }
}
