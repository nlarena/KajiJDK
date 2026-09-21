package org.w3c.dom.bootstrap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DOMImplementationList;
import org.w3c.dom.DOMImplementationSource;

/**
 * KajiLibrary's org.w3c.dom.bootstrap.DOMImplementationRegistry -- where one enters the DOM.
 *
 * <p>The problem it solves is one of bootstrapping: to use DOM a {@link DOMImplementation} is
 * needed, and to get one one would need to have one already. This class breaks that circle, and
 * that is why it lives in a package called <b>bootstrap</b>.
 *
 * <p>Implementations are asked for by <b>features</b>, not by class name: {@code "XML 3.0 LS"} asks
 * for one that knows XML level 3 and also loading and saving. It is what makes the code not depend
 * on who provides it, which is the whole point of the indirection.
 *
 * <h2>The order of search</h2>
 *
 * <p>{@link #newInstance} builds the list of sources with the system property {@link #PROPERTY} --a
 * text with class names separated by spaces-- and with the providers registered as a service. Then
 * each query asks the sources <b>in that order</b> and keeps the first one that answers.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library brings no DOM implementation included, so with no source registered the registry
 * is empty: {@link #getDOMImplementation} returns null and {@link #getDOMImplementationList} a list
 * of length zero. Both are <b>defined</b> answers of the contract --they mean "there is none with
 * those features"-- and they are exactly what the JDK answers when asked for features nobody
 * supports. Registering a source, the registry works as usual.
 */
public final class DOMImplementationRegistry {

    /** The system property with the class names, separated by spaces. */
    public static final String PROPERTY = "org.w3c.dom.DOMImplementationSourceList";

    /** The sources, in order of query. */
    private final List<DOMImplementationSource> sources;

    /** Private: one gets in through {@link #newInstance}. */
    private DOMImplementationRegistry(List<DOMImplementationSource> sources) {
        this.sources = sources;
    }

    /**
     * A registry with the configured sources.
     *
     * <p>See the order in the note of the class.
     *
     * @throws ClassNotFoundException if the property names a class that does not exist
     * @throws InstantiationException if one cannot be built
     * @throws IllegalAccessException if its constructor is not accessible
     * @throws ClassCastException if one is not a {@link DOMImplementationSource}
     */
    public static DOMImplementationRegistry newInstance()
        throws ClassNotFoundException, InstantiationException, IllegalAccessException,
               ClassCastException {
        List<DOMImplementationSource> found = new ArrayList<DOMImplementationSource>();
        String configured = null;
        try {
            configured = System.getProperty(PROPERTY);
        } catch (SecurityException e) {
            // No permission to read it: only the services are left.
        }
        if (configured != null) {
            // Spaces as the separator: it is what the specification says, and that is why a class
            // name with spaces cannot be configured this way.
            String[] names = configured.split(" ");
            int i = 0;
            while (i < names.length) {
                String name = names[i].trim();
                if (name.length() > 0) {
                    found.add(build(name));
                }
                i = i + 1;
            }
        }
        ServiceLoader<DOMImplementationSource> loader =
            ServiceLoader.load(DOMImplementationSource.class);
        Iterator<DOMImplementationSource> it = loader.iterator();
        while (it.hasNext()) {
            found.add(it.next());
        }
        return new DOMImplementationRegistry(found);
    }

    /** It builds a source by name, translating the failures into the ones the contract declares. */
    private static DOMImplementationSource build(String className)
        throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = DOMImplementationRegistry.class.getClassLoader();
        }
        Class<?> found = Class.forName(className, true, loader);
        Object made;
        try {
            made = found.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
        } catch (InstantiationException e) {
            throw e;
        } catch (IllegalAccessException e) {
            throw e;
        } catch (Exception e) {
            // No no-argument constructor, or the constructor threw: for the contract it is the same
            // as not having been able to instantiate it.
            throw new InstantiationException(className + ": " + e);
        }
        return (DOMImplementationSource) made;
    }

    /**
     * The first implementation that has those features.
     *
     * @param features a list separated by spaces, for example {@code "XML 3.0 LS"}
     * @return null if no source has it
     */
    public DOMImplementation getDOMImplementation(String features) {
        int i = 0;
        while (i < this.sources.size()) {
            DOMImplementation found = this.sources.get(i).getDOMImplementation(features);
            if (found != null) {
                return found;
            }
            i = i + 1;
        }
        return null;
    }

    /**
     * All the ones that have those features, from all the sources.
     *
     * @return a list of length zero if there is none
     */
    public DOMImplementationList getDOMImplementationList(String features) {
        List<DOMImplementation> all = new ArrayList<DOMImplementation>();
        int i = 0;
        while (i < this.sources.size()) {
            DOMImplementationList some = this.sources.get(i).getDOMImplementationList(features);
            if (some != null) {
                int j = 0;
                while (j < some.getLength()) {
                    all.add(some.item(j));
                    j = j + 1;
                }
            }
            i = i + 1;
        }
        return new ListOfImplementations(all);
    }

    /**
     * It adds a source at the end.
     *
     * <p>At the end and not at the start: the configured ones win over the ones added by hand,
     * which is what makes the system property serve for forcing an implementation.
     *
     * @throws NullPointerException if it is null
     */
    public void addSource(DOMImplementationSource s) {
        if (s == null) {
            throw new NullPointerException();
        }
        this.sources.add(s);
    }

    /** The list {@link #getDOMImplementationList} returns. */
    private static final class ListOfImplementations implements DOMImplementationList {

        private final List<DOMImplementation> items;

        ListOfImplementations(List<DOMImplementation> items) {
            this.items = items;
        }

        /** Null out of range, as the DOM asks; it does not throw. */
        public DOMImplementation item(int index) {
            if (index < 0 || index >= this.items.size()) {
                return null;
            }
            return this.items.get(index);
        }

        public int getLength() {
            return this.items.size();
        }
    }
}
