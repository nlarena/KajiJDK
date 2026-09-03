package javax.security.auth.callback;

import java.io.IOException;

/**
 * KajiLibrary's javax.security.auth.callback.CallbackHandler -- quien contesta las peticiones.
 *
 * <p>Lo escribe la <b>aplicacion</b>, no el modulo de login: es la mitad que sabe si hay una
 * terminal, una ventana o un archivo de configuracion. Ver {@link Callback} para por que la division
 * esta donde esta.
 *
 * <h2>Las dos formas de decir que no</h2>
 *
 * <p>El unico metodo declara dos excepciones y la diferencia entre ellas decide que hace el modulo
 * que lo llamo:
 *
 * <ul>
 *   <li>{@link UnsupportedCallbackException} -- "no se contestar <b>este</b> callback". El modulo
 *       puede probar con otro, o seguir sin ese dato.
 *   <li>{@link IOException} -- el medio fallo. No hay nada que reintentar.
 * </ul>
 */
public interface CallbackHandler {

    /**
     * Contesta cada callback del arreglo, escribiendo la respuesta <b>en el propio callback</b>.
     *
     * <p>No devuelve nada: cada uno tiene su propio setter, y es ahi donde el que pregunto va a
     * mirar.
     *
     * @throws IOException si el medio de entrada o salida fallo
     * @throws UnsupportedCallbackException si alguno de los callbacks no se sabe contestar
     */
    void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException;
}
