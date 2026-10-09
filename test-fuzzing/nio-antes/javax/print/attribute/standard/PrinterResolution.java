package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.ResolutionSyntax;

/**
 * With how many dots per unit of length it prints.
 *
 * <p>They are two numbers, not one, because printers do not have to be square: the resolution
 * across the paper --<em>cross feed</em>, which depends on the head-- and along the direction the
 * paper advances --<em>feed</em>, which depends on the motor-- are set separately. All the handling
 * of units is in {@link javax.print.attribute.ResolutionSyntax ResolutionSyntax}, which keeps dots
 * per hundred inches so that DPI and DPCM fit exactly in an integer.
 *
 * <p>It is the precise request; the vague one is {@link PrintQuality}.
 */
public final class PrinterResolution extends ResolutionSyntax
    implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = 13090306561090558L;

    /** {@code units} is {@link ResolutionSyntax#DPI} or {@link ResolutionSyntax#DPCM}. */
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
