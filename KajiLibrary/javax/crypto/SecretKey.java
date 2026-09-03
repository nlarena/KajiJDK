package javax.crypto;

import java.security.Key;
import javax.security.auth.Destroyable;

/**
 * Una clave simetrica: la misma sirve para cifrar y para descifrar.
 *
 * <p>No agrega ningun metodo, y eso es exactamente lo que dice. Un `SecretKey` es un {@link Key}
 * --tiene algoritmo, formato y bytes-- que ademas se puede destruir. Lo que aporta como tipo propio
 * es la distincion: un metodo que pide `SecretKey` no acepta una clave publica, y esa comprobacion
 * la hace el compilador en vez de fallar en ejecucion.
 *
 * <p>Que extienda {@link Destroyable} no es decoracion: el material de una clave simetrica es un
 * secreto que conviene borrar de la memoria cuando no se usa mas, y sin ese supertipo no habria
 * forma de pedirlo por contrato.
 */
public interface SecretKey extends Key, Destroyable {

    /**
     * @deprecated Un `serialVersionUID` en una interfaz no hace nada: solo cuenta el de la clase
     *     que implementa. Esta declarado porque el JDK lo declara y sacarlo cambiaria la superficie.
     */
    @Deprecated
    public static final long serialVersionUID = -4795878709595146952L;
}
