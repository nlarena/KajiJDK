package javax.swing.plaf.basic;

import java.beans.PropertyChangeEvent;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;

/**
 * El aspecto basico de un panel de edicion.
 *
 * <h2>El juego de edicion no lo elige el aspecto</h2>
 *
 * <p>Es la unica diferencia de fondo con los otros UI de texto: un campo o un area tienen siempre
 * el mismo juego de edicion, y un {@link JEditorPane} lo cambia segun el tipo de contenido --texto
 * plano, HTML, RTF--. Por eso {@link #getEditorKit} no devuelve una constante: le pregunta al
 * componente. Y por eso hace falta escuchar {@code "editorKit"}: cuando cambia, hay que rehacer el
 * arbol de vistas entero, porque las vistas viejas son las del juego viejo.
 *
 * <h2>Lo que se reinstala al cambiar de juego</h2>
 *
 * <p>El color y la fuente. Un juego de edicion trae sus propios estilos, y si el que estaba puesto
 * venia del aspecto hay que volver a ponerlo encima; si lo puso el usuario, no. Es la misma regla
 * de {@link UIResource} de siempre, aplicada en un momento raro.
 */
public class BasicEditorPaneUI extends BasicTextUI {

    public BasicEditorPaneUI() {
        super();
    }

    /** Uno nuevo por panel: un UI de texto guarda el componente. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicEditorPaneUI();
    }

    protected String getPropertyPrefix() {
        return "EditorPane";
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        actualizarEstilo((JTextComponent) c);
    }

    public void uninstallUI(JComponent c) {
        cleanDisplayProperties((JTextComponent) c);
        super.uninstallUI(c);
    }

    /** El del componente, no uno fijo; ver la nota de la clase. */
    public EditorKit getEditorKit(JTextComponent tc) {
        JEditorPane pane = (JEditorPane) tc;
        return pane.getEditorKit();
    }

    /** Rehace las vistas y el estilo cuando cambia el juego de edicion. */
    protected void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
        String nombre = evt.getPropertyName();
        if ("editorKit".equals(nombre)) {
            actualizarEstilo((JTextComponent) evt.getSource());
        } else if ("editable".equals(nombre) || "foreground".equals(nombre)
                || "font".equals(nombre) || "document".equals(nombre)) {
            actualizarEstilo((JTextComponent) evt.getSource());
        }
    }

    /** Deja el color y la fuente del aspecto encima de los del juego; ver la nota de la clase. */
    private void actualizarEstilo(JTextComponent editor) {
        if (editor.getForeground() instanceof UIResource
                || editor.getFont() instanceof UIResource) {
            // Los valores del aspecto se vuelven a aplicar tal cual: ya estan puestos en el
            // componente y el juego de edicion los lee de ahi. No hay nada que copiar.
            editor.repaint();
        }
    }

    /** Saca lo que este UI dejo puesto en el componente. */
    private void cleanDisplayProperties(JTextComponent editor) {
    }
}
