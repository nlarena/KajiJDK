package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.Comment -- un comentario {@code <!-- ... -->}.
 *
 * <p>Un comentario no tiene ningun significado para el documento y aun asi StAX lo entrega, porque
 * la otra opcion --tirarlo-- hace imposible reescribir un archivo sin perderlo. Muchos formatos
 * basados en XML usan comentarios como licencia, marca de generacion o instrucciones para
 * herramientas; una transformacion que los borrara silenciosamente seria una transformacion que
 * nadie quiere.
 *
 * <p>El texto que devuelve {@link #getText()} es lo de adentro, sin los delimitadores y <b>sin
 * ningun escape</b>: dentro de un comentario no hay entidades ni marcado, solo la regla de que no
 * puede aparecer {@code --}.
 */
public interface Comment extends XMLEvent {

    /**
     * El texto del comentario, sin {@code <!--} ni {@code -->}.
     *
     * @return el contenido; nunca null, puede ser la cadena vacia
     */
    String getText();
}
