package jdk.incubator.vector;

/**
 * Los operadores que se le pasan a un vector para decirle que hacer.
 *
 * <h2>Por que la operacion es un objeto y no un metodo</h2>
 *
 * <p>En vez de {@code v.add(w)}, {@code v.mul(w)}, {@code v.min(w)} y otros cincuenta, el API tiene
 * un solo {@code lanewise(op, w)} y {@code op} es uno de estos objetos. La diferencia no es de
 * gusto: asi se puede escribir un algoritmo que recibe la operacion como parametro y sirve para
 * todas, que es justamente lo que se hace cuando se reduce un arreglo o se compone una expresion.
 *
 * <p>El tipo del operador es el que limita donde entra. {@link Unary} toma un argumento,
 * {@link Binary} dos, {@link Ternary} tres; {@link Comparison} y {@link Test} dan una mascara en
 * lugar de un vector, y {@link Associative} es un {@link Binary} que ademas se puede usar para
 * reducir, porque agrupar de a pares en cualquier orden da el mismo resultado. Que
 * {@code reduceLanes} pida un {@link Associative} y no un {@link Binary} es lo que impide reducir
 * con una resta, que daria un resultado distinto segun como el hardware parta el vector.
 *
 * <h2>Un operador es una descripcion, no una cuenta</h2>
 *
 * <p>Cada constante de aca es un objeto con datos: como se llama, con que simbolo se escribe,
 * cuantos argumentos toma, si es asociativa, sobre que tipos de posicion sirve. No sabe calcular
 * nada. El que calcula es el vector, que recibe el operador y elige la instruccion de maquina.
 *
 * <p>Por eso esta clase esta implementada <strong>entera y de verdad</strong> en esta biblioteca,
 * aunque no haya vectores: los 109 operadores existen, responden lo que corresponde y estan
 * comprobados contra el JDK 25. {@code VectorOperators.ADD.operatorName()} devuelve {@code "+"} aca
 * igual que alla, y {@code ADD.compatibleWith(float.class)} contesta bien.
 *
 * <p>Lo que no se puede es usarlos: hace falta un vector al cual pasarselos, y crear un vector si
 * necesita los intrinsecos de la VM.
 *
 * <h2>Las conversiones</h2>
 *
 * <p>Hay dos maneras de pasar de un tipo a otro y el API las separa con cuidado.
 * {@link #B2D} y sus hermanas conservan el <strong>valor</strong>: es el {@code (double) b} de toda
 * la vida. {@link #REINTERPRET_F2I} y sus hermanas conservan los <strong>bits</strong>: el mismo
 * patron leido como otra cosa.
 *
 * <p>{@code ZERO_EXTEND} es el caso raro del medio. Al ensanchar un entero por bits sobran lugares
 * que hay que llenar, y estas los llenan con ceros en vez de copiar el signo: {@code (byte) -1}
 * ensanchado asi da 255, no -1.
 *
 * @since 16
 */
public abstract class VectorOperators {

    private VectorOperators() {
    }

    /**
     * Los datos que las constantes de abajo necesitan para construirse.
     *
     * <p>Van en una clase aparte por una razon dura: los campos estaticos se inicializan en el orden
     * en que estan escritos, y las cuarenta conversiones de mas abajo llaman a {@link #cast} apenas
     * se crean. Si estos arreglos estuvieran despues de ellas todavia valdrian {@code null} en ese
     * momento y la clase entera fallaria al cargarse. Una clase anidada se inicializa recien cuando
     * se la toca, asi que el orden dentro del archivo deja de importar.
     */
    private static final class Tabla {

        /** Los tipos de posicion, en el orden en que los numera {@code indice}. */
        static final Class<?>[] TIPOS = {
            byte.class, short.class, int.class, long.class, float.class, double.class,
        };

        /** Las letras con las que se arman los nombres de las conversiones. */
        static final String LETRAS = "BSILFD";

        private Tabla() {
        }
    }

    /** Sirve para los seis tipos de posicion. */
    static final int TODOS = 0x3F;

    /** Solo para los cuatro enteros. */
    static final int ENTEROS = 0x0F;

    /** Solo para {@code float} y {@code double}. */
    static final int FLOTANTES = 0x30;

    /**
     * Lo que todo operador sabe decir de si mismo.
     *
     * @since 16
     */
    public interface Operator {

        /**
         * El nombre de la constante, como {@code "ADD"}.
         *
         * @return el nombre
         */
        String name();

