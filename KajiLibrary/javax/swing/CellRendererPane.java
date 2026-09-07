package javax.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * El lugar donde viven los componentes que dibujan celdas.
 *
 * <h2>Por que hace falta un contenedor que no contiene</h2>
 *
 * <p>Una lista de mil elementos no tiene mil componentes: tiene <em>uno</em>, que se configura y se
 * dibuja mil veces en mil lugares distintos. Ese componente prestado tiene que estar en algun lado
 * -- Swing pide que un componente tenga padre para medirse y para dibujarse --, pero no tiene que
 * participar de nada: no se lo puede recorrer con el tabulador, no se lo repinta cuando cambia, no
 * hereda la validacion.
 *
 * <p>Este panel es ese lugar. Es un contenedor de verdad, pero <strong>desactivado a proposito</strong>:
 *
 * <ul>
 * <li>{@link #invalidate} no hace nada -- un dibujante que se invalida no tiene que invalidar a la
 *     lista entera;</li>
 * <li>{@link #paint} y {@link #update} no hacen nada -- los dibujantes los pinta quien los usa,
 *     cuando le toca, y no este panel por su cuenta;</li>
 * <li>{@link #addImpl} saca al componente de donde estuviera antes de agregarlo.</li>
 * </ul>
 *
 * <h2>Como se lo usa</h2>
 *
 * <p>Con {@link #paintComponent}: se le pasa el dibujante ya configurado, el {@code Graphics} de la
 * lista y el rectangulo donde va. El panel lo coloca, lo dibuja ahi y lo deja como estaba. El
 * componente nunca se entera de que fue dibujado mil veces.
 */
public class CellRendererPane extends Container implements Accessible {

    protected AccessibleContext accessibleContext = null;

    /** Un panel vacio, invisible y sin acomodador. */
    public CellRendererPane() {
        super();
        setLayout(null);
        setVisible(false);
    }

    /** No hace nada; ver la nota de la clase. */
    public void invalidate() {
    }

    /** No hace nada; ver la nota de la clase. */
    public void paint(Graphics g) {
    }

    /** No hace nada; ni siquiera borra el fondo. */
    public void update(Graphics g) {
    }

    /**
     * Agrega el componente, sacandolo antes de donde estuviera.
     *
     * <p>Un dibujante que se comparte entre dos listas terminaria de padre en la ultima que lo uso;
     * esto lo mueve en vez de dejarlo en las dos.
     */
    protected void addImpl(Component x, Object constraints, int index) {
        if (x.getParent() == this) {
            return;
        }
        super.addImpl(x, constraints, index);
    }

    /**
     * Dibuja el componente en ese rectangulo del {@code Graphics} dado.
     *
     * <p>Con {@code shouldValidate} en cierto se lo valida antes; hace falta cuando el dibujante
     * tiene hijos que acomodar, y es caro, por eso no es lo de siempre.
     */
    public void paintComponent(Graphics g, Component c, Container p, int x, int y, int w, int h,
            boolean shouldValidate) {
        if (c == null) {
            if (p != null) {
                Color fondo = p.getBackground();
                g.setColor(fondo);
                g.fillRect(x, y, w, h);
            }
            return;
        }
        if (c.getParent() != this) {
            this.add(c);
        }
        c.setBounds(x, y, w, h);
        if (shouldValidate) {
            c.validate();
        }
        // Se traslada el origen en vez de pedirle un Graphics propio al componente: el componente no
        // esta en pantalla y no tiene uno.
        Graphics cg = g.create(x, y, w, h);
        try {
            c.paint(cg);
        } finally {
            cg.dispose();
        }
        // Se lo saca de donde quedo para que un repintado de la lista no lo dibuje otra vez por su
        // cuenta, ahora en el ultimo lugar donde estuvo.
        c.setBounds(-w, -h, 0, 0);
    }

    /** Sin validar. */
    public void paintComponent(Graphics g, Component c, Container p, int x, int y, int w, int h) {
        paintComponent(g, c, p, x, y, w, h, false);
    }

    /** Con el rectangulo dado de una. */
    public void paintComponent(Graphics g, Component c, Container p, Rectangle r) {
        paintComponent(g, c, p, r.x, r.y, r.width, r.height);
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
