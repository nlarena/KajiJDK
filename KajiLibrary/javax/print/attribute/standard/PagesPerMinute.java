package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintServiceAttribute;

/**
 * How many pages per minute the printer puts out in monochrome.
 *
 * <p>Zero is a legitimate answer: it means it takes more than a minute per page, not that it does
 * not print.
 */
public final class PagesPerMinute extends IntegerSyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = -6366403993072862015L;

    public PagesPerMinute(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /**
     * The {@code instanceof} is what keeps a PagesPerMinute from being equal to another
     * integer attribute with the same number.
     */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof PagesPerMinute;
    }

    public final Class<? extends Attribute> getCategory() {
        return PagesPerMinute.class;
    }

    public final String getName() {
        return "pages-per-minute";
    }
}
