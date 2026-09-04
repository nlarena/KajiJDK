package javax.management;

/**
 * La fabrica de consultas: todo el lenguaje de {@code queryNames}/{@code queryMBeans} entra por
 * aca.
 *
 * <p>Las clases que construye son <b>de paquete</b>, y esa es la decision de dise&ntilde;o de la
 * clase entera. Nadie escribe {@code new AndQueryExp(...)}; se escribe
 * {@code Query.and(Query.gt(Query.attr("Carga"), Query.value(80)), ...)}. Asi el arbol de
 * expresiones queda libre de cambiar sin romper a nadie, y las unicas superficies publicas son las
 * dos interfaces --{@link QueryExp} y {@link ValueExp}-- mas las dos clases de valor que el usuario
 * si puede necesitar nombrar.
 *
 * <p>Ojo con las constantes: {@link #GT}, {@link #LT}... y {@link #PLUS}, {@link #MINUS}... son
 * <b>dos numeraciones distintas</b> que arrancan las dos en cero. Un {@code Query.EQ} vale 4 y un
 * {@code Query.DIV} vale 3, y no hay nada en el tipo que impida mezclarlas.
 */
public class Query {

    /** Mayor que: {@value}. */
    public static final int GT = 0;

    /** Menor que: {@value}. */
    public static final int LT = 1;

    /** Mayor o igual: {@value}. */
    public static final int GE = 2;

    /** Menor o igual: {@value}. */
    public static final int LE = 3;

    /** Igual: {@value}. */
    public static final int EQ = 4;

    /** Suma: {@value}. */
    public static final int PLUS = 0;

    /** Resta: {@value}. */
    public static final int MINUS = 1;

    /** Producto: {@value}. */
    public static final int TIMES = 2;

    /** Cociente: {@value}. */
    public static final int DIV = 3;

    /** Publico porque el JDK lo dejo publico; la clase es toda estatica. */
    public Query() {
    }

    /** Las dos a la vez. */
    public static QueryExp and(QueryExp q1, QueryExp q2) {
        return new AndQueryExp(q1, q2);
    }

    /** Alguna de las dos. */
    public static QueryExp or(QueryExp q1, QueryExp q2) {
        return new OrQueryExp(q1, q2);
    }

    /** Mayor que. */
    public static QueryExp gt(ValueExp v1, ValueExp v2) {
        return new BinaryRelQueryExp(GT, v1, v2);
    }

    /** Mayor o igual. */
    public static QueryExp geq(ValueExp v1, ValueExp v2) {
        return new BinaryRelQueryExp(GE, v1, v2);
    }

    /** Menor o igual. */
    public static QueryExp leq(ValueExp v1, ValueExp v2) {
        return new BinaryRelQueryExp(LE, v1, v2);
    }

    /** Menor que. */
    public static QueryExp lt(ValueExp v1, ValueExp v2) {
        return new BinaryRelQueryExp(LT, v1, v2);
    }

    /** Igual. */
    public static QueryExp eq(ValueExp v1, ValueExp v2) {
        return new BinaryRelQueryExp(EQ, v1, v2);
    }

    /** Entre los dos, extremos incluidos. */
    public static QueryExp between(ValueExp v1, ValueExp v2, ValueExp v3) {
        return new BetweenQueryExp(v1, v2, v3);
    }

    /**
     * Coincidencia con patron.
     *
     * <p>Solo acepta un {@link AttributeValueExp} del lado izquierdo, no un `ValueExp` cualquiera:
     * comparar dos constantes con un patron no tendria sentido.
     */
    public static QueryExp match(AttributeValueExp a, StringValueExp s) {
        return new MatchQueryExp(a, s);
    }

    /** El valor de un atributo del MBean. */
    public static AttributeValueExp attr(String name) {
        return new AttributeValueExp(name);
    }

    /** El valor de un atributo, pero solo si el MBean es de la clase dada. */
    public static AttributeValueExp attr(String className, String name) {
        return new QualifiedAttributeValueExp(className, name);
    }

    /** El nombre de la clase del MBean, como si fuera un atributo. */
    public static AttributeValueExp classattr() {
        return new ClassAttributeValueExp();
    }

    /** La negacion. */
    public static QueryExp not(QueryExp queryExp) {
        return new NotQueryExp(queryExp);
    }

    /** Pertenencia a un conjunto explicito. */
    public static QueryExp in(ValueExp val, ValueExp[] valueList) {
        return new InQueryExp(val, valueList);
    }

    /** Constante de cadena. */
    public static StringValueExp value(String val) {
        return new StringValueExp(val);
    }

    /** Constante numerica desde un envoltorio. */
    public static ValueExp value(Number val) {
        return new NumericValueExp(val);
    }

    /** Constante entera. */
    public static ValueExp value(int val) {
        return new NumericValueExp(Integer.valueOf(val));
    }

    /** Constante entera larga. */
    public static ValueExp value(long val) {
        return new NumericValueExp(Long.valueOf(val));
    }

    /** Constante de punto flotante. */
    public static ValueExp value(float val) {
        return new NumericValueExp(Float.valueOf(val));
    }

    /** Constante de punto flotante doble. */
    public static ValueExp value(double val) {
        return new NumericValueExp(Double.valueOf(val));
    }

    /** Constante booleana. */
    public static ValueExp value(boolean val) {
        return new BooleanValueExp(val);
    }

    /** Suma; sobre dos cadenas, concatena. */
    public static ValueExp plus(ValueExp value1, ValueExp value2) {
        return new BinaryOpValueExp(PLUS, value1, value2);
    }

    /** Producto. */
    public static ValueExp times(ValueExp value1, ValueExp value2) {
        return new BinaryOpValueExp(TIMES, value1, value2);
    }

    /** Resta. */
    public static ValueExp minus(ValueExp value1, ValueExp value2) {
        return new BinaryOpValueExp(MINUS, value1, value2);
    }

    /** Cociente. */
    public static ValueExp div(ValueExp value1, ValueExp value2) {
        return new BinaryOpValueExp(DIV, value1, value2);
    }

    /**
     * "Empieza con".
     *
     * <p>Los tres atajos de subcadena <b>escapan</b> el texto antes de pegarle la estrella: un
     * {@code *} adentro del prefijo se busca literal. Es lo que los separa de {@link #match}, donde
     * el patron se toma tal cual.
     */
    public static QueryExp initialSubString(AttributeValueExp a, StringValueExp s) {
        return new MatchQueryExp(a, new StringValueExp(escapar(s.getValue()) + "*"));
    }

    /** "Contiene". */
    public static QueryExp anySubString(AttributeValueExp a, StringValueExp s) {
        return new MatchQueryExp(a, new StringValueExp("*" + escapar(s.getValue()) + "*"));
    }

    /** "Termina con". */
    public static QueryExp finalSubString(AttributeValueExp a, StringValueExp s) {
        return new MatchQueryExp(a, new StringValueExp("*" + escapar(s.getValue())));
    }

    /** El MBean es de esa clase o de una subclase. */
    public static QueryExp isInstanceOf(StringValueExp classNameValue) {
        return new InstanceOfQueryExp(classNameValue);
    }

    /** Antepone una barra a los cuatro caracteres con significado en el patron. */
    private static String escapar(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '*' || c == '?' || c == '[' || c == '\\') {
                b.append('\\');
            }
            b.append(c);
        }
        return b.toString();
    }
}
