package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * How the several documents of the same job are laid out on the paper.
 *
 * <p>It decides two things at once: whether the documents share a sheet --the {@code
 * SINGLE_DOCUMENT} variants may print the end of one and the start of the other on the same side--
 * and whether the copies come out collated or in batches. It only makes sense with {@link Copies}
 * greater than one.
 */
public class MultipleDocumentHandling extends EnumSyntax implements PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = 8098326460746413466L;

    public static final MultipleDocumentHandling SINGLE_DOCUMENT = new MultipleDocumentHandling(0);

    public static final MultipleDocumentHandling SEPARATE_DOCUMENTS_UNCOLLATED_COPIES = new MultipleDocumentHandling(1);

    public static final MultipleDocumentHandling SEPARATE_DOCUMENTS_COLLATED_COPIES = new MultipleDocumentHandling(2);

    public static final MultipleDocumentHandling SINGLE_DOCUMENT_NEW_SHEET = new MultipleDocumentHandling(3);

    private static final String[] myStringTable = {
        "single-document",
        "separate-documents-uncollated-copies",
        "separate-documents-collated-copies",
        "single-document-new-sheet",
    };

    private static final MultipleDocumentHandling[] myEnumValueTable = {
        SINGLE_DOCUMENT,
        SEPARATE_DOCUMENTS_UNCOLLATED_COPIES,
        SEPARATE_DOCUMENTS_COLLATED_COPIES,
        SINGLE_DOCUMENT_NEW_SHEET,
    };

    protected MultipleDocumentHandling(int value) {
        super(value);
    }

    protected String[] getStringTable() {
        return myStringTable;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return myEnumValueTable;
    }

    public final Class<? extends Attribute> getCategory() {
        return MultipleDocumentHandling.class;
    }

    public final String getName() {
        return "multiple-document-handling";
    }
}
