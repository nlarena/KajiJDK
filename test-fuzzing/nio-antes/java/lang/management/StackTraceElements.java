package java.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * It builds a {@link StackTraceElement} out of a {@link CompositeData}.
 *
 * <p>Package-private: not API. {@link MonitorInfo} and {@link ThreadInfo} use it, which are the two
 * that can receive stack frames over the network.
 *
 * <p>The mandatory items are the usual four; the module and class loader ones appeared in Java 9 and
 * are read if they are there.
 */
final class StackTraceElements {

    private StackTraceElements() {
    }

    /** The frame, or null if the datum is null. */
    static StackTraceElement from(CompositeData cd) {
        if (cd == null) {
            return null;
        }
        final String type = "StackTraceElement";
        String declaringClass = CompositeItems.string(cd, "className", type);
        String methodName = CompositeItems.string(cd, "methodName", type);
        String fileName = CompositeItems.string(cd, "fileName", type);
        int lineNumber = CompositeItems.integer(cd, "lineNumber", type);
        Object classLoaderName = CompositeItems.optional(cd, "classLoaderName");
        Object moduleName = CompositeItems.optional(cd, "moduleName");
        Object moduleVersion = CompositeItems.optional(cd, "moduleVersion");
        if (classLoaderName == null && moduleName == null && moduleVersion == null) {
            return new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
        }
        return new StackTraceElement(asString(classLoaderName), asString(moduleName),
                                     asString(moduleVersion), declaringClass, methodName,
                                     fileName, lineNumber);
    }

    private static String asString(Object o) {
        if (o instanceof String) {
            return (String) o;
        }
        return null;
    }
}
