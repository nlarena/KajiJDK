package java.lang.reflect;

// KajiLibrary's java.lang.reflect.ClassFileFormatVersion — one constant per class-file format, i.e.
// per Java SE release from 1.0 to the current one. It exists mostly as the KEY of the version-aware
// reflection API: AccessFlag.locations(ClassFileFormatVersion) and friends ask "what did this flag
// mean at THAT format". KajiJDK models only the CURRENT format, so those overloads collapse to their
// no-argument form — but the enum itself is faithful, so the surface (and `major()`/`latest()`) is
// real.
//
// A KajiLibrary subset: `runtimeVersion()` and `valueOf(Runtime.Version)` are absent because
// `java.lang.Runtime` (and its nested `Version`) are not part of this library yet — a genuine
// dependency wall, not an omission of convenience.
public enum ClassFileFormatVersion {

    // RELEASE_0 and RELEASE_1 share major version 45 (JDK 1.0.2 / 1.1); from RELEASE_2 on, the major
    // is 44 + N. RELEASE_25 is major 69, the format this VM targets.
    RELEASE_0(45),
    RELEASE_1(45),
    RELEASE_2(46),
    RELEASE_3(47),
    RELEASE_4(48),
    RELEASE_5(49),
    RELEASE_6(50),
    RELEASE_7(51),
    RELEASE_8(52),
    RELEASE_9(53),
    RELEASE_10(54),
    RELEASE_11(55),
    RELEASE_12(56),
    RELEASE_13(57),
    RELEASE_14(58),
    RELEASE_15(59),
    RELEASE_16(60),
    RELEASE_17(61),
    RELEASE_18(62),
    RELEASE_19(63),
    RELEASE_20(64),
    RELEASE_21(65),
    RELEASE_22(66),
    RELEASE_23(67),
    RELEASE_24(68),
    RELEASE_25(69);

    private final int major;

    private ClassFileFormatVersion(int major) {
        this.major = major;
    }

    /** The latest class-file format this runtime recognises. */
    public static ClassFileFormatVersion latest() {
        return RELEASE_25;
    }

    /** The {@code major_version} a class file of this format carries (JVMS §4.1). */
    public int major() {
        return this.major;
    }

    /**
     * The format whose {@code major_version} is {@code major}.
     *
     * @throws IllegalArgumentException if {@code major} is not a recognised class-file major version
     */
    public static ClassFileFormatVersion fromMajor(int major) {
        if (major == 45) {
            return RELEASE_1;
        }
        if (major >= 46 && major <= 69) {
            return values()[major - 44];
        }
        throw new IllegalArgumentException("Unsupported class file major version " + major);
    }

    /** The {@link Runtime.Version} that introduced this class-file format. */
    public Runtime.Version runtimeVersion() {
        return Runtime.Version.parse(Integer.toString(this.ordinal()));
    }

    /**
     * The format the given runtime version corresponds to, by its {@code feature()} release.
     *
     * @throws IllegalArgumentException if the feature release has no known class-file format
     */
    public static ClassFileFormatVersion valueOf(Runtime.Version version) {
        int feature = version.feature();
        // RELEASE_N sits at index N (its ordinal), so the feature release indexes values() directly.
        if (feature < 0 || feature > 25) {
            throw new IllegalArgumentException("Unsupported release: " + feature);
        }
        return values()[feature];
    }
}
