package javax.print.attribute;

// Un AttributeSet que solo acepta atributos que sean `PrintRequestAttribute`. La restriccion se
// verifica en tiempo de ejecucion; ver DocAttributeSet.
public interface PrintRequestAttributeSet extends AttributeSet {

    // ClassCastException si `attribute` no es un PrintRequestAttribute.
    boolean add(Attribute attribute);

    // ClassCastException si alguno de los atributos no es un PrintRequestAttribute.
    boolean addAll(AttributeSet attributes);
}
