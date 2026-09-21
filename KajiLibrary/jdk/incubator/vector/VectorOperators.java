package jdk.incubator.vector;

/**
 * The operators passed to a vector to tell it what to do.
 *
 * <h2>Why the operation is an object and not a method</h2>
 *
 * <p>Instead of {@code v.add(w)}, {@code v.mul(w)}, {@code v.min(w)} and fifty others, the API has
 * a single {@code lanewise(op, w)} and {@code op} is one of these objects. The difference is not
 * one of taste: that way an algorithm can be written that receives the operation as a parameter and
 * serves for all of them, which is exactly what is done when an array is reduced or an expression
 * composed.
 *
 * <p>The operator's type is what limits where it fits. {@link Unary} takes one argument, {@link
 * Binary} two, {@link Ternary} three; {@link Comparison} and {@link Test} give a mask instead of a
 * vector, and {@link Associative} is a {@link Binary} that can also be used for reducing, because
 * grouping in pairs in any order gives the same result. That {@code reduceLanes} asks for an {@link
 * Associative} and not a {@link Binary} is what prevents reducing with a subtraction, which would
 * give a different result depending on how the hardware splits the vector.
 *
 * <h2>An operator is a description, not a computation</h2>
 *
 * <p>Each constant here is an object with data: what it is called, with which symbol it is written,
 * how many arguments it takes, whether it is associative, which lane types it serves. It cannot
 * compute anything. The one that computes is the vector, which receives the operator and chooses
 * the machine instruction.
 *
 * <p>That is why this class is implemented <strong>whole and for real</strong> in this library,
 * even though there are no vectors: the 109 operators exist, answer what they should and are
 * checked against JDK 25. {@code VectorOperators.ADD.operatorName()} returns {@code "+"} here just
 * as there, and {@code ADD.compatibleWith(float.class)} answers right.
 *
 * <p>What cannot be done is use them: a vector to pass them to is needed, and creating a vector
 * does need the VM's intrinsics.
 *
 * <h2>The conversions</h2>
 *
 * <p>There are two ways of going from one type to another and the API separates them carefully.
 * {@link #B2D} and its siblings preserve the <strong>value</strong>: it is the good old {@code
 * (double) b}. {@link #REINTERPRET_F2I} and its siblings preserve the <strong>bits</strong>: the
 * same pattern read as something else.
 *
 * <p>{@code ZERO_EXTEND} is the odd case in between. When widening an integer by bits there are
 * places left over that have to be filled, and these fill them with zeros instead of copying the
 * sign: {@code (byte) -1} widened that way gives 255, not -1.
 *
 * @since 16
 */
public abstract class VectorOperators {

    private VectorOperators() {
    }

    /**
     * The data the constants below need in order to be built.
     *
     * <p>They go in a separate class for a hard reason: static fields are initialised in the order
     * in which they are written, and the forty conversions further down call their factory as soon
     * as they are created (thirty call {@code cast} and ten {@code reinterpret}; the note said all
     * forty call {@code cast}). If these arrays came after them they would still be {@code null} at
     * that moment and the whole class would fail to load. A nested class is initialised only when
     * it is touched, so the order within the file stops mattering.
     */
    private static final class Table {

        /** The lane types, in the order {@code typeIndex} numbers them. */
        static final Class<?>[] TYPES = {
            byte.class, short.class, int.class, long.class, float.class, double.class,
        };

        /** The letters the conversion names are built from. */
        static final String LETTERS = "BSILFD";

        private Table() {
        }
    }

    /** Serves all six lane types. */
    static final int ALL = 0x3F;

    /** Only for the four integral types. */
    static final int INTEGRAL = 0x0F;

    /** Only for {@code float} and {@code double}. */
    static final int FLOATING = 0x30;

    /**
     * What every operator can say about itself.
     *
     * @since 16
     */
    public interface Operator {

        /**
         * The name of the constant, such as {@code "ADD"}.
         *
         * @return the name
         */
        String name();

