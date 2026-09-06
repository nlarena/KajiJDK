import java.util.ArrayList;
import java.util.List;

import jdk.dynalink.DynamicLinker;
import jdk.dynalink.DynamicLinkerFactory;
import jdk.dynalink.linker.ConversionComparator;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.LinkerServices;

/**
 * Checks {@code DynamicLinker} and {@code DynamicLinkerFactory} against JDK 25.
 *
 * <h2>What can be compared</h2>
 *
 * <p>The half of the system that decides about types: whether a conversion is possible, which of two
 * targets is better, and which error each option of the factory fails with. That is arithmetic over
 * {@code Class} and depends on nothing.
 *
 * <p>What is not compared is linking. Linking means building a method handle, and this virtual
 * machine has no handles --{@code MethodHandles} says so in its note-- so {@code link} throws
 * {@code UnsupportedOperationException} where the JDK returns the linked site. It is the only
 * difference, and it is written down here rather than hidden in a line that matches by accident.
 *
 * <p>{@link #where()} returns the index of the first answer that differs, or -1.
 */
public class DYN1 {

    static final String[] EXPECTED = {
        "errors-before|[]",
        "built|true|[]|true",
        "site|null",
        "converts|101010101111010100",
        "compares|INDETERMINATE|INDETERMINATE|INDETERMINATE",
        "filter|true",
        "threshold-negative|IllegalArgumentException",
        "threshold-zero|ok",
        "threshold-eight|ok",
        "prio-null|ok",
        "prio-one-null|NullPointerException",
        "prio-list-with-null|NullPointerException",
        "fall-null|ok",
        "fall-list-with-null|NullPointerException",
        "loose|ok",
        "others|ok",
        "no-last-resort|true|true",
        "no-discovery|true|[]",
        "twice|true",
        "link-null|NullPointerException",
    };

    /** What the two classes do, one line per check. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();

        final DynamicLinkerFactory f = new DynamicLinkerFactory();
        a.add("errors-before|" + f.getAutoLoadingErrors());
        final DynamicLinker l = f.createLinker();
        a.add("built|" + (l != null) + "|" + f.getAutoLoadingErrors()
                + "|" + (l.getLinkerServices() != null));
        a.add("site|" + DynamicLinker.getLinkedCallSiteLocation());

        // The conversions the language allows.
        final LinkerServices s = l.getLinkerServices();
        final Class<?>[][] pairs = {
            {int.class, long.class},
            {long.class, int.class},
            {int.class, double.class},
            {double.class, int.class},
            {char.class, int.class},
            {int.class, char.class},
            {byte.class, short.class},
            {short.class, byte.class},
            {int.class, Integer.class},
            {Integer.class, int.class},
            {int.class, Object.class},
            {String.class, Object.class},
            {Object.class, String.class},
            {String.class, CharSequence.class},
            {CharSequence.class, String.class},
            {String.class, String.class},
            {int.class, boolean.class},
            {boolean.class, int.class},
        };
        final StringBuilder conv = new StringBuilder();
        for (int i = 0; i < pairs.length; i++) {
            conv.append(s.canConvert(pairs[i][0], pairs[i][1]) ? '1' : '0');
        }
        a.add("converts|" + conv);

        // The preference between two targets: with no comparators registered there is none.
        a.add("compares|" + s.compareConversion(String.class, Object.class, CharSequence.class)
                + "|" + s.compareConversion(int.class, long.class, double.class)
                + "|" + s.compareConversion(Object.class, Object.class, Object.class));

        // A target that is not filtered comes back as it was.
        a.add("filter|" + (s.filterInternalObjects(null) == null));

        // The factory's configuration.
        a.add("threshold-negative|" + attempt(new Threshold(f, -1)));
        a.add("threshold-zero|" + attempt(new Threshold(f, 0)));
        a.add("threshold-eight|" + attempt(new Threshold(f, 8)));
        a.add("prio-null|" + attempt(new PrioList(f, null)));
        a.add("prio-one-null|" + attempt(new PrioOne(f)));
        final List<GuardingDynamicLinker> withNull = new ArrayList<GuardingDynamicLinker>();
        withNull.add(null);
        a.add("prio-list-with-null|" + attempt(new PrioList(f, withNull)));
        a.add("fall-null|" + attempt(new FallList(f, null)));
        a.add("fall-list-with-null|" + attempt(new FallList(f, withNull)));
        a.add("loose|" + attempt(new PrioLoose(f)));
        a.add("others|" + attempt(new Others(f)));

        // With no last resort it is built too: that is not the same as saying nothing.
        final DynamicLinkerFactory f2 = new DynamicLinkerFactory();
        f2.setFallbackLinkers(new GuardingDynamicLinker[0]);
        final DynamicLinker l2 = f2.createLinker();
        a.add("no-last-resort|" + (l2 != null)
                + "|" + l2.getLinkerServices().canConvert(int.class, long.class));

        // Turning discovery off does not keep it from being built either.
        final DynamicLinkerFactory f3 = new DynamicLinkerFactory();
        f3.setClassLoader(null);
        a.add("no-discovery|" + (f3.createLinker() != null) + "|"
                + f3.getAutoLoadingErrors());

        // More than one can be built.
        a.add("twice|" + (f.createLinker() != f.createLinker()));

        // Linking a null site fails before it gets to the handles.
        a.add("link-null|" + attempt(new LinkNull(l)));

        return a.toArray(new String[a.size()]);
    }

    /** Something run to see what it fails with. */
    interface Throwing {
        void run() throws Exception;
    }

