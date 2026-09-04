package javax.print.event;

import javax.print.DocPrintJob;

/**
 * KajiLibrary's javax.print.event.PrintJobEvent -- cambio de estado de un trabajo.
 *
 * <p>El tipo se lee con {@link #getPrintEventType} y es uno de las seis constantes. Vale entender en
 * que orden pueden llegar, porque no es obvio:
 *
 * <ul>
 *   <li>{@link #DATA_TRANSFER_COMPLETE} dice que el servicio ya termino de leer el documento. No dice
 *       que se imprimio: dice que el {@code Doc} ya se puede cerrar o reusar;
 *   <li>{@link #JOB_COMPLETE}, {@link #JOB_CANCELED} y {@link #JOB_FAILED} son los tres finales
 *       posibles, y son excluyentes;
 *   <li>{@link #REQUIRES_ATTENTION} es papel trabado, sin tinta, bandeja vacia. No es final: el
 *       trabajo puede seguir cuando alguien lo resuelva;
 *   <li>{@link #NO_MORE_EVENTS} es el importante y el que se suele ignorar. Significa que el servicio
 *       <b>deja de informar</b> sobre este trabajo. Puede llegar sin que haya llegado ningun final,
 *       porque hay colas que pierden de vista el trabajo una vez entregado. Un programa que espera
 *       {@code JOB_COMPLETE} sin atajar esto se cuelga para siempre.
 * </ul>
 */
public class PrintJobEvent extends PrintEvent {

    private static final long serialVersionUID = -1711656903622072997L;

    /** Cancelado. */
    public static final int JOB_CANCELED = 101;

    /** Terminado bien. */
    public static final int JOB_COMPLETE = 102;

    /** Fallo. */
    public static final int JOB_FAILED = 103;

    /** Necesita intervencion; no es final. Ver la nota de la clase. */
    public static final int REQUIRES_ATTENTION = 104;

    /** No se informa mas sobre este trabajo. Ver la nota de la clase. */
    public static final int NO_MORE_EVENTS = 105;

    /** El documento ya se leyo entero. Ver la nota de la clase. */
    public static final int DATA_TRANSFER_COMPLETE = 106;

    /** Cual de los seis. */
    private final int reason;

    /**
     * @param source el trabajo
     * @param reason una de las seis constantes
     * @throws IllegalArgumentException si el trabajo es null
     */
    public PrintJobEvent(DocPrintJob source, int reason) {
        super(source);
        this.reason = reason;
    }

    /** Cual de las seis constantes. */
    public int getPrintEventType() {
        return this.reason;
    }

    /** El trabajo. */
    public DocPrintJob getPrintJob() {
        return (DocPrintJob) getSource();
    }
}
