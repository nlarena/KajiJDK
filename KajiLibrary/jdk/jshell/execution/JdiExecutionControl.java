package jdk.jshell.execution;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;

/**
 * El motor remoto que ademas puede mirar la otra maquina virtual con JDI.
 *
 * <h2>Por que hacen falta las dos cosas</h2>
 *
 * <p>El protocolo por flujos alcanza para pedirle al agente que cargue y ejecute. No alcanza para
 * <strong>redefinir</strong>: reemplazar el codigo de una clase ya cargada es una operacion de la
 * maquina virtual, no del programa que corre adentro. Eso se hace desde afuera, con JDI.
 *
 * <p>De ahi que esta clase herede el protocolo de {@link StreamingExecutionControl} y agregue un
 * {@link #vm()}: por el flujo van las ordenes, y por JDI la cirugia.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>{@link #redefine} y {@link #referenceType} necesitan una {@link VirtualMachine} conectada a
 * otro proceso, y conectarse es el transporte de JDI, que esta VM no tiene. El API de
 * {@code com.sun.jdi} esta completo y estas llamadas son las que corresponden; lo que falta esta
 * abajo, no aca.
 *
 * @since 9
 */
public abstract class JdiExecutionControl extends StreamingExecutionControl {

    /**
     * Un motor sobre ese par de flujos.
     *
     * @param out por donde se mandan los comandos
     * @param in por donde llegan las respuestas
     */
    protected JdiExecutionControl(ObjectOutput out, ObjectInput in) {
        super(out, in);
    }

    /**
     * La maquina virtual donde corre el agente.
     *
     * @return la maquina
     * @throws EngineTerminationException si ya no esta
     */
    protected abstract VirtualMachine vm() throws EngineTerminationException;

    /**
     * Reemplaza el codigo de esas clases en la otra maquina virtual.
     *
     * <p>Se hace con {@code VirtualMachine.redefineClasses}, que es la unica forma de cambiar una
     * clase ya cargada. Tiene el limite conocido: se puede cambiar el cuerpo de un metodo y no la
     * forma de la clase, y los marcos que ya estaban en la pila siguen con el codigo viejo.
     *
     * @param cbcs las clases y su bytecode nuevo
     * @throws ClassInstallException si alguna no se pudo reemplazar
     * @throws EngineTerminationException si la otra maquina ya no esta
     */
    @Override
    public void redefine(ClassBytecodes[] cbcs)
            throws ClassInstallException, EngineTerminationException {
        final VirtualMachine maquina = vm();
        final java.util.Map<ReferenceType, byte[]> mapa =
                new java.util.HashMap<ReferenceType, byte[]>();
        final boolean[] puestas = new boolean[cbcs.length];
        for (int i = 0; i < cbcs.length; i++) {
            final ReferenceType rt = referenceType(maquina, cbcs[i].name());
            if (rt == null) {
                throw new ClassInstallException("redefine: no esta cargada " + cbcs[i].name(),
                        puestas);
            }
            mapa.put(rt, cbcs[i].bytecodes());
            puestas[i] = true;
        }
        maquina.redefineClasses(mapa);
    }

    /**
     * El reflejo de esa clase en la otra maquina virtual.
     *
     * <p>Devuelve {@code null} si no hay ninguna con ese nombre, y tambien si hay mas de una: dos
     * cargadores distintos pueden haber cargado clases del mismo nombre, y ahi no se puede elegir
     * sin saber cual queria el que pregunta.
     *
     * @param vm la maquina
     * @param name el nombre completo de la clase
     * @return el reflejo, o {@code null}
     */
    protected ReferenceType referenceType(VirtualMachine vm, String name) {
        final List<ReferenceType> rts = vm.classesByName(name);
        return rts.size() == 1 ? rts.get(0) : null;
    }
}
