package org.digitalsmile.gpio.pin.structs;

import org.digitalsmile.gpio.NativeMemoryException;
import org.digitalsmile.gpio.core.NativeMemoryLayout;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

/**
 * Attribute of line to be configured
 *
 * @param id               attribute identifier with value
 * @param flags            line flags to override
 * @param values           a bitmap containing the values to which the lines will be set, with each bit number corresponding to the index
 * @param debouncePeriodUs the desired debounce period, in microseconds
 */
public record LineAttribute(int id, long flags, long values, int debouncePeriodUs) implements NativeMemoryLayout {
    public static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("id"),
            ValueLayout.JAVA_INT.withName("padding"),
            MemoryLayout.unionLayout(
                    ValueLayout.JAVA_LONG.withName("flags"),
                    ValueLayout.JAVA_LONG.withName("values"),
                    ValueLayout.JAVA_INT.withName("debounce_period_us")
            ).withName("union")
    );
    private static final VarHandle VH_ID = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("id"));
    private static final VarHandle VH_PADDING = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("padding"));
    private static final VarHandle VH_FLAGS = LAYOUT.select(MemoryLayout.PathElement.groupElement("union")).varHandle(MemoryLayout.PathElement.groupElement("flags"));
    private static final VarHandle VH_VALUES = LAYOUT.select(MemoryLayout.PathElement.groupElement("union")).varHandle(MemoryLayout.PathElement.groupElement("values"));
    private static final VarHandle VH_DEBOUNCE_PERIOD_US = LAYOUT.select(MemoryLayout.PathElement.groupElement("union")).varHandle(MemoryLayout.PathElement.groupElement("debounce_period_us"));

    /**
     * Creates an instance of Line Attribute by given attribute id and value.
     *
     * @param id    attribute id
     * @param value attribute value
     * @return line attribute instance
     */
    public static LineAttribute create(AttributeId id, long value) {
        long flags = 0, values = 0;
        int debouncePeriodUs = 0;
        switch (id) {
            case FLAGS -> flags = value;
            case OUTPUT_VALUES -> values = value;
            case ID_DEBOUNCE -> debouncePeriodUs = (int) value;
        }
        return new LineAttribute(id.getValue(), flags, values, debouncePeriodUs);
    }

    /**
     * Creates empty Line Attribute instance.
     *
     * @return empty Line Attribute instance
     */
    public static LineAttribute createEmpty() {
        return new LineAttribute(0, 0, 0, 0);
    }

    @Override
    public MemoryLayout getMemoryLayout() {
        return LAYOUT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public LineAttribute fromBytes(MemorySegment buffer) throws Throwable {
        var id = (int) VH_ID.get(buffer);
        long flags = 0, values = 0;
        int debouncePeriodUs = 0;
        var unionSize = LAYOUT.select(MemoryLayout.PathElement.groupElement("union")).byteSize();
        var unionBuffer = buffer.asSlice(LAYOUT.byteSize() - unionSize, unionSize);
        switch (AttributeId.getByValue(id)) {
            case FLAGS -> flags = (long) VH_FLAGS.get(unionBuffer);
            case OUTPUT_VALUES -> values = (long) VH_VALUES.get(unionBuffer);
            case ID_DEBOUNCE -> debouncePeriodUs = (int) VH_DEBOUNCE_PERIOD_US.get(unionBuffer);
            case null, default ->
                    throw new NativeMemoryException("id value is not one of " + Arrays.toString(AttributeId.values()));
        }

        return new LineAttribute(
                id,
                flags,
                values,
                debouncePeriodUs
        );
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        VH_ID.set(buffer, id);
        VH_PADDING.set(buffer, 0);

        var unionSize = LAYOUT.select(MemoryLayout.PathElement.groupElement("union")).byteSize();
        var unionBuffer = buffer.asSlice(LAYOUT.byteSize() - unionSize, unionSize);
        switch (AttributeId.getByValue(id)) {
            case FLAGS -> VH_FLAGS.set(unionBuffer, flags);
            case OUTPUT_VALUES -> VH_VALUES.set(unionBuffer, values);
            case ID_DEBOUNCE -> VH_DEBOUNCE_PERIOD_US.set(unionBuffer, debouncePeriodUs);
            case null, default ->
                    throw new NativeMemoryException("id value is not one of " + Arrays.toString(AttributeId.values()));
        }
    }

    @Override
    public String toString() {
        return "LineAttribute{" +
                "id=" + id +
                ", flags=" + flags +
                ", values=" + values +
                ", debouncePeriodUs=" + debouncePeriodUs +
                '}';
    }

    /**
     * Attribute id enum helper.
     */
    public enum AttributeId {
        /**
         * flags field is in use
         */
        FLAGS(1),
        /**
         * values field is in use
         */
        OUTPUT_VALUES(2),
        /**
         * debounce_period_us field is in use
         */
        ID_DEBOUNCE(3);

        private final int value;

        /**
         * Creates Attribute id enum by given integer value.
         *
         * @param value integer value of attribute id
         */
        AttributeId(int value) {
            this.value = value;
        }

        /**
         * Gets integer value of attribute id.
         *
         * @return integer value of attribute id
         */
        public int getValue() {
            return value;
        }

        /**
         * Gets attribute id enum by given integer value.
         *
         * @param id integer value of attribute id
         * @return attribute id
         */
        public static AttributeId getByValue(int id) {
            return Arrays.stream(values()).filter(i -> i.getValue() == id).findFirst().orElseThrow();
        }
    }
}
