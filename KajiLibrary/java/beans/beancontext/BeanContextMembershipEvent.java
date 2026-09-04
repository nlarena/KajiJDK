package java.beans.beancontext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Los hijos que se agregaron o se quitaron de un contexto.
 *
 * <p>Un solo evento lleva **varios** hijos y no uno, y eso es deliberado: `addAll` y `removeAll` son
 * una operación, no N. Un oyente que reacomoda algo al cambiar la membresía quiere hacerlo una vez
 * con la lista completa, no una vez por hijo con estados intermedios que nunca existieron para
 * quien hizo el cambio.
 *
 * <p>La colección se copia al construir. Sin copiar, quien pasó la lista podría cambiarla mientras
 * los oyentes la recorren, y dos oyentes verían cosas distintas del mismo evento.
 */
public class BeanContextMembershipEvent extends BeanContextEvent {

    /** Los hijos que el evento nombra. */
    protected Collection children;

    /** El evento con esos hijos. */
    public BeanContextMembershipEvent(BeanContext bc, Collection changes) {
        super(bc);
        if (changes == null) {
            throw new NullPointerException("changes");
        }
        List<Object> copia = new ArrayList<Object>();
        Iterator it = changes.iterator();
        while (it.hasNext()) {
            copia.add(it.next());
        }
        this.children = copia;
    }

    /** El evento con esos hijos. */
    public BeanContextMembershipEvent(BeanContext bc, Object[] changes) {
        super(bc);
        if (changes == null) {
            throw new NullPointerException("changes");
        }
        List<Object> copia = new ArrayList<Object>();
        for (int i = 0; i < changes.length; i++) {
            copia.add(changes[i]);
        }
        this.children = copia;
    }

    /** Cuántos hijos nombra. */
    public int size() {
        return this.children.size();
    }

    /** Si ese objeto es uno de los hijos que nombra. */
    public boolean contains(Object child) {
        return this.children.contains(child);
    }

    /** Los hijos, en un arreglo. */
    public Object[] toArray() {
        return this.children.toArray();
    }

    /** Los hijos, uno por uno. */
    public Iterator iterator() {
        return this.children.iterator();
    }
}
