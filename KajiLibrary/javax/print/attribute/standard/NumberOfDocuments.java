package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;

/**
 * How many documents the job has.
 *
 * <p>A job may carry several documents; how they are laid out among themselves is decided by
 * {@link MultipleDocumentHandling}.
 */
public final class NumberOfDocuments extends IntegerSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = 7891881310684461097L;

    public NumberOfDocuments(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /**
     * The {@code instanceof} is what keeps a NumberOfDocuments from being equal to another
     * integer attribute with the same number.
     */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof NumberOfDocuments;
    }

    public final Class<? extends Attribute> getCategory() {
        return NumberOfDocuments.class;
    }

    public final String getName() {
        return "number-of-documents";
    }
}