        /**
         * The symbol the operation is written with, such as {@code "+"}.
         *
         * <p>It is not always a symbol: when the operation has none, it is the name of the
         * equivalent method ({@code "sqrt"}) or directly the formula ({@code "a!=0?a:b"}).
         *
         * @return the symbol
         */
        String operatorName();

        /**
         * How many arguments it takes.
         *
         * @return one, two or three
         */
        int arity();

        /**
         * Whether the result is a mask and not a vector.
         *
         * @return true for the comparisons and the tests
         */
        boolean isBoolean();

        /**
         * The type of the result, when it can be told without knowing which vector it is applied
         * to.
         *
         * <p>For the ordinary operations it is {@code Object.class}, which here means "the same
         * type as the input". For the comparisons and the tests it is {@code boolean.class}, and
         * for a {@link Conversion} it is the target type.
         *
         * @return the type of the result
         */
        Class<?> rangeType();

        /**
         * Whether grouping in pairs in any order gives the same result.
         *
         * <p>It is what is needed to reduce a vector, because the hardware splits the work as it
         * sees fit and that order is not under the control of whoever writes the algorithm.
         *
         * @return true if it is associative
         */
        boolean isAssociative();

        /**
         * Whether the operation serves lanes of that type.
         *
         * <p>A type that cannot be a vector lane does not return {@code false}: it is an error.
         * Asking whether {@code ADD} serves {@code String} is not a question with an answer.
         *
         * @param elementType the lane type
         * @return true if it serves
         * @throws UnsupportedOperationException if it is not a lane type
         */
        boolean compatibleWith(Class<?> elementType);
    }

    /**
     * A one-argument operator.
     *
     * @since 16
     */
    public interface Unary extends Operator {
    }

    /**
     * A two-argument operator.
     *
     * @since 16
     */
    public interface Binary extends Operator {
    }

    /**
     * A two-argument operator that also serves for reducing.
     *
     * <p>That it inherits from {@link Binary} and not the other way round is the part that matters:
     * where an {@link Associative} is asked for, just any {@link Binary} does not fit, and that
     * leaves subtraction and division out of {@code reduceLanes}, since they would give a different
     * result depending on how the hardware splits the vector.
     *
     * @since 16
     */
    public interface Associative extends Binary {
    }

    /**
     * A three-argument operator.
     *
     * @since 16
     */
    public interface Ternary extends Operator {
    }

    /**
     * A comparison between two vectors, which gives a mask.
     *
     * @since 16
     */
    public interface Comparison extends Operator {
    }

    /**
     * A question about each lane of a vector, which gives a mask.
     *
     * @since 16
     */
    public interface Test extends Operator {
    }

    /**
     * A conversion from one lane type to another.
     *
     * <p>The type parameters are the boxed types --{@code Conversion<Byte, Double>}-- because a
     * type parameter cannot be primitive. The methods, on the other hand, return the primitives:
     * {@code B2D.domainType()} is {@code byte.class}.
     *
     * @param <E> the source type, boxed
     * @param <F> the target type, boxed
     * @since 16
     */
    public interface Conversion<E, F> extends Operator {

        /**
         * The source type.
         *
         * @return the source type, primitive
         */
        Class<E> domainType();

        /**
         * The target type.
         *
         * @return the target type, primitive
         */
        @Override
        Class<F> rangeType();

        /**
         * Checks that this conversion is exactly the one going from that type to that other.
         *
         * <p>It exists to recover the type parameters after going through an unparameterised
         * variable, which is what happens when the conversion is chosen at run time. If it does not
         * match it fails on the spot, and not later with a wrong type going around.
         *
         * @param <E> the expected source type
         * @param <F> the expected target type
         * @param from the expected source type
         * @param to the expected target type
         * @return this same conversion, with the type parameters in place
         * @throws ClassCastException if it is not that conversion
         */
        <E, F> Conversion<E, F> check(Class<E> from, Class<F> to);

        /**
         * The value conversion between those two types.
         *
         * <p>It is the one Java's {@code (double) b} does. When the two types are the same the
         * conversion does nothing, and it exists all the same: it is called {@code COPY_X2X}.
         *
         * @param <E> the source type, boxed
         * @param <F> the target type, boxed
         * @param from the source type
         * @param to the target type
         * @return the conversion
         * @throws UnsupportedOperationException if either is not a lane type
         */
        static <E, F> Conversion<E, F> ofCast(Class<E> from, Class<F> to) {
            return cast(from, to);
        }

