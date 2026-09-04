package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.ResolutionSyntax;

/**
 * Con cuantos puntos por unidad de longitud se imprime.
 *
 * <p>Son dos numeros, no uno, porque las impresoras no tienen por que ser cuadradas: la resolucion
 * a lo ancho del papel --<em>cross feed</em>, la que depende del cabezal-- y la del sentido en que
 * avanza el papel --<em>feed</em>, la que depende del motor-- se fijan por separado. Todo el
 * manejo de unidades esta en {@link javax.print.attribute.ResolutionSyntax ResolutionSyntax}, que
 * guarda en puntos por cien pulgadas para que DPI y DPCM entren exactos en un entero.
 *
 * <p>Es la peticion precisa; la vaga es {@link PrintQuality}.
 */
public final class PrinterResolution extends ResolutionSyntax
    implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = 13090306561090558L;

    /** {@code units} es {@link ResolutionSyntax#DPI} o {@link ResolutionSyntax#DPCM}. */
    public PrinterResolution(int crossFeedResolution, int feedResolution, int units) {
        super(crossFeedResolution, feedResolution, units);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof PrinterResolution;
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterResolution.class;
    }

    public final String getName() {
        return "printer-resolution";
    }
}
