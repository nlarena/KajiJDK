package java.awt;

/**
 * Base de las enumeraciones de {@code java.awt} anteriores a {@code enum}.
 *
 * <p>No es publica --no es API-- pero hace falta: {@code BufferCapabilities.FlipContents} hereda de
 * ella y sin ella no compila. Un valor es un indice mas una tabla de nombres, y de ahi salen el
 * {@code hashCode()} --el indice-- y el {@code toString()} --el nombre--.
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
