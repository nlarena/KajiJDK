package javax.management;

/**
 * An arithmetic operation between two values.
 *
 * <p>Package-private: it is made with {@link Query#plus} and friends. {@code +} on two strings
 * concatenates, as in Java; on numbers it adds. The other three only work for numbers.
 *
 * <p>{@link #toString()} puts parentheses <b>only where they are needed</b>, comparing precedences:
 * {@code a + b * c} is printed like that and not as {@code (a) + ((b) * (c))}. On the right side
 * the condition is stricter than on the left --{@code >=} instead of {@code >}-- because the
 * operations associate to the left and {@code a - (b - c)} is not {@code a - b - c}.
 */
class BinaryOpValueExp extends QueryEval implements ValueExp {

    private static final long serialVersionUID = 1216286847881456786L;

    /**
     * @serial the operator
     */
    private int op;

    /**
     * @serial the left side
     */
    private ValueExp exp1;

    /**
     * @serial the right side
     */
    private ValueExp exp2;

    public BinaryOpValueExp() {
    }

    public BinaryOpValueExp(int op, ValueExp v1, ValueExp v2) {
        this.op = op;
        exp1 = v1;
        exp2 = v2;
    }

    /** One of {@link Query#PLUS}, {@link Query#MINUS}, {@link Query#TIMES}, {@link Query#DIV}. */
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
     * <p>The dispatch goes with chained {@code if}s and not with a {@code switch}, which would be
     * the natural thing: our javac does not yet accept in a {@code case} label a constant that
     * comes from another file, and {@code Query.PLUS} and friends live in {@code Query}. See
     * finding #461.
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
                throw new BadStringOperationException(opText());
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

    private String parens(ValueExp exp, boolean left) throws BadBinaryOpValueExpException {
        boolean paren;
        if (exp instanceof BinaryOpValueExp) {
            int mio = precedence(op);
            int theirs = precedence(((BinaryOpValueExp) exp).op);
            // On the right side the condition is harder because the operations associate to the
            // left: `a - (b - c)` is not `a - b - c`.
            paren = left ? mio > theirs : mio >= theirs;
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

    private String opText() {
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
