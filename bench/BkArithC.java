// **Control de efecto cero** de `BkArith` — el mismo programa, con veneno.
//
// # Qué es el veneno y por qué éste
//
// `if (NEVER != 0) { … new int[2][2] … }`. `NEVER` es un `static int` **no final**, así que
// `javac` no puede plegar la rama y tiene que emitir `getstatic`/`ifeq` y el cuerpo entero detrás.
// Ese cuerpo contiene un **`multianewarray` de dos dimensiones**, que esta VM rechaza con motivo
// propio (`Ineligible::MultiDimensional`) y no como opcode que falte implementar: aloca `1 + n`
// arreglos, una cantidad que depende de un operando y no tiene cota estática, y las salidas de
// este tier reanudan en un límite de instrucción — así que un intento que se queda sin log
// re-ejecuta el opcode entero y nunca progresa. Un opcode que no puede terminar no es un opcode
// que falte.
//
// El veneno se **escanea** (y por eso el método se rechaza entero) pero **no se ejecuta nunca**,
// así que su costo en tiempo es un `getstatic` y un `ifeq` por llamada a `step` — dos opcodes
// contra los ~2400 del lazo — y cero en el resultado. Los dos archivos imprimen el mismo número,
// que es lo que hace que este par sea un experimento y no dos programas parecidos.
//
// # Por qué éste y no el de antes, que es la parte que hay que entender
//
// **El veneno anterior era un `invokedynamic` en la misma rama muerta, y se murió.** No porque el
// `invokedynamic` entrara al subconjunto, sino porque lo que lo hacía refutar el método era que el
// sitio **nunca se había ejecutado**: sin ejecución no hay clase spun que ligar, y la VM no
// ofrecía destino. El día que un sitio de llamada infligable pasó a compilarse como un deopt
// incondicional en vez de rechazar el método —la trampa infrecuente, que existe justamente para
// que una rama fría deje de costar el método caliente— estos cinco controles compilaron dos
// métodos cada uno y dejaron de ser un piso de ruido.
//
// De ahí el criterio del reemplazo, y es el único que importa: **la negativa no puede ser sobre un
// sitio frío.** Un `ldc` de un `String` que nunca corrió, un `new` de una clase sin inicializar, un
// `aastore` sin perfilar — los tres se refutan hoy por lo mismo que se refutaba el indy, y los tres
// son candidatos a la misma trampa. Un `multianewarray` no: se lo rechaza por **lo que el opcode
// es**, lo alcance el programa o no, y eso no cambia porque se agregue una guarda.
//
// Si alguna vez también éste cae —un `multianewarray` frío podría deoptar, por el mismo
// argumento— el reemplazo que queda no es un opcode sino **`ACC_SYNCHRONIZED`**: un método
// `synchronized` no se le ofrece al JIT nunca, porque el intérprete toma el monitor antes de
// llegar al punto de despacho y ese despacho está condicionado a que no haya monitor. Es una
// garantía más fuerte —no es sobre una instrucción— y tiene un precio real: un monitor por
// llamada, que en `BkMonoC`/`BkMegaC` son 300.000. Por eso no es el default.
//
// # Lo que hace que siga siendo un control mañana
//
// Nada de este comentario. Lo que lo sostiene es
// `los_controles_del_banco_no_compilan_nada`, que **cuenta** las compilaciones en cada corrida de la
// suite y falla ruidosamente el día que este veneno también se muera. Tres cargas (`BmField`,
// `BmArray`, `BmInvoke`) dejaron de ser controles en silencio por no tener ese test; éste sí lo
// tuvo, y es lo único que impidió que cinco filas del banco quedaran midiendo nada.
public class BkArithC {
    static int NEVER = 0;

    static int step(int seed, int n) {
        int acc = seed;
        if (NEVER != 0) {
            acc = acc + new int[2][2].length;
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
            acc = acc + new int[2][2].length;
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
