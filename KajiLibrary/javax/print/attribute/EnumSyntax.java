package javax.print.attribute;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

// La clase de sintaxis de los atributos enumerados: un enum anterior a los `enum` del lenguaje.
//
// Una subclase declara sus valores como constantes `public static final` y le da a la clase base
// tres tablas por sobreescritura: `getStringTable()` (los nombres), `getEnumValueTable()` (las
// constantes mismas) y `getOffset()` (con que entero arranca la primera). Ese es todo el
// mecanismo; la clase base solo indexa.
//
// Como los valores son singletons, la igualdad es la de Object -- identidad -- y por eso
// `EnumSyntax` **no** redefine `equals`. Eso obliga a `clone()` a devolver `this` y a que la
// deserializacion pase por `readResolve()`: sin eso, un valor que viaje por un stream volveria
// como una copia distinta y `==` dejaria de funcionar, que es como se usa un enum.
public abstract class EnumSyntax implements Serializable, Cloneable {

    private static final long serialVersionUID = -2739521845085831642L;

    private int value;

    protected EnumSyntax(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    // El mismo objeto: clonar un singleton lo dejaria de ser.
    public Object clone() {
        return this;
    }

    public int hashCode() {
        return this.value;
    }

    // El nombre de la tabla si el valor cae adentro; si no, el entero pelado. Una subclase que no
    // da tabla igual imprime algo util.
    public String toString() {
        int i = this.value - getOffset();
        String[] theTable = getStringTable();
        if (theTable != null && i >= 0 && i < theTable.length && theTable[i] != null) {
            return theTable[i];
        }
        return Integer.toString(this.value);
    }

    // El gancho de la deserializacion: devuelve la constante que le corresponde al entero leido,
    // no el objeto recien construido. Sin esto, `==` contra la constante fallaria despues de un
    // viaje de ida y vuelta por un stream.
    //
    // Aca no hay serializacion que lo llame -- KajiLibrary no tiene ObjectInputStream --, pero el
    // metodo esta y hace lo que dice: es la traduccion entero -> constante, y se puede llamar
    // directo.
    protected Object readResolve() throws ObjectStreamException {
        EnumSyntax[] theTable = getEnumValueTable();
        if (theTable == null) {
            throw new InvalidObjectException("Null enumeration value table for class "
                                             + getClass());
        }
        int theOffset = getOffset();
        int theIndex = this.value - theOffset;
        if (0 > theIndex || theIndex >= theTable.length) {
            throw new InvalidObjectException("Integer value = " + this.value
                                             + " not in valid range " + theOffset + ".."
                                             + (theOffset + theTable.length - 1)
                                             + "for class " + getClass());
        }
        EnumSyntax result = theTable[theIndex];
        if (result == null) {
            throw new InvalidObjectException("No enumeration value for integer value = "
                                             + this.value + "for class " + getClass());
        }
        return result;
    }

    // Los tres ganchos. El default es "no hay tabla", que deja a toString() cayendo al entero y a
    // readResolve() tirando InvalidObjectException -- que es lo correcto para una subclase que no
    // se declaro como enum de verdad.
    protected String[] getStringTable() {
        return null;
    }

    protected EnumSyntax[] getEnumValueTable() {
        return null;
    }

    // El entero de la primera entrada de las tablas. Cero salvo que la subclase diga otra cosa.
    protected int getOffset() {
        return 0;
    }
}
