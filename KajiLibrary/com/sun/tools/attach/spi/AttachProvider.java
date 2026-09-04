package com.sun.tools.attach.spi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

/**
 * Quien sabe adjuntarse a una VM, para un mecanismo de transporte concreto.
 *
 * <h2>Por que es un punto de extension y no una implementacion</h2>
 *
 * <p>Adjuntarse a otro proceso es lo mas dependiente del sistema operativo que hay: en Linux se hace
 * por un socket de dominio Unix en {@code /tmp}, en Windows por memoria compartida y un evento con
 * nombre, y en una VM embebida puede no existir del todo. Ninguna de las tres formas se parece a las
 * otras, asi que {@link VirtualMachine} no las implementa: las busca.
 *
 * <p>{@link #providers} los encuentra por {@link ServiceLoader}. La consecuencia practica es que
 * <strong>un JDK sin proveedores instalados no falla, devuelve una lista vacia</strong> — y
 * {@link VirtualMachine#attach} termina tirando {@link AttachNotSupportedException}, que es el
 * comportamiento correcto y no un error de esta biblioteca.
 *
 * <p>Es la situacion de esta VM hoy: no trae proveedor propio. Lo que hay aca es el mecanismo
 * completo y funcionando; lo que falta es alguien que se registre en el.
 */
public abstract class AttachProvider {

    // La lista se resuelve una vez. El JDK hace lo mismo: los proveedores no aparecen ni
    // desaparecen mientras la VM corre, y volver a recorrer el ServiceLoader en cada `attach`
    // costaria una busqueda en el classpath por llamada.
    private static List<AttachProvider> proveedores;

    /** Para las implementaciones. */
    protected AttachProvider() {
    }

    /** El nombre del proveedor. */
    public abstract String name();

    /** El mecanismo de transporte que usa. */
    public abstract String type();

    /**
     * Se adjunta a la VM identificada por {@code id}.
     *
     * <p>Que es un identificador lo decide cada proveedor. En los que trae el JDK es el pid del
     * proceso, pero nada obliga a eso — de ahi que sea un {@code String} y no un numero.
     */
    public abstract VirtualMachine attachVirtualMachine(String id)
            throws AttachNotSupportedException, IOException;

    /**
     * Se adjunta a la VM que describe {@code vmd}.
     *
     * @throws IllegalArgumentException si el descriptor fue emitido por <em>otro</em> proveedor. No
     *     es rigidez: un identificador solo significa algo dentro del proveedor que lo genero, y
     *     aceptarlo aca adjuntaria a otro proceso o a ninguno
     */
    public VirtualMachine attachVirtualMachine(VirtualMachineDescriptor vmd)
            throws AttachNotSupportedException, IOException {
        if (vmd.provider() != this) {
            throw new IllegalArgumentException("el descriptor no es de este proveedor");
        }
        return attachVirtualMachine(vmd.id());
    }

    /**
     * Las VMs que este proveedor ve ahora.
     *
     * <p>Es una foto, no una vista viva: entre listarlas y adjuntarse, una VM puede haber
     * terminado. Por eso {@link #attachVirtualMachine} puede fallar sobre un descriptor que esta
     * lista acaba de devolver, y no es un error de nadie.
     */
    public abstract List<VirtualMachineDescriptor> listVirtualMachines();

    /**
     * Los proveedores instalados; vacia si no hay ninguno.
     *
     * <p>Vacia y no una excepcion: no tener proveedores es una configuracion legitima —una VM
     * embebida, un entorno que deshabilito el mecanismo— y no una falla. Quien necesite uno se
     * entera al intentar adjuntarse.
     */
    public static List<AttachProvider> providers() {
        synchronized (AttachProvider.class) {
            if (proveedores == null) {
                List<AttachProvider> lista = new ArrayList<AttachProvider>();
                Iterator<AttachProvider> it =
                        ServiceLoader.load(AttachProvider.class).iterator();
                while (it.hasNext()) {
                    lista.add(it.next());
                }
                proveedores = Collections.unmodifiableList(lista);
            }
            return proveedores;
        }
    }
}
