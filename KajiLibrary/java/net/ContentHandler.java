package java.net;

import java.io.IOException;

// El que convierte el cuerpo de una respuesta en un objeto de Java.
//
// Un `ContentHandler` es lo que hace que `url.getContent()` pueda devolver una `Image` y no un
// `InputStream`: se lo elige por el tipo MIME de la respuesta, y el sabe leer ese tipo.
//
// ===========================================================================================
// ENTRA ENTERA, Y NO ES UNA CONCESION
// ===========================================================================================
//
// Es abstracta y su unico metodo abstracto --`getContent(URLConnection)`-- lo escribe quien
// extiende. Esta clase no lee de la red: recibe una conexion ya abierta por otro y le pide el
// flujo. Lo unico que aporta de propio es la sobrecarga con `Class[]`, que es filtrado puro sobre
// el resultado del abstracto.
//
// Lo que no hay en KajiJDK es un CATALOGO de manejadores --el JDK trae los suyos para `text/plain`,
// `image/gif` y demas, en paquetes internos--. Eso no es parte de esta clase: es de
// `URLConnection`, que aca solo consulta la factoria que la aplicacion instale.
public abstract class ContentHandler {

    public ContentHandler() {
    }

    /**
     * Lee el cuerpo de {@code urlc} y lo devuelve como objeto.
     *
     * @throws IOException si falla la lectura
     */
    public abstract Object getContent(URLConnection urlc) throws IOException;

    /**
     * Como {@link #getContent(URLConnection)}, pero solo si el resultado es de alguno de los tipos
     * pedidos; si no, null.
     *
     * <p>Existe para que el llamador pueda decir "dame esto **si** lo podes dar como una `Image`",
     * y no tener que hacer el `instanceof` y descartar despues de haber leido todo. Que devuelva
     * null en vez de tirar es a proposito: no encontrar el tipo pedido no es un error de IO.
     */
    public Object getContent(URLConnection urlc, Class[] classes) throws IOException {
        Object obj = this.getContent(urlc);
        int i = 0;
        while (i < classes.length) {
            if (classes[i].isInstance(obj)) {
                return obj;
            }
            i = i + 1;
        }
        return null;
    }
}
