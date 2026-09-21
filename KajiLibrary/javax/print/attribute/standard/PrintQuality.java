package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * The compromise between speed, ink and quality with which it prints.
 *
 * <p>It is deliberately vague: each printer translates the three steps to its own settings. The
 * concrete request of dots per inch is {@link PrinterResolution}.
 */
public class PrintQuality extends EnumSyntax implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -3072341285225858365L;

    public static final PrintQuality DRAFT = new PrintQuality(3);

    public static final PrintQuality NORMAL = new PrintQuality(4);

    public static final PrintQuality HIGH = new PrintQuality(5);

    private static final String[] myStringTable = {
        "draft",
        "normal",
        "high",
    };

    private static final PrintQuality[] myEnumValueTable = {
        DRAFT,
        NORMAL,
        HIGH,
    };

    protected PrintQuality(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    /** IPP numbers this category from 3; row 0 of the tables is that 3. */
    protected int getOffset() {
        return 3;
    }

    public final Class<? extends Attribute> getCategory() {
        return PrintQuality.class;
    }

    public final String getName() {
        return "print-quality";
    }
}
