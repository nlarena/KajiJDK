package java.text;

/**
 * El Algoritmo Bidireccional de Unicode (UAX #9), que es lo único que hay detrás de {@link Bidi}.
 *
 * <p>El problema que resuelve: un texto que mezcla árabe o hebreo con latín se GUARDA en orden
 * lógico —el orden en que se lee— pero se DIBUJA en un orden distinto, y no hay una regla local que
 * lo determine. Un número dentro de una frase en árabe se escribe de izquierda a derecha aunque la
 * frase vaya al revés, y un signo de puntuación entre dos idiomas toma la dirección de lo que lo
 * rodea. El algoritmo asigna a cada carácter un NIVEL de encaje —par es izquierda a derecha, impar
 * derecha a izquierda— y de esos niveles sale todo lo demás.
 *
 * <p>No es pública porque el JDK tampoco la expone: la cara visible es {@code Bidi}. Vive aparte
 * para que la clase pública quede leyéndose como una API y no como un intérprete de reglas.
 *
 * <p><b>Qué reglas están.</b> P2-P3 (dirección del párrafo), X1-X10 (encajes explícitos,
 * anulaciones y aislantes, con la pila de 125 niveles y los contadores de desborde), W1-W7
 * (resolución de tipos débiles), N1-N2 (neutros), I1-I2 (niveles implícitos) y L1 (reposición de
 * los separadores y del espacio final).
 *
 * <p><b>Qué regla NO está, y qué implica.</b> <b>N0</b>, la de los pares de corchetes que Unicode
 * 6.3 agregó, necesita la tabla {@code BidiBrackets.txt} —qué carácter cierra a cuál— que esta
 * biblioteca no trae. Sin ella los corchetes se resuelven como neutros comunes, que es exactamente
 * lo que hacía el algoritmo antes de 6.3. La diferencia se ve sólo cuando un par de corchetes
 * encierra texto de dirección contraria a la que los rodea; en cualquier otro caso el resultado es
 * idéntico. Se documenta en lugar de inventar una tabla parcial: media tabla de corchetes daría
 * niveles correctos para unos pares y equivocados para otros, sin forma de saber cuál es cuál.
 */
final class AlgoritmoBidi {

    // Los tipos son los de Character.getDirectionality, con los mismos números: reusar la
    // numeración evita una tabla de traducción que sólo podría estar mal.
    static final byte L = 0;
    static final byte R = 1;
    static final byte AL = 2;
    static final byte EN = 3;
    static final byte ES = 4;
    static final byte ET = 5;
    static final byte AN = 6;
    static final byte CS = 7;
    static final byte NSM = 8;
    static final byte BN = 9;
    static final byte B = 10;
    static final byte S = 11;
    static final byte WS = 12;
    static final byte ON = 13;
    static final byte LRE = 14;
    static final byte LRO = 15;
    static final byte RLE = 16;
    static final byte RLO = 17;
    static final byte PDF = 18;
    static final byte LRI = 19;
    static final byte RLI = 20;
    static final byte FSI = 21;
    static final byte PDI = 22;

    static final int MAX_DEPTH = 125;

    private final char[] texto;
    private final byte[] tipoInicial;
    private final byte[] tipo;
    private final byte[] nivel;
    private final int[] parejaPdi;      // por posición: índice del PDI que cierra un aislante
    private final int[] parejaInicio;   // por posición del PDI: índice del aislante que abre
    private final int n;
    private byte nivelParrafo;

    AlgoritmoBidi(char[] texto, byte[] encajes, int nivelBase) {
        this.texto = texto;
        this.n = texto.length;
        this.tipoInicial = new byte[this.n];
        this.tipo = new byte[this.n];
        this.nivel = new byte[this.n];
        this.parejaPdi = new int[this.n];
        this.parejaInicio = new int[this.n];
        for (int i = 0; i < this.n; i = i + 1) {
            byte t = Character.getDirectionality(texto[i]);
            if (t < 0) {
                t = AlgoritmoBidi.L;
            }
            this.tipoInicial[i] = t;
            this.tipo[i] = t;
        }
        this.emparejarAislantes();
        if (nivelBase >= 0) {
            this.nivelParrafo = (byte) nivelBase;
        } else {
            // nivelBase < 0 pide "deducilo del texto" (P2/P3); el -2 significa además "si no hay
            // ningún fuerte, izquierda a derecha" y el -1, "derecha a izquierda".
            int deducido = this.primerFuerte(0, this.n);
            if (deducido < 0) {
                if (nivelBase == -1) {
                    deducido = 1;
                } else {
                    deducido = 0;
                }
            }
            this.nivelParrafo = (byte) deducido;
        }
        this.explicitos();
        this.porSecuencias();
        this.aplicarL1(0, this.n);
        if (encajes != null) {
            this.aplicarEncajes(encajes);
        }
    }

