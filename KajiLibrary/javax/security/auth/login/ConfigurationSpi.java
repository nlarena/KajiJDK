package javax.security.auth.login;

/**
 * KajiLibrary's javax.security.auth.login.ConfigurationSpi -- lo que escribe un proveedor.
 *
 * <p>La mitad de abajo del par: {@link Configuration} es lo que ve la aplicacion y esto es lo que
 * implementa quien provee. La separacion sirve para que los metodos publicos --{@code getProvider},
 * {@code getType}, {@code getParameters}-- no se puedan implementar mal: los contesta la clase de
 * arriba con lo que sabe del pedido, y el proveedor no puede mentir sobre de donde salio.
 */
public abstract class ConfigurationSpi {

    /** Publico por como se lo instancia por reflexion desde el proveedor. */
    public ConfigurationSpi() {
    }

    /**
     * Los modulos configurados para ese nombre.
     *
     * @return null si no hay nada para ese nombre
     */
    protected abstract AppConfigurationEntry[] engineGetAppConfigurationEntry(String name);

    /**
     * Vuelve a leer la configuracion.
     *
     * <p>Por omision no hace nada, para que un proveedor sin origen recargable no tenga que escribir
     * un cuerpo vacio.
     */
    protected void engineRefresh() {
    }
}
