package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.NotationDeclaration -- un {@code <!NOTATION ...>} del DTD.
 *
 * <p>Una notacion le pone nombre a un formato que XML no va a interpretar --{@code
 * <!NOTATION jpeg SYSTEM "image/jpeg">}-- para que una entidad externa sin analizar pueda decir de
 * que tipo son sus datos. Es el mecanismo con que un DTD dice "aca adentro hay algo que no es
 * texto".
 *
 * <p>Quedo practicamente en desuso: el mismo problema hoy se resuelve con un tipo MIME en un
 * atributo, o con datos en base64 dentro del propio documento. Sigue en la API porque sigue en la
 * especificacion de XML 1.0, y porque un documento viejo que la use tiene que poder leerse sin
 * perder informacion.
 *
 * <p>De {@link #getPublicId()} y {@link #getSystemId()} puede faltar cualquiera de los dos --una
 * notacion declarada solo con {@code PUBLIC} no tiene identificador de sistema-- pero no los dos a
 * la vez.
 */
public interface NotationDeclaration extends XMLEvent {

    /**
     * El nombre de la notacion, que es con lo que la referencia una entidad.
     *
     * @return el nombre; nunca null
     */
    String getName();

    /**
     * El identificador publico declarado.
     *
     * @return el identificador publico, o null si la notacion se declaro solo con {@code SYSTEM}
     */
    String getPublicId();

    /**
     * El identificador de sistema declarado.
     *
     * @return el identificador de sistema, o null si la notacion se declaro solo con
     *     {@code PUBLIC}
     */
    String getSystemId();
}
