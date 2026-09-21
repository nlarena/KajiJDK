package javax.management;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The only legitimate way to get an {@link MBeanServer}.
 *
 * <p>The whole class revolves around a two-word distinction that is easy to overlook:
 *
 * <ul>
 *   <li>{@code createMBeanServer} <b>keeps</b> the agent in a static table, and therefore anyone in
 *       the same virtual machine finds it with {@link #findMBeanServer}. It is what lets a
 *       monitoring agent loaded later hook onto the application;
 *   <li>{@code newMBeanServer} does <b>not</b> keep it. It is a private agent of whoever asked for
 *       it.
 * </ul>
 *
 * <p>And from that distinction comes the danger {@link #releaseMBeanServer} explains: since the
 * table is static and holds strong references, an agent created with {@code createMBeanServer} is
 * <b>never collected</b> even if nobody uses it. It has to be released by hand.
 * {@code newMBeanServer} does not have that problem precisely because nobody keeps it.
 *
 * <p>{@link #getClassLoaderRepository} is here: {@code javax.management.loading} exists in this
 * library now, which was the only thing missing.
 */
public class MBeanServerFactory {

    /** Not instantiated: it is a static factory. */
    private MBeanServerFactory() {
    }

    /**
     * The agents created with {@code createMBeanServer}, by {@code MBeanServerId}.
     *
     * <p>With the creation order kept because {@link #findMBeanServer} with {@code null} returns
     * them all, and a stable order is a reproducible answer.
     */
    private static final Map<String, MBeanServer> created =
            new LinkedHashMap<String, MBeanServer>();

    /** The agent builder, resolved only once. */
    private static MBeanServerBuilder constructor = null;

    /**
     * Releases the reference {@code createMBeanServer} left.
     *
     * @throws IllegalArgumentException if the agent was not in the table --it was never created
     *         with {@code createMBeanServer}, or it was already released. Failing is right:
     *         releasing twice usually means someone believes they have an agent that no longer
     *         exists
     */
    public static void releaseMBeanServer(MBeanServer mbeanServer) {
        synchronized (MBeanServerFactory.class) {
            for (Map.Entry<String, MBeanServer> e : created.entrySet()) {
                if (e.getValue() == mbeanServer) {
                    created.remove(e.getKey());
                    return;
                }
            }
        }
        throw new IllegalArgumentException(
            "That MBeanServer was not created with createMBeanServer");
    }

    /** Findable, with the default domain. */
    public static MBeanServer createMBeanServer() {
        return createMBeanServer(null);
    }

    /** Findable, with the given domain. */
    public static MBeanServer createMBeanServer(String domain) {
        MBeanServer s = build(domain);
        synchronized (MBeanServerFactory.class) {
            created.put(idOf(s), s);
        }
        return s;
    }

    /** Private: it is not registered and is collected when nobody references it. */
    public static MBeanServer newMBeanServer() {
        return newMBeanServer(null);
    }

    /** Private, with the given domain. */
    public static MBeanServer newMBeanServer(String domain) {
        return build(domain);
    }

    private static MBeanServer build(String domain) {
        MBeanServerBuilder b = builder();
        MBeanServerDelegate d = b.newMBeanServerDelegate();
        // `outer` goes as null: there is no wrapper, the agent presents itself.
        return b.newMBeanServer(domain, null, d);
    }

    /**
     * The agent builder, the system's or the one {@code javax.management.builder.initial} names.
     */
    private static synchronized MBeanServerBuilder builder() {
        if (constructor != null) {
            return constructor;
        }
        String cls = System.getProperty("javax.management.builder.initial");
        if (cls == null || cls.length() == 0) {
            constructor = new MBeanServerBuilder();
        } else {
            try {
                constructor = (MBeanServerBuilder)
                        Class.forName(cls).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                // The specification asks to fail: silently falling back to the system builder would
                // leave whoever configured the property believing their agent is running.
                throw new JMRuntimeException(
                    "Could not instantiate the MBeanServerBuilder " + cls + ": " + e);
            }
        }
        return constructor;
    }

    private static String idOf(MBeanServer s) {
        try {
            return (String) s.getAttribute(MBeanServerDelegate.DELEGATE_NAME, "MBeanServerId");
        } catch (Exception e) {
            // An agent without a delegate does not follow the specification, but the table needs a
            // key anyway; the object's identity is enough and does not collide.
            return "no-id-" + System.identityHashCode(s);
        }
    }

    /**
     * The findable agents.
     *
     * @param agentId {@code null} returns them all; otherwise, the one with that {@code
     *     MBeanServerId}
     */
    public static synchronized ArrayList<MBeanServer> findMBeanServer(String agentId) {
        ArrayList<MBeanServer> r = new ArrayList<MBeanServer>();
        if (agentId == null) {
            r.addAll(created.values());
        } else {
            MBeanServer s = created.get(agentId);
            if (s != null) {
                r.add(s);
            }
        }
        return r;
    }

    /**
     * That agent's class loader repository.
     *
     * <p>It is the list of loaders the agent consults to load a class whose name reached it from
     * outside. It is built on every query and not frozen, because a loader registers like any other
     * MBean and at any time.
     *
     * <p>Only agents of this factory are accepted. The JDK accepts any {@link MBeanServer} and
     * simply returns {@code server.getClassLoaderRepository()}; an earlier note justified the
     * restriction by saying another agent's loaders are unknown here, but the interface itself can
     * be asked for them.
     *
     * @throws IllegalArgumentException if {@code server} is null, or if it is not an agent of this
     *     factory
     */
    public static javax.management.loading.ClassLoaderRepository getClassLoaderRepository(
            MBeanServer server) {
        if (server instanceof LocalServer) {
            return ((LocalServer) server).getClassLoaderRepository();
        }
        throw new IllegalArgumentException(
                server == null ? "server is null" : "not an MBeanServer of this factory");
    }
}
