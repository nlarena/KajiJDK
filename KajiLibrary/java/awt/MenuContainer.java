package java.awt;

/**
 * Algo que puede contener elementos de menú.
 *
 * <p>Lo implementan {@link Menu}, {@link MenuBar} y {@link Frame} — los tres lugares donde un menú
 * puede colgar. Que la interfaz sea tan chica no es pobreza: es todo lo que un hijo necesita saber
 * de su contenedor, y mantenerla así permite que un marco y un menú, que no se parecen en nada más,
 * sirvan igual de padres.
 */
public interface MenuContainer {

    /** La fuente con la que se dibujan los hijos. */
    Font getFont();

    /** Saca ese hijo. */
    void remove(MenuComponent comp);

    /**
     * Le manda un evento del modelo viejo.
     *
     * @deprecated es del modelo de eventos de 1.0. Se mantiene porque está en la interfaz.
     */
    @Deprecated
    boolean postEvent(Event e);
}
