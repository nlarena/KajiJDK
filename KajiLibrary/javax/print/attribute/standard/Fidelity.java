package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * What to do when the printer does not support some of the requested attributes: fail the whole job
 * ({@code FIDELITY_TRUE}) or print as best it can ({@code FIDELITY_FALSE}).
 *
 * <p>The constants' names carry a prefix because bare {@code TRUE} and {@code FALSE} would not say
 * of what.
 */
public final class Fidelity extends EnumSyntax implements PrintJobAttribute, PrintRequestAttribute {

    private static final long serialVersionUID = 6320827847329172308L;

    public static final Fidelity FIDELITY_TRUE = new Fidelity(0);

    public static final Fidelity FIDELITY_FALSE = new Fidelity(1);

    private static final String[] myStringTable = {
        "true",
        "false",
    };

    private static final Fidelity[] myEnumValueTable = {
        FIDELITY_TRUE,
        FIDELITY_FALSE,
    };

    protected Fidelity(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    public final Class<? extends Attribute> getCategory() {
        return Fidelity.class;
    }

    public final String getName() {
        return "ipp-attribute-fidelity";
    }
}
