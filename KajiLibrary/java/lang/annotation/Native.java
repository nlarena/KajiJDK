package java.lang.annotation;

/**
 * KajiLibrary's java.lang.annotation.Native — it marks a constant that has to appear in a native
 * header.
 *
 * <p>Neither the VM nor anybody else reads it at run time: it is a signal for the tools that
 * generate C headers out of Java classes (historically {@code javah}, today {@code javac -h}). A
 * {@code static final int} marked this way shows up as a {@code #define} in the {@code .h}, and
 * that way the native code stops repeating by hand a number that lives in Java.
 *
 * <p>Its two meta-annotations follow from that, and they are not decoration:
 *
 * <ul>
 *   <li>{@code @Retention(SOURCE)} — the annotation <strong>does not reach the `.class`</strong>.
 *       What consumes it is a processor that already has the source in front of it, so keeping it in
 *       the compiled file would be dead weight. The practical consequence: there is no way of
 *       finding it by reflection, and a test looking for it with {@code getAnnotation} has to expect
 *       {@code null};
 *   <li>{@code @Target(FIELD)} — fields only. A constant is the only thing a header can reproduce;
 *       a method or a class has no value to copy.
 * </ul>
 *
 * <p>This library does not bring the tool that consumes it, and the type is needed all the same:
 * without it, third-party code annotating its constants with {@code @Native} does not compile
 * against this JDK.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Native {
}
