package jdk.dynalink;

/**
 * El espacio de nombres sobre el que actua una {@link Operation}.
 *
 * <p>La interfaz esta **vacia a proposito**: un espacio de nombres es una identidad, no un
 * comportamiento. Lo unico que se le pide a una implementacion es que sea comparable por
 * `equals` — {@link NamespaceOperation#contains} no hace otra cosa. Los tres del lenguaje
 * estan en {@link StandardNamespace}; un lenguaje dinamico que necesite mas (por ejemplo, un
 * espacio de "variables globales") declara los suyos.
 *
 * @since 9
 */
public interface Namespace {
}
