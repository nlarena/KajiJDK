package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * The root of the three ways of saying "which paper": by {@link MediaSizeName size}, by
 * {@link MediaName name} or by {@link MediaTray tray}.
 *
 * <p>The interesting part is that all three report {@code Media.class} as the category, not their
 * own class. That is not a simplification: it is what keeps an attribute set from carrying both
 * "A4" and "manual tray", because both are answers to <b>a single</b> question and in an {@code
 * AttributeSet} the category is the key. Without this trick a job could ask for two contradictory
 * papers and nobody would notice until the printer.
 *
 * <p>For the same reason {@code equals()} is not enough by comparing the integer, as the rest of
 * the {@code EnumSyntax} family does: {@code MediaTray.TOP} and {@code MediaName.NA_LETTER_WHITE}
 * are both zero, and without looking at the concrete class they would come out equal. See the
 * family header in {@link Chromaticity} for the common mechanism.
 */
public abstract class Media extends EnumSyntax
    implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -2823970704630722439L;

    protected Media(int value) {
        super(value);
    }

    /** Equal value <b>and</b> same concrete class: see the header's note. */
    public boolean equals(Object object) {
        return object != null
            && object instanceof Media
            && object.getClass() == this.getClass()
            && ((Media) object).getValue() == this.getValue();
    }

    public final Class<? extends Attribute> getCategory() {
        return Media.class;
    }

    public final String getName() {
        return "media";
    }
}
