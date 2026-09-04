package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Si las copias de un mismo documento salen intercaladas (1,2,3,1,2,3) o de a tandas por pagina
 * (1,1,2,2,3,3).
 *
 * <p>Es a nivel de hoja; el equivalente entre documentos distintos lo decide {@link
 * MultipleDocumentHandling}.
 */
public final class SheetCollate extends EnumSyntax implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = 7080587914259873003L;

    public static final SheetCollate UNCOLLATED = new SheetCollate(0);

    public static final SheetCollate COLLATED = new SheetCollate(1);

    private static final String[] myStringTable = {
        "uncollated",
        "collated",
    };

    private static final SheetCollate[] myEnumValueTable = {
        UNCOLLATED,
        COLLATED,
    };

    protected SheetCollate(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    public final Class<? extends Attribute> getCategory() {
        return SheetCollate.class;
    }

    public final String getName() {
        return "sheet-collate";
    }
}
