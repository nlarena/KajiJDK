package java.net;

// De un tipo MIME al objeto que sabe interpretar un cuerpo de ese tipo.
//
// La factoria es el punto de extension de `URLConnection.getContent()`: sin una instalada, una
// conexion no sabe convertir bytes en objetos y lo dice tirando. Con una, `getContent()` sobre un
// `image/png` puede devolver una imagen en vez de un flujo.
public interface ContentHandlerFactory {

    /**
     * El manejador para {@code mimetype}, o null si esta factoria no conoce ese tipo.
     *
     * <p>Devolver null no es un error: significa "yo no", y quien pregunta sigue buscando.
     */
    ContentHandler createContentHandler(String mimetype);
}
