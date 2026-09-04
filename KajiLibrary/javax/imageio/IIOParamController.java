package javax.imageio;

/**
 * KajiLibrary's javax.imageio.IIOParamController -- pide al usuario que complete los parametros.
 *
 * <p>Un solo metodo. Se asocia a un {@link IIOParam} y, cuando alguien llama
 * {@code activateController()}, este objeto muestra lo que sea --un dialogo, un formulario-- y
 * <b>modifica el propio parametro</b> con lo que el usuario elija.
 *
 * <p>Devolver false significa que el usuario cancelo, y ahi el parametro tiene que quedar <b>como
 * estaba</b>. Es la parte del contrato que se olvida: un controlador que modifica y despues devuelve
 * false deja el parametro a medio cambiar.
 *
 * <p>La interfaz no menciona interfaz grafica en ningun lado, y eso es a proposito: un controlador
 * puede leer de un archivo de configuracion o de la linea de comandos igual de bien.
 */
public interface IIOParamController {

    /**
     * Completa ese parametro.
     *
     * @return si el usuario acepto; false deja el parametro intacto
     */
    boolean activate(IIOParam param);
}
