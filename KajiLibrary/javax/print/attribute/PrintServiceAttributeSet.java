package javax.print.attribute;

// Un AttributeSet que solo acepta atributos que sean `PrintServiceAttribute`. La restriccion se
// verifica en tiempo de ejecucion; ver DocAttributeSet.
public interface PrintServiceAttributeSet extends AttributeSet {

    // ClassCastException si `attribute` no es un PrintServiceAttribute.
    boolean add(Attribute attribute);

    // ClassCastException si alguno de los atributos no es un PrintServiceAttribute.
    boolean addAll(AttributeSet attributes);
}
