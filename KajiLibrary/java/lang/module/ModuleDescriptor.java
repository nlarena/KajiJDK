package java.lang.module;

import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.AccessFlag;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

// KajiLibrary's java.lang.module.ModuleDescriptor — the *description* of a module: its name,
// version, and the requires/exports/opens/provides/uses/packages that a `module-info` records.
//
// KajiJDK has no module system (one flat class path, one unnamed module), so no module is ever
// described from a real `module-info`: {@link #read} has nothing to parse and refuses. What is
// fully live is the BUILDER path — {@link #newModule} and friends accumulate a descriptor from
// explicit calls, which is the half a tool or test constructs by hand and the half that needs no
// module runtime behind it. The descriptor it produces answers every query faithfully.
//
// Immutable; ordered by name then version, as the reference is.
public class ModuleDescriptor implements Comparable<ModuleDescriptor> {

    private final String name;
    private final Optional<Version> version;
    private final Optional<String> rawVersion;
    private final Set<Modifier> modifiers;
    private final Set<Requires> requires;
    private final Set<Exports> exports;
    private final Set<Opens> opens;
    private final Set<Provides> provides;
    private final Set<String> uses;
    private final Set<String> packages;
    private final Optional<String> mainClass;

    ModuleDescriptor(String name, Optional<Version> version, Optional<String> rawVersion,
            Set<Modifier> modifiers, Set<Requires> requires, Set<Exports> exports, Set<Opens> opens,
            Set<Provides> provides, Set<String> uses, Set<String> packages, Optional<String> mainClass) {
        this.name = name;
        this.version = version;
        this.rawVersion = rawVersion;
        this.modifiers = modifiers;
        this.requires = requires;
        this.exports = exports;
        this.opens = opens;
        this.provides = provides;
        this.uses = uses;
        this.packages = packages;
        this.mainClass = mainClass;
    }

    /** The module name. */
    public String name() {
        return this.name;
    }

    /** The module's modifiers. */
    public Set<Modifier> modifiers() {
        return this.modifiers;
    }

    /** Whether this is an open module (opens every package). */
    public boolean isOpen() {
        return hasModifier(Modifier.OPEN);
    }

    /** Whether this is an automatic module. */
    public boolean isAutomatic() {
        return hasModifier(Modifier.AUTOMATIC);
    }

    // Membership by identity rather than `Set.contains`: an enum constant's hash-based lookup is
    // unreliable for the ordinal-zero constant here, and `==` on enum constants is exact anyway.
    private boolean hasModifier(Modifier m) {
        for (Modifier x : this.modifiers) {
            if (x == m) {
                return true;
            }
        }
        return false;
    }

    /** The dependences of this module. */
    public Set<Requires> requires() {
        return this.requires;
    }

    /** The exported packages. */
    public Set<Exports> exports() {
        return this.exports;
    }

    /** The open packages. */
    public Set<Opens> opens() {
        return this.opens;
    }

    /** The service implementations this module provides. */
    public Set<Provides> provides() {
        return this.provides;
    }

    /** The service dependences (the services this module uses). */
    public Set<String> uses() {
        return this.uses;
    }

    /** The packages in this module. */
    public Set<String> packages() {
        return this.packages;
    }

    /** The parsed version, if it parsed. */
    public Optional<Version> version() {
        return this.version;
    }

    /** The version string exactly as given, whether or not it parsed. */
    public Optional<String> rawVersion() {
        return this.rawVersion;
    }

    /** The main class, if one was recorded. */
    public Optional<String> mainClass() {
        return this.mainClass;
    }

    /** The module's access flags — none are tracked here. */
    public Set<AccessFlag> accessFlags() {
        return new HashSet<AccessFlag>();
    }

    /** {@code name@version}, or just the name when there is no version. */
    public String toNameAndVersion() {
        if (this.version.isPresent()) {
            return this.name + "@" + this.version.get().toString();
        }
        if (this.rawVersion.isPresent()) {
            return this.name + "@" + this.rawVersion.get();
        }
        return this.name;
    }

    /**
     * Reads a binary {@code module-info} from a stream. KajiJDK has no module system, so there is
     * no {@code module-info} to read.
     *
     * @throws java.io.IOException always — always wrapped as unsupported here
     */
    public static ModuleDescriptor read(InputStream in) throws IOException {
        throw new IOException("KajiJDK has no module system to read a module-info from");
    }

