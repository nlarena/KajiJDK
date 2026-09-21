package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintServiceAttribute;

/**
 * How many pages per minute the printer puts out in colour.
 *
 * <p>Almost always fewer than {@link PagesPerMinute}. A printer that does not print in colour does
 * not report this attribute --it does not report it as zero-- and {@link ColorSupported} says so.
 */
public final class PagesPerMinuteColor extends IntegerSyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = 1684993151687470944L;

    public PagesPerMinuteColor(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /**
     * The {@code instanceof} is what keeps a PagesPerMinuteColor from being equal to another
     * integer attribute with the same number.
     */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof PagesPerMinuteColor;
    }

    public final Class<? extends Attribute> getCategory() {
        return PagesPerMinuteColor.class;
    }

    public final String getName() {
        return "pages-per-minute-color";
    }
}
