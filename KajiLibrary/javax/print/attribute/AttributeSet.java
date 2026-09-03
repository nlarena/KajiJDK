package javax.print.attribute;

// El javax.print.attribute.AttributeSet de KajiLibrary -- un conjunto de atributos indexado por
// **categoria**.
//
// La regla que define al tipo, y que se ve poco en la firma: el conjunto guarda a lo sumo **un**
// atributo por categoria. `add` de un atributo cuya categoria ya esta reemplaza al que estaba en
// vez de agregar otro. Por eso `get`, `remove` y `containsKey` toman una `Class` (la categoria) y
// `containsValue` toma un `Attribute` (el valor): son dos ejes distintos.
//
// No extiende `java.util.Collection`, aunque se le parezca: la clave es la categoria y el valor el
// atributo, asi que se comporta como un `Map` con la clave metida adentro del valor.
public interface AttributeSet {

    // El atributo de esa categoria, o null si no hay.
    Attribute get(Class<?> category);

    // Agrega el atributo, reemplazando al que hubiera de la misma categoria. Devuelve true si el
    // conjunto cambio -- o sea, si no habia ya un atributo igual en esa categoria.
    boolean add(Attribute attribute);

    // Saca el atributo de esa categoria, si hay. true si el conjunto cambio.
    boolean remove(Class<?> category);

    // Saca el atributo, si esta. true si el conjunto cambio.
    boolean remove(Attribute attribute);

    // Si hay algun atributo de esa categoria.
    boolean containsKey(Class<?> category);

    // Si ese atributo exacto (por equals) esta en el conjunto.
    boolean containsValue(Attribute attribute);

    // Agrega todos los de `attributes`, con la misma regla de reemplazo por categoria.
    boolean addAll(AttributeSet attributes);

    // Cuantas categorias hay -- que es lo mismo que cuantos atributos, por la regla de arriba.
    int size();

    // Los atributos, en un arreglo nuevo. El orden no esta especificado.
    Attribute[] toArray();

    void clear();

    boolean isEmpty();

    // Dos conjuntos son iguales si tienen los mismos atributos. Se redeclara aca, como en el JDK,
    // porque el contrato es mas fuerte que el de Object.
    boolean equals(Object object);

    int hashCode();
}
