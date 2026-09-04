package javax.print.attribute.standard;

import java.net.URI;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.URISyntax;

/**
 * La URI que identifica a la impresora en el protocolo, normalmente {@code ipp:}.
 *
 * <p>Es el identificador con el que se le habla, no el nombre para mostrar --ese es {@link
 * PrinterName}.
 */
public final class PrinterURI extends URISyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = 7923912792485606497L;

    public PrinterURI(URI uri) {
        super(uri);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof PrinterURI;
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterURI.class;
    }

    public final String getName() {
        return "printer-uri";
    }
}
