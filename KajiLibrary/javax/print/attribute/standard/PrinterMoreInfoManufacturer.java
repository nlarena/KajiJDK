package javax.print.attribute.standard;

import java.net.URI;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.URISyntax;

/**
 * Una pagina web del fabricante sobre el <em>modelo</em>, no sobre esta maquina.
 *
 * <p>Es donde estan los drivers y los repuestos; lo especifico de esta impresora esta en {@link
 * PrinterMoreInfo}.
 */
public final class PrinterMoreInfoManufacturer extends URISyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = 3323271346485076608L;

    public PrinterMoreInfoManufacturer(URI uri) {
        super(uri);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof PrinterMoreInfoManufacturer;
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterMoreInfoManufacturer.class;
    }

    public final String getName() {
        return "printer-more-info-manufacturer";
    }
}
