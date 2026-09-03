package javax.xml.transform;

/**
 * KajiLibrary's javax.xml.transform.ErrorListener -- quien recibe los problemas de una transformacion.
 *
 * <p>Existe porque un procesador de XSLT no puede decidir solo que hacer con un error. Una hoja de
 * estilo que referencia una plantilla inexistente es fatal para un servidor que genera facturas y es
 * un aviso ignorable para un editor que muestra una vista previa mientras el usuario escribe. La
 * politica la pone el llamador; el procesador solo **avisa**.
 *
 * <p>Los tres niveles no se distinguen por gravedad sino por **que puede seguir pasando despues**,
 * que es lo unico que el procesador sabe de verdad:
 *
 * <ul>
 *   <li>{@link #warning} -- el procesamiento continua normalmente;
 *   <li>{@link #error} -- se detecto una violacion recuperable; el procesador va a seguir para
 *       poder reportar mas de un error por corrida, pero el resultado ya no es confiable;
 *   <li>{@link #fatalError} -- no se puede continuar; el resultado, si lo hay, esta incompleto.
 * </ul>
 *
 * <p>Y aca esta la parte que sorprende, porque invierte el control: los tres metodos pueden
 * **lanzar** {@link TransformerException}, y lanzarla es la forma de decirle al procesador "pará".
 * Volver normalmente de {@link #error} es autorizarlo a seguir. De ahi la regla que la spec insiste
 * y que un oyente escrito a las apuradas rompe siempre: **un {@code ErrorListener} nunca debe volver
 * normalmente de {@link #fatalError}**, porque el procesador queda habilitado a continuar sobre un
 * estado que el mismo declaro inutilizable, y lo que salga de ahi no significa nada.
 */
public interface ErrorListener {

    /**
     * Un aviso. El procesamiento sigue igual.
     *
     * @param exception el aviso, con su ubicacion si se conoce
     * @throws TransformerException para abortar la transformacion
     */
    void warning(TransformerException exception) throws TransformerException;

    /**
     * Un error recuperable. Volver normalmente autoriza a seguir.
     *
     * @param exception el error, con su ubicacion si se conoce
     * @throws TransformerException para abortar la transformacion
     */
    void error(TransformerException exception) throws TransformerException;

    /**
     * Un error del que no se vuelve. No hay que volver normalmente de aca.
     *
     * @param exception el error, con su ubicacion si se conoce
     * @throws TransformerException para abortar la transformacion, que es lo que corresponde
     */
    void fatalError(TransformerException exception) throws TransformerException;
}