        /**
         * El simbolo con que se escribe la operacion, como {@code "+"}.
         *
         * <p>No siempre es un simbolo: cuando la operacion no tiene uno, es el nombre del metodo
         * equivalente ({@code "sqrt"}) o directamente la formula ({@code "a!=0?a:b"}).
         *
         * @return el simbolo
         */
        String operatorName();

        /**
         * Cuantos argumentos toma.
         *
         * @return uno, dos o tres
         */
        int arity();

        /**
         * Si el resultado es una mascara y no un vector.
         *
         * @return cierto para las comparaciones y las pruebas
         */
        boolean isBoolean();

        /**
         * El tipo del resultado, cuando se lo puede decir sin saber sobre que vector se aplica.
         *
         * <p>Para las operaciones comunes es {@code Object.class}, que aca quiere decir "el mismo
         * tipo que la entrada". Para las comparaciones y las pruebas es {@code boolean.class}, y
         * para una {@link Conversion} es el tipo de destino.
         *
         * @return el tipo del resultado
         */
        Class<?> rangeType();

        /**
         * Si agrupar de a pares en cualquier orden da el mismo resultado.
         *
         * <p>Es lo que hace falta para reducir un vector, porque el hardware parte el trabajo como
         * le conviene y ese orden no esta bajo control de quien escribe el algoritmo.
         *
         * @return cierto si es asociativa
         */
        boolean isAssociative();

        /**
         * Si la operacion sirve para posiciones de ese tipo.
         *
         * <p>Un tipo que no puede ser posicion de un vector no devuelve {@code false}: es un error.
         * Preguntar si {@code ADD} sirve para {@code String} no es una pregunta con respuesta.
         *
         * @param elementType el tipo de la posicion
         * @return cierto si sirve
         * @throws UnsupportedOperationException si no es un tipo de posicion
         */
        boolean compatibleWith(Class<?> elementType);
    }

    /**
     * Un operador de un argumento.
     *
     * @since 16
     */
    public interface Unary extends Operator {
    }

    /**
     * Un operador de dos argumentos.
     *
     * @since 16
     */
    public interface Binary extends Operator {
    }

    /**
     * Un operador de dos argumentos que ademas sirve para reducir.
     *
     * <p>Que herede de {@link Binary} y no al reves es la parte que importa: donde se pide un
     * {@link Associative} no entra un {@link Binary} cualquiera, y eso deja fuera de
     * {@code reduceLanes} a la resta y a la division, que darian un resultado distinto segun como
     * el hardware parta el vector.
     *
     * @since 16
     */
    public interface Associative extends Binary {
    }

    /**
     * Un operador de tres argumentos.
     *
     * @since 16
     */
    public interface Ternary extends Operator {
    }

    /**
     * Una comparacion entre dos vectores, que da una mascara.
     *
     * @since 16
     */
    public interface Comparison extends Operator {
    }

    /**
     * Una pregunta sobre cada posicion de un vector, que da una mascara.
     *
     * @since 16
     */
    public interface Test extends Operator {
    }

    /**
     * Una conversion de un tipo de posicion a otro.
     *
     * <p>Los parametros de tipo son los tipos envueltos --{@code Conversion<Byte, Double>}-- porque
     * un parametro de tipo no puede ser primitivo. Los metodos, en cambio, devuelven los primitivos:
     * {@code B2D.domainType()} es {@code byte.class}.
     *
     * @param <E> el tipo de origen, envuelto
     * @param <F> el tipo de destino, envuelto
     * @since 16
     */
    public interface Conversion<E, F> extends Operator {

        /**
         * El tipo de origen.
         *
         * @return el tipo de origen, primitivo
         */
        Class<E> domainType();

        /**
         * El tipo de destino.
         *
         * @return el tipo de destino, primitivo
         */
        @Override
        Class<F> rangeType();

        /**
         * Comprueba que esta conversion sea justo la que va de ese tipo a ese otro.
         *
         * <p>Existe para poder recuperar los parametros de tipo despues de haber pasado por una
         * variable sin parametrizar, que es lo que pasa cuando la conversion se elige en tiempo de
         * ejecucion. Si no coincide falla en el acto, y no mas tarde con un tipo equivocado dando
         * vueltas.
         *
         * @param <E> el tipo de origen esperado
         * @param <F> el tipo de destino esperado
         * @param from el tipo de origen esperado
         * @param to el tipo de destino esperado
         * @return esta misma conversion, con los parametros de tipo puestos
         * @throws ClassCastException si no es esa conversion
         */
        <E, F> Conversion<E, F> check(Class<E> from, Class<F> to);

