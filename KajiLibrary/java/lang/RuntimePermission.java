package java.lang;

import java.security.BasicPermission;

// El permiso sobre las operaciones del runtime: "exitVM.0", "setIO", "createClassLoader",
// "getClassLoader", "accessDeclaredMembers", "loadLibrary.<nombre>", etc.
//
// Es un `BasicPermission` puro: toda la logica —parseo del comodin final, `implies` jerarquico,
// `equals`/`hashCode` por nombre canonico, coleccion propia— ya vive en la clase base, y esta no
// agrega nada salvo el tipo. Eso no es pereza: en `BasicPermission.implies` la primera prueba es
// `p.getClass() != this.getClass()`, asi que **el tipo es el que separa los espacios de nombres**.
// Sin una clase distinta, un `RuntimePermission("exitVM")` y un `PropertyPermission("exitVM")`
// serian el mismo permiso. Por eso la clase existe aunque su cuerpo este vacio.
//
// Sin acciones: `getActions()` hereda el "" de la base. Los targets con parte variable las simulan
// metiendola en el nombre —`"exitVM.0"`, `"loadLibrary.awt"`— justamente para no necesitarlas, y de
// ahi sale gratis que `"exitVM.*"` implique a todos los codigos de salida.
//
// `final`, como en el JDK: si se pudiera extender, la subclase caeria en otro espacio de nombres y
// dejaria de ser implicada por los `RuntimePermission` que la deberian cubrir.
//
// Que no hace nada aca: KajiJDK no instala un `SecurityManager` (su constructor tira), asi que
// nadie consulta estos permisos en tiempo de ejecucion. La clase esta por el tipo y porque el
// modelo de permisos —construir, comparar, meter en una `Permissions`— si funciona de verdad. Por
// eso mismo esta marcada para remocion desde 25, igual que en el JDK: sin gestor de seguridad que
// los evalue, el tipo entero es superficie que va camino a desaparecer.
@Deprecated(since = "25", forRemoval = true)
public final class RuntimePermission extends BasicPermission {

    public RuntimePermission(String name) {
        super(name);
    }

    // `actions` se ignora, como en el JDK. El constructor de dos argumentos existe porque el
    // formato de las policy files siempre pasa un campo de acciones, aunque para esta clase sea
    // null o "".
    public RuntimePermission(String name, String actions) {
        super(name, actions);
    }
}
