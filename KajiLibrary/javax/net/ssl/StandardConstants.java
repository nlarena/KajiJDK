package javax.net.ssl;

/**
 * Las constantes estandarizadas del protocolo.
 *
 * <p>Hoy tiene una sola: el tipo de nombre de servidor de la extension SNI. Es una clase entera para
 * una constante porque el registro de IANA puede crecer, y el lugar donde iria lo nuevo tiene que
 * existir de antemano.
 */
public final class StandardConstants {

    private StandardConstants() {
    }

    /**
     * El tipo "nombre de host" de la extension SNI, que vale {@code 0}.
     *
     * <p>SNI resuelve un problema concreto: el cliente tiene que decir a que sitio se conecta
     * <strong>antes</strong> de que el servidor le mande un certificado, porque en una sola direccion
     * IP puede haber muchos sitios y cada uno con el suyo. Sin SNI, el servidor tendria que elegir a
     * ciegas.
     *
     * <p>Ver {@link SNIHostName}.
     */
    public static final int SNI_HOST_NAME = 0;
}
