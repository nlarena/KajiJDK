package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;

/**
 * Cuantos documentos tiene el trabajo.
 *
 * <p>Un trabajo puede llevar varios documentos; como se acomodan entre si lo decide {@link
 * MultipleDocumentHandling}.
 */
public final class NumberOfDocuments extends IntegerSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = 7891881310684461097L;

    public NumberOfDocuments(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /** El {@code instanceof} es lo que impide que un NumberOfDocuments de igual a otro atributo
     * entero con el mismo numero. */
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
