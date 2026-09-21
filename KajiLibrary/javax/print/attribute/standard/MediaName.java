package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;

/**
 * A paper named by size <em>and</em> material at once: letter white, A4 transparency.
 *
 * <p>It is one of the three ways of saying "which paper", and that is why it inherits from {@link
 * Media} the {@code getCategory()} that returns {@code Media.class}: choosing by name excludes
 * choosing by {@link MediaSizeName size} or by {@link MediaTray tray}.
 */
public class MediaName extends Media implements Attribute {

    private static final long serialVersionUID = 4653117714524155448L;

    public static final MediaName NA_LETTER_WHITE = new MediaName(0);

    public static final MediaName NA_LETTER_TRANSPARENT = new MediaName(1);

    public static final MediaName ISO_A4_WHITE = new MediaName(2);

    public static final MediaName ISO_A4_TRANSPARENT = new MediaName(3);

    private static final String[] myStringTable = {
        "na-letter-white",
        "na-letter-transparent",
        "iso-a4-white",
        "iso-a4-transparent",
    };

    private static final MediaName[] myEnumValueTable = {
        NA_LETTER_WHITE,
        NA_LETTER_TRANSPARENT,
        ISO_A4_WHITE,
        ISO_A4_TRANSPARENT,
    };

    protected MediaName(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }
}
