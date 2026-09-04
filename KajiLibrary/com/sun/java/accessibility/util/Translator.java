package com.sun.java.accessibility.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeListener;
import java.util.Locale;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleStateSet;

/**
 * Le pone una fachada accesible a un componente que no la tiene.
 *
 * <h2>El problema que resuelve</h2>
 *
 * <p>La accesibilidad es opcional: un componente colabora implementando {@link Accessible}. Los de
 * Swing lo hacen; los de AWT antiguos y los escritos por terceros, muchas veces no — y entonces un
 * lector de pantalla no ve nada de ellos.
 *
 * <p>Esta clase envuelve uno de esos y deriva lo que puede a partir de lo que <em>si</em> tiene: la
 * posicion, el tamano, si esta habilitado, si se ve. Es informacion pobre comparada con la que da un
 * componente que colabora, y es infinitamente mejor que nada.
 *
 * <h2>Como se elige la traduccion</h2>
 *
 * <p>{@link #getAccessible} devuelve el objeto tal cual si ya es {@link Accessible} —no hay nada que
 * traducir— y lo envuelve solo si no lo es. {@link #getTranslatorClass} busca si hay una subclase
 * especializada para ese tipo, que es como se agrega soporte para un componente conocido sin tocar
 * esta clase.
 *
 * <h2>Lo que no puede inventar</h2>
 *
 * <p>{@link #getAccessibleRole} devuelve {@link AccessibleRole#UNKNOWN} y
 * {@link #getAccessibleName} sale del nombre del componente. No hay forma de deducir que un
 * rectangulo gris es un boton, y decir que lo es seria peor que decir que no se sabe: un lector de
 * pantalla anunciaria un control que no se puede activar.
 */
public class Translator extends AccessibleContext implements Accessible, AccessibleComponent {

    /** El objeto envuelto. */
    protected Object source;

    /** Sin fuente todavia; hay que ponersela con {@link #setSource}. */
    public Translator() {
    }

    /** Envolviendo ese objeto. */
    public Translator(Object o) {
        this.source = o;
    }

    /**
     * La subclase de {@code Translator} especializada para ese tipo, o {@code null}.
     *
     * <p>Busca por convencion de nombre en este mismo paquete. Devolver {@code null} —lo normal—
     * significa que se usa esta clase generica.
     */
    protected static Class<?> getTranslatorClass(Class<?> c) {
        if (c == null) {
            return null;
        }
        try {
            return Class.forName("com.sun.java.accessibility.util." + c.getSimpleName()
                    + "Translator");
        } catch (ClassNotFoundException e) {
            return getTranslatorClass(c.getSuperclass());
        }
    }

