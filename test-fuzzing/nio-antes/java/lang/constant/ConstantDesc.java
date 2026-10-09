package java.lang.constant;

// The root of the nominal-descriptor hierarchy: a value that can live in the constant pool and
// describes something WITHOUT resolving it. Every descriptor type here implements it, so it is
// what lets a bootstrap argument list be typed at all.
//
// The JDK declares exactly one method on it, and so does this — see below for why it is
// `default` here and `abstract` there.
public interface ConstantDesc {

    /**
     * Resolves this descriptor into the thing it describes.
     *
     * <p>Two kinds of implementor answer it very differently, which is the interesting part.
     * A value that describes ITSELF -- a String, a boxed number -- resolves by handing itself
     * back, and cannot fail. A descriptor that names something else -- a class, a method type, a
     * method handle -- has to go and find it, and that is the step this library cannot take yet:
     * it needs `java.lang.invoke`, whose content is the VM's method-handle machinery rather than
     * library code. Those implementors say so, by throwing, rather than pretending.
     *
     * <p>Everything else about the package works without resolution, which is why the omission
     * costs so little: descriptors can be built, compared and printed, and that is precisely the
     * property that makes them usable at compile time.
     *
     * @param lookup the lookup whose access rights the resolution is performed under
     * @return the resolved value
     * @throws ReflectiveOperationException if the description cannot be resolved
     */
    Object resolveConstantDesc(java.lang.invoke.MethodHandles.Lookup lookup)
            throws ReflectiveOperationException;
}
