package javax.naming.event;

import java.util.EventObject;
import javax.naming.Binding;

/**
 * KajiLibrary's javax.naming.event.NamingEvent -- algo cambio en el contexto.
 *
 * <p>Un tipo --de los cuatro de abajo-- y hasta dos asociaciones: como estaba la entrada y como
 * quedo. Cuales de las dos vienen depende del tipo, y es lo primero que hay que saber para usarlo:
 *
 * <ul>
 *   <li>{@link #OBJECT_ADDED}: solo la nueva;
 *   <li>{@link #OBJECT_REMOVED}: solo la vieja;
 *   <li>{@link #OBJECT_RENAMED} y {@link #OBJECT_CHANGED}: las dos, salvo que el cambio cruce el
 *       borde del alcance suscrito, y ahi falta la de afuera.
 * </ul>
 *
 * <p>Los campos son {@code protected} y no privados, igual que en el JDK: la clase es de 1999 y sus
 * subclases del proveedor los tocan directo.
 *
 * <p>{@link #getChangeInfo} devuelve lo que el proveedor quiera agregar --un numero de cambio de
 * LDAP, por ejemplo-- y es especifico de cada uno. Depender de el ata el programa a un proveedor.
 */
public class NamingEvent extends EventObject {

    private static final long serialVersionUID = 2716268041038319063L;

    /** Aparecio una entrada. */
    public static final int OBJECT_ADDED = 0;

    /** Desaparecio una. */
    public static final int OBJECT_REMOVED = 1;

    /** Una cambio de nombre. */
    public static final int OBJECT_RENAMED = 2;

    /** Cambio el contenido de una. */
    public static final int OBJECT_CHANGED = 3;

    /** Lo que el proveedor quiera agregar; ver la nota de la clase. */
    protected Object changeInfo;

    /** Cual de los cuatro. */
    protected int type;

    /** Como estaba, o null. */
    protected Binding oldBinding;

    /** Como quedo, o null. */
    protected Binding newBinding;

    /**
     * @param source el contexto donde paso
     * @param type uno de los cuatro de arriba
     * @param newBd como quedo; null si desaparecio
     * @param oldBd como estaba; null si es nueva
     * @param changeInfo lo que el proveedor quiera agregar, o null
     */
    public NamingEvent(EventContext source, int type, Binding newBd, Binding oldBd,
                       Object changeInfo) {
        super(source);
        this.type = type;
        this.changeInfo = changeInfo;
        this.oldBinding = oldBd;
        this.newBinding = newBd;
    }

    /** Cual de los cuatro. */
    public int getType() {
        return this.type;
    }

    /** El contexto donde paso. */
    public EventContext getEventContext() {
        return (EventContext) getSource();
    }

    /** Como estaba, o null. Ver la nota de la clase. */
    public Binding getOldBinding() {
        return this.oldBinding;
    }

    /** Como quedo, o null. */
    public Binding getNewBinding() {
        return this.newBinding;
    }

    /** Lo que el proveedor agrego, o null. */
    public Object getChangeInfo() {
        return this.changeInfo;
    }

    /**
     * Se despacha al metodo que corresponde a su tipo.
     *
     * <p>Un oyente que no implementa la interfaz del tipo del evento no recibe nada: el
     * {@code instanceof} lo filtra en vez de tirar. Es lo correcto -- un repartidor puede tener una
     * lista mezclada de oyentes y no tiene por que saber cual escucha que.
     */
    public void dispatch(NamingListener listener) {
        switch (this.type) {
            case OBJECT_ADDED:
                if (listener instanceof NamespaceChangeListener) {
                    ((NamespaceChangeListener) listener).objectAdded(this);
                }
                break;
            case OBJECT_REMOVED:
                if (listener instanceof NamespaceChangeListener) {
                    ((NamespaceChangeListener) listener).objectRemoved(this);
                }
                break;
            case OBJECT_RENAMED:
                if (listener instanceof NamespaceChangeListener) {
                    ((NamespaceChangeListener) listener).objectRenamed(this);
                }
                break;
            case OBJECT_CHANGED:
                if (listener instanceof ObjectChangeListener) {
                    ((ObjectChangeListener) listener).objectChanged(this);
                }
                break;
            default:
                break;
        }
    }
}
