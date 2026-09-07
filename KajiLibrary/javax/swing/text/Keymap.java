package javax.swing.text;

import javax.swing.Action;
import javax.swing.KeyStroke;

/**
 * Que hace cada tecla: un mapa de combinaciones a acciones.
 *
 * <p>Los mapas se encadenan por su padre de resolucion, igual que los estilos: uno propio con dos
 * teclas cambiadas cuelga del mapa por omision y hereda las demas. Cambiar un atajo de una
 * aplicacion es agregar un eslabon, no copiar la tabla.
 *
 * <p>{@link #getDefaultAction} es la que atiende lo que no coincidio con nada: en un editor, la
 * que inserta el caracter que se escribio.
 *
 * <p>Swing la reemplazo por {@code InputMap} y {@code ActionMap}, que valen para cualquier
 * componente; esta quedo porque los componentes de texto la siguen exponiendo.
 */
public interface Keymap {

    String getName();

    /** La accion para lo que no esta atado; ver la nota de la interfaz. */
    Action getDefaultAction();

    void setDefaultAction(Action a);

    /** La accion de esa combinacion, mirando tambien los mapas padres. */
    Action getAction(KeyStroke key);

    KeyStroke[] getBoundKeyStrokes();

    Action[] getBoundActions();

    KeyStroke[] getKeyStrokesForAction(Action a);

    /** Si esa combinacion esta atada en <em>este</em> mapa, sin mirar los padres. */
    boolean isLocallyDefined(KeyStroke key);

    void addActionForKeyStroke(KeyStroke key, Action a);

    void removeKeyStrokeBinding(KeyStroke keys);

    void removeBindings();

    Keymap getResolveParent();

    void setResolveParent(Keymap parent);
}
