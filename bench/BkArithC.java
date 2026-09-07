// **Control de efecto cero** de `BkArith` — el mismo programa, con veneno.
//
// # Qué es el veneno y por qué éste
//
// `if (NEVER != 0) { … ("" + n) … }`. `NEVER` es un `static int` **no final**, así que `javac` no
// puede plegar la rama y tiene que emitir `getstatic`/`ifeq` y el cuerpo entero detrás. Ese cuerpo
// contiene un `invokedynamic` (`StringConcatFactory.makeConcatWithConstants`), que es el opcode que
// esta VM tiene **estructuralmente** fuera del subconjunto compilable: un `invokedynamic` acá no es
// una llamada, el intérprete vuelve a correr el bootstrap y lo que el bootstrap produce es una
// acción de la VM, no un destino. No hay `MethodId` enlazado que darle a este nivel, así que no hay
// guarda que lo pueda cubrir. Es la razón por la que se eligió éste y no una construcción que
// simplemente *todavía* no compila.
//
// El veneno se **escanea** (y por eso el método se rechaza entero) pero **no se ejecuta nunca**, así
// que su costo en tiempo es un `getstatic` y un `ifeq` por llamada a `step` — dos opcodes contra los
// ~2400 del lazo — y cero en el resultado. Los dos archivos imprimen el mismo número, que es lo que
// hace que este par sea un experimento y no dos programas parecidos.
//
// # Lo que hace que siga siendo un control mañana
//
// Nada de este comentario. Lo que lo sostiene es
// `los_controles_del_banco_no_compilan_nada`, que **cuenta** las compilaciones en cada corrida de la
// suite y falla ruidosamente el día que `invokedynamic` entre al subconjunto. Tres cargas
// (`BmField`, `BmArray`, `BmInvoke`) dejaron de ser controles en silencio por no tener ese test.
public class BkArithC {
    static int NEVER = 0;

    static int step(int seed, int n) {
        int acc = seed;
        if (NEVER != 0) {
            acc = acc + ("" + n).length();
        }
        for (int i = 0; i < n; i++) {
            acc = acc + i;
            acc = acc ^ (acc >> 7);
            if ((i & 15) == 0) {
                acc = acc - 3;
            }
        }
        return acc & 0xFFFFF;
    }

    static int run() {
        int acc = 1;
        if (NEVER != 0) {
            acc = acc + ("" + acc).length();
        }
        for (int k = 0; k < 1000; k++) {
            acc = step(acc, 300);
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
