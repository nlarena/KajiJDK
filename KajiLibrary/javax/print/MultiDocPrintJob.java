package javax.print;

import javax.print.attribute.PrintRequestAttributeSet;

/**
 * KajiLibrary's javax.print.MultiDocPrintJob -- un trabajo que acepta varios documentos.
 *
 * <p>Los documentos van en un solo trabajo, no en varios. La diferencia importa: comparten los
 * atributos de la peticion, salen juntos en la cola, y se cancelan juntos.
 */
public interface MultiDocPrintJob extends DocPrintJob {

    /**
     * Imprime todos.
     *
     * @throws PrintException si algo fallo
     */
    void print(MultiDoc multiDoc, PrintRequestAttributeSet attributes) throws PrintException;
}
