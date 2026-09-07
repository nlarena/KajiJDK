package javax.swing;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Una secuencia hecha de una lista fija, para un {@link JSpinner}.
 *
 * <h2>La lista no puede estar vacia</h2>
 *
 * <p>Todos los constructores y {@link #setList} rechazan nulo y vacio. El motivo es que el modelo
 * guarda una <em>posicion</em>, no un valor, y {@link #getValue} lee la lista en esa posicion: con
 * la lista vacia no habria nada que devolver y cada consulta reventaria. Es mejor que reviente el
 * que la vacia.
 *
 * <h2>El valor se busca por igualdad</h2>
 *
 * <p>{@link #setValue} no guarda lo que se le da: busca donde esta en la lista y guarda esa
 * posicion. Un valor que no esta en la lista es un error, no un valor nuevo. Y si la lista tiene
 * repetidos, gana el primero.
 *
 * <p>Cambiar la lista vuelve la posicion a cero, aunque el valor de antes siga estando: la posicion
 * vieja no significa lo mismo en una lista nueva.
 */
public class SpinnerListModel extends AbstractSpinnerModel implements Serializable {

    private List<?> list;
    private int index;

    /**
     * Con esa lista.
     *
     * @throws IllegalArgumentException si es nula o vacia.
     */
    public SpinnerListModel(List<?> values) {
        if (values == null || values.size() == 0) {
            throw new IllegalArgumentException(
                    "SpinnerListModel(List) expects non-null non-empty List");
        }
        this.list = values;
        this.index = 0;
    }

    /**
     * Con ese arreglo.
     *
     * @throws IllegalArgumentException si es nulo o vacio.
     */
    public SpinnerListModel(Object[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException(
                    "SpinnerListModel(Object[]) expects non-null non-empty Object[]");
        }
        this.list = Arrays.asList(values);
        this.index = 0;
    }

    /** Con un solo elemento de relleno, que es lo que pone el JDK. */
    public SpinnerListModel() {
        this(new Object[] {"empty"});
    }

    /** La lista; no es copia. */
    public List<?> getList() {
        return list;
    }

    /**
     * Cambia la lista y vuelve al primero.
     *
     * @throws IllegalArgumentException si es nula o vacia.
     */
    public void setList(List<?> list) {
        if ((list == null) || (list.size() == 0)) {
            throw new IllegalArgumentException("invalid list");
        }
        if (!list.equals(this.list)) {
            this.list = list;
            index = 0;
            fireStateChanged();
        }
    }

    public Object getValue() {
        return list.get(index);
    }

    /**
     * Se para en ese elemento.
     *
     * @throws IllegalArgumentException si no esta en la lista.
     */
    public void setValue(Object elt) {
        int index = list.indexOf(elt);
        if (index == -1) {
            throw new IllegalArgumentException("invalid sequence element");
        } else if (index != this.index) {
            this.index = index;
            fireStateChanged();
        }
    }

    /** El siguiente, o nulo si ya esta en el ultimo. */
    public Object getNextValue() {
        return (index >= (list.size() - 1)) ? null : list.get(index + 1);
    }

    /** El anterior, o nulo si ya esta en el primero. */
    public Object getPreviousValue() {
        return (index <= 0) ? null : list.get(index - 1);
    }

    /**
     * El primer elemento a partir del actual cuyo texto empieza con ese prefijo.
     *
     * <p>Es lo que usa el editor para saltar escribiendo. Da la vuelta al llegar al final, asi que
     * teclear la misma letra recorre todos los que empiezan con ella.
     */
    Object findNextMatch(String prefix) {
        int max = list.size();
        if (max == 0) {
            return null;
        }
        int counter = index;
        do {
            Object value = list.get(counter);
            String string = value.toString();
            if (string != null && string.startsWith(prefix)) {
                return value;
            }
            counter = (counter + 1) % max;
        } while (counter != index);
        return null;
    }
}
