package java.nio.file.attribute;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

// La vista `"user"`: atributos extendidos, pares nombre-bytes que el usuario cuelga del archivo y
// que el sistema no interpreta.
//
// `read` y `write` toman un `ByteBuffer` en vez de un `byte[]` porque el valor puede ser grande y
// asi quien llama elige donde vive el buffer; el valor se lee o se escribe **entero de una**, no hay
// posicion dentro del atributo.
//
// Sin implementacion en KajiJDK: no hay nativo de atributos extendidos.
public interface UserDefinedFileAttributeView extends FileAttributeView {

    /** Siempre `"user"`. */
    String name();

    /** Los nombres de los atributos que tiene el archivo. */
    List<String> list() throws IOException;

    /** El tamaño en bytes del atributo `name`. */
    int size(String name) throws IOException;

    /** Copia el valor de `name` en `dst`; devuelve cuantos bytes copio. */
    int read(String name, ByteBuffer dst) throws IOException;

    /** Escribe el valor de `name` desde `src`; devuelve cuantos bytes escribio. */
    int write(String name, ByteBuffer src) throws IOException;

    /** Borra el atributo `name`. */
    void delete(String name) throws IOException;
}