    byte nivelParrafo() {
        return this.nivelParrafo;
    }

    byte[] niveles() {
        byte[] out = new byte[this.n];
        for (int i = 0; i < this.n; i = i + 1) {
            out[i] = this.nivel[i];
        }
        return out;
    }

    /**
     * Los encajes que el llamador impuso a mano, aplicados sobre los que calculó el algoritmo.
     *
     * <p>Un valor positivo es un encaje y uno negativo una anulación; el cero deja el nivel
     * calculado. Se aplica al final y no al principio porque el contrato dice que reemplaza el
     * resultado, no que alimente el cálculo.
     */
    private void aplicarEncajes(byte[] encajes) {
        for (int i = 0; i < this.n && i < encajes.length; i = i + 1) {
            byte e = encajes[i];
            if (e == 0) {
                continue;
            }
            int nivelPedido = e;
            if (nivelPedido < 0) {
                nivelPedido = -nivelPedido;
            }
            if (nivelPedido > AlgoritmoBidi.MAX_DEPTH) {
                continue;
            }
            if (e > 0) {
                // Encaje: el texto conserva su dirección propia dentro del nivel impuesto.
                if (this.nivel[i] < nivelPedido) {
                    this.nivel[i] = (byte) nivelPedido;
                }
            } else {
                // Anulación: la dirección del nivel manda sobre la del carácter.
                this.nivel[i] = (byte) nivelPedido;
            }
        }
    }

    // P2/P3: el primer fuerte que no esté dentro de un aislante. Devuelve 0 (L), 1 (R) o -1 si no
    // hay ninguno.
    private int primerFuerte(int desde, int hasta) {
        int i = desde;
        while (i < hasta) {
            byte t = this.tipoInicial[i];
            if (t == AlgoritmoBidi.L) {
                return 0;
            }
            if (t == AlgoritmoBidi.R || t == AlgoritmoBidi.AL) {
                return 1;
            }
            if (t == AlgoritmoBidi.LRI || t == AlgoritmoBidi.RLI || t == AlgoritmoBidi.FSI) {
                // El contenido de un aislante NO cuenta para la dirección de afuera: de eso se
                // trata "aislar". Se salta hasta el PDI que lo cierra.
                int cierre = this.parejaPdi[i];
                if (cierre < 0) {
                    return -1;
                }
                i = cierre;
            }
            i = i + 1;
        }
        return -1;
    }

    // BD9: cada aislante se empareja con su PDI, contando los anidados.
    private void emparejarAislantes() {
        for (int i = 0; i < this.n; i = i + 1) {
            this.parejaPdi[i] = -1;
            this.parejaInicio[i] = -1;
        }
        int[] pila = new int[this.n + 1];
        int tope = 0;
        for (int i = 0; i < this.n; i = i + 1) {
            byte t = this.tipoInicial[i];
            if (t == AlgoritmoBidi.LRI || t == AlgoritmoBidi.RLI || t == AlgoritmoBidi.FSI) {
                pila[tope] = i;
                tope = tope + 1;
            } else if (t == AlgoritmoBidi.PDI && tope > 0) {
                tope = tope - 1;
                this.parejaPdi[pila[tope]] = i;
                this.parejaInicio[i] = pila[tope];
            }
        }
    }

