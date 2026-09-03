package javax.print.attribute;

import java.io.Serializable;

// La clase de sintaxis de los atributos cuyo valor es un entero.
//
// No es un Attribute: es la mitad "valor" que una subclase concreta combina con la mitad
// "categoria" implementando Attribute. Por eso es abstracta y sus constructores son protected --
// nadie de afuera arma un IntegerSyntax suelto.
//
// Ojo con `equals`: compara por `instanceof IntegerSyntax`, no por clase exacta, asi que dos
// atributos de **categorias distintas** con el mismo entero salen iguales. Es lo que hace el JDK y
// se replica tal cual; los conjuntos no se confunden porque indexan por categoria, no por valor.
public abstract class IntegerSyntax implements Serializable, Cloneable {

    private static final long serialVersionUID = 3644574816328081943L;

    private int value;

    protected IntegerSyntax(int value) {
        this.value = value;
    }

    // Con rango: los dos extremos son inclusivos.
    protected IntegerSyntax(int value, int lowerBound, int upperBound) {
        if (value < lowerBound || value > upperBound) {
            throw new IllegalArgumentException("Value " + value + " not in range " + lowerBound
                                               + ".." + upperBound);
        }
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public boolean equals(Object object) {
        if (!(object instanceof IntegerSyntax)) {
            return false;
        }
        return this.value == ((IntegerSyntax) object).value;
    }

    public int hashCode() {
        return this.value;
    }

    public String toString() {
        return "" + this.value;
    }
}
