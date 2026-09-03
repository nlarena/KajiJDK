package jdk.internal.classfile.impl;

import java.lang.classfile.ClassFileVersion;

// El par de versiones del encabezado, como elemento de clase.
public final class ClassFileVersionImpl implements ClassFileVersion {

    private final int mayor;
    private final int menor;

    public ClassFileVersionImpl(int mayor, int menor) {
        this.mayor = mayor;
        this.menor = menor;
    }

    public int majorVersion() {
        return this.mayor;
    }

    public int minorVersion() {
        return this.menor;
    }

    public String toString() {
        return "ClassFileVersion[" + this.mayor + "." + this.menor + "]";
    }
}
