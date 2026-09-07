// Los receptores que comparten `BkMono`/`BkMega` (los limpios) y `BkMonoC`/`BkMegaC` (los
// venenosos) — el par que aísla **el despacho** de todo lo demás.
//
// # Cuatro clases con el mismo cuerpo, a propósito
//
// `BkMono` llama siempre a `BkOpA` y `BkMega` rota entre las cuatro. Como los cuerpos son idénticos
// letra por letra, las dos cargas ejecutan la misma aritmética y devuelven el mismo número: lo único
// que las separa es **cuántas clases distintas ve el sitio de llamada**. Cualquier diferencia de
// tiempo entre esas dos filas es despacho, no trabajo. Son cuatro clases y no una con cuatro
// instancias porque un caché de sitio de llamada discrimina por *clase*, no por identidad.
//
// # Por qué el `if (NEVER)` está también en los limpios
//
// Porque el gemelo tiene que ser el **mismo programa ejecutado**, no uno parecido. `BkOpPA` es
// `BkOpA` con un `invokedynamic` en la rama muerta en vez de una suma; las dos ejecutan
// `getstatic; ifeq; iload_1; iconst_1; iadd; ireturn` y nada más. Si el `if` estuviera sólo en el
// venenoso, el control pagaría dos opcodes por llamada que el tratamiento no paga — sobre un cuerpo
// de cuatro — y su razón dejaría de ser el piso de ruido de esta fila.
//
// La primera versión de esto no tenía el `if` en ninguno de los dos, y el test que custodia los
// controles lo encontró en la primera corrida: `BkMonoC` compilaba **1** método. No era `step` ni
// `run` —los dos envenenados— sino `BkOpA.f`, que se pone caliente por su cuenta. Queda anotado
// porque es exactamente el modo de falla que este hito existe para cerrar, y esta vez lo agarró una
// aserción y no un lector atento seis pasos después.
interface BkOp {
    int f(int x);
}

final class BkOpA implements BkOp {
    static int NEVER = 0;

    public int f(int x) {
        if (NEVER != 0) {
            return x + 2;
        }
        return x + 1;
    }
}

final class BkOpB implements BkOp {
    static int NEVER = 0;

    public int f(int x) {
        if (NEVER != 0) {
            return x + 2;
        }
        return x + 1;
    }
}

final class BkOpC implements BkOp {
    static int NEVER = 0;

    public int f(int x) {
        if (NEVER != 0) {
            return x + 2;
        }
        return x + 1;
    }
}

final class BkOpD implements BkOp {
    static int NEVER = 0;

    public int f(int x) {
        if (NEVER != 0) {
            return x + 2;
        }
        return x + 1;
    }
}
