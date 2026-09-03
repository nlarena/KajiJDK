package javax.management.openmbean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * El tipo de un {@link TabularData}: filas de un {@link CompositeType} dado, indexadas por algunos
 * de sus items.
 *
 * <p>Es un `Map` descrito de forma que se pueda transmitir: el tipo de la fila dice qué columnas
 * hay, y los nombres de índice dicen cuáles de esas columnas forman la clave. La consecuencia que
 * conviene tener presente es que **la clave sale de la fila**, no se pasa aparte: por eso
 * {@link TabularData#put} toma un solo argumento y {@link TabularData#calculateIndex} existe.
 *
 * <p>Los nombres de índice se guardan en el **orden en que se pasaron** y ese orden es parte del
 * tipo: es el orden en que hay que dar los valores en {@link TabularData#get}. Es la diferencia con
 * {@link CompositeType}, donde el orden de los items no cuenta para nada -- ahí no hay nada que
 * ordenar, acá sí.
 */
public class TabularType extends OpenType<TabularData> {

    private static final long serialVersionUID = 6554071860220659261L;

    private final CompositeType rowType;
    private final List<String> indexNames;

    private transient int hash;

    /**
     * Un tipo tabular con esas filas y esa clave.
     *
     * @throws OpenDataException si algún nombre de índice no es un item del tipo de fila
     * @throws IllegalArgumentException si el tipo de fila o el arreglo son nulos, si el arreglo
     *     está vacío, o si alguno de sus elementos está en blanco
     */
    public TabularType(String typeName, String description, CompositeType rowType,
            String[] indexNames) throws OpenDataException {
        super(TabularData.class.getName(), typeName, description);

        if (rowType == null) {
            throw new IllegalArgumentException("el tipo de fila no puede ser nulo");
        }
        if (indexNames == null || indexNames.length == 0) {
            throw new IllegalArgumentException("hacen falta uno o más names de índice");
        }
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < indexNames.length; i++) {
            String n = indexNames[i];
            if (n == null || n.trim().length() == 0) {
                throw new IllegalArgumentException(
                        "el nombre de índice " + i + " está en blanco");
            }
            n = n.trim();
            // Un índice que no es un item del tipo de fila describiría una clave que ninguna fila
            // puede tener. Se comprueba acá y no al poner la primera fila porque el tipo tiene que
            // ser válido por sí solo -- es lo que se transmite.
            if (!rowType.containsKey(n)) {
                throw new OpenDataException(
                        "el índice " + n + " no es un item del tipo de fila");
            }
            names.add(n);
        }
        this.rowType = rowType;
        this.indexNames = Collections.unmodifiableList(names);
    }

    /** El tipo de las filas. */
    public CompositeType getRowType() {
        return this.rowType;
    }

    /** Los items que forman la clave, en orden y de sólo lectura. */
    public List<String> getIndexNames() {
        return this.indexNames;
    }

    /** Si `obj` es un {@link TabularData} cuyo tipo es éste. */
    public boolean isValue(Object obj) {
        if (!(obj instanceof TabularData)) {
            return false;
        }
        return this.equals(((TabularData) obj).getTabularType());
    }

    /** Igualdad por nombre de tipo, tipo de fila y nombres de índice **en orden**. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TabularType)) {
            return false;
        }
        TabularType other = (TabularType) obj;
        return this.getTypeName().equals(other.getTypeName())
                && this.rowType.equals(other.rowType)
                && this.indexNames.equals(other.indexNames);
    }

    public int hashCode() {
        if (this.hash == 0) {
            int h = this.getTypeName().hashCode() + this.rowType.hashCode();
            for (int i = 0; i < this.indexNames.size(); i++) {
                h = h + this.indexNames.get(i).hashCode();
            }
            this.hash = h;
        }
        return this.hash;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TabularType.class.getName());
        sb.append("(name=").append(this.getTypeName());
        sb.append(",rowType=").append(this.rowType.toString());
        sb.append(",indexNames=(");
        for (int i = 0; i < this.indexNames.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(this.indexNames.get(i));
        }
        sb.append("))");
        return sb.toString();
    }
}