        /**
         * The bit conversion between those two types.
         *
         * <p>The bits are preserved and the value is not. When widening an integer the places left
         * over are filled with zeros, not with the sign.
         *
         * @param <E> the source type, boxed
         * @param <F> the target type, boxed
         * @param from the source type
         * @param to the target type
         * @return the conversion
         * @throws UnsupportedOperationException if either is not a lane type
         */
        static <E, F> Conversion<E, F> ofReinterpret(Class<E> from, Class<F> to) {
            return reinterpret(from, to);
        }
    }

    /**
     * Inverts all the bits.
     */
    public static final Unary NOT = unary("NOT", "~", INTEGRAL);

    /**
     * Zero if the lane is zero, and all ones otherwise. It is the way of turning a value into a bit
     * mask that then serves for choosing without branching.
     */
    public static final Unary ZOMO = unary("ZOMO", "a==0?0:-1", INTEGRAL);

    /**
     * The absolute value. With integers it has the same hole as {@code Math.abs}: the type's
     * minimum has no positive and returns itself.
     */
    public static final Unary ABS = unary("ABS", "abs", ALL);

    /**
     * The negation.
     */
    public static final Unary NEG = unary("NEG", "-a", ALL);

    /**
     * How many one bits the lane has.
     */
    public static final Unary BIT_COUNT = unary("BIT_COUNT", "bitCount", INTEGRAL);

    /**
     * How many zeros there are before the first one, counting from the least significant bit.
     */
    public static final Unary TRAILING_ZEROS_COUNT =
            unary("TRAILING_ZEROS_COUNT", "numberOfTrailingZeros", INTEGRAL);

    /**
     * How many zeros there are before the first one, counting from the most significant bit.
     */
    public static final Unary LEADING_ZEROS_COUNT =
            unary("LEADING_ZEROS_COUNT", "numberOfLeadingZeros", INTEGRAL);

    /**
     * Reverses the order of the bits.
     */
    public static final Unary REVERSE = unary("REVERSE", "reverse", INTEGRAL);

    /**
     * Reverses the order of the bytes; it is the endianness swap.
     */
    public static final Unary REVERSE_BYTES = unary("REVERSE_BYTES", "reverseBytes", INTEGRAL);

    /**
     * The sine.
     */
    public static final Unary SIN = unary("SIN", "sin", FLOATING);

    /**
     * The cosine.
     */
    public static final Unary COS = unary("COS", "cos", FLOATING);

    /**
     * The tangent.
     */
    public static final Unary TAN = unary("TAN", "tan", FLOATING);

    /**
     * The arc sine.
     */
    public static final Unary ASIN = unary("ASIN", "asin", FLOATING);

    /**
     * The arc cosine.
     */
    public static final Unary ACOS = unary("ACOS", "acos", FLOATING);

    /**
     * The arc tangent.
     */
    public static final Unary ATAN = unary("ATAN", "atan", FLOATING);

    /**
     * The exponential.
     */
    public static final Unary EXP = unary("EXP", "exp", FLOATING);

    /**
     * The natural logarithm.
     */
    public static final Unary LOG = unary("LOG", "log", FLOATING);

    /**
     * The base-ten logarithm.
     */
    public static final Unary LOG10 = unary("LOG10", "log10", FLOATING);

    /**
     * The square root.
     */
    public static final Unary SQRT = unary("SQRT", "sqrt", FLOATING);

    /**
     * The cube root.
     */
    public static final Unary CBRT = unary("CBRT", "cbrt", FLOATING);

    /**
     * The hyperbolic sine.
     */
    public static final Unary SINH = unary("SINH", "sinh", FLOATING);

    /**
     * The hyperbolic cosine.
     */
    public static final Unary COSH = unary("COSH", "cosh", FLOATING);

    /**
     * The hyperbolic tangent.
     */
    public static final Unary TANH = unary("TANH", "tanh", FLOATING);

