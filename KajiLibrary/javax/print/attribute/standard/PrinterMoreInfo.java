package javax.print.attribute.standard;

import java.net.URI;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.URISyntax;

/**
 * Una pagina web con mas informacion sobre <em>esta</em> impresora en particular: su estado, su
 * cola, sus consumibles.
 */
public final class PrinterMoreInfo extends URISyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = 4555850007675338574L;

    public PrinterMoreInfo(URI uri) {
        super(uri);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof PrinterMoreInfo;
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterMoreInfo.class;
    }

    public final String getName() {
        return "printer-more-info";
    }
}
