package java.net;

import java.io.IOException;
import java.io.OutputStream;

// El canal por donde una respuesta se escribe en el cache.
//
// Aparece del lado de la **escritura**: cuando llega una respuesta que vale la pena guardar,
// `ResponseCache.put` devuelve uno de estos y el cliente copia el cuerpo en su `OutputStream`
// mientras se lo entrega al que pidio.
//
// `abort()` es la parte que hace que el diseno funcione: si la conexion se corta a la mitad, lo que
// se alcanzo a escribir es basura --una respuesta truncada guardada como completa es peor que no
// tener cache-- y el cliente avisa para que el cache tire lo escrito. Un cache sin ese aviso
// terminaria sirviendo respuestas incompletas.
//
// Abstracta, sin logica propia y sin red: es un contrato. Nada omitido.
public abstract class CacheRequest {

    public CacheRequest() {
    }

    /**
     * El flujo donde escribir el cuerpo de la respuesta.
     *
     * @throws IOException si el cache no puede abrirlo
     */
    public abstract OutputStream getBody() throws IOException;

    /** Descarta lo escrito hasta ahora: la respuesta no llego entera y no hay que guardarla. */
    public abstract void abort();
}
