package java.security;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

// A catalogue of cryptographic implementations: which algorithms it knows how to do, and with
// which class.
//
// ===============================================================================================
// WHY IT IS A `Properties` AND NOT JUST ANY MAP
// ===============================================================================================
//
// That it inherits from `Properties` looks like an accident and is history: in 1.2 a provider
// **was** a properties file, with lines such as
//
//     MessageDigest.SHA-256 = com.example.SHA256
//     Alg.Alias.MessageDigest.SHA256 = SHA-256
//     MessageDigest.SHA-256 ImplementedIn = Software
//
// and the whole search for algorithms was looking up a key. In 1.5 `Provider.Service` was added,
// which is the typed way of saying the same thing, but the properties could not be taken out
// because there was code reading them. So this class keeps **both views synchronised**:
// `putService` writes the properties as well, and any change in the properties invalidates the
// table of services so that it is rebuilt. That is most of what this file does.
//
// The rebuilding is lazy —a flag and a complete reassembly— and not incremental on purpose: a
// `putAll` or a `replaceAll` can touch anything, and following them one change at a time is where
// the inconsistencies creep in.
//
// ===============================================================================================
// WHAT IS NOT THERE
// ===============================================================================================
//
// `Provider` is abstract and this class registers no algorithm: the algorithms are put in by
// whoever extends it. In this library the only one that does is `KajiProvider`, with the six
// digests.
//
// `getDefaultSecureRandomService()` is not there because it is package-private in the JDK and there
// is no `SecureRandom` to consume it.
public abstract class Provider extends Properties {

    private final String name;
    private final String versionStr;
    private final double version;
    private final String info;

    // Services put in with `putService`, by "type.algorithm" in lower case.
    private final Map<String, Service> services = new LinkedHashMap<String, Service>();

    // "type.alias" -> "type.algorithm". The aliases of a typed service are resolved here and not by
    // rereading the properties: if not, looking up by alias would land in the deduced copy, which
    // does not know how to instantiate itself like the original.
    private final Map<String, String> serviceAliases = new HashMap<String, String>();

    // Services deduced from the properties. It is reassembled whole when `legacyChanged`.
    private Map<String, Service> legacy = new LinkedHashMap<String, Service>();

    private boolean legacyChanged = true;

    // It cuts the recursion: `putService` writes properties, and those writes must not invalidate
    // the table that generated them.
    private boolean writingService;

    // A provider with a numeric version.
    //
    // Deprecated in the JDK: a `double` cannot express "1.2.3" or "21-ea", and comparing versions
    // by subtracting floats orders them wrongly as soon as there are three components.
    @Deprecated
    protected Provider(String name, double version, String info) {
        this.name = name;
        this.version = version;
        this.versionStr = Double.toString(version);
        this.info = info;
    }

    protected Provider(String name, String versionStr, String info) {
        this.name = name;
        this.versionStr = versionStr;
        this.version = parseVersion(versionStr);
        this.info = info;
    }

