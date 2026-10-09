// #512 -- una clase anidada no hereda el permiso de su clase envolvente para tocar un miembro
// `protected` de una superclase de OTRO paquete.
//
//   bin/javac.exe --emit pa/Base.java pb/Hija.java
//     pb/Hija.java:7: error: el metodo `avisar` es `protected` en `Base` y no es accesible
//                            desde `Interna`
//
// javac del JDK 25 lo acepta (exit 0). JLS 6.6.2.1: el acceso esta permitido dentro del CUERPO de
// una subclase S, y el cuerpo de S incluye a sus clases anidadas; el tipo calificador (`Hija`) es
// S, asi que el acceso es legal.
package pa;

public class Base {

    protected void avisar() {
    }
}