    // X1-X8.
    private void explicitos() {
        byte[] pilaNivel = new byte[AlgoritmoBidi.MAX_DEPTH + 3];
        byte[] pilaAnula = new byte[AlgoritmoBidi.MAX_DEPTH + 3];
        boolean[] pilaAisla = new boolean[AlgoritmoBidi.MAX_DEPTH + 3];
        int tope = 0;
        pilaNivel[0] = this.nivelParrafo;
        pilaAnula[0] = -1;
        pilaAisla[0] = false;
        int desbordeAisla = 0;
        int desbordeEncaje = 0;
        int aislantesValidos = 0;

        for (int i = 0; i < this.n; i = i + 1) {
            byte t = this.tipoInicial[i];
            if (t == AlgoritmoBidi.RLE || t == AlgoritmoBidi.LRE || t == AlgoritmoBidi.RLO
                    || t == AlgoritmoBidi.LRO) {
                this.nivel[i] = pilaNivel[tope];
                boolean derecha = t == AlgoritmoBidi.RLE || t == AlgoritmoBidi.RLO;
                int nuevo = AlgoritmoBidi.siguiente(pilaNivel[tope], derecha);
                if (nuevo <= AlgoritmoBidi.MAX_DEPTH && desbordeAisla == 0 && desbordeEncaje == 0) {
                    tope = tope + 1;
                    pilaNivel[tope] = (byte) nuevo;
                    pilaAisla[tope] = false;
                    if (t == AlgoritmoBidi.RLO) {
                        pilaAnula[tope] = AlgoritmoBidi.R;
                    } else if (t == AlgoritmoBidi.LRO) {
                        pilaAnula[tope] = AlgoritmoBidi.L;
                    } else {
                        pilaAnula[tope] = -1;
                    }
                } else if (desbordeAisla == 0) {
                    desbordeEncaje = desbordeEncaje + 1;
                }
            } else if (t == AlgoritmoBidi.RLI || t == AlgoritmoBidi.LRI || t == AlgoritmoBidi.FSI) {
                boolean derecha;
                if (t == AlgoritmoBidi.FSI) {
                    // X5c: un FSI toma la dirección del primer fuerte de su propio contenido.
                    int cierre = this.parejaPdi[i];
                    int hasta = this.n;
                    if (cierre >= 0) {
                        hasta = cierre;
                    }
                    derecha = this.primerFuerte(i + 1, hasta) == 1;
                } else {
                    derecha = t == AlgoritmoBidi.RLI;
                }
                this.nivel[i] = pilaNivel[tope];
                if (pilaAnula[tope] >= 0) {
                    this.tipo[i] = pilaAnula[tope];
                }
                int nuevo = AlgoritmoBidi.siguiente(pilaNivel[tope], derecha);
                if (nuevo <= AlgoritmoBidi.MAX_DEPTH && desbordeAisla == 0 && desbordeEncaje == 0) {
                    aislantesValidos = aislantesValidos + 1;
                    tope = tope + 1;
                    pilaNivel[tope] = (byte) nuevo;
                    pilaAnula[tope] = -1;
                    pilaAisla[tope] = true;
                } else {
                    desbordeAisla = desbordeAisla + 1;
                }
            } else if (t == AlgoritmoBidi.PDI) {
                if (desbordeAisla > 0) {
                    desbordeAisla = desbordeAisla - 1;
                } else if (aislantesValidos > 0) {
                    desbordeEncaje = 0;
                    while (!pilaAisla[tope]) {
                        tope = tope - 1;
                    }
                    tope = tope - 1;
                    aislantesValidos = aislantesValidos - 1;
                }
                this.nivel[i] = pilaNivel[tope];
                if (pilaAnula[tope] >= 0) {
                    this.tipo[i] = pilaAnula[tope];
                }
            } else if (t == AlgoritmoBidi.PDF) {
                this.nivel[i] = pilaNivel[tope];
                if (desbordeAisla > 0) {
                    // Un PDF no cierra un aislante desbordado: los dos mecanismos no se cruzan.
                    continue;
                }
                if (desbordeEncaje > 0) {
                    desbordeEncaje = desbordeEncaje - 1;
                } else if (!pilaAisla[tope] && tope >= 1) {
                    tope = tope - 1;
                }
            } else if (t == AlgoritmoBidi.B) {
                tope = 0;
                desbordeAisla = 0;
                desbordeEncaje = 0;
                aislantesValidos = 0;
                this.nivel[i] = this.nivelParrafo;
            } else {
                this.nivel[i] = pilaNivel[tope];
                if (pilaAnula[tope] >= 0) {
                    this.tipo[i] = pilaAnula[tope];
                }
            }
        }
    }

    private static int siguiente(int nivel, boolean derecha) {
        if (derecha) {
            return (nivel + 1) | 1;
        }
        return (nivel + 2) & (~1);
    }

