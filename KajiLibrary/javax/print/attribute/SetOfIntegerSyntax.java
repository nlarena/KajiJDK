package javax.print.attribute;

import java.io.Serializable;

// La clase de sintaxis de los atributos cuyo valor es un **conjunto de enteros**, guardado como
// una lista de rangos.
//
// Es la clase con logica de verdad del paquete, y toda la logica esta en una sola idea: la
// **forma canonica**. Se acepta cualquier lista de rangos --desordenada, superpuesta, con rangos
// vacios-- y adentro se guarda siempre la misma representacion: ordenada de menor a mayor, sin
// rangos vacios, y con los que se tocan o se solapan fusionados en uno. Dos rangos son
// "adyacentes" si el de arriba empieza justo despues del de abajo (`ub + 1 == lb`), y en ese caso
// tambien se fusionan.
//
// Canonicalizar temprano es lo que hace que `equals` y `hashCode` sean baratos y correctos:
// `"1-3,4-6"` y `"1-6"` describen el mismo conjunto y tienen que salir iguales, y despues de la
// canonicalizacion lo son componente a componente, sin comparar conjuntos.
//
// Dos detalles heredados del JDK que se replican tal cual porque son observables:
//  - la forma de texto **no** valida el rango de un entero: `"2147483648"` da la vuelta a
//    -2147483648 en vez de fallar, porque los digitos se acumulan con la aritmetica de `int`;
//  - la forma `int[][]` si rechaza los negativos, pero solo en los rangos **no vacios**:
//    `{{5,3}}` es un rango vacio y se descarta antes de mirarle el signo.
//
// Y una divergencia, una sola, que aparece unicamente cuando la primera de esas dos rarezas ya
// produjo un limite negativo: esta explicada donde vive, en `canonicalArrayForm`.
public abstract class SetOfIntegerSyntax implements Serializable, Cloneable {

    private static final long serialVersionUID = 3666874174847632203L;

    // Siempre en forma canonica. Cada fila es {lb, ub}, los dos inclusivos.
    private int[][] members;

    // Los estados del reconocedor de la forma de texto. La gramatica es
    //     ranges = <vacio> | range ("," range)*      range = int | int ("-" | ":") int
    // con espacios permitidos entre tokens pero no adentro de un entero. Hacen falta siete estados
    // y no menos: "despues del limite inferior" tiene que aceptar el guion y "despues del
    // superior" no, y "recien arrancamos" tiene que aceptar el fin de la cadena mientras que
    // "recien vimos una coma" no -- por eso `"1,"` es un error y `"  "` es el conjunto vacio.
    private static final int ST_INICIO = 0;
    private static final int ST_EN_LB = 1;
    private static final int ST_TRAS_LB = 2;
    private static final int ST_ANTES_UB = 3;
    private static final int ST_EN_UB = 4;
    private static final int ST_TRAS_UB = 5;
    private static final int ST_TRAS_COMA = 6;

    protected SetOfIntegerSyntax(String members) {
        this.members = parse(members);
    }

    protected SetOfIntegerSyntax(int[][] members) {
        this.members = parse(members);
    }

    // Un solo entero: el conjunto {member}.
    protected SetOfIntegerSyntax(int member) {
        if (member < 0) {
            throw new IllegalArgumentException();
        }
        this.members = new int[][] {{member, member}};
    }

    // Un rango. Si `lowerBound > upperBound` el rango es vacio y el conjunto queda vacio -- y en
    // ese caso ni se mira el signo, que es por lo que `new X(-1, -5)` no falla y `new X(-1, 5)` si.
    protected SetOfIntegerSyntax(int lowerBound, int upperBound) {
        if (lowerBound <= upperBound) {
            if (lowerBound < 0) {
                throw new IllegalArgumentException();
            }
            this.members = new int[][] {{lowerBound, upperBound}};
        } else {
            this.members = new int[0][];
        }
    }

    // Las dos clasificaciones de caracteres del reconocedor. Van por `Character` y no por un
    // rango ASCII escrito a mano porque el JDK usa `Character.isWhitespace` y
    // `Character.digit(c, 10)`, y la diferencia es observable: `digit` acepta los digitos
    // decimales de cualquier escritura --los arabigo-indios U+0660..U+0669, por ejemplo-- e
    // `isWhitespace` acepta los separadores Unicode y rechaza el espacio duro U+00A0.
    private static boolean esBlanco(char c) {
        return Character.isWhitespace(c);
    }

    private static int digito(char c) {
        return Character.digit(c, 10);
    }

