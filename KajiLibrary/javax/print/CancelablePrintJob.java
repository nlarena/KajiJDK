package javax.print;

/**
 * KajiLibrary's javax.print.CancelablePrintJob -- un trabajo que se puede cancelar.
 *
 * <p>Es una interfaz aparte y no un metodo de {@link DocPrintJob} porque no todos los trabajos se
 * pueden cancelar: hay servicios que entregan a una cola del sistema y despues pierden el control. Un
 * programa pregunta con {@code instanceof} antes de ofrecer un boton de cancelar.
 *
 * <p>Cancelar es asincronico: {@link #cancel} vuelve enseguida y el {@code JOB_CANCELED} llega despues.
 * Puede fallar si el trabajo ya termino, o si llego demasiado tarde -- en cuyo caso lo que llega es
 * {@code JOB_COMPLETE}.
 */
public interface CancelablePrintJob extends DocPrintJob {

    /**
     * Pide cancelarlo.
     *
     * @throws PrintException si ya no se puede
     */
    void cancel() throws PrintException;
}