    // X9: los controles explícitos y los BN no participan de las reglas siguientes. No se borran
    // —haría falta reindexar todo— sino que se saltean, y al final heredan el nivel del anterior.
    private boolean removido(int i) {
        byte t = this.tipoInicial[i];
        return t == AlgoritmoBidi.RLE || t == AlgoritmoBidi.LRE || t == AlgoritmoBidi.RLO
                || t == AlgoritmoBidi.LRO || t == AlgoritmoBidi.PDF || t == AlgoritmoBidi.BN;
    }

    // X10: arma cada secuencia de corridas aisladas y le corre las reglas W, N e I.
    private void porSecuencias() {
        boolean[] visto = new boolean[this.n];
        for (int i = 0; i < this.n; i = i + 1) {
            if (this.removido(i) || visto[i]) {
                continue;
            }
            // Una secuencia arranca en una corrida cuyo primer carácter no sea un PDI que cierre
            // un aislante: ese PDI ya pertenece a la secuencia que abrió su aislante.
            if (this.tipoInicial[i] == AlgoritmoBidi.PDI && this.parejaInicio[i] >= 0) {
                continue;
            }
            int[] indices = this.secuenciaDesde(i, visto);
            if (indices.length == 0) {
                continue;
            }
            this.resolver(indices);
        }
        // X9 borra los controles explícitos y los BN, así que su nivel es "lo que quiera la
        // implementación". La elección importa igual, porque `getLevelAt` los devuelve: acá heredan
        // el nivel del carácter SIGUIENTE, no del anterior. Hacia adelante y no hacia atrás porque
        // un RLE abre el encaje que viene después —es parte de él, no de lo de antes—, y un PDF que
        // cierra queda pegado a lo que sigue. Verificado contra el JDK 25 sobre texto con RLE/PDF y
        // con RLO: con la regla hacia atrás los dos casos dan distinto.
        for (int i = this.n - 1; i >= 0; i = i - 1) {
            if (this.removido(i)) {
                if (i + 1 < this.n) {
                    this.nivel[i] = this.nivel[i + 1];
                } else {
                    this.nivel[i] = this.nivelParrafo;
                }
            }
        }
    }

    private int[] secuenciaDesde(int inicio, boolean[] visto) {
        int[] buffer = new int[this.n];
        int cuenta = 0;
        int i = inicio;
        byte nivelCorrida = this.nivel[inicio];
        while (i < this.n) {
            // Una corrida de nivel: caracteres consecutivos (salteando los removidos) con el
            // mismo nivel.
            while (i < this.n && (this.removido(i) || this.nivel[i] == nivelCorrida)) {
                if (!this.removido(i)) {
                    visto[i] = true;
                    buffer[cuenta] = i;
                    cuenta = cuenta + 1;
                }
                i = i + 1;
            }
            if (cuenta == 0) {
                break;
            }
            int ultimo = buffer[cuenta - 1];
            byte tu = this.tipoInicial[ultimo];
            // Si la corrida termina en un aislante con su PDI, la secuencia sigue en la corrida
            // donde está ese PDI: eso es lo que hace que el texto de afuera del aislante se lea
            // como continuo.
            if ((tu == AlgoritmoBidi.LRI || tu == AlgoritmoBidi.RLI || tu == AlgoritmoBidi.FSI)
                    && this.parejaPdi[ultimo] >= 0) {
                i = this.parejaPdi[ultimo];
            } else {
                break;
            }
        }
        int[] out = new int[cuenta];
        for (int k = 0; k < cuenta; k = k + 1) {
            out[k] = buffer[k];
        }
        return out;
    }

