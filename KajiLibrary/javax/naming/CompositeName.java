package javax.naming;

import java.util.Enumeration;

/**
 * El nombre que **atraviesa** espacios de nombres, con sintaxis fija.
 *
 * <h2>Que es un nombre compuesto</h2>
 *
 * <p>`"jdbc/pool/ventas"` no vive en un solo espacio de nombres: `jdbc` puede resolverlo un
 * contexto y `pool/ventas` otro, de un proveedor distinto y con su propia sintaxis. Un nombre
 * compuesto es la secuencia de esos tramos. Cada componente es un nombre **para otro sistema**, y
 * esta clase no sabe --ni tiene por que-- que significa adentro.
 *
 * <p>Por eso la sintaxis es fija y no configurable, que es la unica diferencia real con
 * `CompoundName`: si dependiera del proveedor no habria manera de escribir un nombre que cruce
 * dos. Es siempre: separador `/`, escape `\`, comillas `"` y `'`, de izquierda a derecha, sin
 * ignorar mayusculas ni recortar blancos. Esos son literalmente los valores por default de
 * `NameImpl`, y por eso esta clase le pasa `null` como sintaxis.
 *
 * <h2>Componentes vacios, que es donde todos se equivocan</h2>
 *
 * <p>Un separador al final agrega un componente vacio: `"a/"` tiene **dos** componentes, `"a"` y
 * `""`. Pero `"/"` tiene **uno** --el vacio-- y no dos, y `""` tiene **cero**. La regla que hace
 * consistentes a los tres es que un nombre cuyos componentes son todos vacios se imprime con un
 * separador de mas, para que `""` y `{""}` no colapsen en la misma cadena. Toda esa aritmetica esta
 * en `NameImpl`; lo que importa aca es que un componente vacio es un componente de verdad y no un
 * artefacto del parseo.
 *
 * <p>Como todos los `Name`, es **mutable**: `add`, `addAll` y `remove` cambian esto y devuelven
 * `this`.
 */
public class CompositeName implements Name {

    private static final long serialVersionUID = 1667768148915813118L;

    /**
     * `transient` porque la forma serial de esta clase es propia --cantidad de componentes y
     * despues cada uno-- y no el volcado del `NameImpl`. Este arbol no tiene `ObjectOutputStream`,
     * asi que esa forma no esta escrita; lo que se sostiene es que el campo no entre en la forma
     * default, que es lo que el JDK real declara.
     */
    private transient NameImpl impl;

    /**
     * Construye desde componentes ya partidos, sin parsear.
     *
     * <p>Es `protected` porque es el constructor que usan las subclases y los metodos de esta
     * misma clase que devuelven nombres nuevos --`getPrefix`, `getSuffix`, `clone`--: ahi los
     * componentes ya estan separados y volver a parsear su forma de cadena seria, ademas de
     * caro, un ida y vuelta que puede perder informacion.
     */
    protected CompositeName(Enumeration<String> comps) {
        impl = new NameImpl(null, comps);
    }

    /** Parsea la cadena con la sintaxis fija del nombre compuesto. */
    public CompositeName(String n) throws InvalidNameException {
        impl = new NameImpl(null, n);
    }

    /** El nombre vacio: cero componentes. */
    public CompositeName() {
        impl = new NameImpl(null);
    }

    /** Vuelve a parsearse: `new CompositeName(x.toString())` es igual a `x`. */
    @Override
    public String toString() {
        return impl.toString();
    }

    /** Solo contra otro `CompositeName`: un `CompoundName` con los mismos componentes no es igual. */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof CompositeName)
            && impl.equals(((CompositeName) obj).impl);
    }

    @Override
    public int hashCode() {
        return impl.hashCode();
    }

    /** Toma `Object` por la edad de la interfaz; tira `ClassCastException` si no es uno de estos. */
    @Override
    public int compareTo(Object obj) {
        if (!(obj instanceof CompositeName)) {
            throw new ClassCastException("Not a CompositeName");
        }
        return impl.compareTo(((CompositeName) obj).impl);
    }

    /** Copia con lista de componentes propia; los componentes son `String` y no hace falta copiarlos. */
    @Override
    public Object clone() {
        return new CompositeName(getAll());
    }

    @Override
    public int size() {
        return impl.size();
    }

    @Override
    public boolean isEmpty() {
        return impl.isEmpty();
    }

    @Override
    public Enumeration<String> getAll() {
        return impl.getAll();
    }

    @Override
    public String get(int posn) {
        return impl.get(posn);
    }

    @Override
    public Name getPrefix(int posn) {
        return new CompositeName(impl.getPrefix(posn));
    }

    @Override
    public Name getSuffix(int posn) {
        return new CompositeName(impl.getSuffix(posn));
    }

    // Los cuatro que comparan con otro nombre devuelven `false` --o tiran-- si el otro no es
    // compuesto. Un `CompoundName` con los mismos componentes significa otra cosa: sus
    // componentes son de un solo espacio de nombres y estos de varios.

    @Override
    public boolean startsWith(Name n) {
        return (n instanceof CompositeName) && impl.startsWith(n.size(), n.getAll());
    }

    @Override
    public boolean endsWith(Name n) {
        return (n instanceof CompositeName) && impl.endsWith(n.size(), n.getAll());
    }

    @Override
    public Name addAll(Name suffix) throws InvalidNameException {
        if (suffix instanceof CompositeName) {
            impl.addAll(suffix.getAll());
            return this;
        }
        throw new InvalidNameException("Not a composite name: " + suffix.toString());
    }

    @Override
    public Name addAll(int posn, Name n) throws InvalidNameException {
        if (n instanceof CompositeName) {
            impl.addAll(posn, n.getAll());
            return this;
        }
        throw new InvalidNameException("Not a composite name: " + n.toString());
    }

    @Override
    public Name add(String comp) throws InvalidNameException {
        impl.add(comp);
        return this;
    }

    @Override
    public Name add(int posn, String comp) throws InvalidNameException {
        impl.add(posn, comp);
        return this;
    }

    @Override
    public Object remove(int posn) throws InvalidNameException {
        return impl.remove(posn);
    }
}
