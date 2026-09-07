import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.ComponentUI;

/**
 * Una interfaz grafica de mentira que anota lo que le piden.
 *
 * <p>La usa {@code java/MUL1.java} para comprobar que {@code javax.swing.plaf.multi} reparte: se
 * registran dos, una como aspecto principal y otra como auxiliar, y se cuenta cuantas llamadas
 * recibio cada una.
 *
 * <p>Esta en su propio archivo porque {@code UIDefaults.getUI} la busca por nombre con
 * {@code Class.forName}, y para eso tiene que ser una clase con nombre propio.
 */
public class MUL1x extends ButtonUI {

    /** Cuantas llamadas recibio la primera instancia; se resetea desde la prueba. */
    public static int llamadasA;

    /** Cuantas recibio la segunda. */
    public static int llamadasB;

    /** Cual de las dos es esta: la del aspecto principal o la del auxiliar. */
    private final boolean esA;

    /** Que devuelve cuando le preguntan un tamano; distinto por instancia, para distinguirlas. */
    private final int ancho;

    public MUL1x(boolean esA, int ancho) {
        this.esA = esA;
        this.ancho = ancho;
    }

    /** La instancia del aspecto principal. */
    public static ComponentUI createUI(JComponent c) {
        return new MUL1x(true, 10);
    }

    private void anotar() {
        if (esA) {
            llamadasA++;
        } else {
            llamadasB++;
        }
    }

    @Override
    public void installUI(JComponent c) {
        anotar();
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        anotar();
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        anotar();
        return new Dimension(ancho, ancho);
    }

    @Override
    public boolean contains(JComponent c, int x, int y) {
        anotar();
        return esA;
    }
}
