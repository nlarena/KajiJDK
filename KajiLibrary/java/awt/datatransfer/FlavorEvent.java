package java.awt.datatransfer;

import java.util.EventObject;

/**
 * Cambió lo que hay en un portapapeles.
 *
 * <p>No dice **qué** cambió: sólo que el contenido es otro. Quien lo reciba tiene que preguntarle al
 * portapapeles, y eso es a propósito — entre el aviso y la consulta el contenido puede haber
 * cambiado otra vez, y un evento que trajera los datos estaría mintiendo la mitad de las veces.
 */
public class FlavorEvent extends EventObject {

    private static final long serialVersionUID = -5842664112252414548L;

    /**
     * Con el portapapeles que cambió.
     *
     * @throws IllegalArgumentException si es `null`
     */
    public FlavorEvent(Clipboard source) {
        super(source);
    }
}
