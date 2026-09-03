package javax.xml.stream.events;

import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.stream.events.Attribute -- un atributo de un elemento, visto como evento.
 *
 * <h2>Un evento que no aparece en el flujo</h2>
 *
 * <p>Es la rareza del modelo: {@code Attribute} extiende {@link XMLEvent} y contesta true a
 * {@link XMLEvent#isAttribute()}, pero un {@link javax.xml.stream.XMLEventReader} nunca devuelve
 * uno. Los atributos llegan colgados del {@link StartElement} que los declara, por
 * {@link StartElement#getAttributes()}.
 *
 * <p>Que igual sea un evento no es capricho: el que reescribe un documento necesita poder pasarle
 * un atributo a {@link javax.xml.stream.XMLEventWriter#add}, y el que filtra necesita poder
 * escribirlo con {@link XMLEvent#writeAsEncodedUnicode}. Sin el tipo comun harian falta dos
 * caminos para lo mismo.
 *
 * <h2>{@link #isSpecified()} y la diferencia con el DTD</h2>
 *
 * <p>Un atributo puede estar en el documento o venir de un valor por omision declarado en el DTD.
 * Los dos se ven igual desde {@link #getValue()}, y {@link #isSpecified()} es lo unico que los
 * distingue. Importa cuando se reescribe: un atributo que el parser invento a partir del DTD y que
 * se vuelve a escribir queda duplicado en el documento de salida si ademas se copia el DTD.
 *
 * <p>Un parser que no lee el DTD --como el de esta biblioteca-- devuelve siempre true, que es la
 * verdad: todo lo que vio estaba escrito.
 */
public interface Attribute extends XMLEvent {

    /**
     * El nombre del atributo, con espacio de nombres si lo tiene.
     *
     * <p>Un atributo sin prefijo <b>no</b> esta en el espacio de nombres por omision --esa es la
     * asimetria con los elementos, y viene de la especificacion de Namespaces-- asi que su
     * {@link QName} tiene el espacio de nombres vacio.
     *
     * @return el nombre calificado; nunca null
     */
    QName getName();

    /**
     * El valor normalizado del atributo.
     *
     * <p>Ya vienen resueltas las referencias a entidad y el fin de linea, y --si el parser lee el
     * DTD-- aplicada la normalizacion que corresponda al tipo declarado.
     *
     * @return el valor; nunca null, puede ser la cadena vacia
     */
    String getValue();

    /**
     * El tipo declarado en el DTD: {@code CDATA}, {@code ID}, {@code IDREF}, {@code NMTOKEN}, etc.
     *
     * @return el tipo, o {@code "CDATA"} si no hay DTD que consultar
     */
    String getDTDType();

    /**
     * Si el atributo estaba escrito en el documento, en vez de venir de un valor por omision.
     *
     * @return true si aparecia en el texto
     */
    boolean isSpecified();
}
