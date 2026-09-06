package jdk.dynalink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import jdk.dynalink.beans.BeansLinker;
import jdk.dynalink.linker.ConversionComparator;
import jdk.dynalink.linker.GuardedInvocationTransformer;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.GuardingDynamicLinkerExporter;
import jdk.dynalink.linker.GuardingTypeConverterFactory;
import jdk.dynalink.linker.MethodHandleTransformer;
import jdk.dynalink.linker.MethodTypeConversionStrategy;
import jdk.dynalink.linker.support.CompositeGuardingDynamicLinker;

/**
 * Puts a {@link DynamicLinker} together.
 *
 * <h2>The order of the linkers is the configuration</h2>
 *
 * <p>A site is offered to each linker in turn and goes to the first one that knows how to serve it.
 * That makes the order the only thing to decide, and that is why there are three groups: the
 * prioritized ones, which see everything first; the ones that load themselves; and the last resorts.
 *
 * <p>The middle ones are discovered through {@link GuardingDynamicLinkerExporter}: it is how a
 * language running on top -- or a library added to the class path -- gets into the system without
 * anybody naming it. {@link #getAutoLoadingErrors} is where the ones that could not be loaded show
 * up; they are collected rather than thrown because one library's broken exporter must not keep the
 * language from starting.
 *
 * <p>The default last resort is {@link BeansLinker}, which serves ordinary Java objects. Putting it
 * at the end and not at the front matters: if it saw the sites first, it would serve the language's
 * own objects as if they were Java objects.
 *
 * <h2>The instability threshold</h2>
 *
 * <p>{@link #setUnstableRelinkThreshold} is how many times a site is allowed to be relinked before
 * it is declared unstable. It is a bet about the program: most sites see a single type, a few see two
 * or three, and very few see many. Chaining helps the first and hurts the last.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The factory works in full: the discovery, the composition and the configuration. What does not
 * work is linking, which belongs to {@link DynamicLinker} and needs method handles.
 *
 * @since 9
 */
public final class DynamicLinkerFactory {

    /** How many relinks before a site is declared unstable, when nothing else is said. */
    private static final int DEFAULT_THRESHOLD = 8;

    private ClassLoader loader;
    private boolean loaderSet;
    private List<GuardingDynamicLinker> prioritized;
    private List<GuardingDynamicLinker> lastResort;
    private boolean synchronize;
    private int unstableThreshold = DEFAULT_THRESHOLD;
    private GuardedInvocationTransformer prelink;
    private MethodTypeConversionStrategy autoConversion;
    private MethodHandleTransformer internalFilter;
    private List<ServiceConfigurationError> errors = Collections.emptyList();

    /** One. */
    public DynamicLinkerFactory() {
    }

    /**
     * Which class loader to look for the self-loading linkers with.
     *
     * <p>Passing {@code null} does not go back to the default one: it turns discovery off. It is how
     * a linker with nothing but what was set by hand is built, which is what a program that does not
     * trust whatever is on the class path wants.
     *
     * @param classLoader the loader, or {@code null} to discover nothing
     */
    public void setClassLoader(final ClassLoader classLoader) {
        this.loader = classLoader;
        this.loaderSet = true;
    }

    /**
     * The linkers that see the sites before anybody else.
     *
     * @param prioritizedLinkers the linkers, or {@code null} for none
     * @throws NullPointerException if the list has any {@code null} element
     */
    public void setPrioritizedLinkers(final List<? extends GuardingDynamicLinker>
            prioritizedLinkers) {
        this.prioritized = copyWithoutNulls(prioritizedLinkers);
    }

    /**
     * The same, loose.
     *
     * @param prioritizedLinkers the linkers
     * @throws NullPointerException if any of them is {@code null}
     */
    public void setPrioritizedLinkers(final GuardingDynamicLinker... prioritizedLinkers) {
        setPrioritizedLinkers(asList(prioritizedLinkers));
    }

    /**
     * A single prioritized one.
     *
     * @param prioritizedLinker the linker
     * @throws NullPointerException if it is {@code null}
     */
    public void setPrioritizedLinker(final GuardingDynamicLinker prioritizedLinker) {
        if (prioritizedLinker == null) {
            throw new NullPointerException("prioritizedLinker");
        }
        final List<GuardingDynamicLinker> one = new ArrayList<GuardingDynamicLinker>();
        one.add(prioritizedLinker);
        this.prioritized = one;
    }

    /**
     * The last-resort linkers.
     *
     * <p>Passing {@code null} restores the default one -- a {@link BeansLinker}; passing an empty
     * list leaves the linker with no last resort, which is different: a site nobody knows how to
     * serve will fail instead of being treated as a Java object.
     *
     * @param fallbackLinkers the linkers, or {@code null} for the default one
     * @throws NullPointerException if the list has any {@code null} element
     */
    public void setFallbackLinkers(final List<? extends GuardingDynamicLinker> fallbackLinkers) {
        this.lastResort = copyWithoutNulls(fallbackLinkers);
    }

    /**
     * The same, loose.
     *
     * @param fallbackLinkers the linkers
     * @throws NullPointerException if any of them is {@code null}
     */
    public void setFallbackLinkers(final GuardingDynamicLinker... fallbackLinkers) {
        setFallbackLinkers(asList(fallbackLinkers));
    }

    /**
     * Whether relinking has to happen under the site's lock.
     *
     * <p>It is needed when the site holds a chain of invocations two threads could be changing at
     * once. It costs, so it is not on by default.
     *
     * @param syncOnRelink true to synchronize
     */
    public void setSyncOnRelink(final boolean syncOnRelink) {
        this.synchronize = syncOnRelink;
    }

