package jdk.jshell.execution;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

import com.sun.jdi.VirtualMachine;
import jdk.jshell.spi.ExecutionEnv;

/**
 * El motor por omision de la herramienta {@code jshell}: los fragmentos corren en otro proceso.
 *
 * <h2>Por que otro proceso</h2>
 *
 * <p>Porque el codigo que se escribe en una sesion interactiva no es de confianza --ni siquiera para
 * quien lo escribe--. Un {@code System.exit(0)} tecleado sin pensar se lleva la sesion puesta si
 * corre en el mismo proceso; en otro, se lleva un proceso que JShell vuelve a levantar.
 *
 * <p>Ademas deja la maquina de JShell limpia: los fragmentos no ensucian su monton, no le dejan
 * hilos vivos y no le cargan clases.
 *
 * <h2>Como esta armado</h2>
 *
 * <p>Dos canales al mismo proceso. Por el de flujos van las ordenes, heredado de
 * {@link StreamingExecutionControl}; por JDI va lo que solo se puede hacer desde afuera, que es
 * redefinir clases. Ver {@link JdiExecutionControl}.
 *
 * <h2>{@link JdiStarter}</h2>
 *
 * <p>Es el punto donde se decide como aparece el otro proceso. Separarlo permite lanzar la otra
 * maquina de una forma particular --otra version de Java, un contenedor, otro usuario-- sin tocar
 * nada del motor.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>No puede funcionar: hace falta una {@link VirtualMachine} conectada a otro proceso, y conectarse
 * es el transporte de JDI, que esta VM no tiene. El motor que si anda es
 * {@link LocalExecutionControl}.
 *
 * @since 9
 */
public class JdiDefaultExecutionControl extends JdiExecutionControl {

    private final VirtualMachine maquina;
    private final Process proceso;

    JdiDefaultExecutionControl(ObjectOutput out, ObjectInput in, VirtualMachine maquina,
            Process proceso) {
        super(out, in);
        this.maquina = maquina;
        this.proceso = proceso;
    }

    /**
     * Llama a ese metodo en el otro proceso.
     *
     * @param className la clase
     * @param methodName el metodo
     * @return la representacion del resultado
     * @throws RunException si el codigo del usuario fallo
     * @throws EngineTerminationException si el otro proceso ya no esta
     * @throws InternalException si fallo el motor
     */
    @Override
    public String invoke(String className, String methodName)
            throws RunException, EngineTerminationException, InternalException {
        return super.invoke(className, methodName);
    }

    /**
     * Corta lo que se este ejecutando en el otro proceso.
     *
     * @throws EngineTerminationException si el otro proceso ya no esta
     * @throws InternalException si no se pudo cortar
     */
    @Override
    public void stop() throws EngineTerminationException, InternalException {
        super.stop();
    }

    /**
     * Cierra la conexion y termina el otro proceso.
     *
     * <p>Matar el proceso ademas de cerrar el canal no es exceso: un agente que dejo de atender
     * pero sigue vivo es un proceso huerfano, y una sesion larga que abre y cierra motores dejaria
     * uno por cada vez.
     */
    @Override
    public void close() {
        super.close();
        if (proceso != null) {
            proceso.destroy();
        }
    }

    /**
     * La maquina virtual donde corre el agente.
     *
     * @return la maquina
     * @throws EngineTerminationException si ya no esta
     */
    @Override
    protected synchronized VirtualMachine vm() throws EngineTerminationException {
        if (maquina == null) {
            throw new EngineTerminationException("VM closed");
        }
        return maquina;
    }

    /**
     * Como se pone en marcha la otra maquina virtual.
     *
     * @since 15
     */
    public interface JdiStarter {

        /**
         * Arranca la otra maquina y devuelve con que hablarle.
         *
         * @param env el entorno de la sesion
         * @param parameters los parametros del proveedor
         * @param port el puerto por el que se van a encontrar
         * @return la maquina y el proceso
         */
        TargetDescription start(ExecutionEnv env, Map<String, String> parameters, int port);

        /**
         * La maquina virtual que se puso en marcha y el proceso que la contiene.
         *
         * <p>Van juntos y no por separado porque quien las recibe necesita las dos: la maquina para
         * hablar por JDI, y el proceso para poder terminarlo. Tener una sin la otra deja o un canal
         * sin forma de cerrarlo, o un proceso sin forma de usarlo.
         *
         * @param vm la maquina virtual
         * @param process el proceso que la contiene
         * @since 15
         */
        final class TargetDescription {

            private final VirtualMachine vm;
            private final Process process;

            /**
             * Con esa maquina y ese proceso.
             *
             * @param vm la maquina virtual
             * @param process el proceso
             */
            public TargetDescription(VirtualMachine vm, Process process) {
                this.vm = vm;
                this.process = process;
            }

            /**
             * La maquina virtual.
             *
             * @return la maquina
             */
            public VirtualMachine vm() {
                return vm;
            }

            /**
             * El proceso.
             *
             * @return el proceso
             */
            public Process process() {
                return process;
            }

            @Override
            public String toString() {
                return "TargetDescription[vm=" + vm + ", process=" + process + "]";
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (!(obj instanceof TargetDescription)) {
                    return false;
                }
                final TargetDescription o = (TargetDescription) obj;
                return (vm == null ? o.vm == null : vm.equals(o.vm))
                        && (process == null ? o.process == null : process.equals(o.process));
            }

            @Override
            public int hashCode() {
                return (vm == null ? 0 : vm.hashCode()) * 31
                        + (process == null ? 0 : process.hashCode());
            }
        }
    }
}
