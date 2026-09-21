package jdk.internal.classfile.impl;

import java.lang.classfile.AccessFlags;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.AccessFlag.Location;
import java.util.Set;

// The `access_flags` mask of a concrete location. The set of flags is built once, with
// `AccessFlag.maskToAccessFlags`, which is the one that knows how to disambiguate the shared bits.
public final class AccessFlagsImpl implements AccessFlags {

    private final int mask;
    private final Location location;
    private final Set<AccessFlag> flags;

    public AccessFlagsImpl(int mask, Location location) {
        this.mask = mask;
        this.location = location;
        this.flags = AccessFlag.maskToAccessFlags(mask, location);
    }

    public int flagsMask() {
        return this.mask;
    }

    public Set<AccessFlag> flags() {
        return this.flags;
    }

    public Location location() {
        return this.location;
    }

    public boolean has(AccessFlag flag) {
        if (!flag.locations().contains(this.location)) {
            throw new IllegalArgumentException(
                    "flag " + flag.name() + " is not valid in " + this.location);
        }
        return (this.mask & flag.mask()) != 0;
    }

    public String toString() {
        return "AccessFlags[0x" + Integer.toHexString(this.mask) + " " + this.location + "]";
    }
}
