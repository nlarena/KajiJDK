package javax.swing.colorchooser;

import java.awt.Color;
import java.awt.Graphics;
import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

/**
 * Una solapa del selector de color: una forma de elegir el mismo color.
 *
 * <p>Cada panel --RGB, HSV, CMYK, las muestras-- presenta el color de una manera distinta, pero
 * **ninguno tiene el color**: todos leen y escriben el {@link ColorSelectionModel} del
 * {@link JColorChooser} que los hospeda. Por eso las solapas quedan sincronizadas sin conocerse
 * entre si, y por eso el ciclo de vida del panel gira alrededor de
 * {@link #installChooserPanel} y {@link #uninstallChooserPanel}: instalarlo es engancharlo al
 * modelo, desinstalarlo es soltarlo.
 *
 * <p>Una subclase implementa cinco cosas: {@link #buildChooser} arma la interfaz una sola vez,
 * {@link #updateChooser} la refresca cada vez que el modelo cambia, y
 * {@link #getDisplayName}, {@link #getSmallDisplayIcon} y {@link #getLargeDisplayIcon} dicen como
 * se lo nombra en la solapa.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>El ciclo de vida y el acceso al modelo estan hechos. {@link #paint} delega en la superclase y
 * no dibuja nada propio --el JDK aprovecha ese punto para refrescar el panel cuando cambia el
 * `LookAndFeel`, y aca no hay ninguno-- y el enganche del `enabled` con el selector, que en el JDK
 * es un {@link PropertyChangeListener} sobre una propiedad ligada, tampoco: el `JComponent` de esta
 * biblioteca no tiene propiedades ligadas todavia.
 */
public abstract class AbstractColorChooserPanel extends JPanel {

    /** El nombre de la propiedad que dice si se puede elegir transparencia. */
    public static final String TRANSPARENCY_ENABLED_PROPERTY = "TransparencyEnabled";

    /**
     * El escucha que en el JDK sigue el `enabled` del selector. Aca no se registra --ver la nota de
     * la clase-- pero el campo queda porque es donde iria.
     */
    private final PropertyChangeListener enabledListener = null;

    /** El selector que hospeda a este panel, o `null` si no esta instalado. */
    private JColorChooser chooser;

    /** Si se puede elegir transparencia. */
    private boolean transparencyEnabled = true;

    /** Para las subclases. */
    protected AbstractColorChooserPanel() {
        super();
    }

    /**
     * Refresca la interfaz del panel con el color que hay en el modelo.
     *
     * <p>Lo llama el selector cada vez que el color cambia, venga de este panel o de otro.
     */
    public abstract void updateChooser();

    /**
     * Arma la interfaz del panel.
     *
     * <p>Se llama una sola vez, desde {@link #installChooserPanel}.
     */
    protected abstract void buildChooser();

    /** El nombre de la solapa. */
    public abstract String getDisplayName();

    /**
     * El caracter mnemonico de la solapa, o 0 si no tiene.
     *
     * <p>Cero por omision: un mnemonico repetido entre solapas es peor que ninguno, y la clase base
     * no puede saber cuales estan libres.
     */
    public int getMnemonic() {
        return 0;
    }

    /**
     * Que letra del nombre subrayar como mnemonico, o -1 si ninguna.
     *
     * <p>Es un indice y no un caracter porque el nombre puede repetir la letra, y hay que subrayar
     * una sola.
     */
    public int getDisplayedMnemonicIndex() {
        return -1;
    }

    /** El icono chico de la solapa, o `null` si no tiene. */
    public abstract Icon getSmallDisplayIcon();

    /** El icono grande de la solapa, o `null` si no tiene. */
    public abstract Icon getLargeDisplayIcon();

    /**
     * Engancha el panel a ese selector y arma su interfaz.
     *
     * <p>Lo llama el selector; una subclase que lo redefina tiene que llamar a `super`, o el panel
     * queda sin modelo.
     */
    public void installChooserPanel(JColorChooser enclosingChooser) {
        if (this.chooser != null) {
            throw new RuntimeException("This chooser panel is already installed");
        }
        this.chooser = enclosingChooser;
        buildChooser();
        updateChooser();
    }

    /**
     * Suelta el panel del selector.
     *
     * <p>Una subclase que lo redefina tiene que llamar a `super`, o el panel queda creyendo que
     * sigue instalado.
     */
    public void uninstallChooserPanel(JColorChooser enclosingChooser) {
        this.chooser = null;
    }

    /** El modelo del selector que lo hospeda, o `null` si no esta instalado. */
    public ColorSelectionModel getColorSelectionModel() {
        return this.chooser == null ? null : this.chooser.getSelectionModel();
    }

    /** El color que hay en el modelo, o `null` si el panel no esta instalado. */
    protected Color getColorFromModel() {
        ColorSelectionModel modelo = getColorSelectionModel();
        return modelo == null ? null : modelo.getSelectedColor();
    }

    /** Escribe el color en el modelo. De paquete: es como el panel le contesta al selector. */
    void setSelectedColor(Color color) {
        ColorSelectionModel modelo = getColorSelectionModel();
        if (modelo != null) {
            modelo.setSelectedColor(color);
        }
    }

    /**
     * Prende o apaga la eleccion de transparencia en este panel.
     *
     * <p>Se guarda; hacerla efectiva es cosa de la subclase, que es la que tiene el control del
     * canal alfa --si es que lo tiene.
     */
    public void setColorTransparencySelectionEnabled(boolean b) {
        this.transparencyEnabled = b;
    }

    /** Si este panel deja elegir transparencia. Por omision, si. */
    public boolean isColorTransparencySelectionEnabled() {
        return this.transparencyEnabled;
    }

    /** Dibuja el panel. Ver la nota de la clase: no agrega nada propio. */
    public void paint(Graphics g) {
        super.paint(g);
    }
}