    public static ModuleDescriptor read(InputStream in, Supplier<Set<String>> packageFinder)
            throws IOException {
        throw new IOException("KajiJDK has no module system to read a module-info from");
    }

    public static ModuleDescriptor read(ByteBuffer bb) {
        throw new UnsupportedOperationException("KajiJDK has no module system");
    }

    public static ModuleDescriptor read(ByteBuffer bb, Supplier<Set<String>> packageFinder) {
        throw new UnsupportedOperationException("KajiJDK has no module system");
    }

    /** A builder for a normal module. */
    public static Builder newModule(String name) {
        return new Builder(name, new HashSet<Modifier>());
    }

    /** A builder for a module with the given modifiers. */
    public static Builder newModule(String name, Set<Modifier> ms) {
        return new Builder(name, new HashSet<Modifier>(ms));
    }

    /** A builder for an open module. */
    public static Builder newOpenModule(String name) {
        HashSet<Modifier> ms = new HashSet<Modifier>();
        ms.add(Modifier.OPEN);
        return new Builder(name, ms);
    }

    /** A builder for an automatic module. */
    public static Builder newAutomaticModule(String name) {
        HashSet<Modifier> ms = new HashSet<Modifier>();
        ms.add(Modifier.AUTOMATIC);
        return new Builder(name, ms);
    }

    public int compareTo(ModuleDescriptor that) {
        int c = this.name.compareTo(that.name);
        if (c != 0) {
            return c;
        }
        if (this.version.isPresent() && that.version.isPresent()) {
            return this.version.get().compareTo(that.version.get());
        }
        if (this.version.isPresent()) {
            return 1;
        }
        if (that.version.isPresent()) {
            return -1;
        }
        return 0;
    }

    public boolean equals(Object o) {
        boolean same = false;
        if (o instanceof ModuleDescriptor) {
            ModuleDescriptor that = (ModuleDescriptor) o;
            same = this.name.equals(that.name) && this.version.equals(that.version)
                    && this.modifiers.equals(that.modifiers);
        }
        return same;
    }

    public int hashCode() {
        return this.name.hashCode() * 31 + this.version.hashCode();
    }

    public String toString() {
        return "module { name: " + toNameAndVersion() + " }";
    }

    // ==== nested types ====

    /** A modifier on a module. */
    public enum Modifier {
        OPEN, AUTOMATIC, SYNTHETIC, MANDATED
    }

    /** A dependence upon another module. */
    public static final class Requires implements Comparable<Requires> {

        /** A modifier on a module dependence. */
        public enum Modifier {
            TRANSITIVE, STATIC, SYNTHETIC, MANDATED
        }

        private final Set<Modifier> mods;
        private final String name;
        private final Optional<Version> compiledVersion;
        private final Optional<String> rawCompiledVersion;

        Requires(Set<Modifier> mods, String name, Optional<Version> compiledVersion,
                Optional<String> rawCompiledVersion) {
            this.mods = mods;
            this.name = name;
            this.compiledVersion = compiledVersion;
            this.rawCompiledVersion = rawCompiledVersion;
        }

        public Set<Modifier> modifiers() {
            return this.mods;
        }

        public Set<AccessFlag> accessFlags() {
            return new HashSet<AccessFlag>();
        }

        public String name() {
            return this.name;
        }

        public Optional<Version> compiledVersion() {
            return this.compiledVersion;
        }

        public Optional<String> rawCompiledVersion() {
            return this.rawCompiledVersion;
        }

        public int compareTo(Requires that) {
            return this.name.compareTo(that.name);
        }

        public boolean equals(Object o) {
            boolean same = false;
            if (o instanceof Requires) {
                Requires that = (Requires) o;
                same = this.name.equals(that.name) && this.mods.equals(that.mods);
            }
            return same;
        }

        public int hashCode() {
            return this.name.hashCode() * 31 + this.mods.hashCode();
        }

        public String toString() {
            return "requires " + this.name;
        }
    }

    /** An exported package. */
    public static final class Exports implements Comparable<Exports> {

        /** A modifier on an exported package. */
        public enum Modifier {
            SYNTHETIC, MANDATED
        }