    /**
     * El objeto visto como {@link Accessible}: el mismo si ya lo era, o envuelto si no.
     *
     * @return {@code null} si {@code o} es {@code null}
     */
    public static Accessible getAccessible(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Accessible) {
            return (Accessible) o;
        }
        Class<?> especializado = getTranslatorClass(o.getClass());
        if (especializado != null) {
            try {
                Translator t = (Translator) especializado.getDeclaredConstructor().newInstance();
                t.setSource(o);
                return t;
            } catch (Exception e) {
                // Una traduccion especializada que no se puede construir no invalida la generica:
                // peor que una fachada pobre es ninguna.
                return new Translator(o);
            }
        }
        return new Translator(o);
    }

    /** El objeto envuelto. */
    public Object getSource() {
        return this.source;
    }

    /** Cambia el objeto envuelto. */
    public void setSource(Object o) {
        this.source = o;
    }

    /**
     * Por la fuente envuelta, no por identidad.
     *
     * <p>Dos traductores del mismo componente representan lo mismo, y como se crean al vuelo en cada
     * consulta, compararlos por identidad daria siempre distinto.
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Translator)) {
            return false;
        }
        Object otra = ((Translator) o).getSource();
        return this.source == null ? otra == null : this.source.equals(otra);
    }

    public int hashCode() {
        return this.source == null ? 0 : this.source.hashCode();
    }

    /** Esta misma clase: es a la vez la fachada y su contexto. */
    public AccessibleContext getAccessibleContext() {
        return this;
    }

    private Component componente() {
        return this.source instanceof Component ? (Component) this.source : null;
    }

    /** El nombre del componente, que es lo unico que hay de donde sacarlo. */
    public String getAccessibleName() {
        Component c = componente();
        return c == null ? null : c.getName();
    }

    /** Cambia el nombre del componente. */
    public void setAccessibleName(String s) {
        Component c = componente();
        if (c != null) {
            c.setName(s);
        }
    }

    /** {@code null}: un componente que no colabora no tiene descripcion que dar. */
    public String getAccessibleDescription() {
        return null;
    }

    /** No hace nada: no hay donde guardarla. */
    public void setAccessibleDescription(String s) {
    }

    /** {@link AccessibleRole#UNKNOWN}; ver la nota de la clase sobre por que no se adivina. */
    public AccessibleRole getAccessibleRole() {
        return AccessibleRole.UNKNOWN;
    }

    /** El estado que se puede derivar del componente: habilitado, visible, con foco. */
    public AccessibleStateSet getAccessibleStateSet() {
        AccessibleStateSet s = new AccessibleStateSet();
        Component c = componente();
        if (c == null) {
            return s;
        }
        if (c.isEnabled()) {
            s.add(javax.accessibility.AccessibleState.ENABLED);
        }
        if (c.isVisible()) {
            s.add(javax.accessibility.AccessibleState.VISIBLE);
        }
        if (c.isShowing()) {
            s.add(javax.accessibility.AccessibleState.SHOWING);
        }
        if (c.isFocusOwner()) {
            s.add(javax.accessibility.AccessibleState.FOCUSED);
        }
        return s;
    }

    /** El padre, tambien traducido si hace falta. */
    public Accessible getAccessibleParent() {
        Component c = componente();
        return c == null ? null : getAccessible(c.getParent());
    }

    /** La posicion entre los hermanos, o {@code -1}. */
    public int getAccessibleIndexInParent() {
        Component c = componente();
        if (c == null || c.getParent() == null) {
            return -1;
        }
        Component[] hermanos = c.getParent().getComponents();
        for (int i = 0; i < hermanos.length; i++) {
            if (hermanos[i] == c) {
                return i;
            }
        }
        return -1;
    }

    /** Cuantos hijos tiene, si es un contenedor. */
    public int getAccessibleChildrenCount() {
        return this.source instanceof java.awt.Container
                ? ((java.awt.Container) this.source).getComponentCount()
                : 0;
    }

    /** El hijo {@code i}, traducido. */
    public Accessible getAccessibleChild(int i) {
        if (!(this.source instanceof java.awt.Container)) {
            return null;
        }
        java.awt.Container cont = (java.awt.Container) this.source;
        if (i < 0 || i >= cont.getComponentCount()) {
            return null;
        }
        return getAccessible(cont.getComponent(i));
    }

    /**
     * El locale del componente.
     *
     * @throws IllegalComponentStateException si el componente no tiene uno todavia
     */
    public Locale getLocale() throws IllegalComponentStateException {
        Component c = componente();
        return c == null ? Locale.getDefault() : c.getLocale();
    }

    /** No hace nada: un componente que no colabora no emite cambios de propiedad accesible. */
    public void addPropertyChangeListener(PropertyChangeListener l) {
    }

    /** No hace nada, por lo mismo. */
    public void removePropertyChangeListener(PropertyChangeListener l) {
    }

    /** El color de fondo. */
    public Color getBackground() {
        Component c = componente();
        return c == null ? null : c.getBackground();
    }

    /** Cambia el color de fondo. */
    public void setBackground(Color color) {
        Component c = componente();
        if (c != null) {
            c.setBackground(color);
        }
    }

    /** El color de frente. */
    public Color getForeground() {
        Component c = componente();
        return c == null ? null : c.getForeground();
    }

    /** Cambia el color de frente. */
    public void setForeground(Color color) {
        Component c = componente();
        if (c != null) {
            c.setForeground(color);
        }
    }

    /** El cursor. */
    public Cursor getCursor() {
        Component c = componente();
        return c == null ? null : c.getCursor();
    }

    /** Cambia el cursor. */
    public void setCursor(Cursor cursor) {
        Component c = componente();
        if (c != null) {
            c.setCursor(cursor);
        }
    }

    /** La tipografia. */
    public Font getFont() {
        Component c = componente();
        return c == null ? null : c.getFont();
    }

    /** Cambia la tipografia. */
    public void setFont(Font f) {
        Component c = componente();
        if (c != null) {
            c.setFont(f);
        }
    }

    /** Las metricas de esa tipografia. */
    public FontMetrics getFontMetrics(Font f) {
        Component c = componente();
        return c == null ? null : c.getFontMetrics(f);
    }

    /** Si esta habilitado. */
    public boolean isEnabled() {
        Component c = componente();
        return c != null && c.isEnabled();
    }

    /** Lo habilita o deshabilita. */
    public void setEnabled(boolean b) {
        Component c = componente();
        if (c != null) {
            c.setEnabled(b);
        }
    }

    /** Si esta marcado como visible. */
    public boolean isVisible() {
        Component c = componente();
        return c != null && c.isVisible();
    }

    /** Lo muestra o lo oculta. */
    public void setVisible(boolean b) {
        Component c = componente();
        if (c != null) {
            c.setVisible(b);
        }
    }

    /**
     * Si de verdad se ve.
     *
     * <p>Distinto de {@link #isVisible}: un componente visible dentro de una ventana cerrada no se
     * muestra, y para una tecnologia de asistencia esa es la diferencia entre leerlo y no.
     */
    public boolean isShowing() {
        Component c = componente();
        return c != null && c.isShowing();
    }

    /** Si ese punto, relativo al componente, cae adentro. */
    public boolean contains(Point p) {
        Component c = componente();
        return c != null && c.contains(p);
    }

    /** Donde esta en la pantalla. */
    public Point getLocationOnScreen() {
        Component c = componente();
        return c == null ? null : c.getLocationOnScreen();
    }

    /** Donde esta dentro de su contenedor. */
    public Point getLocation() {
        Component c = componente();
        return c == null ? null : c.getLocation();
    }

    /** Lo mueve. */
    public void setLocation(Point p) {
        Component c = componente();
        if (c != null) {
            c.setLocation(p);
        }
    }

    /** Su rectangulo. */
    public Rectangle getBounds() {
        Component c = componente();
        return c == null ? null : c.getBounds();
    }

    /** Cambia su rectangulo. */
    public void setBounds(Rectangle r) {
        Component c = componente();
        if (c != null) {
            c.setBounds(r);
        }
    }

    /** Su tamano. */
    public Dimension getSize() {
        Component c = componente();
        return c == null ? null : c.getSize();
    }

    /** Cambia su tamano. */
    public void setSize(Dimension d) {
        Component c = componente();
        if (c != null) {
            c.setSize(d);
        }
    }

    /** El hijo accesible que esta en ese punto. */
    public Accessible getAccessibleAt(Point p) {
        if (!(this.source instanceof java.awt.Container)) {
            return null;
        }
        Component hijo = ((java.awt.Container) this.source).getComponentAt(p);
        return hijo == null || hijo == this.source ? null : getAccessible(hijo);
    }

    /** Si puede recibir el foco con el tabulador. */
    public boolean isFocusTraversable() {
        Component c = componente();
        return c != null && c.isFocusable();
    }

    /** Le pide el foco. */
    public void requestFocus() {
        Component c = componente();
        if (c != null) {
            c.requestFocus();
        }
    }

    /** Escucha los cambios de foco del componente. */
    public synchronized void addFocusListener(FocusListener l) {
        Component c = componente();
        if (c != null) {
            c.addFocusListener(l);
        }
    }

    /** Deja de escucharlos. */
    public synchronized void removeFocusListener(FocusListener l) {
        Component c = componente();
        if (c != null) {
            c.removeFocusListener(l);
        }
    }
}