    /** Runs it and returns "ok" or the simple name of whatever it threw. */
    static String attempt(Throwing r) {
        try {
            r.run();
            return "ok";
        } catch (Throwable t) {
            final String n = t.getClass().getName();
            return n.substring(n.lastIndexOf('.') + 1);
        }
    }

    static class Threshold implements Throwing {
        private final DynamicLinkerFactory f;
        private final int n;

        Threshold(DynamicLinkerFactory f, int n) {
            this.f = f;
            this.n = n;
        }

        public void run() throws Exception {
            f.setUnstableRelinkThreshold(this.n);
        }
    }

    static class PrioList implements Throwing {
        private final DynamicLinkerFactory f;
        private final List<GuardingDynamicLinker> l;

        PrioList(DynamicLinkerFactory f, List<GuardingDynamicLinker> l) {
            this.f = f;
            this.l = l;
        }

        public void run() throws Exception {
            f.setPrioritizedLinkers(this.l);
        }
    }

    static class FallList implements Throwing {
        private final DynamicLinkerFactory f;
        private final List<GuardingDynamicLinker> l;

        FallList(DynamicLinkerFactory f, List<GuardingDynamicLinker> l) {
            this.f = f;
            this.l = l;
        }

        public void run() throws Exception {
            f.setFallbackLinkers(this.l);
        }
    }

    static class PrioOne implements Throwing {
        private final DynamicLinkerFactory f;

        PrioOne(DynamicLinkerFactory f) {
            this.f = f;
        }

        public void run() throws Exception {
            f.setPrioritizedLinker(null);
        }
    }

    static class PrioLoose implements Throwing {
        private final DynamicLinkerFactory f;

        PrioLoose(DynamicLinkerFactory f) {
            this.f = f;
        }

        public void run() throws Exception {
            f.setPrioritizedLinkers(new GuardingDynamicLinker[0]);
            f.setFallbackLinkers(new GuardingDynamicLinker[0]);
        }
    }

    static class Others implements Throwing {
        private final DynamicLinkerFactory f;

        Others(DynamicLinkerFactory f) {
            this.f = f;
        }

        public void run() throws Exception {
            f.setSyncOnRelink(true);
            f.setPrelinkTransformer(null);
            f.setAutoConversionStrategy(null);
            f.setInternalObjectsFilter(null);
        }
    }

    static class LinkNull implements Throwing {
        private final DynamicLinker l;

        LinkNull(DynamicLinker l) {
            this.l = l;
        }

        public void run() throws Exception {
            l.link(null);
        }
    }

    /**
     * The index of the first answer that differs from the JDK's, or -1.
     *
     * @return the index, or -1
     */
    public static int where() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        if (a.length != EXPECTED.length) {
            return 8000 + a.length;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(EXPECTED[i])) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        final String[] a = actual();
        if (args.length > 0) {
            for (int i = 0; i < a.length; i++) {
                System.out.println(a[i]);
            }
            return;
        }
        final int i = where();
        System.out.println(i < 0 ? "no differences"
                : i + ":\n  ours=" + a[i] + "\n  jdk =" + EXPECTED[i]);
    }
}