    /**
     * {@code exp(a)-1}, computed so that it does not lose precision when the argument is small.
     */
    public static final Unary EXPM1 = unary("EXPM1", "expm1", FLOATING);

    /**
     * {@code log(1+a)}, computed so that it does not lose precision when the argument is small.
     */
    public static final Unary LOG1P = unary("LOG1P", "log1p", FLOATING);

    /**
     * The sum.
     */
    public static final Associative ADD = associative("ADD", "+", ALL);

    /**
     * The difference.
     */
    public static final Binary SUB = binary("SUB", "-", ALL);

    /**
     * The product.
     */
    public static final Associative MUL = associative("MUL", "*", ALL);

    /**
     * The quotient.
     */
    public static final Binary DIV = binary("DIV", "/", ALL);

    /**
     * The smaller of the two.
     */
    public static final Associative MIN = associative("MIN", "min", ALL);

    /**
     * The larger of the two.
     */
    public static final Associative MAX = associative("MAX", "max", ALL);

    /**
     * The first of the two that is not zero. It serves to gather partial results where zero means
     * "this lane contributed nothing".
     */
    public static final Associative FIRST_NONZERO = associative("FIRST_NONZERO", "a!=0?a:b", ALL);

    /**
     * The bitwise conjunction.
     */
    public static final Associative AND = associative("AND", "&", INTEGRAL);

    /**
     * The conjunction with the second inverted.
     */
    public static final Binary AND_NOT = binary("AND_NOT", "&~", INTEGRAL);

    /**
     * The bitwise disjunction.
     */
    public static final Associative OR = associative("OR", "|", INTEGRAL);

    /**
     * The bitwise exclusive disjunction.
     */
    public static final Associative XOR = associative("XOR", "^", INTEGRAL);

    /**
     * The signed addition that saturates instead of wrapping around; see {@link VectorMath}.
     */
    public static final Binary SADD = binary("SADD", "+", INTEGRAL);

    /**
     * The unsigned addition that saturates at the top.
     */
    public static final Binary SUADD = binary("SUADD", "+", INTEGRAL);

    /**
     * The signed subtraction that saturates at the ends.
     */
    public static final Binary SSUB = binary("SSUB", "-", INTEGRAL);

    /**
     * The unsigned subtraction that saturates at zero.
     */
    public static final Binary SUSUB = binary("SUSUB", "-", INTEGRAL);

    /**
     * The smaller of the two, compared unsigned.
     */
    public static final Associative UMIN = associative("UMIN", "umin", INTEGRAL);

    /**
     * The larger of the two, compared unsigned.
     */
    public static final Associative UMAX = associative("UMAX", "umax", INTEGRAL);

    /**
     * Shift left.
     */
    public static final Binary LSHL = binary("LSHL", "<<", ALL);

    /**
     * Shift right that preserves the sign.
     */
    public static final Binary ASHR = binary("ASHR", ">>", ALL);

    /**
     * Shift right that shifts in zeros.
     */
    public static final Binary LSHR = binary("LSHR", ">>>", ALL);

    /**
     * Rotate left: what goes out at one end comes in at the other.
     */
    public static final Binary ROL = binary("ROL", "rotateLeft", ALL);

    /**
     * Rotate right.
     */
    public static final Binary ROR = binary("ROR", "rotateRight", ALL);

    /**
     * Gathers the bits of the first that the mask of the second selects, and leaves them packed in
     * the low part.
     */
    public static final Binary COMPRESS_BITS = binary("COMPRESS_BITS", "compressBits", INTEGRAL);

    /**
     * The inverse of {@link #COMPRESS_BITS}: spreads the low bits of the first into the positions
     * the mask of the second marks.
     */
    public static final Binary EXPAND_BITS = binary("EXPAND_BITS", "expandBits", INTEGRAL);

    /**
     * The angle of the point, with the quadrant correctly resolved by the signs of the two
     * arguments.
     */
    public static final Binary ATAN2 = binary("ATAN2", "atan2", FLOATING);

