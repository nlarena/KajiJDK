package javax.swing.plaf.basic;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;

/**
 * El separador de una barra de herramientas: un hueco, y nada dibujado.
 *
 * <p>Es el unico separador que no pinta ninguna linea. En una barra de herramientas lo que separa
 * dos grupos de botones es el aire, no un surco, asi que {@link #paint} esta vacio a proposito y lo
 * unico que importa es el tamano: {@code ToolBar.separatorSize}, medido en Metal (JDK 25) en
 * 10 x 10.
 *
 * <p>{@link #getPreferredSize} lee el tamano del componente en vez de contestar la constante,
 * porque puede ser el que le puso el usuario. Si el separador no tiene ninguno devuelve
 * {@code null}, igual que {@link BasicSeparatorUI#getMinimumSize}.
 */
public class BasicToolBarSeparatorUI extends BasicSeparatorUI {

    /**
     * Un {@link Dimension} pelado, no un {@code DimensionUIResource}.
     *
     * <p>Parece un descuido y esta medido: la tabla del aspecto guarda ahi un tamano sin marcar. La
     * consecuencia es que despues del primer instalado el separador ya no tiene un tamano "del
     * aspecto", asi que un segundo instalado no lo pisa. Marcarlo cambiaria eso y ademas se veria:
     * {@code getSeparatorSize().toString()} dice el nombre de la clase.
     */
    private static final Dimension TAMANIO_POR_OMISION = new Dimension(10, 10);

    public BasicToolBarSeparatorUI() {
    }

    /** Uno nuevo cada vez, como el de la clase de la que sale. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicToolBarSeparatorUI();
    }

    /** El tamano del hueco, solo si el separador no tiene uno propio. */
    protected void installDefaults(JSeparator s) {
        Dimension tamanio = ((JToolBar.Separator) s).getSeparatorSize();
        if (tamanio == null || tamanio instanceof UIResource) {
            ((JToolBar.Separator) s).setSeparatorSize(TAMANIO_POR_OMISION);
        }
    }

    /** Nada; ver la nota de la clase. */
    public void paint(Graphics g, JComponent c) {
    }

    /** El tamano que tenga puesto el separador, o {@code null} si no tiene ninguno. */
    public Dimension getPreferredSize(JComponent c) {
        Dimension tamanio = ((JToolBar.Separator) c).getSeparatorSize();
        if (tamanio != null) {
            return tamanio.getSize();
        }
        return null;
    }
}
