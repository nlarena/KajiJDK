package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;

/**
 * De que bandeja de entrada sale el papel.
 *
 * <p>Es la tercera manera de decir "que papel": en vez de describirlo se elige la ranura, y lo que
 * haya cargado ahi es cosa del operador.
 */
public class MediaTray extends Media implements Attribute {

    private static final long serialVersionUID = -982503611095214703L;

    public static final MediaTray TOP = new MediaTray(0);

    public static final MediaTray MIDDLE = new MediaTray(1);

    public static final MediaTray BOTTOM = new MediaTray(2);

    public static final MediaTray ENVELOPE = new MediaTray(3);

    public static final MediaTray MANUAL = new MediaTray(4);

    public static final MediaTray LARGE_CAPACITY = new MediaTray(5);

    public static final MediaTray MAIN = new MediaTray(6);

    public static final MediaTray SIDE = new MediaTray(7);

    private static final String[] myStringTable = {
        "top",
        "middle",
        "bottom",
        "envelope",
        "manual",
        "large-capacity",
        "main",
        "side",
    };

    private static final MediaTray[] myEnumValueTable = {
        TOP,
        MIDDLE,
        BOTTOM,
        ENVELOPE,
        MANUAL,
        LARGE_CAPACITY,
        MAIN,
        SIDE,
    };

    protected MediaTray(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }
}