    /**
     * The power.
     */
    public static final Binary POW = binary("POW", "pow", FLOATING);

    /**
     * The hypotenuse, without overflowing in the intermediate steps as {@code sqrt(a*a+b*b)} would.
     */
    public static final Binary HYPOT = binary("HYPOT", "hypot", FLOATING);

    /**
     * Chooses bit by bit between the first two according to the third: where the third has a one
     * the second's bit remains, and where it has zero the first's.
     */
    public static final Ternary BITWISE_BLEND = ternary("BITWISE_BLEND", "a^((a^b)&c)", INTEGRAL);

    /**
     * Multiplies and adds with a single rounding at the end, not two; see {@code Math.fma}.
     */
    public static final Ternary FMA = ternary("FMA", "fma", FLOATING);

    /**
     * Whether the lane's bits are all zero. With {@code double} that tells positive zero from
     * negative zero, which {@code == 0} does not.
     */
    public static final Test IS_DEFAULT = test("IS_DEFAULT", "bits(a)==0", ALL);

    /**
     * Whether the sign bit is set. It also looks at the bits, so negative zero gives true.
     */
    public static final Test IS_NEGATIVE = test("IS_NEGATIVE", "bits(a)<0", ALL);

    /**
     * Whether it is neither infinite nor NaN.
     */
    public static final Test IS_FINITE = test("IS_FINITE", "isFinite", FLOATING);

    /**
     * Whether it is not a number.
     */
    public static final Test IS_NAN = test("IS_NAN", "isNaN", FLOATING);

    /**
     * Whether it is infinite.
     */
    public static final Test IS_INFINITE = test("IS_INFINITE", "isInfinite", FLOATING);

    /**
     * Equal.
     */
    public static final Comparison EQ = comparison("EQ", "==", ALL);

    /**
     * Not equal.
     */
    public static final Comparison NE = comparison("NE", "!=", ALL);

    /**
     * Less.
     */
    public static final Comparison LT = comparison("LT", "<", ALL);

    /**
     * Less or equal.
     */
    public static final Comparison LE = comparison("LE", "<=", ALL);

    /**
     * Greater.
     */
    public static final Comparison GT = comparison("GT", ">", ALL);

    /**
     * Greater or equal.
     */
    public static final Comparison GE = comparison("GE", ">=", ALL);

    /**
     * Less, comparing unsigned.
     */
    public static final Comparison ULT = comparison("ULT", "<", INTEGRAL);

    /**
     * Less or equal, comparing unsigned.
     */
    public static final Comparison ULE = comparison("ULE", "<=", INTEGRAL);

    /**
     * Greater, comparing unsigned.
     */
    public static final Comparison UGT = comparison("UGT", ">", INTEGRAL);

    /**
     * Greater or equal, comparing unsigned.
     */
    public static final Comparison UGE = comparison("UGE", ">=", INTEGRAL);

    /**
     * Converts {@code byte} to {@code double} preserving the value.
     */
    public static final Conversion<Byte, Double> B2D = cast(byte.class, double.class);

    /**
     * Converts {@code byte} to {@code float} preserving the value.
     */
    public static final Conversion<Byte, Float> B2F = cast(byte.class, float.class);

    /**
     * Converts {@code byte} to {@code int} preserving the value.
     */
    public static final Conversion<Byte, Integer> B2I = cast(byte.class, int.class);

    /**
     * Converts {@code byte} to {@code long} preserving the value.
     */
    public static final Conversion<Byte, Long> B2L = cast(byte.class, long.class);

    /**
     * Converts {@code byte} to {@code short} preserving the value.
     */
    public static final Conversion<Byte, Short> B2S = cast(byte.class, short.class);

    /**
     * Converts {@code double} to {@code byte} preserving the value.
     */
    public static final Conversion<Double, Byte> D2B = cast(double.class, byte.class);

    /**
     * Converts {@code double} to {@code float} preserving the value.
     */
    public static final Conversion<Double, Float> D2F = cast(double.class, float.class);

    /**
     * Converts {@code double} to {@code int} preserving the value.
     */
    public static final Conversion<Double, Integer> D2I = cast(double.class, int.class);

