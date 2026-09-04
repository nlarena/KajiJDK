package javax.management;

/**
 * Una cuenta entre dos valores.
 *
 * <p>De paquete: se fabrica con {@link Query#plus} y compa&ntilde;ia. El {@code +} sobre dos cadenas
 * concatena, como en Java; sobre numeros suma. Las otras tres solo valen para numeros.
 *
 * <p>{@link #toString()} pone parentesis <b>solo donde hacen falta</b>, comparando precedencias:
 * {@code a + b * c} se imprime asi y no {@code (a) + ((b) * (c))}. Del lado derecho la condicion es
 * mas estricta que del izquierdo --{@code >=} en vez de {@code >}-- porque las operaciones asocian
 * a izquierda y {@code a - (b - c)} no es {@code a - b - c}.
 */
class BinaryOpValueExp extends QueryEval implements ValueExp {

    private static final long serialVersionUID = 1216286847881456786L;

    /**
     * @serial el operador
     */
    private int op;

    /**
     * @serial el lado izquierdo
     */
    private ValueExp exp1;

    /**
     * @serial el lado derecho
     */
    private ValueExp exp2;

    public BinaryOpValueExp() {
    }

    public BinaryOpValueExp(int op, ValueExp v1, ValueExp v2) {
        this.op = op;
        exp1 = v1;
        exp2 = v2;
    }

    /** Uno de {@link Query#PLUS}, {@link Query#MINUS}, {@link Query#TIMES}, {@link Query#DIV}. */
    public int getOperator() {
        return op;
    }

    public ValueExp getLeftValue() {
        return exp1;
    }

    public ValueExp getRightValue() {
        return exp2;
    }

    /**
     * <p>El despacho va con `if` encadenados y no con un `switch`: nuestro javac todavia no acepta
     * en una etiqueta `case` una constante que viene de otro archivo, y `Query.PLUS` y
     * compa&ntilde;ia viven en `Query`. Ver el hallazgo #461.
     */
    public ValueExp apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        ValueExp val1 = exp1.apply(name);
        ValueExp val2 = exp2.apply(name);

        if (val1 instanceof NumericValueExp && val2 instanceof NumericValueExp) {
            NumericValueExp n1 = (NumericValueExp) val1;
            NumericValueExp n2 = (NumericValueExp) val2;
            if (n1.isLong() && n2.isLong()) {
                long a = n1.longValue();
                long b = n2.longValue();
                if (op == Query.PLUS) {
                    return Query.value(a + b);
                }
                if (op == Query.MINUS) {
                    return Query.value(a - b);
                }
                if (op == Query.TIMES) {
                    return Query.value(a * b);
                }
                if (op == Query.DIV) {
                    return Query.value(a / b);
                }
                throw new BadBinaryOpValueExpException(this);
            }
            double a = n1.doubleValue();
            double b = n2.doubleValue();
            if (op == Query.PLUS) {
                return Query.value(a + b);
            }
            if (op == Query.MINUS) {
                return Query.value(a - b);
            }
            if (op == Query.TIMES) {
                return Query.value(a * b);
            }
            if (op == Query.DIV) {
                return Query.value(a / b);
            }
            throw new BadBinaryOpValueExpException(this);
        }

        if (val1 instanceof StringValueExp && val2 instanceof StringValueExp) {
            if (op != Query.PLUS) {
                throw new BadStringOperationException(opTexto());
            }
            return new StringValueExp(((StringValueExp) val1).getValue()
                    + ((StringValueExp) val2).getValue());
        }
        throw new BadBinaryOpValueExpException(this);
    }

    public String toString() {
        try {
            return parens(exp1, true) + " " + opString() + " " + parens(exp2, false);
        } catch (BadBinaryOpValueExpException e) {
            return "invalid expression";
        }
    }

    private String parens(ValueExp exp, boolean izquierda) throws BadBinaryOpValueExpException {
        boolean paren;
        if (exp instanceof BinaryOpValueExp) {
            int mio = precedence(op);
            int suyo = precedence(((BinaryOpValueExp) exp).op);
            // Del lado derecho la condicion es mas dura porque las operaciones asocian a
            // izquierda: `a - (b - c)` no es `a - b - c`.
            paren = izquierda ? mio > suyo : mio >= suyo;
        } else {
            paren = false;
        }
        return paren ? "(" + exp + ")" : exp.toString();
    }

    private int precedence(int o) throws BadBinaryOpValueExpException {
        if (o == Query.PLUS || o == Query.MINUS) {
            return 0;
        }
        if (o == Query.TIMES || o == Query.DIV) {
            return 1;
        }
        throw new BadBinaryOpValueExpException(this);
    }

    private String opString() throws BadBinaryOpValueExpException {
        if (op == Query.PLUS) {
            return "+";
        }
        if (op == Query.MINUS) {
            return "-";
        }
        if (op == Query.TIMES) {
            return "*";
        }
        if (op == Query.DIV) {
            return "/";
        }
        throw new BadBinaryOpValueExpException(this);
    }

    private String opTexto() {
        if (op == Query.PLUS) {
            return "+";
        }
        if (op == Query.MINUS) {
            return "-";
        }
        if (op == Query.TIMES) {
            return "*";
        }
        if (op == Query.DIV) {
            return "/";
        }
        return "?";
    }

    public void setMBeanServer(MBeanServer s) {
        super.setMBeanServer(s);
    }
}
