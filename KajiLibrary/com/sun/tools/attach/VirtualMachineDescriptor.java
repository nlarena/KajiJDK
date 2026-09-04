package com.sun.tools.attach;

import com.sun.tools.attach.spi.AttachProvider;

/**
 * La descripcion de una VM que se puede ver desde afuera: quien la vio, como se llama y como se la
 * nombra.
 *
 * <h2>Por que el proveedor forma parte de la identidad</h2>
 *
 * <p>El {@link #id} no es unico por si solo: es una cadena que <em>solo significa algo dentro del
 * proveedor que la genero</em>. Dos proveedores distintos pueden usar el mismo texto para VMs
 * distintas. De ahi que {@link #equals} compare las dos cosas, y de ahi que
 * {@link AttachProvider#attachVirtualMachine(VirtualMachineDescriptor)} rechace un descriptor
 * ajeno en vez de intentarlo igual.
 *
 * <p>Es inmutable, y eso importa: es una <strong>foto</strong>. La VM que describe puede haber
 * terminado hace rato, y el descriptor seguiria diciendo lo mismo.
 */
public class VirtualMachineDescriptor {

    private final AttachProvider provider;
    private final String id;
    private final String displayName;

    /**
     * @throws NullPointerException si el proveedor o el identificador son {@code null}
     */
    public VirtualMachineDescriptor(AttachProvider provider, String id, String displayName) {
        if (provider == null) {
            throw new NullPointerException("provider");
        }
        if (id == null) {
            throw new NullPointerException("id");
        }
        this.provider = provider;
        this.id = id;
        this.displayName = displayName;
    }

    /** Sin nombre para mostrar: se usa el identificador, que es lo que hay. */
    public VirtualMachineDescriptor(AttachProvider provider, String id) {
        this(provider, id, id);
    }

    /** Quien vio esta VM. */
    public AttachProvider provider() {
        return this.provider;
    }

    /** Como la nombra su proveedor. */
    public String id() {
        return this.id;
    }

    /** Un nombre para mostrarle a una persona; puede ser el identificador mismo. */
    public String displayName() {
        return this.displayName;
    }

    /**
     * Sobre el proveedor y el identificador, que son la identidad. El nombre para mostrar queda
     * afuera a proposito: es decoracion, y dos descriptores de la misma VM podrian traerlo distinto.
     */
    public int hashCode() {
        return this.provider.hashCode() * 127 + this.id.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof VirtualMachineDescriptor) {
            VirtualMachineDescriptor otro = (VirtualMachineDescriptor) obj;
            return otro.provider() == this.provider && otro.id().equals(this.id);
        }
        return false;
    }

    public String toString() {
        String s = this.provider.toString() + ": " + this.id;
        if (this.displayName != null && !this.displayName.equals(this.id)) {
            s = s + " " + this.displayName;
        }
        return s;
    }
}