    /**
     * Converts {@code double} to {@code long} preserving the value.
     */
    public static final Conversion<Double, Long> D2L = cast(double.class, long.class);

    /**
     * Converts {@code double} to {@code short} preserving the value.
     */
    public static final Conversion<Double, Short> D2S = cast(double.class, short.class);

    /**
     * Converts {@code float} to {@code byte} preserving the value.
     */
    public static final Conversion<Float, Byte> F2B = cast(float.class, byte.class);

    /**
     * Converts {@code float} to {@code double} preserving the value.
     */
    public static final Conversion<Float, Double> F2D = cast(float.class, double.class);

    /**
     * Converts {@code float} to {@code int} preserving the value.
     */
    public static final Conversion<Float, Integer> F2I = cast(float.class, int.class);

    /**
     * Converts {@code float} to {@code long} preserving the value.
     */
    public static final Conversion<Float, Long> F2L = cast(float.class, long.class);

    /**
     * Converts {@code float} to {@code short} preserving the value.
     */
    public static final Conversion<Float, Short> F2S = cast(float.class, short.class);

    /**
     * Converts {@code int} to {@code byte} preserving the value.
     */
    public static final Conversion<Integer, Byte> I2B = cast(int.class, byte.class);

    /**
     * Converts {@code int} to {@code double} preserving the value.
     */
    public static final Conversion<Integer, Double> I2D = cast(int.class, double.class);

    /**
     * Converts {@code int} to {@code float} preserving the value.
     */
    public static final Conversion<Integer, Float> I2F = cast(int.class, float.class);

    /**
     * Converts {@code int} to {@code long} preserving the value.
     */
    public static final Conversion<Integer, Long> I2L = cast(int.class, long.class);

    /**
     * Converts {@code int} to {@code short} preserving the value.
     */
    public static final Conversion<Integer, Short> I2S = cast(int.class, short.class);

    /**
     * Converts {@code long} to {@code byte} preserving the value.
     */
    public static final Conversion<Long, Byte> L2B = cast(long.class, byte.class);

    /**
     * Converts {@code long} to {@code double} preserving the value.
     */
    public static final Conversion<Long, Double> L2D = cast(long.class, double.class);

    /**
     * Converts {@code long} to {@code float} preserving the value.
     */
    public static final Conversion<Long, Float> L2F = cast(long.class, float.class);

    /**
     * Converts {@code long} to {@code int} preserving the value.
     */
    public static final Conversion<Long, Integer> L2I = cast(long.class, int.class);

    /**
     * Converts {@code long} to {@code short} preserving the value.
     */
    public static final Conversion<Long, Short> L2S = cast(long.class, short.class);

    /**
     * Converts {@code short} to {@code byte} preserving the value.
     */
    public static final Conversion<Short, Byte> S2B = cast(short.class, byte.class);

    /**
     * Converts {@code short} to {@code double} preserving the value.
     */
    public static final Conversion<Short, Double> S2D = cast(short.class, double.class);

    /**
     * Converts {@code short} to {@code float} preserving the value.
     */
    public static final Conversion<Short, Float> S2F = cast(short.class, float.class);

    /**
     * Converts {@code short} to {@code int} preserving the value.
     */
    public static final Conversion<Short, Integer> S2I = cast(short.class, int.class);

    /**
     * Converts {@code short} to {@code long} preserving the value.
     */
    public static final Conversion<Short, Long> S2L = cast(short.class, long.class);

    /**
     * Reads the bits of a {@code double} again as if they were a {@code long}.
     */
    public static final Conversion<Double, Long> REINTERPRET_D2L =
            reinterpret(double.class, long.class);

    /**
     * Reads the bits of a {@code float} again as if they were a {@code int}.
     */
    public static final Conversion<Float, Integer> REINTERPRET_F2I =
            reinterpret(float.class, int.class);

    /**
     * Reads the bits of a {@code int} again as if they were a {@code float}.
     */
    public static final Conversion<Integer, Float> REINTERPRET_I2F =
            reinterpret(int.class, float.class);

