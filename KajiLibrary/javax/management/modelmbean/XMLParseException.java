package javax.management.modelmbean;

/**
 * KajiLibrary's javax.management.modelmbean.XMLParseException -- no se pudo leer un descriptor
 * escrito en XML.
 *
 * <p>La lanza el constructor de {@link DescriptorSupport} que recibe una cadena XML. Es el unico
 * lugar del paquete donde hay XML, y esta ahi por una idea de 1999 que no prospero: guardar los
 * descriptores de un MBean en un archivo.
 *
 * <p>El constructor con {@link Exception} envuelve lo que fallo de verdad. El mensaje que arma no es
 * el del envoltorio ni el de la causa, sino los dos concatenados, que es lo que hace el JDK.
 */
public class XMLParseException extends Exception {

    private static final long serialVersionUID = 3176664577895105181L;

    /** Sin detalle. */
    public XMLParseException() {
        super("XML Parse Exception.");
    }

    /** Con un mensaje. */
    public XMLParseException(String s) {
        super("XML Parse Exception. " + s);
    }

    /** Con la causa y un mensaje. */
    public XMLParseException(Exception e, String s) {
        super("XML Parse Exception. " + s + ((e == null) ? "" : e.toString()));
    }
}
