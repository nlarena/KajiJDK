package javax.xml.stream;

/**
 * Una ubicacion inmutable dentro del documento.
 *
 * <p>De paquete: {@link Location} es la interfaz publica y esto es nada mas que cinco campos.
 *
 * <p>Las tres coordenadas numericas valen -1 cuando no se conocen, que es lo que manda la interfaz.
 * Aca siempre se conocen porque el parser las lleva mientras consume caracteres; la constante
 * {@link #NINGUNA} es la que se usa para los eventos que fabrica {@link XMLEventFactory} a mano,
 * que no salieron de ningun documento.
 */
final class KajiLocation implements Location {

    /** La que se le pone a un evento que no vino de un documento. */
    static final KajiLocation NONE = new KajiLocation(-1, -1, -1, null, null);

    private final int line;
    private final int column;
    private final int offset;
    private final String publicId;
    private final String systemId;

    KajiLocation(int line, int column, int offset, String publicId, String systemId) {
        this.line = line;
        this.column = column;
        this.offset = offset;
        this.publicId = publicId;
        this.systemId = systemId;
    }

    public int getLineNumber() {
        return line;
    }

    public int getColumnNumber() {
        return column;
    }

    public int getCharacterOffset() {
        return offset;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getSystemId() {
        return systemId;
    }

    public String toString() {
        return "Line number = " + line
                + "\nColumn number = " + column
                + "\nSystem Id = " + systemId
                + "\nPublic Id = " + publicId
                + "\nLocation Uri= " + systemId
                + "\nCharacterOffset = " + offset
                + "\n";
    }
}
