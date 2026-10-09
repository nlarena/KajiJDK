package javax.print;

/**
 * KajiLibrary's javax.print.MultiDocPrintService -- a printer that accepts jobs of several
 * documents.
 *
 * <p>It is looked up with {@code PrintServiceLookup.lookupMultiDocPrintServices}, which also
 * filters by the formats that all have to be supported at once.
 */
public interface MultiDocPrintService extends PrintService {

    /** A new job that accepts several documents. */
    MultiDocPrintJob createMultiDocPrintJob();
}
