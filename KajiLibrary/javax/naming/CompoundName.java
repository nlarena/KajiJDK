package javax.naming;

import java.util.Enumeration;
import java.util.Properties;

/**
 * El nombre **dentro de un** espacio de nombres, con la sintaxis que le dicta el proveedor.
 *
 * <h2>La diferencia con `CompositeName`</h2>
 *
 * <p>`CompositeName` cruza espacios de nombres y por eso su sintaxis es fija. Este vive adentro de
 * uno solo, y ahi la sintaxis es la que el proveedor diga: LDAP separa con coma y cuenta de derecha
 * a izquierda, un sistema de archivos de Windows separa con contrabarra y de izquierda a derecha, y
 * un espacio plano no separa nada. Un componente de un `CompositeName` es, tipicamente, la forma de
 * cadena de un `CompoundName`.
 *
 * <h2>El `Properties` de sintaxis</h2>
 *
 * <p>Las claves que se miran son `jndi.syntax.direction` --`left_to_right`, `right_to_left` o
 * `flat`--, `.separator`, `.separator2`, `.escape`, `.beginquote`, `.endquote`, `.beginquote2`,
 * `.endquote2`, `.ignorecase`, `.trimblanks`, `.ava` y `.typeval`. La direccion es obligatoria y
 * el separador lo es salvo en plano.
 *
 * <p>De derecha a izquierda **no** es cosmetico: en `cn=juan,o=acme` con direccion
 * `right_to_left`, `get(0)` es `o=acme` y no `cn=juan`. El componente 0 es siempre el mas
 * significativo, o sea el mas cercano a la raiz, y de que lado de la cadena cae eso es
 * precisamente lo que la direccion dice. Todo lo que sigue --`getPrefix`, `startsWith`, `add`--
 * habla en indices, asi que hereda esa convencion sin volver a mencionarla.
 *
 * <h2>El `Properties` no se copia, y es a proposito</h2>
 *
 * <p>`mySyntax` guarda el objeto que se paso, no una copia, y `clone` se lo pasa al nombre nuevo.
 * Es lo que hace el JDK real y tiene sentido: un proveedor tiene **una** instancia de sintaxis y
 * la comparte entre todos sus nombres. El precio es que modificar ese `Properties` despues de
 * construir un nombre es un error del que llama; la sintaxis ya quedo leida en el `NameImpl` y el
 * nombre no se entera.
 *
 * <h2>Comparar es asimetrico</h2>
 *
 * <p>`equals`, `compareTo`, `startsWith` y `endsWith` normalizan con la sintaxis **de este**
 * nombre, no la del otro. Si esta ignora mayusculas y la del otro no, `a.equals(b)` puede ser
 * `true` y `b.equals(a)` `false`. Esta en el contrato del JDK y no se puede arreglar sin cambiarlo.
 */
public class CompoundName implements Name {

    private static final long serialVersionUID = 3513100557083972036L;

    /** Ver la nota de `transient` en `CompositeName`: la forma serial es propia. */
    private transient NameImpl impl;

    /**
     * La sintaxis, tal cual la paso el que construyo. `protected` para las subclases y `transient`
     * porque la forma serial escribe las propiedades una por una, no el objeto.
     */
    protected transient Properties mySyntax;

    /** Desde componentes ya partidos. Ver la nota del constructor equivalente en `CompositeName`. */
    protected CompoundName(Enumeration<String> comps, Properties syntax) {
        if (syntax == null) {
            throw new NullPointerException();
        }
        mySyntax = syntax;
        impl = new NameImpl(syntax, comps);
    }

    /** Parsea `n` con `syntax`. La sintaxis es obligatoria: sin ella no hay como partir la cadena. */
    public CompoundName(String n, Properties syntax) throws InvalidNameException {
        if (syntax == null) {
            throw new NullPointerException();
        }
        mySyntax = syntax;
        impl = new NameImpl(syntax, n);
    }

    /** Vuelve a parsearse **con la misma sintaxis**: sin ella la cadena sola no alcanza. */
    @Override
    public String toString() {
        return impl.toString();
    }

    /** Compara componentes, no sintaxis: dos nombres con sintaxis distintas pueden dar `true`. */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof CompoundName)
            && impl.equals(((CompoundName) obj).impl);
    }

    @Override
    public int hashCode() {
        return impl.hashCode();
    }

    /** Comparte el `Properties` con el original; ver la cabecera. */
    @Override
    public Object clone() {
        return new CompoundName(getAll(), mySyntax);
    }

    @Override
    public int compareTo(Object obj) {
        if (!(obj instanceof CompoundName)) {
            throw new ClassCastException("Not a CompoundName");
        }
        return impl.compareTo(((CompoundName) obj).impl);
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

    /** El nombre nuevo hereda esta sintaxis: un prefijo de un nombre LDAP sigue siendo LDAP. */
    @Override
    public Name getPrefix(int posn) {
        return new CompoundName(impl.getPrefix(posn), mySyntax);
    }

    @Override
    public Name getSuffix(int posn) {
        return new CompoundName(impl.getSuffix(posn), mySyntax);
    }

    @Override
    public boolean startsWith(Name n) {
        return (n instanceof CompoundName) && impl.startsWith(n.size(), n.getAll());
    }

    @Override
    public boolean endsWith(Name n) {
        return (n instanceof CompoundName) && impl.endsWith(n.size(), n.getAll());
    }

    @Override
    public Name addAll(Name suffix) throws InvalidNameException {
        if (suffix instanceof CompoundName) {
            impl.addAll(suffix.getAll());
            return this;
        }
        throw new InvalidNameException("Not a compound name: " + suffix.toString());
    }

    @Override
    public Name addAll(int posn, Name n) throws InvalidNameException {
        if (n instanceof CompoundName) {
            impl.addAll(posn, n.getAll());
            return this;
        }
        throw new InvalidNameException("Not a compound name: " + n.toString());
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
