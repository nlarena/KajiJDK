package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * El compromiso entre velocidad, tinta y calidad con el que se imprime.
 *
 * <p>Es deliberadamente vago: cada impresora traduce los tres escalones a sus propios ajustes. La
 * peticion concreta de puntos por pulgada es {@link PrinterResolution}.
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

    /** IPP numera esta categoria desde 3; la fila 0 de las tablas es ese 3. */
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