        /**
         * La conversion de valor entre esos dos tipos.
         *
         * <p>Es la que hace el {@code (double) b} de Java. Cuando los dos tipos son el mismo la
         * conversion no hace nada, y aun asi existe: se llama {@code COPY_X2X}.
         *
         * @param <E> el tipo de origen, envuelto
         * @param <F> el tipo de destino, envuelto
         * @param from el tipo de origen
         * @param to el tipo de destino
         * @return la conversion
         * @throws UnsupportedOperationException si alguno no es un tipo de posicion
         */
        static <E, F> Conversion<E, F> ofCast(Class<E> from, Class<F> to) {
            return cast(from, to);
        }

        /**
         * La conversion de bits entre esos dos tipos.
         *
         * <p>Los bits se conservan y el valor no. Al ensanchar un entero los lugares que sobran se
         * llenan con ceros, no con el signo.
         *
         * @param <E> el tipo de origen, envuelto
         * @param <F> el tipo de destino, envuelto
         * @param from el tipo de origen
         * @param to el tipo de destino
         * @return la conversion
         * @throws UnsupportedOperationException si alguno no es un tipo de posicion
         */
        static <E, F> Conversion<E, F> ofReinterpret(Class<E> from, Class<F> to) {
            return reinterpret(from, to);
        }
    }

    /**
     * Invierte todos los bits.
     */
    public static final Unary NOT = unaria("NOT", "~", ENTEROS);

    /**
     * Cero si la posicion es cero, y todos unos si no. Es la forma de convertir un valor en una
     * mascara de bits que despues sirve para elegir sin ramificar.
     */
    public static final Unary ZOMO = unaria("ZOMO", "a==0?0:-1", ENTEROS);

    /**
     * El valor absoluto. Con enteros tiene el mismo agujero que {@code Math.abs}: el minimo del
     * tipo no tiene positivo y se devuelve a si mismo.
     */
    public static final Unary ABS = unaria("ABS", "abs", TODOS);

    /**
     * El opuesto.
     */
    public static final Unary NEG = unaria("NEG", "-a", TODOS);

    /**
     * Cuantos bits en uno tiene la posicion.
     */
    public static final Unary BIT_COUNT = unaria("BIT_COUNT", "bitCount", ENTEROS);

    /**
     * Cuantos ceros hay antes del primer uno, contando desde el bit menos significativo.
     */
    public static final Unary TRAILING_ZEROS_COUNT =
            unaria("TRAILING_ZEROS_COUNT", "numberOfTrailingZeros", ENTEROS);

    /**
     * Cuantos ceros hay antes del primer uno, contando desde el bit mas significativo.
     */
    public static final Unary LEADING_ZEROS_COUNT =
            unaria("LEADING_ZEROS_COUNT", "numberOfLeadingZeros", ENTEROS);

    /**
     * Da vuelta el orden de los bits.
     */
    public static final Unary REVERSE = unaria("REVERSE", "reverse", ENTEROS);

    /**
     * Da vuelta el orden de los bytes; es el cambio de extremo.
     */
    public static final Unary REVERSE_BYTES = unaria("REVERSE_BYTES", "reverseBytes", ENTEROS);

    /**
     * El seno.
     */
    public static final Unary SIN = unaria("SIN", "sin", FLOTANTES);

    /**
     * El coseno.
     */
    public static final Unary COS = unaria("COS", "cos", FLOTANTES);

    /**
     * La tangente.
     */
    public static final Unary TAN = unaria("TAN", "tan", FLOTANTES);

    /**
     * El arco seno.
     */
    public static final Unary ASIN = unaria("ASIN", "asin", FLOTANTES);

    /**
     * El arco coseno.
     */
    public static final Unary ACOS = unaria("ACOS", "acos", FLOTANTES);

    /**
     * El arco tangente.
     */
    public static final Unary ATAN = unaria("ATAN", "atan", FLOTANTES);

    /**
     * La exponencial.
     */
    public static final Unary EXP = unaria("EXP", "exp", FLOTANTES);

    /**
     * El logaritmo natural.
     */
    public static final Unary LOG = unaria("LOG", "log", FLOTANTES);

    /**
     * El logaritmo en base diez.
     */
    public static final Unary LOG10 = unaria("LOG10", "log10", FLOTANTES);

    /**
     * La raiz cuadrada.
     */
    public static final Unary SQRT = unaria("SQRT", "sqrt", FLOTANTES);

    /**
     * La raiz cubica.
     */
    public static final Unary CBRT = unaria("CBRT", "cbrt", FLOTANTES);

    /**
     * El seno hiperbolico.
     */
    public static final Unary SINH = unaria("SINH", "sinh", FLOTANTES);

