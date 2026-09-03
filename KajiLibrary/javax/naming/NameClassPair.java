package javax.naming;

/**
 * Lo que devuelve `Context.list()`: el nombre de una atadura y el nombre de la clase de lo que hay
 * atado, **sin traer el objeto**.
 *
 * <p>Esa es toda la razon de que el tipo exista y de que `list` y `listBindings` sean dos metodos
 * distintos. Listar un contexto con mil ataduras y materializar los mil objetos --abrir mil
 * conexiones, deserializar mil grafos-- para despues mirar el nombre de uno solo es exactamente lo
 * que hay que evitar. `list` devuelve estos pares, que son metadatos que el servidor ya tenia; el
 * que quiere el objeto hace un `lookup` puntual, o usa `listBindings` y recibe `Binding`, que es
 * esta misma clase mas el objeto.
 *
 * <h2>El nombre es relativo, salvo cuando no</h2>
 *
 * <p>`getName()` devuelve un nombre **relativo al contexto que se listo**, no absoluto: listar
 * `ou=gente` da `cn=juan`, no `cn=juan,ou=gente`. `isRelative()` es `false` en el unico caso donde
 * no se puede: cuando la atadura apunta afuera del contexto y el nombre es una URL. Esa es la
 * razon de que el flag exista, y por eso `toString()` lo marca: un consumidor que arme
 * `contexto + "/" + nombre` a ciegas produciria basura para esas entradas.
 *
 * <p>`getNameInNamespace()` es el nombre absoluto, y es **opcional**: tira
 * `UnsupportedOperationException` si el proveedor no lo lleno. No devuelve `null` porque `null` se
 * confundiria con "el nombre absoluto es vacio", que es lo que vale para la raiz.
 */
public class NameClassPair implements java.io.Serializable {

    private static final long serialVersionUID = 5620776610160863339L;

    private String name;
    private String className;
    private String fullName;
    private boolean isRel;

    /** Relativo por default, que es el caso normal: casi ninguna atadura apunta afuera. */
    public NameClassPair(String name, String className) {
        this(name, className, true);
    }

    public NameClassPair(String name, String className, boolean isRelative) {
        this.name = name;
        this.className = className;
        this.isRel = isRelative;
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

    /**
     * El nombre absoluto, si el proveedor lo puso.
     *
     * @throws UnsupportedOperationException si no lo puso
     */
    public String getNameInNamespace() {
        if (fullName == null) {
            throw new UnsupportedOperationException();
        }
        return fullName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setClassName(String name) {
        this.className = name;
    }

    public void setNameInNamespace(String fullName) {
        this.fullName = fullName;
    }

    public boolean isRelative() {
        return isRel;
    }

    public void setRelative(boolean r) {
        this.isRel = r;
    }

    /** Marca lo no relativo al frente: es la unica diferencia que cambia como se usa el nombre. */
    @Override
    public String toString() {
        return (isRelative() ? "" : "(not relative)") + getName() + ": " + getClassName();
    }
}
