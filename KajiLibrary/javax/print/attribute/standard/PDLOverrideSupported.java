package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintServiceAttribute;

/**
 * Si la impresora intenta que los atributos del trabajo le ganen a las instrucciones que el propio
 * documento trae adentro.
 *
 * <p>Un PostScript puede pedir dos caras por su cuenta; {@code ATTEMPTED} dice que la impresora va
 * a tratar de imponer lo que diga el trabajo, sin prometer que lo logre.
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