    /**
     * El coseno hiperbolico.
     */
    public static final Unary COSH = unaria("COSH", "cosh", FLOTANTES);

    /**
     * La tangente hiperbolica.
     */
    public static final Unary TANH = unaria("TANH", "tanh", FLOTANTES);

    /**
     * {@code exp(a)-1}, calculado de forma que no pierda precision cuando el argumento es chico.
     */
    public static final Unary EXPM1 = unaria("EXPM1", "expm1", FLOTANTES);

    /**
     * {@code log(1+a)}, calculado de forma que no pierda precision cuando el argumento es chico.
     */
    public static final Unary LOG1P = unaria("LOG1P", "log1p", FLOTANTES);

    /**
     * La suma.
     */
    public static final Associative ADD = asociativa("ADD", "+", TODOS);

    /**
     * La resta.
     */
    public static final Binary SUB = binaria("SUB", "-", TODOS);

    /**
     * El producto.
     */
    public static final Associative MUL = asociativa("MUL", "*", TODOS);

    /**
     * El cociente.
     */
    public static final Binary DIV = binaria("DIV", "/", TODOS);

    /**
     * El menor de los dos.
     */
    public static final Associative MIN = asociativa("MIN", "min", TODOS);

    /**
     * El mayor de los dos.
     */
    public static final Associative MAX = asociativa("MAX", "max", TODOS);

    /**
     * El primero de los dos que no sea cero. Sirve para juntar resultados parciales donde el cero
     * significa "esta posicion no aporto nada".
     */
    public static final Associative FIRST_NONZERO = asociativa("FIRST_NONZERO", "a!=0?a:b", TODOS);

    /**
     * La conjuncion bit a bit.
     */
    public static final Associative AND = asociativa("AND", "&", ENTEROS);

    /**
     * La conjuncion con el segundo invertido.
     */
    public static final Binary AND_NOT = binaria("AND_NOT", "&~", ENTEROS);

    /**
     * La disyuncion bit a bit.
     */
    public static final Associative OR = asociativa("OR", "|", ENTEROS);

    /**
     * La disyuncion exclusiva bit a bit.
     */
    public static final Associative XOR = asociativa("XOR", "^", ENTEROS);

    /**
     * La suma con signo que satura en vez de dar la vuelta; ver {@link VectorMath}.
     */
    public static final Binary SADD = binaria("SADD", "+", ENTEROS);

    /**
     * La suma sin signo que satura arriba.
     */
    public static final Binary SUADD = binaria("SUADD", "+", ENTEROS);

    /**
     * La resta con signo que satura en los extremos.
     */
    public static final Binary SSUB = binaria("SSUB", "-", ENTEROS);

    /**
     * La resta sin signo que satura en cero.
     */
    public static final Binary SUSUB = binaria("SUSUB", "-", ENTEROS);

    /**
     * El menor de los dos, comparados sin signo.
     */
    public static final Associative UMIN = asociativa("UMIN", "umin", ENTEROS);

    /**
     * El mayor de los dos, comparados sin signo.
     */
    public static final Associative UMAX = asociativa("UMAX", "umax", ENTEROS);

    /**
     * Corrimiento a la izquierda.
     */
    public static final Binary LSHL = binaria("LSHL", "<<", TODOS);

    /**
     * Corrimiento a la derecha que conserva el signo.
     */
    public static final Binary ASHR = binaria("ASHR", ">>", TODOS);

    /**
     * Corrimiento a la derecha que mete ceros.
     */
    public static final Binary LSHR = binaria("LSHR", ">>>", TODOS);

    /**
     * Rotacion a la izquierda: lo que sale por un extremo entra por el otro.
     */
    public static final Binary ROL = binaria("ROL", "rotateLeft", TODOS);

    /**
     * Rotacion a la derecha.
     */
    public static final Binary ROR = binaria("ROR", "rotateRight", TODOS);

    /**
     * Junta los bits del primero que la mascara del segundo selecciona, y los deja pegados en la
     * parte baja.
     */
    public static final Binary COMPRESS_BITS = binaria("COMPRESS_BITS", "compressBits", ENTEROS);

    /**
     * La inversa de {@link #COMPRESS_BITS}: reparte los bits bajos del primero en las posiciones
     * que la mascara del segundo marca.
     */
    public static final Binary EXPAND_BITS = binaria("EXPAND_BITS", "expandBits", ENTEROS);

    /**
     * El angulo del punto, con el cuadrante bien resuelto por los signos de los dos argumentos.
     */
    public static final Binary ATAN2 = binaria("ATAN2", "atan2", FLOTANTES);

