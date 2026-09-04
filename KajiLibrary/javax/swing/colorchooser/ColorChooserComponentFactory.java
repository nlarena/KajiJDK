package javax.swing.colorchooser;

import javax.swing.JComponent;

/**
 * Fabrica los componentes con los que viene armado un {@link javax.swing.JColorChooser}.
 *
 * <p>La usa el aspecto instalado, no el programa: es el punto desde el que un `LookAndFeel` arma el
 * selector por omision --las cuatro solapas y el recuadro de vista previa.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Los dos metodos lanzan {@link UnsupportedOperationException}. Lo que fabrican son componentes
 * **de interfaz**: deslizadores, campos con formato, un diagrama de color que se pinta pixel por
 * pixel. Nada de eso existe en esta biblioteca, y no es una cuestion de escribir mas codigo sino de
 * que no hay pintado ni eventos donde apoyarlo.
 *
 * <p>La alternativa mala seria devolver un arreglo vacio de
 * {@link AbstractColorChooserPanel}: el que llama lo leeria como "este aspecto no trae solapas", que
 * es una respuesta valida y falsa, y armaria un selector vacio sin enterarse de nada.
 */
public class ColorChooserComponentFactory {

    /** No se instancia: es una clase de fabrica. */
    private ColorChooserComponentFactory() {
    }

    /**
     * Las solapas de siempre: RGB, HSV, HSL, CMYK y las muestras.
     *
     * <p><b>No implementado en esta biblioteca.</b> Ver la nota de la clase.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    public static AbstractColorChooserPanel[] getDefaultChooserPanels() {
        throw new UnsupportedOperationException(
                "cannot build the default color chooser panels: they are interactive Swing "
                + "components (sliders, formatted fields, a painted color diagram) and this "
                + "library has no painting or event dispatch");
    }

    /**
     * El recuadro que muestra el color elegido junto al anterior.
     *
     * <p><b>No implementado en esta biblioteca.</b> Ver la nota de la clase.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    public static JComponent getPreviewPanel() {
        throw new UnsupportedOperationException(
                "cannot build the color preview panel: it is a painted Swing component and this "
                + "library has no painting");
    }
}
