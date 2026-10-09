package java.awt;

/**
 * Base of the {@code java.awt} enumerations that predate {@code enum}.
 *
 * <p>It is not public --it is not API-- but it is needed: {@code BufferCapabilities.FlipContents}
 * inherits from it and does not compile without it. A value is an index plus a table of names, and
 * from that come {@code hashCode()} --the index-- and {@code toString()} --the name--.
 */
abstract class AttributeValue {

    private int value;

    private String[] names;

    protected AttributeValue(int value, String[] names) {
        this.value = value;
        this.names = names;
    }

    public int hashCode() {
        return value;
    }

    public String toString() {
        return names[value];
    }
}