    /**
     * La potencia.
     */
    public static final Binary POW = binaria("POW", "pow", FLOTANTES);

    /**
     * La hipotenusa, sin desbordar en los pasos intermedios como haria {@code sqrt(a*a+b*b)}.
     */
    public static final Binary HYPOT = binaria("HYPOT", "hypot", FLOTANTES);

    /**
     * Elige bit a bit entre los dos primeros segun el tercero: donde el tercero tiene un uno queda
     * el bit del segundo, y donde tiene cero queda el del primero.
     */
    public static final Ternary BITWISE_BLEND = ternaria("BITWISE_BLEND", "a^((a^b)&c)", ENTEROS);

    /**
     * Multiplica y suma con un solo redondeo al final, no dos; ver {@code Math.fma}.
     */
    public static final Ternary FMA = ternaria("FMA", "fma", FLOTANTES);

    /**
     * Si los bits de la posicion son todos cero. Con {@code double} eso distingue el cero positivo
     * del negativo, cosa que {@code == 0} no hace.
     */
    public static final Test IS_DEFAULT = prueba("IS_DEFAULT", "bits(a)==0", TODOS);

    /**
     * Si el bit de signo esta prendido. Tambien mira los bits, asi que el cero negativo da cierto.
     */
    public static final Test IS_NEGATIVE = prueba("IS_NEGATIVE", "bits(a)<0", TODOS);

    /**
     * Si no es infinito ni NaN.
     */
    public static final Test IS_FINITE = prueba("IS_FINITE", "isFinite", FLOTANTES);

    /**
     * Si no es un numero.
     */
    public static final Test IS_NAN = prueba("IS_NAN", "isNaN", FLOTANTES);

    /**
     * Si es infinito.
     */
    public static final Test IS_INFINITE = prueba("IS_INFINITE", "isInfinite", FLOTANTES);

    /**
     * Igual.
     */
    public static final Comparison EQ = comparacion("EQ", "==", TODOS);

    /**
     * Distinto.
     */
    public static final Comparison NE = comparacion("NE", "!=", TODOS);

    /**
     * Menor.
     */
    public static final Comparison LT = comparacion("LT", "<", TODOS);

    /**
     * Menor o igual.
     */
    public static final Comparison LE = comparacion("LE", "<=", TODOS);

    /**
     * Mayor.
     */
    public static final Comparison GT = comparacion("GT", ">", TODOS);

    /**
     * Mayor o igual.
     */
    public static final Comparison GE = comparacion("GE", ">=", TODOS);

    /**
     * Menor, comparando sin signo.
     */
    public static final Comparison ULT = comparacion("ULT", "<", ENTEROS);

    /**
     * Menor o igual, comparando sin signo.
     */
    public static final Comparison ULE = comparacion("ULE", "<=", ENTEROS);

    /**
     * Mayor, comparando sin signo.
     */
    public static final Comparison UGT = comparacion("UGT", ">", ENTEROS);

    /**
     * Mayor o igual, comparando sin signo.
     */
    public static final Comparison UGE = comparacion("UGE", ">=", ENTEROS);

    /**
     * Convierte {@code byte} a {@code double} conservando el valor.
     */
    public static final Conversion<Byte, Double> B2D = cast(byte.class, double.class);

    /**
     * Convierte {@code byte} a {@code float} conservando el valor.
     */
    public static final Conversion<Byte, Float> B2F = cast(byte.class, float.class);

    /**
     * Convierte {@code byte} a {@code int} conservando el valor.
     */
    public static final Conversion<Byte, Integer> B2I = cast(byte.class, int.class);

    /**
     * Convierte {@code byte} a {@code long} conservando el valor.
     */
    public static final Conversion<Byte, Long> B2L = cast(byte.class, long.class);

    /**
     * Convierte {@code byte} a {@code short} conservando el valor.
     */
    public static final Conversion<Byte, Short> B2S = cast(byte.class, short.class);

    /**
     * Convierte {@code double} a {@code byte} conservando el valor.
     */
    public static final Conversion<Double, Byte> D2B = cast(double.class, byte.class);

    /**
     * Convierte {@code double} a {@code float} conservando el valor.
     */
    public static final Conversion<Double, Float> D2F = cast(double.class, float.class);

    /**
     * Convierte {@code double} a {@code int} conservando el valor.
     */
    public static final Conversion<Double, Integer> D2I = cast(double.class, int.class);

    /**
     * Convierte {@code double} a {@code long} conservando el valor.
     */
    public static final Conversion<Double, Long> D2L = cast(double.class, long.class);

