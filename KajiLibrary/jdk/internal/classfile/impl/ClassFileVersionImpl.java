package jdk.internal.classfile.impl;

import java.lang.classfile.ClassFileVersion;

// The pair of versions of the header, as a class element.
public final class ClassFileVersionImpl implements ClassFileVersion {

    private final int major;
    private final int minor;

    public ClassFileVersionImpl(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public int majorVersion() {
        return this.major;
    }

    public int minorVersion() {
        return this.minor;
    }

    public String toString() {
        return "ClassFileVersion[" + this.major + "." + this.minor + "]";
    }
}
