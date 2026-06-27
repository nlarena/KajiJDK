// A concrete exception. Extends java.lang.RuntimeException — which javac resolves to
// the JDK's for type-checking, but the bytecode just names "java/lang/RuntimeException",
// and our JVM loads OUR version from the classpath. So the whole chain
// Boom -> RuntimeException -> Exception -> Throwable -> Object is ours and loadable.
class Boom extends RuntimeException {
}