    /**
     * Convierte {@code double} a {@code short} conservando el valor.
     */
    public static final Conversion<Double, Short> D2S = cast(double.class, short.class);

    /**
     * Convierte {@code float} a {@code byte} conservando el valor.
     */
    public static final Conversion<Float, Byte> F2B = cast(float.class, byte.class);

    /**
     * Convierte {@code float} a {@code double} conservando el valor.
     */
    public static final Conversion<Float, Double> F2D = cast(float.class, double.class);

    /**
     * Convierte {@code float} a {@code int} conservando el valor.
     */
    public static final Conversion<Float, Integer> F2I = cast(float.class, int.class);

    /**
     * Convierte {@code float} a {@code long} conservando el valor.
     */
    public static final Conversion<Float, Long> F2L = cast(float.class, long.class);

    /**
     * Convierte {@code float} a {@code short} conservando el valor.
     */
    public static final Conversion<Float, Short> F2S = cast(float.class, short.class);

    /**
     * Convierte {@code int} a {@code byte} conservando el valor.
     */
    public static final Conversion<Integer, Byte> I2B = cast(int.class, byte.class);

    /**
     * Convierte {@code int} a {@code double} conservando el valor.
     */
    public static final Conversion<Integer, Double> I2D = cast(int.class, double.class);

    /**
     * Convierte {@code int} a {@code float} conservando el valor.
     */
    public static final Conversion<Integer, Float> I2F = cast(int.class, float.class);

    /**
     * Convierte {@code int} a {@code long} conservando el valor.
     */
    public static final Conversion<Integer, Long> I2L = cast(int.class, long.class);

    /**
     * Convierte {@code int} a {@code short} conservando el valor.
     */
    public static final Conversion<Integer, Short> I2S = cast(int.class, short.class);

    /**
     * Convierte {@code long} a {@code byte} conservando el valor.
     */
    public static final Conversion<Long, Byte> L2B = cast(long.class, byte.class);

    /**
     * Convierte {@code long} a {@code double} conservando el valor.
     */
    public static final Conversion<Long, Double> L2D = cast(long.class, double.class);

    /**
     * Convierte {@code long} a {@code float} conservando el valor.
     */
    public static final Conversion<Long, Float> L2F = cast(long.class, float.class);

    /**
     * Convierte {@code long} a {@code int} conservando el valor.
     */
    public static final Conversion<Long, Integer> L2I = cast(long.class, int.class);

    /**
     * Convierte {@code long} a {@code short} conservando el valor.
     */
    public static final Conversion<Long, Short> L2S = cast(long.class, short.class);

    /**
     * Convierte {@code short} a {@code byte} conservando el valor.
     */
    public static final Conversion<Short, Byte> S2B = cast(short.class, byte.class);

    /**
     * Convierte {@code short} a {@code double} conservando el valor.
     */
    public static final Conversion<Short, Double> S2D = cast(short.class, double.class);

    /**
     * Convierte {@code short} a {@code float} conservando el valor.
     */
    public static final Conversion<Short, Float> S2F = cast(short.class, float.class);

    /**
     * Convierte {@code short} a {@code int} conservando el valor.
     */
    public static final Conversion<Short, Integer> S2I = cast(short.class, int.class);

    /**
     * Convierte {@code short} a {@code long} conservando el valor.
     */
    public static final Conversion<Short, Long> S2L = cast(short.class, long.class);

    /**
     * Vuelve a leer los bits de un {@code double} como si fueran un {@code long}.
     */
    public static final Conversion<Double, Long> REINTERPRET_D2L =
            reinterpret(double.class, long.class);

    /**
     * Vuelve a leer los bits de un {@code float} como si fueran un {@code int}.
     */
    public static final Conversion<Float, Integer> REINTERPRET_F2I =
            reinterpret(float.class, int.class);

    /**
     * Vuelve a leer los bits de un {@code int} como si fueran un {@code float}.
     */
    public static final Conversion<Integer, Float> REINTERPRET_I2F =
            reinterpret(int.class, float.class);

    /**
     * Vuelve a leer los bits de un {@code long} como si fueran un {@code double}.
     */
    public static final Conversion<Long, Double> REINTERPRET_L2D =
            reinterpret(long.class, double.class);

    /**
     * Convierte {@code byte} a {@code int} conservando los bits y rellenando con ceros; el valor
     * cambia si el original era negativo.
     */
    public static final Conversion<Byte, Integer> ZERO_EXTEND_B2I =
            reinterpret(byte.class, int.class);

