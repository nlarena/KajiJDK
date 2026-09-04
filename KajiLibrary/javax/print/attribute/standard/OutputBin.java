package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * En que bandeja de salida cae el papel impreso.
 *
 * <p>{@code FACE_UP} y {@code FACE_DOWN} no son lugares sino como queda apilado, que es lo que
 * decide si el orden de las paginas sale derecho o al reves.
 */
public class OutputBin extends EnumSyntax implements PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -3718893309873137109L;

    public static final OutputBin TOP = new OutputBin(0);

    public static final OutputBin MIDDLE = new OutputBin(1);

    public static final OutputBin BOTTOM = new OutputBin(2);

    public static final OutputBin SIDE = new OutputBin(3);

    public static final OutputBin LEFT = new OutputBin(4);

    public static final OutputBin RIGHT = new OutputBin(5);

    public static final OutputBin CENTER = new OutputBin(6);

    public static final OutputBin REAR = new OutputBin(7);

    public static final OutputBin FACE_UP = new OutputBin(8);

    public static final OutputBin FACE_DOWN = new OutputBin(9);

    public static final OutputBin LARGE_CAPACITY = new OutputBin(10);

    private static final String[] myStringTable = {
        "top",
        "middle",
        "bottom",
        "side",
        "left",
        "right",
        "center",
        "rear",
        "face-up",
        "face-down",
        "large-capacity",
    };

    private static final OutputBin[] myEnumValueTable = {
        TOP,
        MIDDLE,
        BOTTOM,
        SIDE,
        LEFT,
        RIGHT,
        CENTER,
        REAR,
        FACE_UP,
        FACE_DOWN,
        LARGE_CAPACITY,
    };

    protected OutputBin(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    public final Class<? extends Attribute> getCategory() {
        return OutputBin.class;
    }

    public final String getName() {
        return "output-bin";
    }
}