    /**
     * Reads the bits of a {@code long} again as if they were a {@code double}.
     */
    public static final Conversion<Long, Double> REINTERPRET_L2D =
            reinterpret(long.class, double.class);

    /**
     * Converts {@code byte} to {@code int} preserving the bits and filling with zeros; the value
     * changes if the original was negative.
     */
    public static final Conversion<Byte, Integer> ZERO_EXTEND_B2I =
            reinterpret(byte.class, int.class);

    /**
     * Converts {@code byte} to {@code long} preserving the bits and filling with zeros; the value
     * changes if the original was negative.
     */
    public static final Conversion<Byte, Long> ZERO_EXTEND_B2L =
            reinterpret(byte.class, long.class);

    /**
     * Converts {@code byte} to {@code short} preserving the bits and filling with zeros; the value
     * changes if the original was negative.
     */
    public static final Conversion<Byte, Short> ZERO_EXTEND_B2S =
            reinterpret(byte.class, short.class);

    /**
     * Converts {@code int} to {@code long} preserving the bits and filling with zeros; the value
     * changes if the original was negative.
     */
    public static final Conversion<Integer, Long> ZERO_EXTEND_I2L =
            reinterpret(int.class, long.class);

    /**
     * Converts {@code short} to {@code int} preserving the bits and filling with zeros; the value
     * changes if the original was negative.
     */
    public static final Conversion<Short, Integer> ZERO_EXTEND_S2I =
            reinterpret(short.class, int.class);

    /**
     * Converts {@code short} to {@code long} preserving the bits and filling with zeros; the value
     * changes if the original was negative.
     */
    public static final Conversion<Short, Long> ZERO_EXTEND_S2L =
            reinterpret(short.class, long.class);


    // ------------------------------------------------------------------
    // The implementation. Nothing here is API: these are the objects that back the
    // constants above. They go inside and not in a separate file because outside
    // there would be a cycle -- the outer class needs the factories and the factories
    // need the inner interfaces.
    // ------------------------------------------------------------------


        /** The position of that type in {@link Table#TYPES}, or -1 if it is not a lane type. */
        static int typeIndex(final Class<?> t) {
            for (int i = 0; i < Table.TYPES.length; i++) {
                if (Table.TYPES[i] == t) {
                    return i;
                }
            }
            return -1;
        }

        static int requiredTypeIndex(final Class<?> t) {
            final int i = typeIndex(t);
            if (i < 0) {
                throw new UnsupportedOperationException("Bad vector element type: " + t
                        + " (should be a primitive type such as byte.class with a known bit-size)");
            }
            return i;
        }

        /** What they all share: the data and the answers that come from it. */
        abstract static class Base implements Operator {

            private final String name;
            private final String symbol;
            private final int arity;
            private final boolean boolResult;
            private final boolean associative;
            private final int mask;
            private final Class<?> range;

