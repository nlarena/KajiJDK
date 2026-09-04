package javax.print;

/**
 * KajiLibrary's javax.print.MultiDocPrintService -- una impresora que acepta trabajos de varios
 * documentos.
 *
 * <p>Se busca con {@code PrintServiceLookup.lookupMultiDocPrintServices}, que ademas filtra por los
 * formatos que tienen que estar todos soportados a la vez.
 */
public interface MultiDocPrintService extends PrintService {

    /** Un trabajo nuevo que acepta varios documentos. */
    MultiDocPrintJob createMultiDocPrintJob();
}
