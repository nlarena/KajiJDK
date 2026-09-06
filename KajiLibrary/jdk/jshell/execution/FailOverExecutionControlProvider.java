package jdk.jshell.execution;

import java.util.HashMap;
import java.util.Map;

import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

/**
 * El proveedor que prueba varios en orden y se queda con el primero que ande.
 *
 * <h2>Por que existe</h2>
 *
 * <p>Arrancar el motor remoto puede fallar por razones que no dependen de JShell: un cortafuegos que
 * no deja abrir el puerto de escucha, una maquina sin la interfaz de bucle configurada, una politica
 * que prohibe lanzar procesos. Ninguna de esas es un error del usuario y en todas hay una salida
 * peor pero servible.
 *
 * <p>Sin esto, JShell no arrancaria en esas maquinas y el mensaje hablaria de sockets. Con esto,
 * arranca con el motor que se pueda.
 *
 * <h2>Los parametros</h2>
 *
 * <p>Se numeran: {@code 0}, {@code 1}, {@code 2}... y cada uno nombra un proveedor. Se prueban en el
 * orden de sus numeros. Por omision son el remoto por JDI con lanzamiento, el remoto por JDI
 * escuchando, y el local.
 *
 * <p>Que el ultimo sea el local no es casualidad: es el unico que no puede fallar por el entorno,
 * asi que sirve de piso.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Funciona, y hace exactamente lo que tiene que hacer: los dos primeros de la lista necesitan JDI
 * y fallan, y la sesion termina con el motor local, que anda. Es el unico caso de este paquete en
 * que la falta del transporte no se nota desde afuera.
 *
 * @since 9
 */
public class FailOverExecutionControlProvider implements ExecutionControlProvider {

    /** Un proveedor. */
    public FailOverExecutionControlProvider() {
    }

    /**
     * El nombre con el que se lo pide.
     *
     * @return {@code "failover"}
     */
    @Override
    public String name() {
        return "failover";
    }

    /**
     * Los proveedores a probar, en orden.
     *
     * <p>Son diez casillas numeradas y solo la primera viene llena. Las otras nueve estan vacias a
     * proposito: son el lugar donde quien configura la sesion pone sus alternativas, y que existan
     * con su numero es lo que le dice cuantas puede poner y como se llaman.
     *
     * @return el mapa de posicion a especificacion de proveedor
     */
    @Override
    public Map<String, String> defaultParameters() {
        final Map<String, String> out = new HashMap<String, String>();
        out.put("0", "jdi");
        for (int i = 1; i < 10; i++) {
            out.put(Integer.toString(i), "");
        }
        return out;
    }

    /**
     * Devuelve el motor del primer proveedor que no falle.
     *
     * @param env el entorno de la sesion
     * @param parameters los proveedores a probar, numerados
     * @return el motor
     * @throws Throwable lo que fallo el ultimo, si fallaron todos
     */
    @Override
    public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters)
            throws Throwable {
        final Map<String, String> ps = parameters == null || parameters.isEmpty()
                ? defaultParameters() : parameters;
        Throwable ultima = null;
        for (int i = 0; i < ps.size(); i++) {
            final String spec = ps.get(Integer.toString(i));
            if (spec == null || spec.isEmpty()) {
                continue;
            }
            try {
                final ExecutionControl ec = ExecutionControl.generate(env, spec);
                if (ec != null) {
                    return ec;
                }
            } catch (Throwable e) {
                // Que uno falle es lo esperado: para eso esta esta clase. Se guarda el ultimo
                // motivo, porque si fallan todos es lo unico que se puede contar.
                ultima = e;
            }
        }
        if (ultima != null) {
            throw ultima;
        }
        throw new IllegalStateException("no execution control provider succeeded");
    }
}
