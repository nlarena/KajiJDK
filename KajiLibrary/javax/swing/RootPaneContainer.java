package javax.swing;

import java.awt.Component;
import java.awt.Container;

/**
 * Lo implementa lo que contiene un {@link JRootPane}: ventanas, dialogos y applets.
 *
 * <h2>Para que sirve tenerla</h2>
 *
 * <p>Los seis metodos son atajos a los del panel raiz. Existen para que se pueda escribir
 * {@code ventana.getContentPane()} en lugar de {@code ventana.getRootPane().getContentPane()}, y
 * sobre todo para que un metodo pueda recibir "algo que tiene contenido" sin saber si es una
 * ventana, un dialogo o un applet.
 *
 * <p>Es tambien lo que recuerda que a estos contenedores no se les agregan componentes
 * directamente; ver la nota de {@link JRootPane}.
 */
public interface RootPaneContainer {

    /** El panel raiz. */
    JRootPane getRootPane();

    void setContentPane(Container contentPane);

    /** Donde va lo que agrega el programa. */
    Container getContentPane();

    void setLayeredPane(JLayeredPane layeredPane);

    JLayeredPane getLayeredPane();

    void setGlassPane(Component glassPane);

    /** El componente de arriba de todo; ver la nota de {@link JRootPane}. */
    Component getGlassPane();
}
