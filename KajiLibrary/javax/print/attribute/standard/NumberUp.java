package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * How many of the document's pages fit on one side of the sheet.
 *
 * <p>In which order those cells are filled is said by {@link PresentationDirection}. It starts at 1
 * because zero pages per side means nothing.
 */
public final class NumberUp extends IntegerSyntax implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -3040436486786527811L;

    public NumberUp(int value) {
        super(value, 1, Integer.MAX_VALUE);
    }

    /**
     * The {@code instanceof} is what keeps a NumberUp from being equal to another
     * integer attribute with the same number.
     */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof NumberUp;
    }

    public final Class<? extends Attribute> getCategory() {
        return NumberUp.class;
    }

    public final String getName() {
        return "number-up";
    }
}
