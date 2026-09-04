package javax.swing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.colorchooser.ColorSelectionModel;
import javax.swing.colorchooser.DefaultColorSelectionModel;

/**
 * El selector de color: un panel con varias formas de elegir un color y una sola respuesta.
 *
 * <p>La pieza que lo ordena todo es el {@link ColorSelectionModel}: las solapas --RGB, HSV, CMYK,
 * las muestras-- son {@link AbstractColorChooserPanel} distintos que **comparten el mismo modelo**.
 * Por eso mover un deslizador en RGB actualiza lo que se ve en HSV sin que ninguno de los dos sepa
 * del otro: los dos escuchan al modelo.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta la parte que es estado: los constructores, el modelo, el color, los paneles y el panel de
 * vista previa. Falta lo que necesita ventanas o `LookAndFeel`: `showDialog` y `createDialog`
 * --que abren un dialogo modal-- y `getUI`/`setUI`/`updateUI`.
 *
 * <p>Tampoco se disparan los eventos de cambio de propiedad al reemplazar el modelo o los paneles:
 * el `JComponent` de esta biblioteca todavia no tiene el soporte de propiedades ligadas, y
 * fabricarlo aca solo para esta clase seria peor.
 */
public class JColorChooser extends JComponent {


    /** El nombre de la propiedad ligada del modelo de seleccion. */
    public static final String SELECTION_MODEL_PROPERTY = "selectionModel";

    /** El nombre de la propiedad ligada del panel de vista previa. */
    public static final String PREVIEW_PANEL_PROPERTY = "previewPanel";

    /** El nombre de la propiedad ligada del arreglo de paneles. */
    public static final String CHOOSER_PANELS_PROPERTY = "chooserPanels";

    private ColorSelectionModel selectionModel;
    private JComponent previewPanel;
    private AbstractColorChooserPanel[] chooserPanels = new AbstractColorChooserPanel[0];
    private boolean dragEnabled;

    /** Un selector con el blanco elegido. */
    public JColorChooser() {
        this(Color.white);
    }

    /**
     * Un selector con ese color elegido.
     *
     * @param initialColor el color inicial
     */
    public JColorChooser(Color initialColor) {
        this(new DefaultColorSelectionModel(initialColor));
    }

    /**
     * Un selector sobre ese modelo.
     *
     * @param model el modelo que comparten los paneles
     */
    public JColorChooser(ColorSelectionModel model) {
        super();
        this.selectionModel = model;
    }

    /** La clave con la que el `LookAndFeel` busca el aspecto: {@code "ColorChooserUI"}. */
    public String getUIClassID() {
        return "ColorChooserUI";
    }

    /** El color elegido. */
    public Color getColor() {
        return this.selectionModel.getSelectedColor();
    }

    /**
     * Elige ese color.
     *
     * @throws NullPointerException si es nulo
     */
    public void setColor(Color color) {
        this.selectionModel.setSelectedColor(color);
    }

    /**
     * Elige el color con esas tres componentes.
     *
     * @throws IllegalArgumentException si alguna se sale de 0..255
     */
    public void setColor(int r, int g, int b) {
        setColor(new Color(r, g, b));
    }

    /**
     * Elige el color empaquetado en un entero, en el formato 0xRRGGBB.
     *
     * <p>Los ocho bits de mas arriba se ignoran: el selector no elige transparencia por esta via.
     */
    public void setColor(int c) {
        setColor(new Color(c & 0xFFFFFF));
    }

    /**
     * Prende o apaga el arrastre del color hacia afuera del selector.
     *
     * <p>Se guarda, pero no hay arrastre: eso lo maneja el aspecto instalado, que aca no hay.
     */
    public void setDragEnabled(boolean b) {
        this.dragEnabled = b;
    }

    /** Si el color se puede arrastrar afuera. */
    public boolean getDragEnabled() {
        return this.dragEnabled;
    }

    /**
     * Fija el panel que muestra el color elegido, o `null` para que no haya ninguno.
     *
     * <p>El JDK distingue `null` de un componente vacio: `null` pide el panel de siempre, y un
     * `JPanel` sin nada adentro es como se pide que no haya vista previa. Aca vale lo mismo.
     */
    public void setPreviewPanel(JComponent preview) {
        this.previewPanel = preview;
    }

    /** El panel de vista previa, o `null` si es el de siempre. */
    public JComponent getPreviewPanel() {
        return this.previewPanel;
    }

    /** Agrega un panel de eleccion al final de los que ya hay. */
    public void addChooserPanel(AbstractColorChooserPanel panel) {
        AbstractColorChooserPanel[] nuevos =
                new AbstractColorChooserPanel[this.chooserPanels.length + 1];
        System.arraycopy(this.chooserPanels, 0, nuevos, 0, this.chooserPanels.length);
        nuevos[this.chooserPanels.length] = panel;
        setChooserPanels(nuevos);
    }

    /**
     * Saca un panel de eleccion.
     *
     * @return el panel que se saco
     * @throws IllegalArgumentException si ese panel no estaba
     */
    public AbstractColorChooserPanel removeChooserPanel(AbstractColorChooserPanel panel) {
        int donde = -1;
        for (int i = 0; i < this.chooserPanels.length; i++) {
            if (this.chooserPanels[i] == panel) {
                donde = i;
                break;
            }
        }
        if (donde < 0) {
            throw new IllegalArgumentException("chooser panel not in this chooser");
        }
        List<AbstractColorChooserPanel> quedan = new ArrayList<AbstractColorChooserPanel>();
        for (int i = 0; i < this.chooserPanels.length; i++) {
            if (i != donde) {
                quedan.add(this.chooserPanels[i]);
            }
        }
        AbstractColorChooserPanel[] nuevos = new AbstractColorChooserPanel[quedan.size()];
        for (int i = 0; i < nuevos.length; i++) {
            nuevos[i] = quedan.get(i);
        }
        setChooserPanels(nuevos);
        panel.uninstallChooserPanel(this);
        return panel;
    }

    /** Reemplaza el juego de paneles de eleccion. */
    public void setChooserPanels(AbstractColorChooserPanel[] panels) {
        this.chooserPanels = panels;
    }

    /** Los paneles de eleccion. */
    public AbstractColorChooserPanel[] getChooserPanels() {
        return this.chooserPanels;
    }

    /** El modelo que comparten los paneles. */
    public ColorSelectionModel getSelectionModel() {
        return this.selectionModel;
    }

    /**
     * Reemplaza el modelo.
     *
     * <p>Los paneles instalados siguen escuchando al **modelo viejo** hasta que se los reinstale;
     * es asi tambien en el JDK.
     */
    public void setSelectionModel(ColorSelectionModel newModel) {
        this.selectionModel = newModel;
    }
}
