package java.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * Arma un {@link StackTraceElement} desde un {@link CompositeData}.
 *
 * <p>De acceso de paquete: no es API. Lo usan {@link MonitorInfo} y {@link ThreadInfo}, que son los
 * dos que pueden recibir marcos de pila por la red.
 *
 * <p>Los items obligatorios son los cuatro de siempre; los de modulo y cargador de clases aparecieron
 * en Java 9 y se leen si estan.
 */
final class StackTraceElements {

    private StackTraceElements() {
    }

    /** El marco, o null si el dato es null. */
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
