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
 * Who knows how to attach to a VM, for a concrete transport mechanism.
 *
 * <h2>Why it is an extension point and not an implementation</h2>
 *
 * <p>Attaching to another process is the most operating-system-dependent thing there is: on
 * Linux it is done over a Unix domain socket in {@code /tmp}, on Windows over shared memory
 * and a named event, and on an embedded VM it may not exist at all. None of the three forms is
 * like the others, so {@link VirtualMachine} does not implement them: it looks for them.
 *
 * <p>{@link #providers} finds them by {@link ServiceLoader}. The practical consequence is that
 * <strong>a JDK with no providers installed does not fail, it returns an empty list</strong> --
 * and {@link VirtualMachine#attach} ends up throwing {@link AttachNotSupportedException}, which
 * is the correct behaviour and not an error of this library.
 *
 * <p>It is this VM's situation today: it brings no provider of its own. What there is here is
 * the complete mechanism, working; what is missing is somebody to register in it.
 */
public abstract class AttachProvider {

    // The list is resolved once. The JDK does the same: the providers neither appear nor
        // disappear while the VM runs, and walking the ServiceLoader again on each `attach` would
        // cost a search in the classpath per call.
    private static List<AttachProvider> cachedProviders;

    /** For the implementations. */
    protected AttachProvider() {
    }

    /** The provider's name. */
    public abstract String name();

    /** The transport mechanism it uses. */
    public abstract String type();

    /**
     * It attaches to the VM identified by {@code id}.
     *
     * <p>What an identifier is is decided by each provider. In those the JDK brings it is the
     * process's pid, but nothing forces that -- hence it is a {@code String} and not a number.
     */
    public abstract VirtualMachine attachVirtualMachine(String id)
            throws AttachNotSupportedException, IOException;

    /**
     * It attaches to the VM {@code vmd} describes.
     *
     * @throws IllegalArgumentException if the descriptor was emitted by <em>another</em> provider.
     *     It is not rigidity: an identifier only means something inside the provider that generated
     *     it, and accepting it here would attach to another process or to none
     */
    public VirtualMachine attachVirtualMachine(VirtualMachineDescriptor vmd)
            throws AttachNotSupportedException, IOException {
        if (vmd.provider() != this) {
            throw new IllegalArgumentException("the descriptor is not from this provider");
        }
        return attachVirtualMachine(vmd.id());
    }

    /**
     * The VMs this provider sees now.
     *
     * <p>It is a snapshot, not a live view: between listing them and attaching, a VM may have
     * finished. That is why {@link #attachVirtualMachine} may fail over a descriptor this list has
     * just returned, and it is nobody's error.
     */
    public abstract List<VirtualMachineDescriptor> listVirtualMachines();

    /**
     * The installed providers; empty if there are none.
     *
     * <p>Empty and not an exception: not having providers is a legitimate configuration -- an
     * embedded VM, an environment that disabled the mechanism -- and not a failure. Whoever needs
     * one finds out on trying to attach.
     */
    public static List<AttachProvider> providers() {
        synchronized (AttachProvider.class) {
            if (cachedProviders == null) {
                List<AttachProvider> list = new ArrayList<AttachProvider>();
                Iterator<AttachProvider> it =
                        ServiceLoader.load(AttachProvider.class).iterator();
                while (it.hasNext()) {
                    list.add(it.next());
                }
                cachedProviders = Collections.unmodifiableList(list);
            }
            return cachedProviders;
        }
    }
}
