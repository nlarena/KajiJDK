package javax.lang.model;
public enum SourceVersion {
    RELEASE_0, RELEASE_1, RELEASE_2, RELEASE_3, RELEASE_4, RELEASE_5, RELEASE_6,
    RELEASE_7, RELEASE_8, RELEASE_9, RELEASE_10, RELEASE_11, RELEASE_17, RELEASE_21, RELEASE_25;
    public static SourceVersion latest() { return RELEASE_25; }
    public static SourceVersion latestSupported() { return RELEASE_25; }
}
