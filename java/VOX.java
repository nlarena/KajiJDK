import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorOperators.Conversion;
import jdk.incubator.vector.VectorOperators.Operator;

/**
 * Comprueba {@code VectorOperators} contra el JDK 25.
 *
 * <p>Arma una linea con todo lo que cada operador dice de si mismo y la compara con la que produjo
 * el JDK real, que esta abajo escrita a mano. {@link #run()} devuelve el indice de la primera que no
 * coincide, o -1 si coinciden todas: un entero alcanza porque {@code run-headless} no vacia la
 * consola.
 */
public class VOX {

    static final Class<?>[] TS = {byte.class, short.class, int.class, long.class,
                                  float.class, double.class};

    static final int TOTAL = 184;

    static final String[] ESPERADO = {
        "NOT|~|1|false|false|java.lang.Object|111100|NOT",
        "ZOMO|a==0?0:-1|1|false|false|java.lang.Object|111100|ZOMO",
        "ABS|abs|1|false|false|java.lang.Object|111111|ABS",
        "NEG|-a|1|false|false|java.lang.Object|111111|NEG",
        "BIT_COUNT|bitCount|1|false|false|java.lang.Object|111100|BIT_COUNT",
        "TRAILING_ZEROS_COUNT|numberOfTrailingZeros|1|false|false|java.lang.Object|111100|TRAILING_ZEROS_COUNT",
        "LEADING_ZEROS_COUNT|numberOfLeadingZeros|1|false|false|java.lang.Object|111100|LEADING_ZEROS_COUNT",
        "REVERSE|reverse|1|false|false|java.lang.Object|111100|REVERSE",
        "REVERSE_BYTES|reverseBytes|1|false|false|java.lang.Object|111100|REVERSE_BYTES",
        "SIN|sin|1|false|false|java.lang.Object|000011|SIN",
        "COS|cos|1|false|false|java.lang.Object|000011|COS",
        "TAN|tan|1|false|false|java.lang.Object|000011|TAN",
        "ASIN|asin|1|false|false|java.lang.Object|000011|ASIN",
        "ACOS|acos|1|false|false|java.lang.Object|000011|ACOS",
        "ATAN|atan|1|false|false|java.lang.Object|000011|ATAN",
        "EXP|exp|1|false|false|java.lang.Object|000011|EXP",
        "LOG|log|1|false|false|java.lang.Object|000011|LOG",
        "LOG10|log10|1|false|false|java.lang.Object|000011|LOG10",
        "SQRT|sqrt|1|false|false|java.lang.Object|000011|SQRT",
        "CBRT|cbrt|1|false|false|java.lang.Object|000011|CBRT",
        "SINH|sinh|1|false|false|java.lang.Object|000011|SINH",
        "COSH|cosh|1|false|false|java.lang.Object|000011|COSH",
        "TANH|tanh|1|false|false|java.lang.Object|000011|TANH",
        "EXPM1|expm1|1|false|false|java.lang.Object|000011|EXPM1",
        "LOG1P|log1p|1|false|false|java.lang.Object|000011|LOG1P",
        "ADD|+|2|false|true|java.lang.Object|111111|ADD",
        "SUB|-|2|false|false|java.lang.Object|111111|SUB",
        "MUL|*|2|false|true|java.lang.Object|111111|MUL",
        "DIV|/|2|false|false|java.lang.Object|111111|DIV",
        "MIN|min|2|false|true|java.lang.Object|111111|MIN",
        "MAX|max|2|false|true|java.lang.Object|111111|MAX",
        "FIRST_NONZERO|a!=0?a:b|2|false|true|java.lang.Object|111111|FIRST_NONZERO",
        "AND|&|2|false|true|java.lang.Object|111100|AND",
        "AND_NOT|&~|2|false|false|java.lang.Object|111100|AND_NOT",
        "OR|||2|false|true|java.lang.Object|111100|OR",
        "XOR|^|2|false|true|java.lang.Object|111100|XOR",
        "SADD|+|2|false|false|java.lang.Object|111100|SADD",
        "SUADD|+|2|false|false|java.lang.Object|111100|SUADD",
        "SSUB|-|2|false|false|java.lang.Object|111100|SSUB",
        "SUSUB|-|2|false|false|java.lang.Object|111100|SUSUB",
        "UMIN|umin|2|false|true|java.lang.Object|111100|UMIN",
        "UMAX|umax|2|false|true|java.lang.Object|111100|UMAX",
        "LSHL|<<|2|false|false|java.lang.Object|111111|LSHL",
        "ASHR|>>|2|false|false|java.lang.Object|111111|ASHR",
        "LSHR|>>>|2|false|false|java.lang.Object|111111|LSHR",
        "ROL|rotateLeft|2|false|false|java.lang.Object|111111|ROL",
        "ROR|rotateRight|2|false|false|java.lang.Object|111111|ROR",
        "COMPRESS_BITS|compressBits|2|false|false|java.lang.Object|111100|COMPRESS_BITS",
        "EXPAND_BITS|expandBits|2|false|false|java.lang.Object|111100|EXPAND_BITS",
        "ATAN2|atan2|2|false|false|java.lang.Object|000011|ATAN2",
        "POW|pow|2|false|false|java.lang.Object|000011|POW",
        "HYPOT|hypot|2|false|false|java.lang.Object|000011|HYPOT",
        "BITWISE_BLEND|a^((a^b)&c)|3|false|false|java.lang.Object|111100|BITWISE_BLEND",
        "FMA|fma|3|false|false|java.lang.Object|000011|FMA",
        "IS_DEFAULT|bits(a)==0|1|true|false|boolean|111111|IS_DEFAULT",
        "IS_NEGATIVE|bits(a)<0|1|true|false|boolean|111111|IS_NEGATIVE",
        "IS_FINITE|isFinite|1|true|false|boolean|000011|IS_FINITE",
        "IS_NAN|isNaN|1|true|false|boolean|000011|IS_NAN",
        "IS_INFINITE|isInfinite|1|true|false|boolean|000011|IS_INFINITE",
        "EQ|==|2|true|false|boolean|111111|EQ",
        "NE|!=|2|true|false|boolean|111111|NE",
        "LT|<|2|true|false|boolean|111111|LT",
        "LE|<=|2|true|false|boolean|111111|LE",
        "GT|>|2|true|false|boolean|111111|GT",
        "GE|>=|2|true|false|boolean|111111|GE",
        "ULT|<|2|true|false|boolean|111100|ULT",
        "ULE|<=|2|true|false|boolean|111100|ULE",
        "UGT|>|2|true|false|boolean|111100|UGT",
        "UGE|>=|2|true|false|boolean|111100|UGE",
        "B2D|byte-C-double|1|false|false|double|111111|B2D|byte|double",
        "B2F|byte-C-float|1|false|false|float|111111|B2F|byte|float",
        "B2I|byte-C-int|1|false|false|int|111111|B2I|byte|int",
        "B2L|byte-C-long|1|false|false|long|111111|B2L|byte|long",
        "B2S|byte-C-short|1|false|false|short|111111|B2S|byte|short",
        "D2B|double-C-byte|1|false|false|byte|111111|D2B|double|byte",
        "D2F|double-C-float|1|false|false|float|111111|D2F|double|float",
        "D2I|double-C-int|1|false|false|int|111111|D2I|double|int",
        "D2L|double-C-long|1|false|false|long|111111|D2L|double|long",
        "D2S|double-C-short|1|false|false|short|111111|D2S|double|short",
        "F2B|float-C-byte|1|false|false|byte|111111|F2B|float|byte",
        "F2D|float-C-double|1|false|false|double|111111|F2D|float|double",
        "F2I|float-C-int|1|false|false|int|111111|F2I|float|int",
        "F2L|float-C-long|1|false|false|long|111111|F2L|float|long",
        "F2S|float-C-short|1|false|false|short|111111|F2S|float|short",
        "I2B|int-C-byte|1|false|false|byte|111111|I2B|int|byte",
        "I2D|int-C-double|1|false|false|double|111111|I2D|int|double",
        "I2F|int-C-float|1|false|false|float|111111|I2F|int|float",
        "I2L|int-C-long|1|false|false|long|111111|I2L|int|long",
        "I2S|int-C-short|1|false|false|short|111111|I2S|int|short",
        "L2B|long-C-byte|1|false|false|byte|111111|L2B|long|byte",
        "L2D|long-C-double|1|false|false|double|111111|L2D|long|double",
        "L2F|long-C-float|1|false|false|float|111111|L2F|long|float",
        "L2I|long-C-int|1|false|false|int|111111|L2I|long|int",
        "L2S|long-C-short|1|false|false|short|111111|L2S|long|short",
        "S2B|short-C-byte|1|false|false|byte|111111|S2B|short|byte",
        "S2D|short-C-double|1|false|false|double|111111|S2D|short|double",
        "S2F|short-C-float|1|false|false|float|111111|S2F|short|float",
        "S2I|short-C-int|1|false|false|int|111111|S2I|short|int",
        "S2L|short-C-long|1|false|false|long|111111|S2L|short|long",
        "REINTERPRET_D2L|double-R-long|1|false|false|long|111111|REINTERPRET_D2L|double|long",
        "REINTERPRET_F2I|float-R-int|1|false|false|int|111111|REINTERPRET_F2I|float|int",
        "REINTERPRET_I2F|int-R-float|1|false|false|float|111111|REINTERPRET_I2F|int|float",
        "REINTERPRET_L2D|long-R-double|1|false|false|double|111111|REINTERPRET_L2D|long|double",
        "ZERO_EXTEND_B2I|byte-Z-int|1|false|false|int|111111|ZERO_EXTEND_B2I|byte|int",
        "ZERO_EXTEND_B2L|byte-Z-long|1|false|false|long|111111|ZERO_EXTEND_B2L|byte|long",
        "ZERO_EXTEND_B2S|byte-Z-short|1|false|false|short|111111|ZERO_EXTEND_B2S|byte|short",
        "ZERO_EXTEND_I2L|int-Z-long|1|false|false|long|111111|ZERO_EXTEND_I2L|int|long",
        "ZERO_EXTEND_S2I|short-Z-int|1|false|false|int|111111|ZERO_EXTEND_S2I|short|int",
        "ZERO_EXTEND_S2L|short-Z-long|1|false|false|long|111111|ZERO_EXTEND_S2L|short|long",
        "COPY_B2B|byte-I-byte|1|false|false|byte|111111|COPY_B2B|byte|byte",
        "COPY_B2B|byte-I-byte|1|false|false|byte|111111|COPY_B2B|byte|byte",
        "B2S|byte-C-short|1|false|false|short|111111|B2S|byte|short",
        "ZERO_EXTEND_B2S|byte-Z-short|1|false|false|short|111111|ZERO_EXTEND_B2S|byte|short",
        "B2I|byte-C-int|1|false|false|int|111111|B2I|byte|int",
        "ZERO_EXTEND_B2I|byte-Z-int|1|false|false|int|111111|ZERO_EXTEND_B2I|byte|int",
        "B2L|byte-C-long|1|false|false|long|111111|B2L|byte|long",
        "ZERO_EXTEND_B2L|byte-Z-long|1|false|false|long|111111|ZERO_EXTEND_B2L|byte|long",
        "B2F|byte-C-float|1|false|false|float|111111|B2F|byte|float",
        "REINTERPRET_B2F|byte-R-float|1|false|false|float|111111|REINTERPRET_B2F|byte|float",
        "B2D|byte-C-double|1|false|false|double|111111|B2D|byte|double",
        "REINTERPRET_B2D|byte-R-double|1|false|false|double|111111|REINTERPRET_B2D|byte|double",
        "S2B|short-C-byte|1|false|false|byte|111111|S2B|short|byte",
        "REINTERPRET_S2B|short-R-byte|1|false|false|byte|111111|REINTERPRET_S2B|short|byte",
        "COPY_S2S|short-I-short|1|false|false|short|111111|COPY_S2S|short|short",
        "COPY_S2S|short-I-short|1|false|false|short|111111|COPY_S2S|short|short",
        "S2I|short-C-int|1|false|false|int|111111|S2I|short|int",
        "ZERO_EXTEND_S2I|short-Z-int|1|false|false|int|111111|ZERO_EXTEND_S2I|short|int",
        "S2L|short-C-long|1|false|false|long|111111|S2L|short|long",
        "ZERO_EXTEND_S2L|short-Z-long|1|false|false|long|111111|ZERO_EXTEND_S2L|short|long",
        "S2F|short-C-float|1|false|false|float|111111|S2F|short|float",
        "REINTERPRET_S2F|short-R-float|1|false|false|float|111111|REINTERPRET_S2F|short|float",
        "S2D|short-C-double|1|false|false|double|111111|S2D|short|double",
        "REINTERPRET_S2D|short-R-double|1|false|false|double|111111|REINTERPRET_S2D|short|double",
        "I2B|int-C-byte|1|false|false|byte|111111|I2B|int|byte",
        "REINTERPRET_I2B|int-R-byte|1|false|false|byte|111111|REINTERPRET_I2B|int|byte",
        "I2S|int-C-short|1|false|false|short|111111|I2S|int|short",
        "REINTERPRET_I2S|int-R-short|1|false|false|short|111111|REINTERPRET_I2S|int|short",
        "COPY_I2I|int-I-int|1|false|false|int|111111|COPY_I2I|int|int",
        "COPY_I2I|int-I-int|1|false|false|int|111111|COPY_I2I|int|int",
        "I2L|int-C-long|1|false|false|long|111111|I2L|int|long",
        "ZERO_EXTEND_I2L|int-Z-long|1|false|false|long|111111|ZERO_EXTEND_I2L|int|long",
        "I2F|int-C-float|1|false|false|float|111111|I2F|int|float",
        "REINTERPRET_I2F|int-R-float|1|false|false|float|111111|REINTERPRET_I2F|int|float",
        "I2D|int-C-double|1|false|false|double|111111|I2D|int|double",
        "REINTERPRET_I2D|int-R-double|1|false|false|double|111111|REINTERPRET_I2D|int|double",
        "L2B|long-C-byte|1|false|false|byte|111111|L2B|long|byte",
        "REINTERPRET_L2B|long-R-byte|1|false|false|byte|111111|REINTERPRET_L2B|long|byte",
        "L2S|long-C-short|1|false|false|short|111111|L2S|long|short",
        "REINTERPRET_L2S|long-R-short|1|false|false|short|111111|REINTERPRET_L2S|long|short",
        "L2I|long-C-int|1|false|false|int|111111|L2I|long|int",
        "REINTERPRET_L2I|long-R-int|1|false|false|int|111111|REINTERPRET_L2I|long|int",
        "COPY_L2L|long-I-long|1|false|false|long|111111|COPY_L2L|long|long",
        "COPY_L2L|long-I-long|1|false|false|long|111111|COPY_L2L|long|long",
        "L2F|long-C-float|1|false|false|float|111111|L2F|long|float",
        "REINTERPRET_L2F|long-R-float|1|false|false|float|111111|REINTERPRET_L2F|long|float",
        "L2D|long-C-double|1|false|false|double|111111|L2D|long|double",
        "REINTERPRET_L2D|long-R-double|1|false|false|double|111111|REINTERPRET_L2D|long|double",
        "F2B|float-C-byte|1|false|false|byte|111111|F2B|float|byte",
        "REINTERPRET_F2B|float-R-byte|1|false|false|byte|111111|REINTERPRET_F2B|float|byte",
        "F2S|float-C-short|1|false|false|short|111111|F2S|float|short",
        "REINTERPRET_F2S|float-R-short|1|false|false|short|111111|REINTERPRET_F2S|float|short",
        "F2I|float-C-int|1|false|false|int|111111|F2I|float|int",
        "REINTERPRET_F2I|float-R-int|1|false|false|int|111111|REINTERPRET_F2I|float|int",
        "F2L|float-C-long|1|false|false|long|111111|F2L|float|long",
        "REINTERPRET_F2L|float-R-long|1|false|false|long|111111|REINTERPRET_F2L|float|long",
        "COPY_F2F|float-I-float|1|false|false|float|111111|COPY_F2F|float|float",
        "COPY_F2F|float-I-float|1|false|false|float|111111|COPY_F2F|float|float",
        "F2D|float-C-double|1|false|false|double|111111|F2D|float|double",
        "REINTERPRET_F2D|float-R-double|1|false|false|double|111111|REINTERPRET_F2D|float|double",
        "D2B|double-C-byte|1|false|false|byte|111111|D2B|double|byte",
        "REINTERPRET_D2B|double-R-byte|1|false|false|byte|111111|REINTERPRET_D2B|double|byte",
        "D2S|double-C-short|1|false|false|short|111111|D2S|double|short",
        "REINTERPRET_D2S|double-R-short|1|false|false|short|111111|REINTERPRET_D2S|double|short",
        "D2I|double-C-int|1|false|false|int|111111|D2I|double|int",
        "REINTERPRET_D2I|double-R-int|1|false|false|int|111111|REINTERPRET_D2I|double|int",
        "D2L|double-C-long|1|false|false|long|111111|D2L|double|long",
        "REINTERPRET_D2L|double-R-long|1|false|false|long|111111|REINTERPRET_D2L|double|long",
        "D2F|double-C-float|1|false|false|float|111111|D2F|double|float",
        "REINTERPRET_D2F|double-R-float|1|false|false|float|111111|REINTERPRET_D2F|double|float",
        "COPY_D2D|double-I-double|1|false|false|double|111111|COPY_D2D|double|double",
        "COPY_D2D|double-I-double|1|false|false|double|111111|COPY_D2D|double|double",
        "check-ok|I2L",
        "check-mal|java.lang.ClassCastException|I2L: not int -> int",
        "tipo-malo|java.lang.UnsupportedOperationException|Bad vector element type: class java.lang.String (should be a primitive type such as byte.class with a known bit-size)",
    };

