package javax.print.attribute.standard;

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.TextSyntax;

/*
 * FAMILY HEADER -- this package's {@code TextSyntax} attributes.
 *
 * <p>Free text with the language it is written in. The (string, locale) pair goes together because
 * a job name or an operator message is shown to a person, and without knowing the language they
 * cannot even be sorted or broken into lines properly.
 *
 * <p>The mechanism is in {@link javax.print.attribute.TextSyntax TextSyntax}: a null string is an
 * error, a null locale means "the local one" and is resolved to the default in the constructor.
 * {@code toString()} returns the bare string, without the locale.
 *
 * <p>Unlike {@link javax.print.attribute.EnumSyntax EnumSyntax}, here the value comes from no
 * table: it is what the user wrote. The only thing each subclass contributes is which question that
 * text answers.
 */

/**
 * The name of a single document within the job.
 *
 * <p>A job with several documents gives each one its own; the whole job's name is {@link JobName}.
 */
public final class DocumentName extends TextSyntax implements DocAttribute {

    private static final long serialVersionUID = 7883105848533280430L;

    public DocumentName(String documentName, Locale locale) {
        super(documentName, locale);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof DocumentName;
    }

    public final Class<? extends Attribute> getCategory() {
        return DocumentName.class;
    }

    public final String getName() {
        return "document-name";
    }
}