            Base(final String name, final String symbol, final int arity, final boolean boolResult,
                    final boolean associative, final int mask, final Class<?> range) {
                this.name = name;
                this.symbol = symbol;
                this.arity = arity;
                this.boolResult = boolResult;
                this.associative = associative;
                this.mask = mask;
                this.range = range;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public String operatorName() {
                return symbol;
            }

            @Override
            public int arity() {
                return arity;
            }

            @Override
            public boolean isBoolean() {
                return boolResult;
            }

            @Override
            public boolean isAssociative() {
                return associative;
            }

            @Override
            public Class<?> rangeType() {
                return range;
            }

            @Override
            public boolean compatibleWith(final Class<?> elementType) {
                // A type that is not a lane type does not give `false`, it gives an error: asking
                // whether `ADD` serves `String` is not a question with an answer, it is the asker's
                // error.
                return (mask & 1 << requiredTypeIndex(elementType)) != 0;
            }

            @Override
            public String toString() {
                return name;
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

        static final class Assoc extends Base implements Associative {
            Assoc(final String n, final String s, final int m) {
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

        static final class TestOp extends Base implements Test {
            TestOp(final String n, final String s, final int m) {
                super(n, s, 1, true, false, m, boolean.class);
            }
        }

        /**
         * A conversion, which besides being an operator knows from which type to which type it
         * goes.
         *
         * <p>The type parameters are the boxed ones --{@code Conversion<Byte, Double>}-- but {@link
         * #domainType()} and {@link #rangeType()} return the primitives, which is what the JDK
         * says. The two {@code Class}es are kept unparameterised and converted on the way out:
         * there is no way of writing a {@code Class<Byte>} pointing at {@code byte.class} without
         * that step.
         */
        static final class Conv<E, F> extends Base implements Conversion<E, F> {

            final Class<?> domain;

            Conv(final String n, final String s, final Class<?> domain, final Class<?> range) {
                super(n, s, 1, false, false, ALL, range);
                this.domain = domain;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Class<E> domainType() {
                return (Class<E>) domain;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Class<F> rangeType() {
                return (Class<F>) super.rangeType();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <E2, F2> Conversion<E2, F2> check(final Class<E2> from, final Class<F2> to) {
                if (from != domain || to != super.rangeType()) {
                    throw new ClassCastException(name() + ": not " + from.getName() + " -> "
                            + to.getName());
                }
                return (Conversion<E2, F2>) this;
            }
        }

        static Unary unary(final String n, final String s, final int m) {
            return new Un(n, s, m);
        }

        static Binary binary(final String n, final String s, final int m) {
            return new Bin(n, s, m);
        }

        static Associative associative(final String n, final String s, final int m) {
            return new Assoc(n, s, m);
        }

        static Ternary ternary(final String n, final String s, final int m) {
            return new Ter(n, s, m);
        }

        static Comparison comparison(final String n, final String s, final int m) {
            return new Cmp(n, s, m);
        }

        static Test test(final String n, final String s, final int m) {
            return new TestOp(n, s, m);
        }

        /**
         * The value conversion: the number is preserved, not the bits.
         *
         * <p>The name and the symbol are built with the same rule as the JDK's: {@code B2D} and
         * {@code byte-C-double} for different types, {@code COPY_B2B} and {@code byte-I-byte} when
         * they are the same, because there the conversion does nothing.
         *
         * @param <E> the source type, boxed
         * @param <F> the target type, boxed
         * @param domain the source type
         * @param range the target type
         * @return the conversion
         */
        static <E, F> Conversion<E, F> cast(final Class<?> domain, final Class<?> range) {
            final int d = requiredTypeIndex(domain);
            final int r = requiredTypeIndex(range);
            if (d == r) {
                return new Conv<E, F>("COPY_" + pair(d, r), key(domain, "I", range), domain, range);
            }
            return new Conv<E, F>(pair(d, r), key(domain, "C", range), domain, range);
        }

        /**
         * The bit conversion: the bits are preserved, not the number.
         *
         * <p>Widening an integer is the special case: the missing bits have to be invented, and
         * this conversion sets them to zero instead of copying the sign. That is why the name there
         * says {@code ZERO_EXTEND}: {@code (byte) -1} reinterpreted to {@code int} gives 255, not
         * -1.
         *
         * @param <E> the source type, boxed
         * @param <F> the target type, boxed
         * @param domain the source type
         * @param range the target type
         * @return the conversion
         */
        static <E, F> Conversion<E, F> reinterpret(final Class<?> domain, final Class<?> range) {
            final int d = requiredTypeIndex(domain);
            final int r = requiredTypeIndex(range);
            if (d == r) {
                return new Conv<E, F>("COPY_" + pair(d, r), key(domain, "I", range), domain, range);
            }
            if (d < 4 && r < 4 && r > d) {
                return new Conv<E, F>("ZERO_EXTEND_" + pair(d, r), key(domain, "Z", range),
                        domain, range);
            }
            return new Conv<E, F>("REINTERPRET_" + pair(d, r), key(domain, "R", range),
                    domain, range);
        }

        static String pair(final int d, final int r) {
            return "" + Table.LETTERS.charAt(d) + '2' + Table.LETTERS.charAt(r);
        }

        static String key(final Class<?> domain, final String kind, final Class<?> range) {
            return domain.getName() + "-" + kind + "-" + range.getName();
        }
}
