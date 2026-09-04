package java.awt.datatransfer;

/**
 * A quien avisarle cuando le sacan el portapapeles.
 *
 * <p>El portapapeles tiene **un** dueño por vez: el último que copió. Cuando otro copia, el anterior
 * recibe este aviso, y ahí es cuando puede soltar lo que estaba guardando para poder entregarlo.
 *
 * <p>No hay garantía de cuándo llega ni de que llegue: si el programa se cierra antes, no llega
 * nunca. Por eso no sirve para liberar nada crítico.
 */
public interface ClipboardOwner {

    /** Avisa que otro se quedó con el portapapeles. */
    void lostOwnership(Clipboard clipboard, Transferable contents);
}
