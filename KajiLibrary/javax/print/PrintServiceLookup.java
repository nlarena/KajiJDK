package javax.print;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ServiceLoader;
import javax.print.attribute.AttributeSet;

/**
 * KajiLibrary's javax.print.PrintServiceLookup -- finds printers.
 *
 * <p>It has two faces that are best not confused. The <b>static</b> methods are the API of whoever
 * looks for a printer; the <b>abstract</b> ones are what whoever provides printers implements. The
 * same class is both the facade and the provider contract.
 *
 * <h2>Where the providers come from</h2>
 *
 * <p>From two places that add up:
 *
 * <ul>
 *   <li>the ones declared as a {@code javax.print.PrintServiceLookup} service and found with
 *       {@link ServiceLoader}. It is how the operating system's printers show up;
 *   <li>the ones registered by hand with {@link #registerServiceProvider}.
 * </ul>
 *
 * <p>{@link #registerService} is different from both: it registers <b>a loose printer</b>, without
 * a provider. It serves to add something put together in the program.
 *
 * <h2>The filters</h2>
 *
 * <p>The lookups take a {@link DocFlavor} and an {@link AttributeSet}, and both accept null to say
 * "I do not care". The set does not filter by equality: it filters by what the printer <b>can</b>
 * give, so asking for two copies returns the ones that support at least two.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library ships no provider: talking to the operating system's print system needs native
 * code, and there is none. The methods work --they walk the {@link ServiceLoader} and the ones
 * registered by hand-- and return empty or null because there is nothing to find, which is exactly
 * what the API defines for a machine without printers. Registering a provider, this works without
 * changes.
 */
public abstract class PrintServiceLookup {

    /** The providers registered by hand. */
    private static final ArrayList<PrintServiceLookup> REGISTERED =
        new ArrayList<PrintServiceLookup>();

    /** The loose printers registered by hand. */
    private static final ArrayList<PrintService> REGISTERED_SERVICES =
        new ArrayList<PrintService>();

    /** For subclasses. */
    protected PrintServiceLookup() {
    }

    /**
     * The printers that accept that format and those attributes.
     *
     * @param flavor the format, or null not to filter
     * @param attributes what one intends to ask for, or null
     * @return the ones that serve; never null, may be empty
     */
    public static final PrintService[] lookupPrintServices(DocFlavor flavor,
                                                           AttributeSet attributes) {
        ArrayList<PrintService> found = new ArrayList<PrintService>();
        Iterator<PrintServiceLookup> providers = allProviders();
        while (providers.hasNext()) {
            PrintService[] some = providers.next().getPrintServices(flavor, attributes);
            addAll(found, some);
        }
        int i = 0;
        while (i < REGISTERED_SERVICES.size()) {
            PrintService s = REGISTERED_SERVICES.get(i);
            if (matches(s, flavor, attributes) && !found.contains(s)) {
                found.add(s);
            }
            i = i + 1;
        }
        return found.toArray(new PrintService[found.size()]);
    }

    /**
     * The ones that accept jobs of several documents.
     *
     * <p>The first argument is an <b>array</b> of formats, and it has to be read right: a printer
     * qualifies if it supports <b>all</b> of them, not any. It is what corresponds, because the
     * documents of a job go together.
     */
    public static final MultiDocPrintService[] lookupMultiDocPrintServices(
        DocFlavor[] flavors, AttributeSet attributes) {
        ArrayList<MultiDocPrintService> found = new ArrayList<MultiDocPrintService>();
        Iterator<PrintServiceLookup> providers = allProviders();
        while (providers.hasNext()) {
            MultiDocPrintService[] some =
                providers.next().getMultiDocPrintServices(flavors, attributes);
            if (some != null) {
                int i = 0;
                while (i < some.length) {
                    if (some[i] != null && !found.contains(some[i])) {
                        found.add(some[i]);
                    }
                    i = i + 1;
                }
            }
        }
        return found.toArray(new MultiDocPrintService[found.size()]);
    }

    /** The default printer, or null if there is none. */
    public static final PrintService lookupDefaultPrintService() {
        Iterator<PrintServiceLookup> providers = allProviders();
        while (providers.hasNext()) {
            PrintService s = providers.next().getDefaultPrintService();
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    /**
     * Registers a provider.
     *
     * @return whether it was registered; false if it is null or was already there
     */
    public static boolean registerServiceProvider(PrintServiceLookup sp) {
        if (sp == null) {
            return false;
        }
        synchronized (REGISTERED) {
            int i = 0;
            while (i < REGISTERED.size()) {
                if (REGISTERED.get(i).getClass() == sp.getClass()) {
                    return false;
                }
                i = i + 1;
            }
            REGISTERED.add(sp);
        }
        return true;
    }

    /**
     * Registers a loose printer. See the class note on the difference from
     * {@link #registerServiceProvider}.
     *
     * @return whether it was registered; false if it is null or was already there
     */
    public static boolean registerService(PrintService service) {
        if (service == null || service instanceof StreamPrintService) {
            return false;
        }
        synchronized (REGISTERED_SERVICES) {
            if (REGISTERED_SERVICES.contains(service)) {
                return false;
            }
            REGISTERED_SERVICES.add(service);
        }
        return true;
    }

    /** This provider's ones that serve for that format and those attributes. */
    public abstract PrintService[] getPrintServices(DocFlavor flavor, AttributeSet attributes);

    /** All of this provider's. */
    public abstract PrintService[] getPrintServices();

    /** The multi-document ones. See {@link #lookupMultiDocPrintServices}: all are needed. */
    public abstract MultiDocPrintService[] getMultiDocPrintServices(DocFlavor[] flavors,
                                                                    AttributeSet attributes);

    /** This provider's default one, or null. */
    public abstract PrintService getDefaultPrintService();

    /** The ones from the {@link ServiceLoader} and the ones registered by hand, in that order. */
    private static Iterator<PrintServiceLookup> allProviders() {
        ArrayList<PrintServiceLookup> all = new ArrayList<PrintServiceLookup>();
        try {
            Iterator<PrintServiceLookup> loaded =
                ServiceLoader.load(PrintServiceLookup.class).iterator();
            while (loaded.hasNext()) {
                all.add(loaded.next());
            }
        } catch (Throwable e) {
            // A broken provider cannot bring the whole lookup down; the rest go on.
        }
        synchronized (REGISTERED) {
            all.addAll(REGISTERED);
        }
        return all.iterator();
    }

    /** Adds the ones that are not repeated. */
    private static void addAll(ArrayList<PrintService> into, PrintService[] some) {
        if (some == null) {
            return;
        }
        int i = 0;
        while (i < some.length) {
            if (some[i] != null && !into.contains(some[i])) {
                into.add(some[i]);
            }
            i = i + 1;
        }
    }

    /** The filter applied to a loose registered printer. */
    private static boolean matches(PrintService s, DocFlavor flavor, AttributeSet attributes) {
        if (flavor != null && !s.isDocFlavorSupported(flavor)) {
            return false;
        }
        if (attributes != null) {
            AttributeSet bad = s.getUnsupportedAttributes(flavor, attributes);
            if (bad != null && !bad.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
