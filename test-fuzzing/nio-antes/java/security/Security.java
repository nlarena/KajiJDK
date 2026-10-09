package java.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

// The list of providers of the process, in order, and the security properties.
//
// ===============================================================================================
// THE ORDER IS THE API
// ===============================================================================================
//
// The only thing this class does is keep an **ordered** list, and that order is all of its
// semantics: `MessageDigest.getInstance("SHA-256")` keeps the first provider that offers it. That
// is why `insertProviderAt(p, 1)` is a genuinely privileged operation — it puts a provider ahead of
// all of them and with that redefines which code runs behind each algorithm of the process, without
// touching a line of the one that calls it.
//
// ===============================================================================================
// WHAT IS NOT THERE, AND WHY
// ===============================================================================================
//
// **It does not read the system's `java.security` file.** In a real JDK the initial list of
// providers and the properties come from `$JAVA_HOME/conf/security/java.security`. Here the initial
// list is a single provider —`KajiProvider`, with the digests implemented in this library— and the
// properties start empty. Reading the file of whichever JDK is installed would be worse than not
// reading it: it would promise algorithms this library does not have.
//
// **There is no discovery through `ServiceLoader`.** A provider is added by calling `addProvider`.
//
// **There is no permission check.** In the JDK each of these methods consults a
// `SecurityPermission` with the `SecurityManager`, which since JDK 24 is permanently disabled and
// checks nothing. A control that does not exist is not simulated.
public final class Security {

    // The list, in order of preference.
    private static final ArrayList<Provider> providers = new ArrayList<Provider>();

    // The security properties. They start empty: see the header.
    private static final Properties props = new Properties();

    static {
        providers.add(new KajiProvider());
    }

    // Purely static: it is not instantiated.
    private Security() {
    }

    // Deprecated since 1.2 and with no direct replacement. It looks for a property of the form
    // "<propName>.<algName>" and returns its value.
    @Deprecated
    public static String getAlgorithmProperty(String algName, String propName) {
        if (algName == null || propName == null) {
            return null;
        }
        return props.getProperty(propName + "." + algName);
    }

    // It inserts the provider at the given position (1 is the first) and returns where it was left,
    // or -1 if there was one with that name already.
    //
    // Returning -1 instead of replacing is deliberate: if inserting stepped on the one that was
    // there, adding a provider of one's own could silently deactivate another with the same name.
    // To change it one has to take it out first, and that shows in the code.
    public static synchronized int insertProviderAt(Provider provider, int position) {
        String name = provider.getName();
        if (getProvider(name) != null) {
            return -1;
        }
        int n = providers.size();
        if (position < 1 || position > n) {
            position = n + 1;
        }
        providers.add(position - 1, provider);
        return position;
    }

    // It adds the provider at the end. It returns its position, or -1 if it was there already.
    public static int addProvider(Provider provider) {
        return insertProviderAt(provider, 0);
    }

    // It takes out the provider with that name. If it is not there, it does nothing — it is not an
    // error.
    //
    // The ones that are left **move forward**: taking out the second of three leaves the third
    // second. A provider that wants to keep its position has to insert itself again.
    public static synchronized void removeProvider(String name) {
        int i = 0;
        while (i < providers.size()) {
            if (providers.get(i).getName().equals(name)) {
                providers.remove(i);
                return;
            }
            i = i + 1;
        }
    }

    // Every provider, in order. It is a copy: reordering the returned array reorders nothing.
    public static synchronized Provider[] getProviders() {
        Provider[] a = new Provider[providers.size()];
        int i = 0;
        while (i < providers.size()) {
            a[i] = providers.get(i);
            i = i + 1;
        }
        return a;
    }

    public static synchronized Provider getProvider(String name) {
        int i = 0;
        while (i < providers.size()) {
            Provider p = providers.get(i);
            if (p.getName().equals(name)) {
                return p;
            }
            i = i + 1;
        }
        return null;
    }

    // The providers that satisfy the filter, or null if none.
    //
    // The filter is a string with two forms: "MessageDigest.SHA-256" —it has the service— or
    // "MessageDigest.SHA-256 ImplementedIn:Software" —it has it and with that attribute. Returning
    // null instead of an empty array is ugly but it is the contract, and there is code that
    // compares against null.
    public static Provider[] getProviders(String filter) {
        if (filter == null) {
            throw new NullPointerException("filter cannot be null");
        }
        String f = filter.trim();
        if (f.isEmpty()) {
            throw new InvalidParameterException("filter cannot be empty");
        }
        String key = f;
        String value = null;
        int colon = f.indexOf(':');
        if (colon >= 0) {
            key = f.substring(0, colon).trim();
            value = f.substring(colon + 1).trim();
        }
        java.util.HashMap<String, String> m = new java.util.HashMap<String, String>();
        m.put(key, value);
        return getProviders(m);
    }

    // The version with several filters: a provider has to meet **all** of them.
    public static Provider[] getProviders(Map<String, String> filter) {
        if (filter == null) {
            throw new NullPointerException("filter cannot be null");
        }
        if (filter.isEmpty()) {
            return getProviders();
        }
        Provider[] all = getProviders();
        ArrayList<Provider> ok = new ArrayList<Provider>();
        int i = 0;
        while (i < all.length) {
            if (matches(all[i], filter)) {
                ok.add(all[i]);
            }
            i = i + 1;
        }
        if (ok.isEmpty()) {
            return null;
        }
        Provider[] a = new Provider[ok.size()];
        int j = 0;
        while (j < ok.size()) {
            a[j] = ok.get(j);
            j = j + 1;
        }
        return a;
    }

    private static boolean matches(Provider p, Map<String, String> filter) {
        Iterator<String> it = filter.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            if (key == null) {
                return false;
            }
            String k = key.trim();
            int dot = k.indexOf('.');
            if (dot <= 0 || dot >= k.length() - 1) {
                throw new InvalidParameterException("Invalid filter key: " + key);
            }
            String type = k.substring(0, dot);
            String rest = k.substring(dot + 1);
            String attr = null;
            int sp = rest.indexOf(' ');
            if (sp > 0) {
                attr = rest.substring(sp + 1).trim();
                rest = rest.substring(0, sp);
            }
            Provider.Service s = p.getService(type, rest);
            if (s == null) {
                return false;
            }
            if (attr != null) {
                String has = s.getAttribute(attr);
                String wanted = filter.get(key);
                if (has == null) {
                    return false;
                }
                if (wanted != null && !wanted.isEmpty() && !wanted.equalsIgnoreCase(has)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static String getProperty(String key) {
        return props.getProperty(key);
    }

    public static void setProperty(String key, String datum) {
        props.put(key, datum);
    }

    // The names of algorithm available for a type of service, in upper case and without repeating.
    public static Set<String> getAlgorithms(String serviceName) {
        if (serviceName == null) {
            throw new NullPointerException("serviceName cannot be null");
        }
        if (serviceName.isEmpty()) {
            return new HashSet<String>();
        }
        LinkedHashSet<String> out = new LinkedHashSet<String>();
        Provider[] all = getProviders();
        int i = 0;
        while (i < all.length) {
            Iterator<Provider.Service> it = all[i].getServices().iterator();
            while (it.hasNext()) {
                Provider.Service s = it.next();
                if (s.getType().equalsIgnoreCase(serviceName)) {
                    out.add(s.getAlgorithm().toUpperCase());
                }
            }
            i = i + 1;
        }
        return out;
    }
}
