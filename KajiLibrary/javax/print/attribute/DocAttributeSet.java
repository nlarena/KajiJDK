package javax.print.attribute;

// Un AttributeSet que solo acepta atributos que sean `DocAttribute`.
//
// La restriccion no se puede poner en la firma -- `add` sigue tomando un `Attribute`, porque hay
// que poder pasarle un AttributeSet cualquiera a `addAll` --, asi que se cumple en tiempo de
// ejecucion: lo que no sea DocAttribute sale por ClassCastException. Redeclarar `add`/`addAll` aca
// existe justamente para documentar esa excepcion; no cambia la firma.
public interface DocAttributeSet extends AttributeSet {

    // ClassCastException si `attribute` no es un DocAttribute.
    boolean add(Attribute attribute);

    // ClassCastException si alguno de los atributos no es un DocAttribute.
    boolean addAll(AttributeSet attributes);
}