    // El reconocedor. Devuelve la forma canonica; `null` es el conjunto vacio, no un error.
    private static int[][] parse(String members) {
        int[][] crudos = new int[8][];
        int cuantos = 0;
        int n = (members == null) ? 0 : members.length();
        int estado = ST_INICIO;
        int lb = 0;
        int ub = 0;
        int i = 0;
        while (i < n) {
            char c = members.charAt(i);
            i++;
            int d = digito(c);
            if (estado == ST_INICIO || estado == ST_TRAS_COMA) {
                if (esBlanco(c)) {
                    continue;
                }
                if (d < 0) {
                    throw new IllegalArgumentException();
                }
                lb = d;
                estado = ST_EN_LB;
            } else if (estado == ST_EN_LB) {
                if (d >= 0) {
                    // Sin control de desborde, a proposito: es lo que hace el JDK.
                    lb = lb * 10 + d;
                } else if (esBlanco(c)) {
                    estado = ST_TRAS_LB;
                } else if (c == '-' || c == ':') {
                    estado = ST_ANTES_UB;
                } else if (c == ',') {
                    crudos = agregar(crudos, cuantos, lb, lb);
                    cuantos++;
                    estado = ST_TRAS_COMA;
                } else {
                    throw new IllegalArgumentException();
                }
            } else if (estado == ST_TRAS_LB) {
                if (esBlanco(c)) {
                    continue;
                }
                if (c == '-' || c == ':') {
                    estado = ST_ANTES_UB;
                } else if (c == ',') {
                    crudos = agregar(crudos, cuantos, lb, lb);
                    cuantos++;
                    estado = ST_TRAS_COMA;
                } else {
                    throw new IllegalArgumentException();
                }
            } else if (estado == ST_ANTES_UB) {
                if (esBlanco(c)) {
                    continue;
                }
                if (d < 0) {
                    throw new IllegalArgumentException();
                }
                ub = d;
                estado = ST_EN_UB;
            } else if (estado == ST_EN_UB) {
                if (d >= 0) {
                    ub = ub * 10 + d;
                } else if (esBlanco(c)) {
                    estado = ST_TRAS_UB;
                } else if (c == ',') {
                    crudos = agregar(crudos, cuantos, lb, ub);
                    cuantos++;
                    estado = ST_TRAS_COMA;
                } else {
                    throw new IllegalArgumentException();
                }
            } else {
                // ST_TRAS_UB
                if (esBlanco(c)) {
                    continue;
                }
                if (c == ',') {
                    crudos = agregar(crudos, cuantos, lb, ub);
                    cuantos++;
                    estado = ST_TRAS_COMA;
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }
        // El fin de la cadena es valido en cinco de los siete estados. Los dos que no lo aceptan
        // son los que quedaron esperando algo: ST_ANTES_UB (vimos el guion) y ST_TRAS_COMA (vimos
        // la coma).
        if (estado == ST_EN_LB || estado == ST_TRAS_LB) {
            crudos = agregar(crudos, cuantos, lb, lb);
            cuantos++;
        } else if (estado == ST_EN_UB || estado == ST_TRAS_UB) {
            crudos = agregar(crudos, cuantos, lb, ub);
            cuantos++;
        } else if (estado != ST_INICIO) {
            throw new IllegalArgumentException();
        }
        return canonicalArrayForm(crudos, cuantos);
    }

    // La forma `int[][]`. Cada fila es {n} o {lb, ub}; cualquier otro largo es un error.
    private static int[][] parse(int[][] members) {
        int n = (members == null) ? 0 : members.length;
        int[][] crudos = new int[n < 1 ? 1 : n][];
        int cuantos = 0;
        for (int i = 0; i < n; i++) {
            int lb;
            int ub;
            if (members[i].length == 1) {
                lb = members[i][0];
                ub = members[i][0];
            } else if (members[i].length == 2) {
                lb = members[i][0];
                ub = members[i][1];
            } else {
                throw new IllegalArgumentException();
            }
            if (lb <= ub) {
                if (lb < 0) {
                    throw new IllegalArgumentException();
                }
                crudos = agregar(crudos, cuantos, lb, ub);
                cuantos++;
            }
        }
        return canonicalArrayForm(crudos, cuantos);
    }

    // Apila {lb, ub} en la posicion `cuantos`, agrandando el arreglo si hace falta.
    private static int[][] agregar(int[][] crudos, int cuantos, int lb, int ub) {
        int[][] destino = crudos;
        if (cuantos >= destino.length) {
            int[][] mayor = new int[destino.length * 2 + 1][];
            for (int i = 0; i < cuantos; i++) {
                mayor[i] = destino[i];
            }
            destino = mayor;
        }
        destino[cuantos] = new int[] {lb, ub};
        return destino;
    }

    // Ordena, descarta los vacios y fusiona los que se solapan o se tocan.
    //
    // La fusion se decide con `long` y no con `int`. El motivo, medido por ablacion (se cambio la
    // comparacion a `int` y la prueba 60 de PrnSetIntSyntaxTest paso a fallar): con `ub + 1` en
    // `int`, un rango que termina en Integer.MAX_VALUE da la vuelta a Integer.MIN_VALUE y la
    // comparacion `lb <= ub + 1` sale falsa contra **cualquier** limite inferior. O sea que el
    // error del desbordamiento es dejar de fusionar lo que si es adyacente, no fusionar de mas:
    // `{{0, MAX}, {MAX, MAX}}` tiene que dar "0-2147483647" y con `int` daba dos rangos.
    //
    // **Divergencia conocida y unica contra el JDK, verificada corriendo el mismo programa contra
    // los dos.** El JDK fusiona con `Math.max(lba, lbb) - Math.min(uba, ubb) <= 1`, en `int`, y esa
    // resta desborda cuando uno de los limites es negativo. Limites negativos no se pueden meter
    // por ninguna de las cuatro formas validas --todas rechazan el signo-- pero si aparecen por el
    // desborde de la forma de texto que documenta la cabecera, y ahi las dos implementaciones se
    // separan:
    //
    //     "0,2147483648"   JDK: "-2147483648-0"    nuestro: "-2147483648,0"
    //
    // El JDK fusiona los dos puntos en un rango de cuatro mil millones de elementos porque la
    // resta le dio la vuelta; nosotros los dejamos separados, que es lo que son. Se prefirio no
    // replicar el desbordamiento: la entrada ya es basura en los dos casos, y copiar el segundo
    // desborde para tapar el primero haria que `contains(-5)` devolviera true.
    private static int[][] canonicalArrayForm(int[][] crudos, int cuantos) {
        // Insercion: la lista es corta y asi no hace falta un Comparator.
        for (int i = 1; i < cuantos; i++) {
            int[] actual = crudos[i];
            int j = i - 1;
            while (j >= 0 && (crudos[j][0] > actual[0]
                              || (crudos[j][0] == actual[0] && crudos[j][1] > actual[1]))) {
                crudos[j + 1] = crudos[j];
                j--;
            }
            crudos[j + 1] = actual;
        }
        int[][] fusionados = new int[cuantos][];
        int usados = 0;
        for (int i = 0; i < cuantos; i++) {
            int lb = crudos[i][0];
            int ub = crudos[i][1];
            if (lb > ub) {
                continue;
            }
            if (usados > 0 && ((long) lb) <= ((long) fusionados[usados - 1][1]) + 1L) {
                if (ub > fusionados[usados - 1][1]) {
                    fusionados[usados - 1][1] = ub;
                }
            } else {
                fusionados[usados] = new int[] {lb, ub};
                usados++;
            }
        }
        int[][] resultado = new int[usados][];
        for (int i = 0; i < usados; i++) {
            resultado[i] = fusionados[i];
        }
        return resultado;
    }

    // Una copia: el arreglo interno no sale nunca.
    public int[][] getMembers() {
        int n = this.members.length;
        int[][] result = new int[n][2];
        for (int i = 0; i < n; i++) {
            result[i][0] = this.members[i][0];
            result[i][1] = this.members[i][1];
        }
        return result;
    }

    // Los rangos estan ordenados, asi que se puede cortar apenas se pasa.
    public boolean contains(int x) {
        int n = this.members.length;
        for (int i = 0; i < n; i++) {
            if (x < this.members[i][0]) {
                return false;
            }
            if (x <= this.members[i][1]) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(IntegerSyntax attribute) {
        return contains(attribute.getValue());
    }

    // El menor miembro **estrictamente mayor** que x, o -1 si no hay.
    public int next(int x) {
        int n = this.members.length;
        for (int i = 0; i < n; i++) {
            if (x < this.members[i][0]) {
                return this.members[i][0];
            }
            if (x < this.members[i][1]) {
                return x + 1;
            }
        }
        return -1;
    }

    // Componente a componente: los dos lados estan canonicalizados, asi que alcanza.
    public boolean equals(Object object) {
        if (!(object instanceof SetOfIntegerSyntax)) {
            return false;
        }
        int[][] otros = ((SetOfIntegerSyntax) object).members;
        int n = this.members.length;
        if (n != otros.length) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            if (this.members[i][0] != otros[i][0] || this.members[i][1] != otros[i][1]) {
                return false;
            }
        }
        return true;
    }

    // La suma de los extremos. Barato y consistente con equals gracias a la canonicalizacion.
    public int hashCode() {
        int result = 0;
        int n = this.members.length;
        for (int i = 0; i < n; i++) {
            result += this.members[i][0] + this.members[i][1];
        }
        return result;
    }

    // "1-5,7,10-12". Un rango de un solo elemento se imprime sin guion; el conjunto vacio es la
    // cadena vacia.
    public String toString() {
        StringBuilder result = new StringBuilder();
        int n = this.members.length;
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                result.append(',');
            }
            int lb = this.members[i][0];
            int ub = this.members[i][1];
            if (lb == ub) {
                result.append(lb);
            } else {
                result.append(lb);
                result.append('-');
                result.append(ub);
            }
        }
        return result.toString();
    }
}
