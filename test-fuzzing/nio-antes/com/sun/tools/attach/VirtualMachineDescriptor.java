package com.sun.tools.attach;

import com.sun.tools.attach.spi.AttachProvider;

/**
 * The description of a VM that may be seen from outside: who saw it, what it is called and how
 * it is named.
 *
 * <h2>Why the provider is part of the identity</h2>
 *
 * <p>The {@link #id} is not unique by itself: it is a string that <em>only means something
 * inside the provider that generated it</em>. Two different providers may use the same text for
 * different VMs. Hence {@link #equals} compares the two things, and hence
 * {@link AttachProvider#attachVirtualMachine(VirtualMachineDescriptor)} rejects another's
 * descriptor instead of trying it all the same.
 *
 * <p>It is immutable, and that matters: it is a <strong>snapshot</strong>. The VM it describes
 * may have finished a while ago, and the descriptor would go on saying the same.
 */
public class VirtualMachineDescriptor {

    private final AttachProvider provider;
    private final String id;
    private final String displayName;

    /**
     * @throws NullPointerException if the provider or the identifier is {@code null}
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

    /** With no name to show: the identifier is used, which is what there is. */
    public VirtualMachineDescriptor(AttachProvider provider, String id) {
        this(provider, id, id);
    }

    /** Who saw this VM. */
    public AttachProvider provider() {
        return this.provider;
    }

    /** How its provider names it. */
    public String id() {
        return this.id;
    }

    /** A name to show a person; it may be the identifier itself. */
    public String displayName() {
        return this.displayName;
    }

    /**
     * Over the provider and the identifier, which are the identity. The name to show is left out
     * on purpose: it is decoration, and two descriptors of the same VM could bring it
     * differently.
     */
    public int hashCode() {
        return this.provider.hashCode() * 127 + this.id.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof VirtualMachineDescriptor) {
            VirtualMachineDescriptor other = (VirtualMachineDescriptor) obj;
            return other.provider() == this.provider && other.id().equals(this.id);
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
