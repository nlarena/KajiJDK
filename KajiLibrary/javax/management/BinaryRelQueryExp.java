package javax.management;

/**
 * Una comparacion entre dos valores.
 *
 * <p>De paquete: se fabrica con {@link Query#eq}, {@link Query#gt} y compa&ntilde;ia. Los cinco
 * operadores viven en la misma clase con un `int` para distinguirlos, en vez de cinco clases: es
 * como esta en el JDK y lo que hace que {@code getOperator()} tenga sentido.
 *
 * <p>La comparacion se elige por el <b>tipo del lado izquierdo</b>, y esa asimetria importa.
 * Numeros enteros se comparan en `long` --si los dos lo son--, en `double` si alguno es flotante,
 * cadenas por orden lexicografico y booleanos con {@code false &lt; true}.
 */
class BinaryRelQueryExp extends QueryEval implements QueryExp {

    private static final long serialVersionUID = -5690656271650491000L;

    /**
     * @serial el operador
     */
    private int relOp;

    /**
     * @serial el lado izquierdo
     */
    private ValueExp exp1;

    /**
     * @serial el lado derecho
     */
    private ValueExp exp2;

    public BinaryRelQueryExp() {
    }

    public BinaryRelQueryExp(int op, ValueExp v1, ValueExp v2) {
        relOp = op;
        exp1 = v1;
        exp2 = v2;
    }

    /** Uno de {@link Query#GT}, {@link Query#LT}, {@link Query#GE}, {@link Query#LE},
     * {@link Query#EQ}. */
    public int getOperator() {
        return relOp;
    }

    public ValueExp getLeftValue() {
        return exp1;
    }

    public ValueExp getRightValue() {
        return exp2;
    }

    /**
     * <p>El despacho va con `if` encadenados y no con un `switch`, que seria lo natural: nuestro
     * javac todavia no acepta en una etiqueta `case` una constante que viene de otro archivo, y
     * `Query.GT` y compa&ntilde;ia viven en `Query`. Ver el hallazgo #461.
     */
    public boolean apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        Object val1 = exp1.apply(name);
        Object val2 = exp2.apply(name);

        if (val1 instanceof NumericValueExp) {
            NumericValueExp n1 = (NumericValueExp) val1;
            NumericValueExp n2 = (NumericValueExp) val2;
            if (n1.isLong() && n2.isLong()) {
                long a = n1.longValue();
                long b = n2.longValue();
                if (relOp == Query.GT) {
                    return a > b;
                }
                if (relOp == Query.LT) {
                    return a < b;
                }
                if (relOp == Query.GE) {
                    return a >= b;
                }
                if (relOp == Query.LE) {
                    return a <= b;
                }
                return relOp == Query.EQ && a == b;
            }
            // Con los operadores crudos y no con `Double.compare`, para que NaN se comporte como
            // en el JDK: NaN no es igual, ni mayor, ni menor que nada, ni siquiera que si mismo.
            double a = n1.doubleValue();
            double b = n2.doubleValue();
            if (relOp == Query.GT) {
                return a > b;
            }
            if (relOp == Query.LT) {
                return a < b;
            }
            if (relOp == Query.GE) {
                return a >= b;
            }
            if (relOp == Query.LE) {
                return a <= b;
            }
            return relOp == Query.EQ && a == b;
        }

        if (val1 instanceof BooleanValueExp) {
            boolean a = ((BooleanValueExp) val1).getValue().booleanValue();
            boolean b = ((BooleanValueExp) val2).getValue().booleanValue();
            // El orden natural del tipo: false < true.
            if (relOp == Query.GT) {
                return a && !b;
            }
            if (relOp == Query.LT) {
                return !a && b;
            }
            if (relOp == Query.GE) {
                return a || !b;
            }
            if (relOp == Query.LE) {
                return !a || b;
            }
            return relOp == Query.EQ && a == b;
        }

        if (val1 instanceof StringValueExp && val2 instanceof StringValueExp) {
            String a = ((StringValueExp) val1).getValue();
            String b = ((StringValueExp) val2).getValue();
            if (a == null || b == null) {
                return relOp == Query.EQ && a == b;
            }
            int c = a.compareTo(b);
            if (relOp == Query.GT) {
                return c > 0;
            }
            if (relOp == Query.LT) {
                return c < 0;
            }
            if (relOp == Query.GE) {
                return c >= 0;
            }
            if (relOp == Query.LE) {
                return c <= 0;
            }
            return relOp == Query.EQ && c == 0;
        }
        return false;
    }

    public String toString() {
        return "(" + exp1 + ") " + relOpString() + " (" + exp2 + ")";
    }

    private String relOpString() {
        if (relOp == Query.GT) {
            return ">";
        }
        if (relOp == Query.LT) {
            return "<";
        }
        if (relOp == Query.GE) {
            return ">=";
        }
        if (relOp == Query.LE) {
            return "<=";
        }
        return "=";
    }
}
