package java.security;

// Los parametros de un generador determinista de bits aleatorios (DRBG), segun NIST SP 800-90Ar1.
//
// ===============================================================================================
// QUE ES UN DRBG Y POR QUE LOS PARAMETROS SON TRES CLASES
// ===============================================================================================
//
// Un DRBG no produce aleatoriedad: la **estira**. Se lo siembra una vez con entropia de verdad y a
// partir de ahi genera bits deterministicamente. Toda su seguridad esta en la semilla y en que su
// estado interno no se filtre.
//
// De ahi salen los tres momentos que cada clase describe:
//
//   - `Instantiation` es la creacion: cuanta fuerza se pide, si se va a poder resembrar, y una
//     "cadena de personalizacion" que separa a este generador de otro sembrado con la misma
//     entropia. Esa cadena es lo que evita que dos maquinas clonadas —dos VMs de la misma imagen—
//     produzcan la misma secuencia.
//   - `NextBytes` es cada pedido de bits.
//   - `Reseed` es volver a mezclar entropia fresca.
//
// La resistencia a prediccion es la propiedad que mas se malinterpreta: quiere decir que quien vea
// el estado interno **ahora** no puede predecir los bits que vengan despues, porque antes de
// generarlos se mezcla entropia nueva. Es cara —pide entropia de verdad en cada llamada— y por eso
// se pide por operacion y no se deja prendida.
//
// Esta clase es un **descriptor** y nada mas: no genera un solo bit. Se pasa a
// `SecureRandom.getInstance("DRBG", params)`, que en esta biblioteca no existe (ver `Signature`
// para por que no hay `SecureRandom`). Se declara igual porque es datos puros y porque describir
// correctamente lo que se pide es independiente de que haya quien lo cumpla.
public final class DrbgParameters {

    // No se instancia: es solo el techo de las tres clases anidadas y de sus fabricas.
    private DrbgParameters() {
    }

    // Que sabe hacer un DRBG mas alla de generar.
    public enum Capability {

        // Resembrar y resistencia a prediccion.
        PR_AND_RESEED,

        // Solo resembrar.
        RESEED_ONLY,

        // Ninguna de las dos: una vez sembrado, genera hasta agotarse.
        NONE;

        // El nombre tal como lo escribe la propiedad de seguridad `securerandom.drbg.config`:
        // "pr_and_reseed", "reseed_only", "none".
        @Override
        public String toString() {
            return this.name().toLowerCase(java.util.Locale.ROOT);
        }

        // PR implica reseed: no se puede mezclar entropia por operacion sin poder resembrar.
        public boolean supportsReseeding() {
            return this != NONE;
        }

        public boolean supportsPredictionResistance() {
            return this == PR_AND_RESEED;
        }
    }

    // Lo que se pide al crear el DRBG.
    public static final class Instantiation implements SecureRandomParameters {

        private final int strength;
        private final Capability capability;
        private final byte[] personalizationString;

        private Instantiation(int strength, Capability capability, byte[] personalizationString) {
            this.strength = strength;
            this.capability = capability;
            this.personalizationString = personalizationString;
        }

        // La fuerza en bits, o -1 para "la que el proveedor prefiera". Un DRBG puede dar **mas** de
        // lo pedido, nunca menos.
        public int getStrength() {
            return this.strength;
        }

        public Capability getCapability() {
            return this.capability;
        }

        // Copia de la cadena de personalizacion, o null. Es lo que separa a este generador de otro
        // sembrado con la misma entropia; puede ser algo tan mundano como el nombre de la maquina.
        public byte[] getPersonalizationString() {
            return copiar(this.personalizationString);
        }

        // Imprime la cadena de personalizacion entera, no un resumen. No es un descuido del JDK:
        // esa cadena **no es secreta** —su unico trabajo es separar dos generadores, no aportar
        // entropia— asi que verla en un log no debilita nada.
        //
        // El formato se arma a mano en vez de con `java.util.Arrays.toString(byte[])` porque esa
        // sobrecarga esta rota en esta biblioteca: imprime cada byte como **caracter** en lugar de
        // como numero, asi que {1} sale como el caracter de control 0x01 y no como "1". Ver el
        // informe; cuando se arregle, esto se puede reemplazar por la llamada directa.
        @Override
        public String toString() {
            return this.strength + "," + this.capability + ","
                + listar(this.personalizationString);
        }
    }

    // Lo que se pide en cada generacion de bits.
    public static final class NextBytes implements SecureRandomParameters {

        private final int strength;
        private final boolean predictionResistance;
        private final byte[] additionalInput;

        private NextBytes(int strength, boolean predictionResistance, byte[] additionalInput) {
            this.strength = strength;
            this.predictionResistance = predictionResistance;
            this.additionalInput = additionalInput;
        }

        public int getStrength() {
            return this.strength;
        }

        // Si hay que mezclar entropia fresca antes de generar. Caro, y solo tiene sentido si el
        // DRBG se creo con `PR_AND_RESEED`.
        public boolean getPredictionResistance() {
            return this.predictionResistance;
        }

        // Copia de la entrada adicional, o null. Se mezcla con el estado solo para esta llamada.
        public byte[] getAdditionalInput() {
            return copiar(this.additionalInput);
        }
    }

    // Lo que se pide al resembrar.
    public static final class Reseed implements SecureRandomParameters {

        private final boolean predictionResistance;
        private final byte[] additionalInput;

        private Reseed(boolean predictionResistance, byte[] additionalInput) {
            this.predictionResistance = predictionResistance;
            this.additionalInput = additionalInput;
        }

        public boolean getPredictionResistance() {
            return this.predictionResistance;
        }

        public byte[] getAdditionalInput() {
            return copiar(this.additionalInput);
        }
    }

    private static byte[] copiar(byte[] b) {
        if (b == null) {
            return null;
        }
        byte[] c = new byte[b.length];
        System.arraycopy(b, 0, c, 0, b.length);
        return c;
    }

    // El mismo formato que `java.util.Arrays.toString(byte[])`: "null", "[]", "[1, 2]".
    private static String listar(byte[] b) {
        if (b == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        while (i < b.length) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append((int) b[i]);
            i = i + 1;
        }
        sb.append(']');
        return sb.toString();
    }

    // Los parametros de creacion. `strength` puede ser -1 para dejar elegir al proveedor; cualquier
    // otro negativo es un error, porque pedir "fuerza -3" no significa nada.
    public static Instantiation instantiation(int strength, Capability capability,
                                              byte[] personalizationString) {
        if (strength < -1) {
            throw new IllegalArgumentException("Invalid strength: " + strength);
        }
        if (capability == null) {
            throw new NullPointerException("Capability is null");
        }
        return new Instantiation(strength, capability, copiar(personalizationString));
    }

    public static NextBytes nextBytes(int strength, boolean predictionResistance,
                                      byte[] additionalInput) {
        if (strength < -1) {
            throw new IllegalArgumentException("Invalid strength: " + strength);
        }
        return new NextBytes(strength, predictionResistance, copiar(additionalInput));
    }

    public static Reseed reseed(boolean predictionResistance, byte[] additionalInput) {
        return new Reseed(predictionResistance, copiar(additionalInput));
    }
}