    private void resolver(int[] idx) {
        int m = idx.length;
        byte nivelSec = this.nivel[idx[0]];

        // sos/eos: la dirección "de afuera" a cada lado, que es la del nivel más alto entre la
        // secuencia y su vecino. Sin esto, un neutro al borde no tendría contra qué resolverse.
        byte sos = this.direccionDe(this.mayor(nivelSec, this.nivelAntesDe(idx[0])));
        int ultimo = idx[m - 1];
        byte tu = this.tipoInicial[ultimo];
        byte eos;
        if ((tu == AlgoritmoBidi.LRI || tu == AlgoritmoBidi.RLI || tu == AlgoritmoBidi.FSI)
                && this.parejaPdi[ultimo] < 0) {
            // Un aislante sin cierre deja el resto del párrafo adentro: el borde de la secuencia
            // es el del párrafo.
            eos = this.direccionDe(this.mayor(nivelSec, this.nivelParrafo));
        } else {
            eos = this.direccionDe(this.mayor(nivelSec, this.nivelDespuesDe(ultimo)));
        }

        byte[] t = new byte[m];
        for (int k = 0; k < m; k = k + 1) {
            t[k] = this.tipo[idx[k]];
        }

        // W1: una marca sin espacio toma el tipo del anterior; después de un aislante o un PDI, ON.
        byte anterior = sos;
        for (int k = 0; k < m; k = k + 1) {
            if (t[k] == AlgoritmoBidi.NSM) {
                if (anterior == AlgoritmoBidi.LRI || anterior == AlgoritmoBidi.RLI
                        || anterior == AlgoritmoBidi.FSI || anterior == AlgoritmoBidi.PDI) {
                    t[k] = AlgoritmoBidi.ON;
                } else {
                    t[k] = anterior;
                }
            }
            anterior = t[k];
        }

        // W2: un número europeo pasa a árabe si el último fuerte fue una letra árabe.
        byte fuerte = sos;
        for (int k = 0; k < m; k = k + 1) {
            if (t[k] == AlgoritmoBidi.L || t[k] == AlgoritmoBidi.R || t[k] == AlgoritmoBidi.AL) {
                fuerte = t[k];
            } else if (t[k] == AlgoritmoBidi.EN && fuerte == AlgoritmoBidi.AL) {
                t[k] = AlgoritmoBidi.AN;
            }
        }

        // W3: la letra árabe ya cumplió su papel en W2 y de acá en más es R.
        for (int k = 0; k < m; k = k + 1) {
            if (t[k] == AlgoritmoBidi.AL) {
                t[k] = AlgoritmoBidi.R;
            }
        }

        // W4: un separador ENTRE dos números del mismo tipo se vuelve número. "1.234" es un
        // número; "1." seguido de otra cosa, no.
        for (int k = 1; k + 1 < m; k = k + 1) {
            if (t[k] == AlgoritmoBidi.ES && t[k - 1] == AlgoritmoBidi.EN
                    && t[k + 1] == AlgoritmoBidi.EN) {
                t[k] = AlgoritmoBidi.EN;
            } else if (t[k] == AlgoritmoBidi.CS && t[k - 1] == t[k + 1]
                    && (t[k - 1] == AlgoritmoBidi.EN || t[k - 1] == AlgoritmoBidi.AN)) {
                t[k] = t[k - 1];
            }
        }

        // W5: una tira de terminadores pegada a un número europeo se vuelve número ("$12", "12%").
        for (int k = 0; k < m; k = k + 1) {
            if (t[k] != AlgoritmoBidi.ET) {
                continue;
            }
            int fin = k;
            while (fin < m && t[fin] == AlgoritmoBidi.ET) {
                fin = fin + 1;
            }
            boolean pegado = (k > 0 && t[k - 1] == AlgoritmoBidi.EN)
                    || (fin < m && t[fin] == AlgoritmoBidi.EN);
            if (pegado) {
                for (int j = k; j < fin; j = j + 1) {
                    t[j] = AlgoritmoBidi.EN;
                }
            }
            k = fin - 1;
        }

        // W6: lo que quedó de separadores y terminadores es neutro.
        for (int k = 0; k < m; k = k + 1) {
            if (t[k] == AlgoritmoBidi.ET || t[k] == AlgoritmoBidi.ES || t[k] == AlgoritmoBidi.CS) {
                t[k] = AlgoritmoBidi.ON;
            }
        }

        // W7: un número europeo precedido de texto latino ES texto latino.
        fuerte = sos;
        for (int k = 0; k < m; k = k + 1) {
            if (t[k] == AlgoritmoBidi.L || t[k] == AlgoritmoBidi.R) {
                fuerte = t[k];
            } else if (t[k] == AlgoritmoBidi.EN && fuerte == AlgoritmoBidi.L) {
                t[k] = AlgoritmoBidi.L;
            }
        }

        // N1/N2: una tira de neutros toma la dirección que la rodea si a los dos lados es la
        // misma, y si no, la del encaje. Los números cuentan como derecha a izquierda acá.
        for (int k = 0; k < m; k = k + 1) {
            if (!AlgoritmoBidi.esNeutro(t[k])) {
                continue;
            }
            int fin = k;
            while (fin < m && AlgoritmoBidi.esNeutro(t[fin])) {
                fin = fin + 1;
            }
            byte izq;
            if (k == 0) {
                izq = sos;
            } else {
                izq = AlgoritmoBidi.comoFuerte(t[k - 1]);
            }
            byte der;
            if (fin == m) {
                der = eos;
            } else {
                der = AlgoritmoBidi.comoFuerte(t[fin]);
            }
            byte resuelto;
            if (izq == der) {
                resuelto = izq;
            } else {
                resuelto = this.direccionDe(nivelSec);
            }
            for (int j = k; j < fin; j = j + 1) {
                t[j] = resuelto;
            }
            k = fin - 1;
        }

        // I1/I2: los niveles implícitos. En un nivel par la derecha sube uno y los números dos;
        // en uno impar, la izquierda y los números suben uno.
        for (int k = 0; k < m; k = k + 1) {
            byte nv = nivelSec;
            if ((nivelSec & 1) == 0) {
                if (t[k] == AlgoritmoBidi.R) {
                    nv = (byte) (nivelSec + 1);
                } else if (t[k] == AlgoritmoBidi.AN || t[k] == AlgoritmoBidi.EN) {
                    nv = (byte) (nivelSec + 2);
                }
            } else {
                if (t[k] == AlgoritmoBidi.L || t[k] == AlgoritmoBidi.AN
                        || t[k] == AlgoritmoBidi.EN) {
                    nv = (byte) (nivelSec + 1);
                }
            }
            this.nivel[idx[k]] = nv;
            this.tipo[idx[k]] = t[k];
        }
    }

