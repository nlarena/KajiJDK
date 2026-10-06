// Repro del finding #567 -- ABIERTO. Paso 2 de 2.
//
// Como se corre, y los dos pasos son el punto: si `Fuera` se compila en la MISMA unidad que esto,
// sale bien.
//
//   javac --emit -cp KajiLibrary Fuera.java
//   javac --emit -cp "KajiLibrary;<este directorio>" Usa2.java
//   java -cp <este directorio> Usa2
//
// Nuestro `Usa2$1` sale asi:
//
//   class Usa2$1 extends Fuera$Pide        super_class: Fuera$Pide      interfaces: 0
//
// y tiene que salir asi:
//
//   class Usa2$1 implements Fuera$Pide     super_class: java/lang/Object   interfaces: 1
//
// La HotSpot rechaza la clase al cargarla:
//
//   IncompatibleClassChangeError: class Usa2$1 has interface Fuera$Pide as super class
//
// Nuestra VM la tolera y contesta 7. Severidad alta: la clase no se puede llevar a ninguna otra JVM.
//
// Explica CINCO de las ocho clases que el cruce con la JVM real todavia rechaza: `IoTest` (anonima
// sobre `ObjectInputFilter$FilterInfo`), `WalkTest` (sobre `DirectoryStream$Filter`),
// `KajiBeansHumoX`, `SecTest` y `UtilAuditTest`.
public class Usa2 {
    static int usa() {
        Fuera.Pide p = new Fuera.Pide() {
            public int cuanto() {
                return 7;
            }
        };
        return p.cuanto();
    }

    public static void main(String[] a) {
        System.out.println(usa());
    }
}
