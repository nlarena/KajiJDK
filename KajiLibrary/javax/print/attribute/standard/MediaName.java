package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;

/**
 * Un papel nombrado por tamano <em>y</em> material a la vez: carta blanco, A4 transparencia.
 *
 * <p>Es una de las tres maneras de decir "que papel", y por eso hereda de {@link Media} el {@code
 * getCategory()} que devuelve {@code Media.class}: elegir por nombre excluye elegir por {@link
 * MediaSizeName tamano} o por {@link MediaTray bandeja}.
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
