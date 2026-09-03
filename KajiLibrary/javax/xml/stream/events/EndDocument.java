package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.EndDocument -- el final del documento.
 *
 * <p>No declara ningun miembro propio, y eso es lo unico interesante que tiene: es un evento cuya
 * informacion se agota en existir. Se emite una sola vez, despues del cierre del elemento raiz y de
 * lo que haya en el epilogo, y despues de el {@link javax.xml.stream.XMLEventReader#hasNext()}
 * devuelve false.
 *
 * <p>Vale la pena que sea un evento y no simplemente el fin del bucle: quien acumula eventos en una
 * lista para reescribirlos necesita poder representar "aca terminaba", y quien escribe con
 * {@link javax.xml.stream.XMLEventWriter} lo usa para cerrar lo que quede abierto.
 */
public interface EndDocument extends XMLEvent {
}
