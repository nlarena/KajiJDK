package org.xml.sax;

/**
 * KajiLibrary's org.xml.sax.SAXParseException -- un `SAXException` que ademas sabe **donde** paso.
 *
 * <p>Lo que agrega sobre `SAXException` son las cuatro coordenadas de un {@link Locator}
 * --identificador publico, identificador de sistema, linea y columna-- pero **copiadas**, no la
 * referencia al localizador. Esa copia es la razon de ser de la clase: el `Locator` que da el parser
 * esta vivo y sigue moviendose, asi que guardarlo dentro de una excepcion que se va a mirar despues
 * daria la posicion equivocada. Aca se congelan en el constructor y ya no cambian.
 *
 * <p>Es el tipo que reciben los tres metodos de {@link ErrorHandler}, y por eso llega tanto para
 * errores fatales como para avisos: que la instancia sea una excepcion no significa que se haya
 * lanzado. Un `warning` la construye, se la pasa al manejador y sigue parseando.
 *
 * <p><strong>Un detalle del `toString` que parece un error de tipeo y no lo es.</strong> El JDK
 * escribe el nombre de la clase y **pega** `publicId: ...` sin separador, mientras que los otros tres
 * campos si van precedidos por `"; "`. Sale
 * `org.xml.sax.SAXParseExceptionpublicId: p; systemId: s; lineNumber: 3; columnNumber: 7; mensaje`.
 * Se reproduce tal cual porque el formato es observable y hay salidas de herramientas que ya lo
 * tienen escrito; "arreglarlo" seria cambiar el comportamiento, no corregirlo.
 */
public class SAXParseException extends SAXException {

    static final long serialVersionUID = -5651165872476709336L;

    private String publicId;
    private String systemId;
    private int lineNumber;
    private int columnNumber;

    /**
     * @param locator si es `null`, las cuatro coordenadas quedan en "desconocido" en vez de fallar:
     *        un parser puede reportar un problema antes de tener una posicion.
     */
    public SAXParseException(String message, Locator locator) {
        super(message);
        if (locator != null) {
            init(locator.getPublicId(), locator.getSystemId(),
                    locator.getLineNumber(), locator.getColumnNumber());
        } else {
            init(null, null, -1, -1);
        }
    }

    public SAXParseException(String message, Locator locator, Exception e) {
        super(message, e);
        if (locator != null) {
            init(locator.getPublicId(), locator.getSystemId(),
                    locator.getLineNumber(), locator.getColumnNumber());
        } else {
            init(null, null, -1, -1);
        }
    }

    /** La forma explicita, para quien no tiene un `Locator` a mano. */
    public SAXParseException(String message, String publicId, String systemId,
            int lineNumber, int columnNumber) {
        super(message);
        init(publicId, systemId, lineNumber, columnNumber);
    }

    public SAXParseException(String message, String publicId, String systemId,
            int lineNumber, int columnNumber, Exception e) {
        super(message, e);
        init(publicId, systemId, lineNumber, columnNumber);
    }

    private void init(String publicId, String systemId, int lineNumber, int columnNumber) {
        this.publicId = publicId;
        this.systemId = systemId;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public String getPublicId() {
        return this.publicId;
    }

    public String getSystemId() {
        return this.systemId;
    }

    /** Base 1, y -1 si no se sabe. Apunta al **final** del constructo, no a su principio. */
    public int getLineNumber() {
        return this.lineNumber;
    }

    /** Base 1, y -1 si no se sabe. */
    public int getColumnNumber() {
        return this.columnNumber;
    }

    /**
     * Ver el comentario de la cabecera: el pegote entre el nombre de la clase y `publicId` es del
     * JDK y se copia a proposito. Los campos en -1 o `null` no se escriben, asi que una excepcion sin
     * posicion sale como `org.xml.sax.SAXParseException; mensaje`.
     */
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getName());
        String message = getLocalizedMessage();
        if (this.publicId != null) {
            buf.append("publicId: ").append(this.publicId);
        }
        if (this.systemId != null) {
            buf.append("; systemId: ").append(this.systemId);
        }
        if (this.lineNumber != -1) {
            buf.append("; lineNumber: ").append(this.lineNumber);
        }
        if (this.columnNumber != -1) {
            buf.append("; columnNumber: ").append(this.columnNumber);
        }
        if (message != null) {
            buf.append("; ").append(message);
        }
        return buf.toString();
    }
}
