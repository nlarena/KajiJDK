// Repro de #244 (y de #220, que era el mismo bug) - un enum ANIDADO en un paquete que boot/
// provee a medias.
//
// El diagnostico original decia "VM: ... la VM panickea", y los dos findings apuntaban a la VM:
//   #244  `panicked at objects_operations.rs:410: field_offset: field not found`, con el stack
//         pasando por `unwind_with`.
//   #220  "el comportamiento de un metodo depende del constant pool de OTROS metodos".
// Los dos eran, en realidad, **#110 del COMPILADOR**: el lector de `.class` del classpath
// descartaba el `access_flags` de los campos, asi que un campo `static` de una clase externa se
// modelaba como de instancia y el emisor sacaba `getfield` donde iba `getstatic`. La VM tenia
// razon: ese campo, como campo de instancia, no existe. El "parpadeo" de #220 era que agregar o
// quitar una clase cambiaba que simbolo externo ganaba la clave por nombre simple.
//
// Los dos enums de abajo son los que lo disparaban de verdad. Con el javac congelado de antes de
// la correccion, los dos metodos panican; ahora los dos devuelven 0.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_244.java
//   bin\run-headless.exe KajiLibrary\repros\finding_244.class accessMode   -> 0
//   bin\run-headless.exe KajiLibrary\repros\finding_244.class kind         -> 0
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.invoke.VarHandle;

public class finding_244 {

    public static int accessMode() {
        VarHandle.AccessMode m = VarHandle.AccessMode.GET;
        if (m == null) {
            return 1;
        }
        if (VarHandle.AccessMode.values().length == 0) {
            return 2;
        }
        return 0;
    }

    public static int kind() {
        DirectMethodHandleDesc.Kind k = DirectMethodHandleDesc.Kind.STATIC;
        if (k == null) {
            return 1;
        }
        if (DirectMethodHandleDesc.Kind.values().length == 0) {
            return 2;
        }
        return 0;
    }
}
