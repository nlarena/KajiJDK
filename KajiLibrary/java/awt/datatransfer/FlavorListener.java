package java.awt.datatransfer;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que cambió lo que hay en el portapapeles.
 *
 * <p>El uso típico es habilitar o deshabilitar el botón de pegar según haya algo pegable.
 */
public interface FlavorListener extends EventListener {

    /** Avisa que el contenido del portapapeles es otro. */
    void flavorsChanged(FlavorEvent e);
}