    /**
     * How many relinks before a site is declared unstable.
     *
     * @param unstableRelinkThreshold how many; zero declares it unstable from the first one
     * @throws IllegalArgumentException if it is negative
     */
    public void setUnstableRelinkThreshold(final int unstableRelinkThreshold) {
        if (unstableRelinkThreshold < 0) {
            throw new IllegalArgumentException("unstableRelinkThreshold < 0");
        }
        this.unstableThreshold = unstableRelinkThreshold;
    }

    /**
     * What to do to each invocation before installing it.
     *
     * <p>It is where a language puts what it wants around every invocation: counting calls, checking
     * permissions, wrapping exceptions.
     *
     * @param prelinkTransformer what to do to it, or {@code null} for nothing
     */
    public void setPrelinkTransformer(final GuardedInvocationTransformer prelinkTransformer) {
        this.prelink = prelinkTransformer;
    }

    /**
     * How to convert what the Java language does not know how to convert.
     *
     * <p>It runs after Java's conversion, not in its place.
     *
     * @param autoConversionStrategy the strategy, or {@code null} for none
     */
    public void setAutoConversionStrategy(
            final MethodTypeConversionStrategy autoConversionStrategy) {
        this.autoConversion = autoConversionStrategy;
    }

    /**
     * What to do to the language's internal objects when they leave for the outside.
     *
     * <p>A language that represents its values with classes of its own does not want those classes
     * leaking into the Java code hosting it; this is where they are wrapped or converted.
     *
     * @param internalObjectsFilter the filter, or {@code null} to filter nothing
     */
    public void setInternalObjectsFilter(final MethodHandleTransformer internalObjectsFilter) {
        this.internalFilter = internalObjectsFilter;
    }

    /**
     * Puts the linker together out of what it was told.
     *
     * <p>It may be called more than once: each call discovers again and builds another linker.
     *
     * @return the linker
     */
    public DynamicLinker createLinker() {
        final List<ServiceConfigurationError> bad = new ArrayList<ServiceConfigurationError>();
        final List<GuardingDynamicLinker> all = new ArrayList<GuardingDynamicLinker>();
        if (this.prioritized != null) {
            all.addAll(this.prioritized);
        }
        all.addAll(discover(bad));
        if (this.lastResort == null) {
            all.add(new BeansLinker());
        } else {
            all.addAll(this.lastResort);
        }
        this.errors = Collections.unmodifiableList(bad);

        // The conversion factories and the comparators come out of those same linkers: a linker that
        // knows how to convert says so by implementing the interface, not by registering apart.
        final List<GuardingTypeConverterFactory> factories =
                new ArrayList<GuardingTypeConverterFactory>();
        final List<ConversionComparator> comparators = new ArrayList<ConversionComparator>();
        for (int i = 0; i < all.size(); i++) {
            final GuardingDynamicLinker l = all.get(i);
            if (l instanceof GuardingTypeConverterFactory) {
                factories.add((GuardingTypeConverterFactory) l);
            }
            if (l instanceof ConversionComparator) {
                comparators.add((ConversionComparator) l);
            }
        }
        final LinkerServicesImpl services = new LinkerServicesImpl(
                new CompositeGuardingDynamicLinker(all), factories, comparators,
                this.autoConversion, this.internalFilter);
        return new DynamicLinker(services, this.prelink, this.synchronize, this.unstableThreshold);
    }

    /**
     * The linkers that could not be loaded in the last {@link #createLinker}.
     *
     * @return the errors, or an empty list if there were none
     */
    public List<ServiceConfigurationError> getAutoLoadingErrors() {
        return this.errors;
    }

    /**
     * The linkers that announce themselves.
     *
     * <p>A broken exporter is written down and skipped: the list of linkers that did load is more
     * useful than a failed startup, and whoever wants to hear about it has
     * {@link #getAutoLoadingErrors}.
     */
    private List<GuardingDynamicLinker> discover(final List<ServiceConfigurationError> bad) {
        final List<GuardingDynamicLinker> out = new ArrayList<GuardingDynamicLinker>();
        if (this.loaderSet && this.loader == null) {
            return out;
        }
        final ClassLoader cl = this.loaderSet ? this.loader
                : Thread.currentThread().getContextClassLoader();
        final ServiceLoader<GuardingDynamicLinkerExporter> loaded =
                ServiceLoader.load(GuardingDynamicLinkerExporter.class, cl);
        final Iterator<GuardingDynamicLinkerExporter> it = loaded.iterator();
        while (true) {
            final GuardingDynamicLinkerExporter exporter;
            try {
                if (!it.hasNext()) {
                    return out;
                }
                exporter = it.next();
            } catch (final ServiceConfigurationError e) {
                bad.add(e);
                continue;
            }
            final List<GuardingDynamicLinker> theirs = exporter.get();
            if (theirs != null) {
                for (int i = 0; i < theirs.size(); i++) {
                    if (theirs.get(i) != null) {
                        out.add(theirs.get(i));
                    }
                }
            }
        }
    }

    /** A copy with no nulls, or {@code null} if the list was {@code null}. */
    private static List<GuardingDynamicLinker> copyWithoutNulls(
            final List<? extends GuardingDynamicLinker> list) {
        if (list == null) {
            return null;
        }
        final List<GuardingDynamicLinker> out = new ArrayList<GuardingDynamicLinker>();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == null) {
                throw new NullPointerException("List has at least one null element");
            }
            out.add(list.get(i));
        }
        return out;
    }

    private static List<GuardingDynamicLinker> asList(final GuardingDynamicLinker[] a) {
        if (a == null) {
            return null;
        }
        final List<GuardingDynamicLinker> out = new ArrayList<GuardingDynamicLinker>();
        for (int i = 0; i < a.length; i++) {
            out.add(a[i]);
        }
        return out;
    }
}
