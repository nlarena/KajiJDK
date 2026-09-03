package jdk.internal.classfile.impl;

import java.lang.classfile.AccessFlags;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.AccessFlag.Location;
import java.util.Set;

// La máscara `access_flags` de una ubicación concreta. El conjunto de banderas se arma una sola vez,
// con `AccessFlag.maskToAccessFlags`, que es quien sabe desambiguar los bits compartidos.
public final class AccessFlagsImpl implements AccessFlags {

    private final int mascara;
    private final Location ubicacion;
    private final Set<AccessFlag> banderas;

    public AccessFlagsImpl(int mascara, Location ubicacion) {
        this.mascara = mascara;
        this.ubicacion = ubicacion;
        this.banderas = AccessFlag.maskToAccessFlags(mascara, ubicacion);
    }

    public int flagsMask() {
        return this.mascara;
    }

    public Set<AccessFlag> flags() {
        return this.banderas;
    }

    public Location location() {
        return this.ubicacion;
    }

    public boolean has(AccessFlag flag) {
        if (!flag.locations().contains(this.ubicacion)) {
            throw new IllegalArgumentException(
                    "la bandera " + flag.name() + " no vale en " + this.ubicacion);
        }
        return (this.mascara & flag.mask()) != 0;
    }

    public String toString() {
        return "AccessFlags[0x" + Integer.toHexString(this.mascara) + " " + this.ubicacion + "]";
    }
}
