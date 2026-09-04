package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

// Una respuesta que salio del cache en vez de la red.
//
// Es el lado de **lectura**: `ResponseCache.get` devuelve una de estas cuando tiene guardado lo que
// se esta pidiendo, y el cliente la usa en lugar de conectarse.
//
// Que devuelva los headers y no solo el cuerpo no es un detalle: sin ellos no se puede saber si lo
// guardado sigue vigente (`Expires`, `Cache-Control`, `ETag`), ni revalidarlo contra el servidor, ni
// entregar el `Content-Type` correcto. Un cache que guardara solo bytes serviria contenido sin saber
// que es ni hasta cuando vale.
//
// Abstracta, sin logica propia y sin red: es un contrato. Nada omitido de esta clase.
//
// **Su subclase `SecureCacheResponse` no esta**, y el motivo no es ella: es lo que sus siete
// miembros nombran. Cinco devuelven o tiran tipos de `javax.net.ssl` --`SSLSession`,
// `SSLPeerUnverifiedException`-- y ese paquete no existe en este arbol, porque no hay TLS. Un metodo
// declarado con un tipo que no existe no compila, y crear esas dos clases para poder nombrarlas
// seria inventar la fachada de una capa que no esta detras.
//
// Y la clase no serviria igual: una `SecureCacheResponse` es una respuesta **HTTPS** guardada, con
// la cadena de certificados con la que se recibio. Nada de este arbol puede producir una, porque
// nada de este arbol habla TLS. Falta la clase entera, que es mas honesto que una que no se puede
// construir.
public abstract class CacheResponse {

    public CacheResponse() {
    }

    /**
     * Los headers de la respuesta guardada.
     *
     * <p>La clave null, si esta, es la linea de estado ("HTTP/1.1 200 OK"), que no tiene nombre de
     * header. Es la misma convencion que {@link URLConnection#getHeaderFields}.
     *
     * @throws IOException si no se pueden leer del cache
     */
    public abstract Map<String, List<String>> getHeaders() throws IOException;

    /**
     * El cuerpo guardado.
     *
     * @throws IOException si no se puede abrir
     */
    public abstract InputStream getBody() throws IOException;
}
