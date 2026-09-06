package jdk.jshell.execution;

import java.util.Collections;
import java.util.Map;

import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

/**
 * El proveedor del motor local: ejecuta los fragmentos en el mismo proceso que JShell.
 *
 * <h2>Cuando conviene</h2>
 *
 * <p>Cuando se quiere que los fragmentos vean lo que ya esta cargado --por ejemplo, al embeber
 * JShell dentro de una aplicacion para inspeccionarla desde adentro-- y cuando arrancar otro proceso
 * cuesta demasiado.
 *
 * <h2>Cuando no</h2>
 *
 * <p>Cuando lo que se ejecuta puede no ser de confianza. Un fragmento local comparte el monton, los
 * hilos y los archivos abiertos con JShell: un {@code System.exit(0)} del usuario se lleva la sesion
 * puesta. Por eso el motor por omision de la herramienta {@code jshell} no es este sino el remoto.
 *
 * @since 9
 */
public class LocalExecutionControlProvider implements ExecutionControlProvider {

    /** Un proveedor. */
    public LocalExecutionControlProvider() {
    }

    /**
     * El nombre con el que se lo pide.
     *
     * @return {@code "local"}
     */
    @Override
    public String name() {
        return "local";
    }

    /**
     * Los parametros que admite.
     *
     * @return un mapa vacio: este motor no tiene nada que configurar
     */
    @Override
    public Map<String, String> defaultParameters() {
        return Collections.<String, String>emptyMap();
    }

    /**
     * Fabrica el motor.
     *
     * @param env el entorno de la sesion
     * @param parameters los parametros; este motor no usa ninguno
     * @return el motor
     */
    @Override
    public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters) {
        return createExecutionControl(env, parameters);
    }

    /**
     * Lo mismo que {@link #generate}, sin declarar excepciones.
     *
     * <p>Existe porque este proveedor no puede fallar al construir el motor --no abre puertos ni
     * arranca procesos-- y quien lo usa directamente no tiene por que envolverlo en un
     * {@code try}.
     *
     * @param env el entorno de la sesion
     * @param parameters los parametros
     * @return el motor
     */
    public ExecutionControl createExecutionControl(ExecutionEnv env,
            Map<String, String> parameters) {
        return new LocalExecutionControl();
    }
}
