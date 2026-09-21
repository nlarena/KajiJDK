package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Whether the copies of the same document come out collated (1,2,3,1,2,3) or in batches per page
 * (1,1,2,2,3,3).
 *
 * <p>It is at the sheet level; the equivalent between different documents is decided by
 * {@link MultipleDocumentHandling}.
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
