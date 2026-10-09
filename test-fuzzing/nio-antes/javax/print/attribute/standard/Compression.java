package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.EnumSyntax;

/**
 * Which algorithm the document's data stream comes compressed with.
 *
 * <p>It describes the transport, not the content: the printer has to decompress before
 * looking at the document's format.
 */
public class Compression extends EnumSyntax implements DocAttribute {

    private static final long serialVersionUID = -5716748913324997674L;

    public static final Compression NONE = new Compression(0);

    public static final Compression DEFLATE = new Compression(1);

    public static final Compression GZIP = new Compression(2);

    public static final Compression COMPRESS = new Compression(3);

    private static final String[] myStringTable = {
        "none",
        "deflate",
        "gzip",
        "compress",
    };

    private static final Compression[] myEnumValueTable = {
        NONE,
        DEFLATE,
        GZIP,
        COMPRESS,
    };

    protected Compression(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    public final Class<? extends Attribute> getCategory() {
        return Compression.class;
    }

    public final String getName() {
        return "compression";
    }
}