    /**
     * Convierte {@code byte} a {@code long} conservando los bits y rellenando con ceros; el valor
     * cambia si el original era negativo.
     */
    public static final Conversion<Byte, Long> ZERO_EXTEND_B2L =
            reinterpret(byte.class, long.class);

    /**
     * Convierte {@code byte} a {@code short} conservando los bits y rellenando con ceros; el valor
     * cambia si el original era negativo.
     */
    public static final Conversion<Byte, Short> ZERO_EXTEND_B2S =
            reinterpret(byte.class, short.class);

    /**
     * Convierte {@code int} a {@code long} conservando los bits y rellenando con ceros; el valor
     * cambia si el original era negativo.
     */
    public static final Conversion<Integer, Long> ZERO_EXTEND_I2L =
            reinterpret(int.class, long.class);

    /**
     * Convierte {@code short} a {@code int} conservando los bits y rellenando con ceros; el valor
     * cambia si el original era negativo.
     */
    public static final Conversion<Short, Integer> ZERO_EXTEND_S2I =
            reinterpret(short.class, int.class);

    /**
     * Convierte {@code short} a {@code long} conservando los bits y rellenando con ceros; el valor
     * cambia si el original era negativo.
     */
    public static final Conversion<Short, Long> ZERO_EXTEND_S2L =
            reinterpret(short.class, long.class);


    // ------------------------------------------------------------------
    // La implementacion. Nada de aca es API: son los objetos que respaldan las
    // constantes de arriba. Van adentro y no en un archivo aparte porque afuera
    // habria un ciclo -- la clase de afuera necesita las fabricas y las fabricas
    // necesitan las interfaces de adentro.
    // ------------------------------------------------------------------


        /** La posicion de ese tipo en {@link #Tabla.TIPOS}, o -1 si no es un tipo de posicion. */
        static int indice(final Class<?> t) {
            for (int i = 0; i < Tabla.TIPOS.length; i++) {
                if (Tabla.TIPOS[i] == t) {
                    return i;
                }
            }
            return -1;
        }

        static int indiceExigido(final Class<?> t) {
            final int i = indice(t);
            if (i < 0) {
                throw new UnsupportedOperationException("Bad vector element type: " + t
                        + " (should be a primitive type such as byte.class with a known bit-size)");
            }
            return i;
        }

        /** Lo comun a todos: los datos y las respuestas que salen de ellos. */
        abstract static class Base implements Operator {

            private final String nombre;
            private final String simbolo;
            private final int aridad;
            private final boolean booleano;
            private final boolean asociativo;
            private final int mascara;
            private final Class<?> rango;

            Base(final String nombre, final String simbolo, final int aridad, final boolean booleano,
                    final boolean asociativo, final int mascara, final Class<?> rango) {
                this.nombre = nombre;
                this.simbolo = simbolo;
                this.aridad = aridad;
                this.booleano = booleano;
                this.asociativo = asociativo;
                this.mascara = mascara;
                this.rango = rango;
            }

            @Override
            public String name() {
                return nombre;
            }

            @Override
            public String operatorName() {
                return simbolo;
            }

            @Override
            public int arity() {
                return aridad;
            }

            @Override
            public boolean isBoolean() {
                return booleano;
            }

            @Override
            public boolean isAssociative() {
                return asociativo;
            }

            @Override
            public Class<?> rangeType() {
                return rango;
            }

            @Override
            public boolean compatibleWith(final Class<?> elementType) {
                // Un tipo que no es de posicion no da `false`, da error: preguntar si `ADD` sirve para
                // `String` no es una pregunta con respuesta, es un error de quien pregunta.
                return (mascara & 1 << indiceExigido(elementType)) != 0;
            }

            @Override
            public String toString() {
                return nombre;
            }
        }

        static final class Un extends Base implements Unary {
            Un(final String n, final String s, final int m) {
                super(n, s, 1, false, false, m, Object.class);
            }
        }

        static final class Bin extends Base implements Binary {
            Bin(final String n, final String s, final int m) {
                super(n, s, 2, false, false, m, Object.class);
            }
        }

        static final class Asoc extends Base implements Associative {
            Asoc(final String n, final String s, final int m) {
                super(n, s, 2, false, true, m, Object.class);
            }
        }

        static final class Ter extends Base implements Ternary {
            Ter(final String n, final String s, final int m) {
                super(n, s, 3, false, false, m, Object.class);
            }
        }

        static final class Cmp extends Base implements Comparison {
            Cmp(final String n, final String s, final int m) {
                super(n, s, 2, true, false, m, boolean.class);
            }
        }