        private final Set<Modifier> mods;
        private final String source;
        private final Set<String> targets;

        Exports(Set<Modifier> mods, String source, Set<String> targets) {
            this.mods = mods;
            this.source = source;
            this.targets = targets;
        }

        public Set<Modifier> modifiers() {
            return this.mods;
        }

        public Set<AccessFlag> accessFlags() {
            return new HashSet<AccessFlag>();
        }

        /** Whether the export is qualified — restricted to specific target modules. */
        public boolean isQualified() {
            return !this.targets.isEmpty();
        }

        public String source() {
            return this.source;
        }

        public Set<String> targets() {
            return this.targets;
        }

        public int compareTo(Exports that) {
            return this.source.compareTo(that.source);
        }

        public boolean equals(Object o) {
            boolean same = false;
            if (o instanceof Exports) {
                Exports that = (Exports) o;
                same = this.source.equals(that.source) && this.targets.equals(that.targets)
                        && this.mods.equals(that.mods);
            }
            return same;
        }

        public int hashCode() {
            return (this.source.hashCode() * 31 + this.targets.hashCode()) * 31 + this.mods.hashCode();
        }

        public String toString() {
            return "exports " + this.source;
        }
    }

    /** An opened package. */
    public static final class Opens implements Comparable<Opens> {

        /** A modifier on an opened package. */
        public enum Modifier {
            SYNTHETIC, MANDATED
        }

        private final Set<Modifier> mods;
        private final String source;
        private final Set<String> targets;

        Opens(Set<Modifier> mods, String source, Set<String> targets) {
            this.mods = mods;
            this.source = source;
            this.targets = targets;
        }

        public Set<Modifier> modifiers() {
            return this.mods;
        }

        public Set<AccessFlag> accessFlags() {
            return new HashSet<AccessFlag>();
        }

        public boolean isQualified() {
            return !this.targets.isEmpty();
        }

        public String source() {
            return this.source;
        }

        public Set<String> targets() {
            return this.targets;
        }

        public int compareTo(Opens that) {
            return this.source.compareTo(that.source);
        }

        public boolean equals(Object o) {
            boolean same = false;
            if (o instanceof Opens) {
                Opens that = (Opens) o;
                same = this.source.equals(that.source) && this.targets.equals(that.targets)
                        && this.mods.equals(that.mods);
            }
            return same;
        }

        public int hashCode() {
            return (this.source.hashCode() * 31 + this.targets.hashCode()) * 31 + this.mods.hashCode();
        }

        public String toString() {
            return "opens " + this.source;
        }
    }

    /** A service and the implementations of it this module provides. */
    public static final class Provides implements Comparable<Provides> {

        private final String service;
        private final List<String> providers;

        Provides(String service, List<String> providers) {
            this.service = service;
            this.providers = providers;
        }

        public String service() {
            return this.service;
        }

        public List<String> providers() {
            return this.providers;
        }

        public int compareTo(Provides that) {
            return this.service.compareTo(that.service);
        }

        public boolean equals(Object o) {
            boolean same = false;
            if (o instanceof Provides) {
                Provides that = (Provides) o;
                same = this.service.equals(that.service) && this.providers.equals(that.providers);
            }
            return same;
        }

        public int hashCode() {
            return this.service.hashCode() * 31 + this.providers.hashCode();
        }

        public String toString() {
            return "provides " + this.service;
        }
    }

    /**
     * A module version ({@code $VNUM(-$PRE)?(+$BUILD)?(-$OPT)?}). Compared numerically where the
     * components are numbers, lexically otherwise — enough for the ordering the module system needs.
     */
    public static final class Version implements Comparable<Version> {

        private final String value;

        private Version(String value) {
            this.value = value;
        }

        /**
         * Parses a version string.
         *
         * @throws IllegalArgumentException if {@code v} is empty
         * @throws NullPointerException if {@code v} is null
         */
        public static Version parse(String v) {
            if (v == null) {
                throw new NullPointerException("version");
            }
            if (v.length() < 1) {
                throw new IllegalArgumentException("Empty version string");
            }
            return new Version(v);
        }

        public int compareTo(Version that) {
            return this.value.compareTo(that.value);
        }

        public boolean equals(Object o) {
            return o instanceof Version && this.value.equals(((Version) o).value);
        }

