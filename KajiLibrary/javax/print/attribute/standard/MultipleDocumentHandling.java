package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Como se acomodan en el papel los varios documentos de un mismo trabajo.
 *
 * <p>Decide dos cosas de una vez: si los documentos comparten hoja --las variantes {@code
 * SINGLE_DOCUMENT} pueden imprimir el final de uno y el principio del otro en la misma cara-- y si
 * las copias salen intercaladas o de a tandas. Solo tiene sentido con {@link Copies} mayor que uno.
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
