package javax.print;

import javax.print.attribute.PrintJobAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.event.PrintJobAttributeListener;
import javax.print.event.PrintJobListener;

/**
 * KajiLibrary's javax.print.DocPrintJob -- un trabajo de impresion.
 *
 * <p>Se saca de {@code PrintService.createPrintJob()} y sirve <b>una sola vez</b>: llamar
 * {@link #print} dos veces sobre el mismo trabajo lanza {@code PrintException}. Para imprimir otra
 * cosa hay que pedirle otro trabajo al servicio.
 *
 * <p>{@link #print} bloquea hasta que el servicio tomo el documento, no hasta que salio el papel. Para
 * saber que paso despues hay que registrar un {@link PrintJobListener} <b>antes</b> de llamarla.
 *
 * <p>El escucha de atributos lleva un conjunto que dice cuales interesan; ver
 * {@link PrintJobAttributeListener}.
 */
public interface DocPrintJob {

    /** De que impresora es. */
    PrintService getPrintService();

    /** Los atributos actuales del trabajo. */
    PrintJobAttributeSet getAttributes();

    /** Registra un escucha de estado. Antes de {@link #print}. */
    void addPrintJobListener(PrintJobListener listener);

    /** Lo da de baja. */
    void removePrintJobListener(PrintJobListener listener);

    /**
     * Registra un escucha de atributos.
     *
     * @param attributes cuales interesan; null significa todos
     */
    void addPrintJobAttributeListener(PrintJobAttributeListener listener,
                                      PrintJobAttributeSet attributes);

    /** Lo da de baja. */
    void removePrintJobAttributeListener(PrintJobAttributeListener listener);

    /**
     * Imprime. Ver la nota de la clase: se usa una sola vez y no espera al papel.
     *
     * @throws PrintException si fallo, o si este trabajo ya se uso
     */
    void print(Doc doc, PrintRequestAttributeSet attributes) throws PrintException;
}
