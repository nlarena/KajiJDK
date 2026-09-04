package java.awt.desktop;

import java.util.EventObject;

/**
 * KajiLibrary's java.awt.desktop.AppEvent -- la raiz de los eventos del escritorio.
 *
 * <p>No agrega nada sobre {@link EventObject}: existe para que los quince eventos del paquete tengan
 * un tipo comun, y para que su constructor --de acceso de paquete-- ponga siempre la misma fuente.
 *
 * <p>Que el constructor no sea publico es lo que garantiza que estos eventos los emita el escritorio y
 * no cualquiera. Las subclases si tienen constructor publico, para poder probarlas.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>En el JDK la fuente es la instancia de {@code java.awt.Desktop}. Esta biblioteca todavia no tiene
 * esa clase, asi que la fuente es un objeto propio de este paquete. {@link EventObject} exige una
 * fuente no nula y la documentacion de {@code AppEvent} no promete de que clase es, asi que esto es
 * legal; lo que no se puede hacer es castear {@link #getSource} a {@code Desktop}.
 */
public class AppEvent extends EventObject {

    private static final long serialVersionUID = -5958503993556009432L;

    /**
     * La fuente comun de todos estos eventos. Ver la nota de la clase.
     *
     * <p>Su {@code toString} lo dice, para que quien inspeccione un evento entienda que esta viendo en
     * lugar de suponer que es un {@code Desktop}.
     */
    private static final Object SOURCE = new EventSource();

    /** La fuente comun. Named y no anonima para que su {@code toString} sea legible en un volcado. */
    private static final class EventSource {
        @Override
        public String toString() {
            return "java.awt.desktop (no Desktop in this library)";
        }
    }

    /** De acceso de paquete a proposito; ver la nota de la clase. */
    AppEvent() {
        super(SOURCE);
    }
}
