package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/*
 * FAMILY HEADER -- this package's {@code EnumSyntax} attributes.
 *
 * <p>Two thirds of {@code javax.print.attribute.standard} are the same class written many times: a
 * named integer. The whole mechanism is in {@link javax.print.attribute.EnumSyntax EnumSyntax} and
 * each subclass only parameterises it with three things.
 *
 * <ul>
 * <li>{@code getStringTable()} -- each value's IPP name. It is what {@code toString()} prints, and
 *     the JDK specifies it to the character: {@code "two-sided-long-edge"}, not {@code
 *     "TWO_SIDED_LONG_EDGE"}. An entry may be {@code null} when IPP reserved a number Java does not
 *     expose; there {@code toString()} falls back to the bare integer.</li>
 * <li>{@code getEnumValueTable()} -- the constants in the same order, so that {@code readResolve()}
 *     can turn an integer back into <em>the</em> constant and {@code ==} keeps working after a trip
 *     through a stream.</li>
 * <li>{@code getOffset()} -- the integer of the first row. It is zero except where IPP started the
 *     numbering at 3 ({@code Finishings}, {@code OrientationRequested}, {@code PrintQuality}).</li>
 * </ul>
 *
 * <p>The values are <b>singletons</b>: the constructor is {@code protected} so that a printer can
 * declare values of its own, but nobody makes the standard ones twice. That is why {@code equals()}
 * is inherited from {@code Object} --identity-- except in {@link Media}, where the concrete class
 * has to be compared too.
 *
 * <p>The name tables are <b>standards data</b> (RFC 2911 / IPP), not locale data: they depend
 * neither on CLDR nor on any printer, so they go in complete.
 */

/**
 * Whether the job is printed in colour or in black and white.
 *
 * <p>It is a request about the <em>document</em>, not about the printer: {@code MONOCHROME} on a
 * colour printer asks it not to use colour ink, and says nothing about what the printer can do --
 * {@link ColorSupported} answers that.
 */
public final class Chromaticity extends EnumSyntax implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = 4660543931355214012L;

    public static final Chromaticity MONOCHROME = new Chromaticity(0);

    public static final Chromaticity COLOR = new Chromaticity(1);

    private static final String[] myStringTable = {
        "monochrome",
        "color",
    };

    private static final Chromaticity[] myEnumValueTable = {
        MONOCHROME,
        COLOR,
    };

    protected Chromaticity(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    public final Class<? extends Attribute> getCategory() {
        return Chromaticity.class;
    }

    public final String getName() {
        return "chromaticity";
    }
}