    private static boolean esNeutro(byte t) {
        return t == AlgoritmoBidi.B || t == AlgoritmoBidi.S || t == AlgoritmoBidi.WS
                || t == AlgoritmoBidi.ON || t == AlgoritmoBidi.LRI || t == AlgoritmoBidi.RLI
                || t == AlgoritmoBidi.FSI || t == AlgoritmoBidi.PDI;
    }

    private static byte comoFuerte(byte t) {
        if (t == AlgoritmoBidi.EN || t == AlgoritmoBidi.AN || t == AlgoritmoBidi.R) {
            return AlgoritmoBidi.R;
        }
        return AlgoritmoBidi.L;
    }

    private byte direccionDe(int nivel) {
        if ((nivel & 1) == 0) {
            return AlgoritmoBidi.L;
        }
        return AlgoritmoBidi.R;
    }

    private int mayor(int a, int b) {
        if (a > b) {
            return a;
        }
        return b;
    }

    private int nivelAntesDe(int i) {
        int k = i - 1;
        while (k >= 0 && this.removido(k)) {
            k = k - 1;
        }
        if (k < 0) {
            return this.nivelParrafo;
        }
        return this.nivel[k];
    }

    private int nivelDespuesDe(int i) {
        int k = i + 1;
        while (k < this.n && this.removido(k)) {
            k = k + 1;
        }
        if (k >= this.n) {
            return this.nivelParrafo;
        }
        return this.nivel[k];
    }

    /**
     * L1 sobre un rango: los separadores y el espacio que los precede vuelven al nivel del párrafo.
     *
     * <p>Es lo que hace que el espacio final de una línea no se dibuje del lado equivocado. Se
     * aplica sobre los tipos ORIGINALES, no sobre los resueltos: un espacio que N1 convirtió en
     * "derecha" sigue siendo un espacio para esta regla.
     */
    void aplicarL1(int desde, int hasta) {
        boolean enCola = true;
        for (int i = hasta - 1; i >= desde; i = i - 1) {
            byte t = this.tipoInicial[i];
            if (t == AlgoritmoBidi.B || t == AlgoritmoBidi.S) {
                this.nivel[i] = this.nivelParrafo;
                enCola = true;
            } else if (enCola && (t == AlgoritmoBidi.WS || t == AlgoritmoBidi.LRI
                    || t == AlgoritmoBidi.RLI || t == AlgoritmoBidi.FSI
                    || t == AlgoritmoBidi.PDI || this.removido(i))) {
                this.nivel[i] = this.nivelParrafo;
            } else {
                enCola = false;
            }
        }
    }
}
