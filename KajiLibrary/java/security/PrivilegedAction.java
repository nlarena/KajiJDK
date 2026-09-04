package java.security;

// Un bloque de codigo para correr con los privilegios de quien lo define.
//
// Deprecada junto con `AccessController`: desde que el `SecurityManager` esta deshabilitado, correr
// "con privilegios" y correr normal son lo mismo. Sobrevive como tipo funcional porque hay firmas
// en varias bibliotecas que lo nombran.
@FunctionalInterface
@Deprecated
public interface PrivilegedAction<T> {

    T run();
}