    static String desc(Operator o) {
        StringBuilder b = new StringBuilder();
        b.append(o.name()).append('|').append(o.operatorName()).append('|').append(o.arity());
        b.append('|').append(o.isBoolean()).append('|').append(o.isAssociative());
        b.append('|').append(o.rangeType().getName()).append('|');
        for (int i = 0; i < TS.length; i++) {
            try {
                b.append(o.compatibleWith(TS[i]) ? '1' : '0');
            } catch (Throwable e) {
                b.append('X');
            }
        }
        b.append('|').append(o.toString());
        if (o instanceof Conversion) {
            Conversion<?, ?> c = (Conversion<?, ?>) o;
            b.append('|').append(c.domainType().getName());
            b.append('|').append(c.rangeType().getName());
        }
        return b.toString();
    }

    static String[] actual() {
        String[] a = new String[TOTAL];
        int k = 0;
        a[k++] = desc(VectorOperators.NOT);
        a[k++] = desc(VectorOperators.ZOMO);
        a[k++] = desc(VectorOperators.ABS);
        a[k++] = desc(VectorOperators.NEG);
        a[k++] = desc(VectorOperators.BIT_COUNT);
        a[k++] = desc(VectorOperators.TRAILING_ZEROS_COUNT);
        a[k++] = desc(VectorOperators.LEADING_ZEROS_COUNT);
        a[k++] = desc(VectorOperators.REVERSE);
        a[k++] = desc(VectorOperators.REVERSE_BYTES);
        a[k++] = desc(VectorOperators.SIN);
        a[k++] = desc(VectorOperators.COS);
        a[k++] = desc(VectorOperators.TAN);
        a[k++] = desc(VectorOperators.ASIN);
        a[k++] = desc(VectorOperators.ACOS);
        a[k++] = desc(VectorOperators.ATAN);
        a[k++] = desc(VectorOperators.EXP);
        a[k++] = desc(VectorOperators.LOG);
        a[k++] = desc(VectorOperators.LOG10);
        a[k++] = desc(VectorOperators.SQRT);
        a[k++] = desc(VectorOperators.CBRT);
        a[k++] = desc(VectorOperators.SINH);
        a[k++] = desc(VectorOperators.COSH);
        a[k++] = desc(VectorOperators.TANH);
        a[k++] = desc(VectorOperators.EXPM1);
        a[k++] = desc(VectorOperators.LOG1P);
        a[k++] = desc(VectorOperators.ADD);
        a[k++] = desc(VectorOperators.SUB);
        a[k++] = desc(VectorOperators.MUL);
        a[k++] = desc(VectorOperators.DIV);
        a[k++] = desc(VectorOperators.MIN);
        a[k++] = desc(VectorOperators.MAX);
        a[k++] = desc(VectorOperators.FIRST_NONZERO);
        a[k++] = desc(VectorOperators.AND);
        a[k++] = desc(VectorOperators.AND_NOT);
        a[k++] = desc(VectorOperators.OR);
        a[k++] = desc(VectorOperators.XOR);
        a[k++] = desc(VectorOperators.SADD);
        a[k++] = desc(VectorOperators.SUADD);
        a[k++] = desc(VectorOperators.SSUB);
        a[k++] = desc(VectorOperators.SUSUB);
        a[k++] = desc(VectorOperators.UMIN);
        a[k++] = desc(VectorOperators.UMAX);
        a[k++] = desc(VectorOperators.LSHL);
        a[k++] = desc(VectorOperators.ASHR);
        a[k++] = desc(VectorOperators.LSHR);
        a[k++] = desc(VectorOperators.ROL);
        a[k++] = desc(VectorOperators.ROR);
        a[k++] = desc(VectorOperators.COMPRESS_BITS);
        a[k++] = desc(VectorOperators.EXPAND_BITS);
        a[k++] = desc(VectorOperators.ATAN2);
        a[k++] = desc(VectorOperators.POW);
        a[k++] = desc(VectorOperators.HYPOT);
        a[k++] = desc(VectorOperators.BITWISE_BLEND);
        a[k++] = desc(VectorOperators.FMA);
        a[k++] = desc(VectorOperators.IS_DEFAULT);
        a[k++] = desc(VectorOperators.IS_NEGATIVE);
        a[k++] = desc(VectorOperators.IS_FINITE);
        a[k++] = desc(VectorOperators.IS_NAN);
        a[k++] = desc(VectorOperators.IS_INFINITE);
        a[k++] = desc(VectorOperators.EQ);
        a[k++] = desc(VectorOperators.NE);
        a[k++] = desc(VectorOperators.LT);
        a[k++] = desc(VectorOperators.LE);
        a[k++] = desc(VectorOperators.GT);
        a[k++] = desc(VectorOperators.GE);
        a[k++] = desc(VectorOperators.ULT);
        a[k++] = desc(VectorOperators.ULE);
        a[k++] = desc(VectorOperators.UGT);
        a[k++] = desc(VectorOperators.UGE);
        a[k++] = desc(VectorOperators.B2D);
        a[k++] = desc(VectorOperators.B2F);
        a[k++] = desc(VectorOperators.B2I);
        a[k++] = desc(VectorOperators.B2L);
        a[k++] = desc(VectorOperators.B2S);
        a[k++] = desc(VectorOperators.D2B);
        a[k++] = desc(VectorOperators.D2F);
        a[k++] = desc(VectorOperators.D2I);
        a[k++] = desc(VectorOperators.D2L);
        a[k++] = desc(VectorOperators.D2S);
        a[k++] = desc(VectorOperators.F2B);
        a[k++] = desc(VectorOperators.F2D);
        a[k++] = desc(VectorOperators.F2I);
        a[k++] = desc(VectorOperators.F2L);
        a[k++] = desc(VectorOperators.F2S);
        a[k++] = desc(VectorOperators.I2B);
        a[k++] = desc(VectorOperators.I2D);
        a[k++] = desc(VectorOperators.I2F);
        a[k++] = desc(VectorOperators.I2L);
        a[k++] = desc(VectorOperators.I2S);
        a[k++] = desc(VectorOperators.L2B);
        a[k++] = desc(VectorOperators.L2D);
        a[k++] = desc(VectorOperators.L2F);
        a[k++] = desc(VectorOperators.L2I);
        a[k++] = desc(VectorOperators.L2S);
        a[k++] = desc(VectorOperators.S2B);
        a[k++] = desc(VectorOperators.S2D);
        a[k++] = desc(VectorOperators.S2F);
        a[k++] = desc(VectorOperators.S2I);
        a[k++] = desc(VectorOperators.S2L);
        a[k++] = desc(VectorOperators.REINTERPRET_D2L);
        a[k++] = desc(VectorOperators.REINTERPRET_F2I);
        a[k++] = desc(VectorOperators.REINTERPRET_I2F);
        a[k++] = desc(VectorOperators.REINTERPRET_L2D);
        a[k++] = desc(VectorOperators.ZERO_EXTEND_B2I);
        a[k++] = desc(VectorOperators.ZERO_EXTEND_B2L);
        a[k++] = desc(VectorOperators.ZERO_EXTEND_B2S);
        a[k++] = desc(VectorOperators.ZERO_EXTEND_I2L);
        a[k++] = desc(VectorOperators.ZERO_EXTEND_S2I);
        a[k++] = desc(VectorOperators.ZERO_EXTEND_S2L);
        for (int i = 0; i < TS.length; i++) {
            for (int j = 0; j < TS.length; j++) {
                a[k++] = desc(Conversion.ofCast(TS[i], TS[j]));
                a[k++] = desc(Conversion.ofReinterpret(TS[i], TS[j]));
            }
        }
        a[k++] = "check-ok|" + VectorOperators.I2L.check(int.class, long.class);
        try {
            VectorOperators.I2L.check(int.class, int.class);
            a[k++] = "check-mal|sin error";
        } catch (Throwable e) {
            a[k++] = "check-mal|" + e.getClass().getName() + "|" + e.getMessage();
        }
        try {
            VectorOperators.ADD.compatibleWith(String.class);
            a[k++] = "tipo-malo|sin error";
        } catch (Throwable e) {
            a[k++] = "tipo-malo|" + e.getClass().getName() + "|" + e.getMessage();
        }
        return a;
    }

    /**
     * El indice del primer descriptor que no coincide con el del JDK, o -1 si coinciden todos.
     *
     * @return el indice, o -1
     */
    public static int run() {
        String[] a = actual();
        for (int i = 0; i < TOTAL; i++) {
            if (!a[i].equals(ESPERADO[i])) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        String[] a = actual();
        if (args.length > 0) {
            for (int i = 0; i < a.length; i++) {
                System.out.println(a[i]);
            }
            return;
        }
        int i = run();
        System.out.println(i < 0 ? "sin diferencias" : (i + ": nuestro=" + a[i]
                + "  jdk=" + ESPERADO[i]));
    }
}
