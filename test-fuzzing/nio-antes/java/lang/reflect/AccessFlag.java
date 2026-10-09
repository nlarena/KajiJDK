package java.lang.reflect;

import java.util.EnumSet;
import java.util.Set;

// KajiLibrary's java.lang.reflect.AccessFlag — a symbolic name for one bit of a JVMS access_flags
// mask (§4.1, §4.5, §4.6, §4.7.6, §4.7.25). It is what turns a raw `int` of flags into a set of
// meanings: `Class.accessFlags()`, `Field.accessFlags()`, etc. return a `Set<AccessFlag>` instead of
// asking callers to remember that `0x0400` is ABSTRACT here but not on a field.
//
// The same bit means different things in different places — `0x0020` is SUPER on a class, OPEN on a
// module, TRANSITIVE on a module requirement and SYNCHRONIZED on a method — so every flag also
// records WHERE it is valid (its {@link Location}s), and `maskToAccessFlags` disambiguates by
// location: at each place a given bit resolves to exactly one flag.
//
// Each flag records WHERE it is valid as a {@code Set<Location>}, built eagerly in the constructor
// exactly as the JDK does — one {@code EnumSet} per constant, materialised while the enum is still
// initialising. The constructor is handed a bitmask over {@code Location.ordinal()} (a compact way
// to write the valid places in the source) and expands it into the set on the spot.
//
// A KajiLibrary subset: KajiJDK models only the CURRENT class-file format, so the version-aware
// overloads (`locations(ClassFileFormatVersion)`, `maskToAccessFlags(int, Location, …)`) collapse to
// their no-argument form. The masks, locations and `sourceModifier` of the current format are
// faithful.
public enum AccessFlag {

    // mask, whether it is a source-level modifier, and a bitmask of the Location ordinals it is
    // valid at: CLASS=0x01, FIELD=0x02, METHOD=0x04, INNER_CLASS=0x08, METHOD_PARAMETER=0x10,
    // MODULE=0x20, MODULE_REQUIRES=0x40, MODULE_EXPORTS=0x80, MODULE_OPENS=0x100.
    PUBLIC(0x0001, true, 0x00F),
    PRIVATE(0x0002, true, 0x00E),
    PROTECTED(0x0004, true, 0x00E),
    STATIC(0x0008, true, 0x00E),
    FINAL(0x0010, true, 0x01F),
    SUPER(0x0020, false, 0x001),
    OPEN(0x0020, false, 0x020),
    TRANSITIVE(0x0020, false, 0x040),
    SYNCHRONIZED(0x0020, true, 0x004),
    STATIC_PHASE(0x0040, false, 0x040),
    VOLATILE(0x0040, true, 0x002),
    BRIDGE(0x0040, false, 0x004),
    TRANSIENT(0x0080, true, 0x002),
    VARARGS(0x0080, false, 0x004),
    NATIVE(0x0100, true, 0x004),
    INTERFACE(0x0200, false, 0x009),
    ABSTRACT(0x0400, true, 0x00D),
    STRICT(0x0800, true, 0x004),
    SYNTHETIC(0x1000, false, 0x1FF),
    ANNOTATION(0x2000, false, 0x009),
    ENUM(0x4000, false, 0x00B),
    MANDATED(0x8000, false, 0x1F0),
    MODULE(0x8000, false, 0x001);

    private final int mask;
    private final boolean sourceModifier;
    // The set of locations, built **eagerly** in the constructor as in the JDK — one EnumSet per
    // constant, materialised while the enum is still initialising. (This is what the VM's
    // invokevirtual-through-a-superinterface bug once made impossible; with that fixed, the natural
    // eager form is back.)
    private final Set<Location> locations;

    private AccessFlag(int mask, boolean sourceModifier, int locationBits) {
        this.mask = mask;
        this.sourceModifier = sourceModifier;
        this.locations = locationSet(locationBits);
    }

    // Expand a bitmask over Location.ordinal() into the Set<Location> it stands for.
    private static Set<Location> locationSet(int locationBits) {
        EnumSet<Location> set = EnumSet.noneOf(Location.class);
        for (Location location : Location.values()) {
            if ((locationBits & (1 << location.ordinal())) != 0) {
                set.add(location);
            }
        }
        return set;
    }

    /** The bit this flag occupies in an {@code access_flags} mask. */
    public int mask() {
        return this.mask;
    }

    /** Whether this flag is also a source-level modifier (e.g. {@code public}, {@code final}). */
    public boolean sourceModifier() {
        return this.sourceModifier;
    }

    /** The kinds of location where this flag may appear. */
    public Set<Location> locations() {
        return this.locations;
    }

    /**
     * The locations for a given format. KajiJDK models only the current format, so this is the same
     * set as {@link #locations()} for every {@code cffv}.
     */
    public Set<Location> locations(ClassFileFormatVersion cffv) {
        return this.locations();
    }

    /** The flags set in {@code mask} that are valid at {@code location}. */
    public static Set<AccessFlag> maskToAccessFlags(int mask, Location location) {
        EnumSet<AccessFlag> result = EnumSet.noneOf(AccessFlag.class);
        for (AccessFlag flag : values()) {
            if ((mask & flag.mask) != 0 && flag.locations.contains(location)) {
                result.add(flag);
            }
        }
        return result;
    }

    /** As {@link #maskToAccessFlags(int, Location)}; the format is not distinguished here. */
    public static Set<AccessFlag> maskToAccessFlags(int mask, Location location,
            ClassFileFormatVersion cffv) {
        return maskToAccessFlags(mask, location);
    }

    /**
     * A place an {@code access_flags} mask can appear: on a class, a field, a method, an inner-class
     * record, a method parameter, or one of the module-attribute structures. Each knows the mask of
     * flags valid there. The {@code ordinal()} of each constant is the bit {@link AccessFlag} uses
     * in its {@code locationBits}, so their order here is load-bearing.
     */
    public enum Location {

        CLASS(0xF631),
        FIELD(0x50DF),
        METHOD(0x1DFF),
        INNER_CLASS(0x761F),
        METHOD_PARAMETER(0x9010),
        MODULE(0x9020),
        MODULE_REQUIRES(0x9060),
        MODULE_EXPORTS(0x9000),
        MODULE_OPENS(0x9000);

        private final int flagsMask;

        private Location(int flagsMask) {
            this.flagsMask = flagsMask;
        }

        /** The OR of every flag mask valid at this location. */
        public int flagsMask() {
            return this.flagsMask;
        }

        /** As {@link #flagsMask()}; the format is not distinguished (current format only). */
        public int flagsMask(ClassFileFormatVersion cffv) {
            return this.flagsMask;
        }

        /** The set of flags valid at this location. */
        public Set<AccessFlag> flags() {
            return AccessFlag.maskToAccessFlags(this.flagsMask, this);
        }

        /** As {@link #flags()}; the format is not distinguished (current format only). */
        public Set<AccessFlag> flags(ClassFileFormatVersion cffv) {
            return this.flags();
        }
    }
}