        static final class Prueba extends Base implements Test {
            Prueba(final String n, final String s, final int m) {
                super(n, s, 1, true, false, m, boolean.class);
            }
        }

        /**
         * Una conversion, que ademas de operador sabe de que tipo a que tipo va.
         *
         * <p>Los parametros de tipo son los envueltos --{@code Conversion<Byte, Double>}-- pero
         * {@link #domainType()} y {@link #rangeType()} devuelven los primitivos, que es lo que dice el
         * JDK. Los dos {@code Class} se guardan sin parametrizar y se convierten al salir: no hay forma
         * de escribir {@code Class<Byte>} apuntando a {@code byte.class} sin ese paso.
         */
        static final class Conv<E, F> extends Base implements Conversion<E, F> {

            final Class<?> dominio;

            Conv(final String n, final String s, final Class<?> dominio, final Class<?> rango) {
                super(n, s, 1, false, false, TODOS, rango);
                this.dominio = dominio;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Class<E> domainType() {
                return (Class<E>) dominio;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Class<F> rangeType() {
                return (Class<F>) super.rangeType();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <E2, F2> Conversion<E2, F2> check(final Class<E2> from, final Class<F2> to) {
                if (from != dominio || to != super.rangeType()) {
                    throw new ClassCastException(name() + ": not " + from.getName() + " -> "
                            + to.getName());
                }
                return (Conversion<E2, F2>) this;
            }
        }

        static Unary unaria(final String n, final String s, final int m) {
            return new Un(n, s, m);
        }

        static Binary binaria(final String n, final String s, final int m) {
            return new Bin(n, s, m);
        }

        static Associative asociativa(final String n, final String s, final int m) {
            return new Asoc(n, s, m);
        }

        static Ternary ternaria(final String n, final String s, final int m) {
            return new Ter(n, s, m);
        }

        static Comparison comparacion(final String n, final String s, final int m) {
            return new Cmp(n, s, m);
        }

        static Test prueba(final String n, final String s, final int m) {
            return new Prueba(n, s, m);
        }

        /**
         * La conversion de valor: el numero se conserva, no los bits.
         *
         * <p>El nombre y el simbolo se arman con la misma regla que el JDK: {@code B2D} y
         * {@code byte-C-double} para tipos distintos, {@code COPY_B2B} y {@code byte-I-byte} cuando son
         * el mismo, porque ahi la conversion no hace nada.
         *
         * @param <E> el tipo de origen, envuelto
         * @param <F> el tipo de destino, envuelto
         * @param dominio el tipo de origen
         * @param rango el tipo de destino
         * @return la conversion
         */
        static <E, F> Conversion<E, F> cast(final Class<?> dominio, final Class<?> rango) {
            final int d = indiceExigido(dominio);
            final int r = indiceExigido(rango);
            if (d == r) {
                return new Conv<E, F>("COPY_" + par(d, r), marca(dominio, "I", rango), dominio, rango);
            }
            return new Conv<E, F>(par(d, r), marca(dominio, "C", rango), dominio, rango);
        }

        /**
         * La conversion de bits: los bits se conservan, no el numero.
         *
         * <p>Ensanchar un entero es el caso especial: los bits que faltan hay que inventarlos, y esta
         * conversion los pone en cero en vez de copiar el signo. Por eso ahi el nombre dice
         * {@code ZERO_EXTEND}: {@code (byte) -1} reinterpretado a {@code int} da 255, no -1.
         *
         * @param <E> el tipo de origen, envuelto
         * @param <F> el tipo de destino, envuelto
         * @param dominio el tipo de origen
         * @param rango el tipo de destino
         * @return la conversion
         */
        static <E, F> Conversion<E, F> reinterpret(final Class<?> dominio, final Class<?> rango) {
            final int d = indiceExigido(dominio);
            final int r = indiceExigido(rango);
            if (d == r) {
                return new Conv<E, F>("COPY_" + par(d, r), marca(dominio, "I", rango), dominio, rango);
            }
            if (d < 4 && r < 4 && r > d) {
                return new Conv<E, F>("ZERO_EXTEND_" + par(d, r), marca(dominio, "Z", rango),
                        dominio, rango);
            }
            return new Conv<E, F>("REINTERPRET_" + par(d, r), marca(dominio, "R", rango),
                    dominio, rango);
        }

        static String par(final int d, final int r) {
            return "" + Tabla.LETRAS.charAt(d) + '2' + Tabla.LETRAS.charAt(r);
        }

        static String marca(final Class<?> dominio, final String clase, final Class<?> rango) {
            return dominio.getName() + "-" + clase + "-" + rango.getName();
        }
}