    // The `double` that corresponds to the first two components of the string.
    //
    // It only exists so that `getVersion()` goes on answering something reasonable. If it cannot be
    // read, it returns 0: inventing a number would be worse than saying "I do not know".
    private static double parseVersion(String s) {
        if (s == null) {
            return 0d;
        }
        int i = 0;
        int dots = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '.') {
                dots = dots + 1;
                if (dots > 1) {
                    break;
                }
            } else if (c < '0' || c > '9') {
                break;
            }
            i = i + 1;
        }
        if (i == 0) {
            return 0d;
        }
        try {
            return Double.parseDouble(s.substring(0, i));
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    // It returns a provider configured with `configArg`.
    //
    // The base implementation does **not** know how to configure itself and says so by throwing. It
    // is the behaviour of the JDK and it is the right one: only a provider that has something to
    // configure —typically one that talks to a PKCS#11 token— can know what the argument means.
    public Provider configure(String configArg) {
        throw new UnsupportedOperationException("configure is not supported");
    }

    // Whether this provider needs no configuration, or has received it already. The base one is
    // always ready.
    public boolean isConfigured() {
        return true;
    }

    public String getName() {
        return this.name;
    }

    @Deprecated
    public double getVersion() {
        return this.version;
    }

    public String getVersionStr() {
        return this.versionStr;
    }

    public String getInfo() {
        return this.info;
    }

    @Override
    public String toString() {
        return this.name + " version " + this.versionStr;
    }

    // -------------------------------------------------------------------------------------------
    // The properties view. Everything that mutates invalidates the deduced table of services.
    // -------------------------------------------------------------------------------------------

    @Override
    public synchronized void clear() {
        this.services.clear();
        this.serviceAliases.clear();
        this.legacyChanged = true;
        super.clear();
    }

    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        this.legacyChanged = true;
        super.load(inStream);
    }

    @Override
    public synchronized void putAll(Map<?, ?> t) {
        this.legacyChanged = true;
        super.putAll(t);
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        if (!this.writingService) {
            this.legacyChanged = true;
        }
        return super.put(key, value);
    }

    @Override
    public synchronized Object putIfAbsent(Object key, Object value) {
        this.legacyChanged = true;
        return super.putIfAbsent(key, value);
    }

    @Override
    public synchronized Object remove(Object key) {
        if (!this.writingService) {
            this.legacyChanged = true;
        }
        return super.remove(key);
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
        this.legacyChanged = true;
        return super.remove(key, value);
    }

    @Override
    public synchronized boolean replace(Object key, Object oldValue, Object newValue) {
        this.legacyChanged = true;
        return super.replace(key, oldValue, newValue);
    }

    @Override
    public synchronized Object replace(Object key, Object value) {
        this.legacyChanged = true;
        return super.replace(key, value);
    }

    @Override
    public synchronized void replaceAll(
            BiFunction<? super Object, ? super Object, ? extends Object> function) {
        this.legacyChanged = true;
        super.replaceAll(function);
    }

    @Override
    public synchronized Object compute(Object key,
            BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        this.legacyChanged = true;
        return super.compute(key, remappingFunction);
    }

    @Override
    public synchronized Object computeIfAbsent(Object key,
            Function<? super Object, ? extends Object> mappingFunction) {
        this.legacyChanged = true;
        return super.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public synchronized Object computeIfPresent(Object key,
            BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        this.legacyChanged = true;
        return super.computeIfPresent(key, remappingFunction);
    }

    @Override
    public synchronized Object merge(Object key, Object value,
            BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        this.legacyChanged = true;
        return super.merge(key, value, remappingFunction);
    }

    // The three views are handed over **unmodifiable**. It is not tidiness: mutating through the
    // view would skip the `put`s above and leave the table of services describing a catalogue that
    // is no longer the one that is there.
    @Override
    public synchronized Set<Map.Entry<Object, Object>> entrySet() {
        return Collections.unmodifiableSet(super.entrySet());
    }

    @Override
    public Set<Object> keySet() {
        return Collections.unmodifiableSet(super.keySet());
    }

    @Override
    public Collection<Object> values() {
        return Collections.unmodifiableCollection(super.values());
    }

    // -------------------------------------------------------------------------------------------
    // The services view.
    // -------------------------------------------------------------------------------------------

    // The service that implements `algorithm` for `type`, or null.
    //
    // It looks first among the ones put in with `putService` and then among the ones deduced from
    // the properties: if both define the same pair, the typed one wins, which is the one that
    // brings the complete attributes.
    public synchronized Service getService(String type, String algorithm) {
        if (type == null || algorithm == null) {
            throw new NullPointerException();
        }
        String key = key(type, algorithm);
        Service s = this.services.get(key);
        if (s != null) {
            return s;
        }
        String canonical = this.serviceAliases.get(key);
        if (canonical != null) {
            s = this.services.get(canonical);
            if (s != null) {
                return s;
            }
        }
        this.ensureLegacy();
        return this.legacy.get(key);
    }

    // Every service of this provider, without repeating.
    //
    // It is deduplicated by the pair (type, algorithm) **of the service**, not by the key it was
    // found with: an algorithm with three aliases is three times in the lookup tables and has to
    // come out of here only once. When the same pair appears typed and deduced, the typed one wins.
    public synchronized Set<Service> getServices() {
        this.ensureLegacy();
        LinkedHashMap<String, Service> seen = new LinkedHashMap<String, Service>();
        Iterator<String> it = this.legacy.keySet().iterator();
        while (it.hasNext()) {
            Service s = this.legacy.get(it.next());
            seen.put(key(s.getType(), s.getAlgorithm()), s);
        }
        Iterator<String> it2 = this.services.keySet().iterator();
        while (it2.hasNext()) {
            Service s = this.services.get(it2.next());
            seen.put(key(s.getType(), s.getAlgorithm()), s);
        }
        LinkedHashSet<Service> out = new LinkedHashSet<Service>();
        Iterator<String> it3 = seen.keySet().iterator();
        while (it3.hasNext()) {
            out.add(seen.get(it3.next()));
        }
        return Collections.unmodifiableSet(out);
    }

    // It adds a service, and writes the equivalent properties as well so that whoever reads the
    // provider the old way sees the same thing.
    protected synchronized void putService(Service s) {
        if (s == null) {
            throw new NullPointerException();
        }
        if (s.getProvider() != this) {
            throw new IllegalArgumentException(
                "service.getProvider() must match this Provider object");
        }
        String canonical = key(s.getType(), s.getAlgorithm());
        this.services.put(canonical, s);
        this.writingService = true;
        try {
            super.put(s.getType() + "." + s.getAlgorithm(), s.getClassName());
            List<String> alias = s.getAliases();
            int i = 0;
            while (i < alias.size()) {
                this.serviceAliases.put(key(s.getType(), alias.get(i)), canonical);
                super.put("Alg.Alias." + s.getType() + "." + alias.get(i), s.getAlgorithm());
                i = i + 1;
            }
            Iterator<String> at = s.attributeNames().iterator();
            while (at.hasNext()) {
                String a = at.next();
                super.put(s.getType() + "." + s.getAlgorithm() + " " + a, s.getAttribute(a));
            }
        } finally {
            this.writingService = false;
        }
    }

    protected synchronized void removeService(Service s) {
        if (s == null) {
            throw new NullPointerException();
        }
        this.services.remove(key(s.getType(), s.getAlgorithm()));
        this.writingService = true;
        try {
            super.remove(s.getType() + "." + s.getAlgorithm());
            List<String> alias = s.getAliases();
            int i = 0;
            while (i < alias.size()) {
                this.serviceAliases.remove(key(s.getType(), alias.get(i)));
                super.remove("Alg.Alias." + s.getType() + "." + alias.get(i));
                i = i + 1;
            }
            Iterator<String> at = s.attributeNames().iterator();
            while (at.hasNext()) {
                super.remove(s.getType() + "." + s.getAlgorithm() + " " + at.next());
            }
        } finally {
            this.writingService = false;
        }
    }

    // The names of type and algorithm are case-insensitive: "SHA-256" and "sha-256" are the same
    // algorithm, and the catalogue has to find it written either way.
    private static String key(String type, String algorithm) {
        return type.toLowerCase() + "." + algorithm.toLowerCase();
    }

    // It reassembles the deduced table if any property changed.
    //
    // Three passes because the lines can come in any order: first the ones that define classes,
    // then the attributes —which need the service to exist already— and last the aliases, which
    // point at an algorithm that may have been defined afterwards.
    private void ensureLegacy() {
        if (!this.legacyChanged) {
            return;
        }
        this.legacyChanged = false;
        LinkedHashMap<String, Service> fresh = new LinkedHashMap<String, Service>();
        ArrayList<String[]> attrs = new ArrayList<String[]>();
        ArrayList<String[]> alias = new ArrayList<String[]>();

        Iterator<Map.Entry<Object, Object>> it = super.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, Object> e = it.next();
            if (!(e.getKey() instanceof String) || !(e.getValue() instanceof String)) {
                continue;
            }
            String k = ((String) e.getKey()).trim();
            String v = ((String) e.getValue()).trim();
            if (k.startsWith("Alg.Alias.")) {
                String rest = k.substring("Alg.Alias.".length());
                int p = rest.indexOf('.');
                if (p > 0 && p < rest.length() - 1) {
                    alias.add(new String[] {rest.substring(0, p), rest.substring(p + 1), v});
                }
                continue;
            }
            int sp = k.indexOf(' ');
            if (sp > 0) {
                String left = k.substring(0, sp);
                String attr = k.substring(sp + 1).trim();
                int p = left.indexOf('.');
                if (p > 0 && p < left.length() - 1 && !attr.isEmpty()) {
                    attrs.add(new String[] {left.substring(0, p), left.substring(p + 1), attr, v});
                }
                continue;
            }
            int p = k.indexOf('.');
            if (p > 0 && p < k.length() - 1) {
                String type = k.substring(0, p);
                String alg = k.substring(p + 1);
                fresh.put(key(type, alg),
                    new Service(this, type, alg, v, new ArrayList<String>(),
                                new HashMap<String, String>()));
            }
        }

        int i = 0;
        while (i < attrs.size()) {
            String[] a = attrs.get(i);
            Service s = fresh.get(key(a[0], a[1]));
            if (s != null) {
                s.addAttribute(a[2], a[3]);
            }
            i = i + 1;
        }

        i = 0;
        while (i < alias.size()) {
            String[] a = alias.get(i);
            Service s = fresh.get(key(a[0], a[2]));
            if (s != null) {
                s.addAlias(a[1]);
                fresh.put(key(a[0], a[1]), s);
            }
            i = i + 1;
        }
        this.legacy = fresh;
    }

    // ===========================================================================================
    // A concrete algorithm offered by a provider.
    // ===========================================================================================
    //
    // What it contributes over the equivalent properties line is that it **knows how to instantiate
    // itself**: instead of each factory reading a class name and doing reflection on its own, the
    // service is asked and it decides how. A provider that has the classes to hand can subclass
    // this and return them directly, without reflection — which is what `KajiProvider` does.
    public static class Service {

        private final Provider provider;
        private final String type;
        private final String algorithm;
        private final String className;
        private final List<String> aliases;
        private final Map<String, String> attributes;

        public Service(Provider provider, String type, String algorithm, String className,
                       List<String> aliases, Map<String, String> attributes) {
            if (provider == null || type == null || algorithm == null || className == null) {
                throw new NullPointerException();
            }
            this.provider = provider;
            this.type = type;
            this.algorithm = algorithm;
            this.className = className;
            this.aliases = aliases == null
                ? new ArrayList<String>() : new ArrayList<String>(aliases);
            this.attributes = new HashMap<String, String>();
            if (attributes != null) {
                Iterator<String> it = attributes.keySet().iterator();
                while (it.hasNext()) {
                    String k = it.next();
                    this.attributes.put(k.toLowerCase(), attributes.get(k));
                }
            }
        }

        public final String getType() {
            return this.type;
        }

        public final String getAlgorithm() {
            return this.algorithm;
        }

        public final Provider getProvider() {
            return this.provider;
        }

        public final String getClassName() {
            return this.className;
        }

        public final String getAttribute(String name) {
            if (name == null) {
                throw new NullPointerException();
            }
            return this.attributes.get(name.toLowerCase());
        }

        // The aliases of this algorithm. Package-private in the JDK; here too.
        final List<String> getAliases() {
            return this.aliases;
        }

        final void addAttribute(String type, String value) {
            this.attributes.put(type.toLowerCase(), value);
        }

        final void removeAttribute(String type, String value) {
            this.attributes.remove(type.toLowerCase());
        }

        final void addAlias(String alias) {
            if (!this.aliases.contains(alias)) {
                this.aliases.add(alias);
            }
        }

        final Set<String> attributeNames() {
            return this.attributes.keySet();
        }

        // A new instance of the implementation.
        //
        // The base one loads the class by name and uses the no-argument constructor.
        // `constructorParameter` is only accepted by a few types of service in the JDK —the ones
        // that receive a key or some parameters when they are built— and none of them exists in
        // this library, so here passing something other than null is an error of the caller and is
        // said as such.
        public Object newInstance(Object constructorParameter)
                throws NoSuchAlgorithmException {
            if (constructorParameter != null) {
                throw new InvalidParameterException(
                    "constructorParameter not used with " + this.type + " engines");
            }
            try {
                Class<?> c = Class.forName(this.className);
                return c.newInstance();
            } catch (Exception e) {
                throw new NoSuchAlgorithmException(
                    "Error constructing implementation (algorithm: " + this.algorithm
                    + ", provider: " + this.provider.getName() + ", class: " + this.className
                    + ")", e);
            }
        }

        // Whether this service can be used with the given parameter.
        //
        // No type of service of this library uses a parameter, so the only honest answer for a
        // non-null parameter is to reject it, and for null it is yes.
        public boolean supportsParameter(Object parameter) {
            if (parameter != null) {
                throw new InvalidParameterException(
                    "supportsParameter() not used with " + this.type + " engines");
            }
            return true;
        }

        @Override
        public String toString() {
            String s = this.provider.getName() + ": " + this.type + "." + this.algorithm
                + " -> " + this.className;
            if (!this.aliases.isEmpty()) {
                s = s + "\n  aliases: " + this.aliases.toString();
            }
            if (!this.attributes.isEmpty()) {
                s = s + "\n  attributes: " + this.attributes.toString();
            }
            return s + "\n";
        }
    }
}
