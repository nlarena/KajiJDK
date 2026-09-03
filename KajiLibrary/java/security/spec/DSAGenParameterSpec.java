package java.security.spec;

// Que parametros de dominio DSA generar: el largo de p, el de q y el de la semilla.
//
// Es la unica spec del paquete que valida **combinaciones** y no valores sueltos, y la razon es que
// FIPS 186-3 no deja elegir los dos largos por separado: solo (1024, 160), (2048, 224), (2048, 256)
// y (3072, 256) son legales. La restriccion no es burocratica —el largo de q fija el costo del mejor
// ataque generico contra el logaritmo discreto en el subgrupo, y el de p el del mejor ataque de
// criba sobre el grupo entero— y elegir un q chico con un p grande da un par que parece fuerte y no
// lo es. Que la clase rechace las combinaciones invalidas en el constructor es lo que impide que ese
// error se descubra recien cuando ya hay claves emitidas.
public final class DSAGenParameterSpec implements AlgorithmParameterSpec {

    private final int primePLen;
    private final int subprimeQLen;
    private final int seedLen;

    // Sin largo de semilla explicito se usa el de q, que es el minimo legal.
    public DSAGenParameterSpec(int primePLen, int subprimeQLen) {
        this(primePLen, subprimeQLen, subprimeQLen);
    }

    public DSAGenParameterSpec(int primePLen, int subprimeQLen, int seedLen) {
        switch (primePLen) {
            case 1024:
                if (subprimeQLen != 160) {
                    throw new IllegalArgumentException(
                        "subprimeQLen must be 160 when primePLen=1024");
                }
                break;
            case 2048:
                if (subprimeQLen != 224 && subprimeQLen != 256) {
                    throw new IllegalArgumentException(
                        "subprimeQLen must be 224 or 256 when primePLen=2048");
                }
                break;
            case 3072:
                if (subprimeQLen != 256) {
                    throw new IllegalArgumentException(
                        "subprimeQLen must be 256 when primePLen=3072");
                }
                break;
            default:
                throw new IllegalArgumentException("primePLen must be 1024, 2048, or 3072");
        }
        // Una semilla mas corta que q le pondria un techo a la entropia de todo el dominio: no
        // importa cuan grande sea p si el proceso que lo genero arranco de menos bits.
        if (seedLen < subprimeQLen) {
            throw new IllegalArgumentException(
                "seedLen must be equal to or greater than subprimeQLen");
        }
        this.primePLen = primePLen;
        this.subprimeQLen = subprimeQLen;
        this.seedLen = seedLen;
    }

    public int getPrimePLength() {
        return this.primePLen;
    }

    public int getSubprimeQLength() {
        return this.subprimeQLen;
    }

    public int getSeedLength() {
        return this.seedLen;
    }
}
