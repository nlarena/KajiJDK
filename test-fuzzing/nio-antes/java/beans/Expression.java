package java.beans;

// A Statement that also keeps what the call returned. The difference from Statement is exactly
// that: a Statement is executed for its effect, an Expression for its value.
//
// The value is worked out once only, lazily. The mark for "not worked out yet" cannot be null —null
// is a legitimate result— so a sentinel of its own is used. Without it, an expression returning null
// would be re-evaluated on every getValue().
public class Expression extends Statement {

    // The "not worked out" sentinel. A private, unique object: no method can return it.
    private static final Object NOT_COMPUTED = new Object();

    private Object value = NOT_COMPUTED;

    public Expression(Object target, String methodName, Object[] arguments) {
        super(target, methodName, arguments);
    }

    // With the value already known: nothing is going to be executed.
    public Expression(Object value, Object target, String methodName, Object[] arguments) {
        super(target, methodName, arguments);
        this.value = value;
    }

    // It executes and stores the result, even if it is null.
    public void execute() throws Exception {
        this.value = this.emitCall();
    }

    // The value, executing the call the first time it is asked for.
    public Object getValue() throws Exception {
        if (this.value == NOT_COMPUTED) {
            this.value = this.emitCall();
        }
        return this.value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String toString() {
        String v = this.value == NOT_COMPUTED ? "<unbound>" : String.valueOf(this.value);
        return v + "=" + super.toString();
    }
}
