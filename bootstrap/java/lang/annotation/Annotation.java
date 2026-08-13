package java.lang.annotation;

// The common supertype of every annotation type. `@interface Foo {}` is compiled as an
// interface that implicitly extends this one, so a class file that declares an annotation
// type names `java/lang/annotation/Annotation` in its interfaces table — which is why the
// VM needs this type on the boot classpath before it can even *load* `Foo.class`.
//
// In the real JDK this interface also declares annotationType(), equals(), hashCode() and
// toString(), all implemented by the proxy the JDK synthesises for each annotation type.
// We synthesise no proxies (see Class.isAnnotationPresent), so there is nothing here to
// implement and the interface stays a pure marker.
public interface Annotation {
}
