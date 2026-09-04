package java.net;

// Quien sabe fabricar el manejador de un protocolo.
//
// Es el punto de extension que hace a `java.net.URL` abierta: la clase no trae un manejador por
// protocolo cableado adentro, sino que le pregunta a la fabrica instalada. Asi un programa puede
// ensenarle a `URL` un esquema que la plataforma no conoce --`classpath:`, `res:`, uno propio-- sin
// tocar `URL`.
//
// Un solo metodo, y devolver null es parte del contrato: significa "de ese protocolo no se nada",
// y ahi `URL` sigue con sus manejadores internos.
//
// Computacion pura: nada omitido.
public interface URLStreamHandlerFactory {

    /**
     * El manejador de {@code protocol}, o null si esta fabrica no conoce ese protocolo.
     *
     * @param protocol el esquema, en minusculas y sin los dos puntos ("http", "file")
     */
    URLStreamHandler createURLStreamHandler(String protocol);
}
