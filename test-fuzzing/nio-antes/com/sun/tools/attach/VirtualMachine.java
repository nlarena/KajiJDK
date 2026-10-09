package com.sun.tools.attach;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.sun.tools.attach.spi.AttachProvider;

/**
 * A VM under way, seen from another process.
 *
 * <h2>What "attaching" is</h2>
 *
 * <p>It is getting a channel with a VM that is already running and that was not started for
 * that. What it enables is loading an <strong>agent</strong> inside it -- code that runs with
 * its permissions, sees its classes and may instrument them. It is how the profilers and the
 * debuggers that hook on to a live process work, and it is also the reason there is an
 * {@link AttachPermission}: whoever can attach can execute anything inside the target.
 *
 * <h2>The three loaders, and how they differ</h2>
 *
 * <ul>
 * <li>{@link #loadAgent} -- a Java agent: a JAR with {@code Agent-Class} in the manifest;</li>
 * <li>{@link #loadAgentLibrary} -- a native library, looked up by name in the system's
 *     path;</li>
 * <li>{@link #loadAgentPath} -- a native library, by absolute path.</li>
 * </ul>
 *
 * <p>The last two differ only in how the file is found, and they are two because whoever loads
 * by name wants the system to resolve the platform's convention ({@code lib*.so},
 * {@code *.dll}) and whoever loads by path already knows exactly which one it wants.
 *
 * <h2>How all this is resolved</h2>
 *
 * <p>None of this is implemented here: the three static methods delegate to the installed
 * {@link AttachProvider}s, which are who know the operating system's mechanism. <strong>With no
 * providers installed</strong> -- this VM's case -- {@link #list} returns an empty list and
 * {@link #attach} throws {@link AttachNotSupportedException}. It is the correct behaviour and
 * the same a JDK whose providers were taken out gives: the mechanism is whole, what is missing
 * is somebody to register in it.
 */
public abstract class VirtualMachine {

    private final AttachProvider provider;
    private final String id;

    /**
     * For a provider's implementations.
     *
     * @throws NullPointerException if the provider or the identifier is {@code null}
     */
    protected VirtualMachine(AttachProvider provider, String id) {
        if (provider == null) {
            throw new NullPointerException("provider");
        }
        if (id == null) {
            throw new NullPointerException("id");
        }
        this.provider = provider;
        this.id = id;
    }

    /**
     * The VMs each installed provider sees, together.
     *
     * <p>A snapshot: between listing them and attaching, a VM may have finished.
     */
    public static List<VirtualMachineDescriptor> list() {
        List<VirtualMachineDescriptor> all = new ArrayList<VirtualMachineDescriptor>();
        List<AttachProvider> providers = AttachProvider.providers();
        for (int i = 0; i < providers.size(); i++) {
            all.addAll(providers.get(i).listVirtualMachines());
        }
        return all;
    }

    /**
     * It attaches to the VM identified by {@code id}, trying each provider until one can.
     *
     * <p>To try in order and not to choose is the right thing: an identifier only means something
     * inside a provider, so there is no way of knowing beforehand which one understands it. That
     * one should say {@link AttachNotSupportedException} is not an error -- it is its way of saying
     * "this one is not mine".
     *
     * @throws AttachNotSupportedException if no provider recognizes it, or if there is none
     *     installed
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public static VirtualMachine attach(String id)
            throws AttachNotSupportedException, IOException {
        if (id == null) {
            throw new NullPointerException("id");
        }
        List<AttachProvider> providers = AttachProvider.providers();
        if (providers.isEmpty()) {
            throw new AttachNotSupportedException("there is not a single provider installed");
        }
        AttachNotSupportedException last = null;
        for (int i = 0; i < providers.size(); i++) {
            try {
                return providers.get(i).attachVirtualMachine(id);
            } catch (AttachNotSupportedException e) {
                // The last one is kept and it goes on: that this provider does not recognize it
                                // says nothing about those that are left.
                last = e;
            }
        }
        throw last;
    }

    /**
     * It attaches to the VM {@code vmd} describes, with the provider that saw it.
     *
     * <p>Here it does not try with them all, and it is not an inconsistency with
     * {@link #attach(String)}: a descriptor <em>already says</em> which provider it came out of,
     * so there is nothing to guess.
     */
    public static VirtualMachine attach(VirtualMachineDescriptor vmd)
            throws AttachNotSupportedException, IOException {
        if (vmd == null) {
            throw new NullPointerException("vmd");
        }
        return vmd.provider().attachVirtualMachine(vmd);
    }

    /**
     * It releases the target VM.
     *
     * <p>What the agent has already loaded goes on inside: releasing closes the channel, it does
     * not undo what was done.
     */
    public abstract void detach() throws IOException;

    /** The provider that got this channel. */
    public final AttachProvider provider() {
        return this.provider;
    }

    /** How its provider names this VM. */
    public final String id() {
        return this.id;
    }

    /** It loads a native agent library, by name, with options. */
    public abstract void loadAgentLibrary(String agentLibrary, String options)
            throws AgentLoadException, AgentInitializationException, IOException;

    /** The same, with no options. */
    public void loadAgentLibrary(String agentLibrary)
            throws AgentLoadException, AgentInitializationException, IOException {
        loadAgentLibrary(agentLibrary, null);
    }

    /** It loads a native agent library, by absolute path, with options. */
    public abstract void loadAgentPath(String agentPath, String options)
            throws AgentLoadException, AgentInitializationException, IOException;

    /** The same, with no options. */
    public void loadAgentPath(String agentPath)
            throws AgentLoadException, AgentInitializationException, IOException {
        loadAgentPath(agentPath, null);
    }

    /** It loads a Java agent: a JAR with {@code Agent-Class} in its manifest. */
    public abstract void loadAgent(String agent, String options)
            throws AgentLoadException, AgentInitializationException, IOException;

    /** The same, with no options. */
    public void loadAgent(String agent)
            throws AgentLoadException, AgentInitializationException, IOException {
        loadAgent(agent, null);
    }

    /**
     * The target VM's system properties.
     *
     * <p>They are its own, not this process's: it is the cheap way of finding out which version
     * of Java it runs with, in which directory, and with which classpath.
     */
    public abstract Properties getSystemProperties() throws IOException;

    /** The properties the already loaded agents left in the target VM. */
    public abstract Properties getAgentProperties() throws IOException;

    /** It starts the target VM's management agent with that configuration. */
    public abstract void startManagementAgent(Properties agentProperties) throws IOException;

    /**
     * It starts the local management agent and returns its JMX address.
     *
     * <p>Local means that only something of the same machine may connect. It is what allows a tool
     * such as a monitor to connect to a process that started with no management option at all.
     */
    public abstract String startLocalManagementAgent() throws IOException;

    /** Over the provider and the identifier, the same as {@link VirtualMachineDescriptor}. */
    public int hashCode() {
        return this.provider.hashCode() * 127 + this.id.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof VirtualMachine) {
            VirtualMachine other = (VirtualMachine) obj;
            return other.provider() == this.provider && other.id().equals(this.id);
        }
        return false;
    }

    public String toString() {
        return this.provider.toString() + ": " + this.id;
    }
}