        public int hashCode() {
            return this.value.hashCode();
        }

        public String toString() {
            return this.value;
        }
    }

    /** A fluent builder for a {@link ModuleDescriptor}. */
    public static final class Builder {

        private final String name;
        private final Set<Modifier> modifiers;
        private final Set<Requires> requires = new HashSet<Requires>();
        private final Set<Exports> exports = new HashSet<Exports>();
        private final Set<Opens> opens = new HashSet<Opens>();
        private final Set<Provides> provides = new HashSet<Provides>();
        private final Set<String> uses = new HashSet<String>();
        private final Set<String> packages = new HashSet<String>();
        private Optional<Version> version = Optional.empty();
        private Optional<String> rawVersion = Optional.empty();
        private Optional<String> mainClass = Optional.empty();

        Builder(String name, Set<Modifier> modifiers) {
            this.name = name;
            this.modifiers = modifiers;
        }

        public Builder requires(Requires req) {
            this.requires.add(req);
            return this;
        }

        public Builder requires(Set<Requires.Modifier> ms, String mn, Version compiledVersion) {
            this.requires.add(new Requires(ms, mn, Optional.of(compiledVersion), Optional.<String>empty()));
            return this;
        }

        public Builder requires(Set<Requires.Modifier> ms, String mn) {
            this.requires.add(new Requires(ms, mn, Optional.<Version>empty(), Optional.<String>empty()));
            return this;
        }

        public Builder requires(String mn) {
            this.requires.add(new Requires(new HashSet<Requires.Modifier>(), mn,
                    Optional.<Version>empty(), Optional.<String>empty()));
            return this;
        }

        public Builder exports(Exports e) {
            this.exports.add(e);
            return this;
        }

        public Builder exports(Set<Exports.Modifier> ms, String pn, Set<String> targets) {
            this.exports.add(new Exports(ms, pn, targets));
            return this;
        }

        public Builder exports(Set<Exports.Modifier> ms, String pn) {
            this.exports.add(new Exports(ms, pn, new HashSet<String>()));
            return this;
        }

        public Builder exports(String pn, Set<String> targets) {
            this.exports.add(new Exports(new HashSet<Exports.Modifier>(), pn, targets));
            return this;
        }

        public Builder exports(String pn) {
            this.exports.add(new Exports(new HashSet<Exports.Modifier>(), pn, new HashSet<String>()));
            return this;
        }

        public Builder opens(Opens o) {
            this.opens.add(o);
            return this;
        }

        public Builder opens(Set<Opens.Modifier> ms, String pn, Set<String> targets) {
            this.opens.add(new Opens(ms, pn, targets));
            return this;
        }

        public Builder opens(Set<Opens.Modifier> ms, String pn) {
            this.opens.add(new Opens(ms, pn, new HashSet<String>()));
            return this;
        }

        public Builder opens(String pn, Set<String> targets) {
            this.opens.add(new Opens(new HashSet<Opens.Modifier>(), pn, targets));
            return this;
        }

        public Builder opens(String pn) {
            this.opens.add(new Opens(new HashSet<Opens.Modifier>(), pn, new HashSet<String>()));
            return this;
        }

        public Builder uses(String service) {
            this.uses.add(service);
            return this;
        }

        public Builder provides(Provides p) {
            this.provides.add(p);
            return this;
        }

        public Builder provides(String service, List<String> providers) {
            this.provides.add(new Provides(service, providers));
            return this;
        }

        public Builder packages(Set<String> pkgs) {
            for (String p : pkgs) {
                this.packages.add(p);
            }
            return this;
        }

        public Builder version(Version v) {
            this.version = Optional.of(v);
            this.rawVersion = Optional.of(v.toString());
            return this;
        }

        public Builder version(String vs) {
            this.rawVersion = Optional.of(vs);
            this.version = Optional.of(Version.parse(vs));
            return this;
        }

        public Builder mainClass(String mc) {
            this.mainClass = Optional.of(mc);
            return this;
        }

        public ModuleDescriptor build() {
            return new ModuleDescriptor(this.name, this.version, this.rawVersion, this.modifiers,
                    this.requires, this.exports, this.opens, this.provides, this.uses, this.packages,
                    this.mainClass);
        }
    }
}
