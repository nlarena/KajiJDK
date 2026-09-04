package jdk.jshell.spi;

import java.util.HashMap;
import java.util.Map;

/**
 * La fabrica de un {@link ExecutionControl}: lo que se instala y lo que se nombra.
 *
 * <p>Es lo que se declara como proveedor de servicio en un modulo o en un JAR, y lo que
 * {@link ExecutionControl#generate(ExecutionEnv, String)} busca por nombre. La separacion entre la
 * fabrica y el motor no es ceremonia: `jshell` tiene que poder **listar** los motores disponibles y
 * mostrar sus parametros sin arrancar ninguno.
 *
 * <p>De ahi que {@link #defaultParameters()} sea parte del proveedor y no del motor: son los
 * parametros que el motor aceptaria, y hay que conocerlos antes de crearlo.
 *
 * @since 9
 */
public interface ExecutionControlProvider {

    /**
     * El nombre con el que se lo elige.
     *
     * <p>Tiene que ser un identificador Java: es lo que va antes de los dos puntos en una
     * especificacion de motor.
     */
    String name();

    /**
     * Los parametros que acepta, con sus valores por omision.
     *
     * <p>Por omision, ninguno. El mapa que devuelve se puede modificar y pasarle despues a
     * {@link #generate}: es una copia, no el estado del proveedor.
     */
    default Map<String, String> defaultParameters() {
        return new HashMap<String, String>();
    }

    /**
     * Crea el motor.
     *
     * @param env lo que `jshell` le presta al motor
     * @param parameters los parametros; las claves que falten toman su valor por omision
     * @return el motor
     * @throws Throwable lo que sea que falle al crearlo
     */
    ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters) throws Throwable;
}
