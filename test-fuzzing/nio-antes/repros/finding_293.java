// Repro de #293 - un `new` con argumentos que no matchean ningun constructor compilaba igual,
// llamaba al constructor SIN argumentos, y dejaba los argumentos en la pila.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_293.java
//   bin\run-headless.exe KajiLibrary\repros\finding_293.class conCopia
//
// Este archivo son los CONTROLES: los `new` legitimos, que tienen que seguir andando. Los que
// tienen que ser RECHAZADOS estan en finding_293b.java, que no compila a proposito.
//
// ANTES, `new HashSet<String>(unaLista())` -- con `HashSet(Collection)` inexistente -- emitia:
//
//   0: new           java/util/HashSet
//   3: dup
//   4: invokestatic  unaLista:()Ljava/util/List;     <- el argumento queda empujado
//   7: invokespecial java/util/HashSet."<init>":()V  <- descriptor SIN parametros
//  10: invokevirtual java/util/HashSet.size:()I
//
// Segui la pila: despues del `dup` hay [HashSet, HashSet]; el `invokestatic` deja
// [HashSet, HashSet, List]; y el `invokespecial ()V` saca UNO -- la List. O sea que corrio
// `HashSet.<init>` sobre un objeto que no es un HashSet, dejo el objeto nuevo sin inicializar, y
// la pila desbalanceada para todo lo que siguiera. Con un argumento primitivo es peor todavia:
// `new Propia(7)` corria `<init>` sobre el entero 7.
//
// Compilaba en silencio, daba resultados sin sentido (el conjunto salia vacio), y el sintoma
// aparecia lejos: la prueba de comportamiento de TreeMap moria con
// `arithmetic_operations.rs:315: expected an int on the operand stack, found Reference(10146)`
// cincuenta lineas mas abajo, por la pila corrida.
//
// La JVM real rechaza ese bytecode en la verificacion.
//
// NO se chequeaba nunca: ni para una clase del classpath, ni para una del propio archivo, ni
// cuando la clase no tenia ningun constructor sin argumentos (`new StringBuilder("x", "y")`
// tambien salia `()V`). El unico caso que si se cazaba era el de un METODO de una clase fuente
// con la aridad equivocada.
//
// AHORA: dos mitades.
//   - `attribute.rs` reporta "no se encontro un constructor aplicable" cuando la clase es del
//     fuente, con las mismas notas de candidatos que ya daba una llamada a metodo.
//   - `codegen.rs` se planta si llega un `new` **con argumentos** sin constructor resuelto. Es la
//     red para el caso del classpath, donde la resolucion es indulgente a proposito -- y es el
//     mismo guard que `super(...)`/`this(...)` ya tenia, escrito con la misma razon.
//
// Lo que destapo el arreglo, y por eso vale: CUATRO sitios de la propia biblioteca estaban
// emitiendo ese bytecode. Dos eran #294, uno era #295, y el cuarto era un `HashSet(Collection)`
// que no existia (se agrego).
//
// `conCopia` -> 2, `conMensaje` -> 4, `conDosArgs` -> 3, `sinArgs` -> 0.
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class finding_293 {

    private static List<String> unaLista() {
        List<String> l = new ArrayList<String>();
        l.add("a");
        l.add("b");
        l.add("a");
        return l;
    }

    // El constructor de copia que faltaba, y que ahora existe. Con el bug daba 0.
    public static int conCopia() {
        HashSet<String> s = new HashSet<String>(unaLista());
        return s.size();
    }

    // Un constructor de una clase del classpath con un argumento String.
    public static int conMensaje() {
        Exception e = new IllegalStateException("hola");
        return e.getMessage().length();
    }

    // Uno de dos argumentos, de este mismo archivo.
    public static int conDosArgs() {
        Par p = new Par(1, 2);
        return p.suma();
    }

    // Y el sin argumentos, que es el unico que el `()V` implicito puede cubrir.
    public static int sinArgs() {
        Map<String, String> m = new HashMap<String, String>();
        return m.size();
    }
}

class Par {

    private final int a;
    private final int b;

    Par(int a, int b) {
        this.a = a;
        this.b = b;
    }

    int suma() {
        return this.a + this.b;
    }
}
