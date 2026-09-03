package javax.print.attribute;

// Un AttributeSet que solo acepta atributos que sean `PrintJobAttribute`. La restriccion se
// verifica en tiempo de ejecucion; ver DocAttributeSet.
public interface PrintJobAttributeSet extends AttributeSet {

    // ClassCastException si `attribute` no es un PrintJobAttribute.
    boolean add(Attribute attribute);

    // ClassCastException si alguno de los atributos no es un PrintJobAttribute.
    boolean addAll(AttributeSet attributes);
}
