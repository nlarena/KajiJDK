package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintServiceAttribute;

/**
 * Whether the printer tries to make the job's attributes win over the instructions the document
 * itself carries inside.
 *
 * <p>A PostScript may ask for two sides on its own; {@code ATTEMPTED} says the printer is going to
 * try to impose what the job says, without promising it succeeds.
 */
public class PDLOverrideSupported extends EnumSyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = -4393264467928463934L;

    public static final PDLOverrideSupported NOT_ATTEMPTED = new PDLOverrideSupported(0);

    public static final PDLOverrideSupported ATTEMPTED = new PDLOverrideSupported(1);

    private static final String[] myStringTable = {
        "not-attempted",
        "attempted",
    };

    private static final PDLOverrideSupported[] myEnumValueTable = {
        NOT_ATTEMPTED,
        ATTEMPTED,
    };

    protected PDLOverrideSupported(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    public final Class<? extends Attribute> getCategory() {
        return PDLOverrideSupported.class;
    }

    public final String getName() {
        return "pdl-override-supported";
    }
}
