package java.beans;

import java.util.IdentityHashMap;
import java.util.Map;

// The engine of bean persistence. It writes nothing: what it does is **rebuild the object in
// parallel**. For each call described to it, it runs the equivalent call on a copy it is building,
// and stores the correspondence "old object -> expression that produces it".
//
// That copy is the key to everything. It is what lets `initialize` ask "does the new one have this
// value already?" and emit nothing when the answer is yes. Without the copy, a bean with twenty
// properties at their default value would come out with twenty useless calls.
//
// Writing is the subclasses' problem: XMLEncoder overrides `writeStatement`/`writeExpression` to
// also note down what happens and print it afterwards.
//
// The map of links is by **identity**, not by equality: two equal but distinct lists are two objects
// of the graph and have to come out twice. With an ordinary HashMap they would be merged and the
// rebuilt graph would have aliasing the original did not have.
public class Encoder {

    // The register of delegates is static, as in the JDK: `setPersistenceDelegate` changes how
    // that type is stored for every encoder, not for this one.
    private static final Map<Class<?>, PersistenceDelegate> registry =
        new java.util.HashMap<Class<?>, PersistenceDelegate>();

    private final Map<Object, Expression> links = new IdentityHashMap<Object, Expression>();

    private ExceptionListener exceptionListener;

    public Encoder() {
    }

    // It writes an object: it looks for whoever knows how to remake it and hands over.
    protected void writeObject(Object o) {
        if (o != this) {
            PersistenceDelegate info = this.getPersistenceDelegate(o == null ? null : o.getClass());
            info.writeObject(o, this);
        }
    }

    public void setExceptionListener(ExceptionListener exceptionListener) {
        this.exceptionListener = exceptionListener;
    }

    // It never returns null: if nobody set one, the default prints the problem and carries on.
    // Carrying on is on purpose -- a graph with a property that cannot be read is stored all the
    // same, without that property, rather than not being stored at all.
    public ExceptionListener getExceptionListener() {
        return this.exceptionListener != null ? this.exceptionListener : Delegates.DEFAULT_LISTENER;
    }

    // Whoever knows how to remake that type. The order is: what was registered by hand, what the
    // type's BeanInfo says, and lastly the built-in rule matching its shape.
    public PersistenceDelegate getPersistenceDelegate(Class<?> type) {
        PersistenceDelegate d = readRegistry(type);
        if (d == null) {
            d = beanInfoDelegate(type);
        }
        if (d == null) {
            d = Delegates.forType(type);
        }
        return d;
    }

    public void setPersistenceDelegate(Class<?> type, PersistenceDelegate delegate) {
        writeRegistry(type, delegate);
    }

    private static synchronized PersistenceDelegate readRegistry(Class<?> type) {
        return type == null ? null : registry.get(type);
    }

    private static synchronized void writeRegistry(Class<?> type, PersistenceDelegate d) {
        if (type != null) {
            if (d == null) {
                registry.remove(type);
            } else {
                registry.put(type, d);
            }
        }
    }

    // A BeanInfo may bring its own delegate in its BeanDescriptor's "persistenceDelegate"
    // attribute. It is how the JDK lets a class say how it is stored without touching the
    // Encoder.
    private static PersistenceDelegate beanInfoDelegate(Class<?> type) {
        PersistenceDelegate d = null;
        if (type != null && !type.isPrimitive() && !type.isArray()) {
            try {
                BeanDescriptor bd = Introspector.getBeanInfo(type).getBeanDescriptor();
                if (bd != null) {
                    Object v = bd.getValue("persistenceDelegate");
                    if (v instanceof PersistenceDelegate) {
                        d = (PersistenceDelegate) v;
                    }
                }
            } catch (Exception e) {
                d = null;
            }
        }
        return d;
    }

    // It takes the object out of the map and returns what it was worth. The delegate's writeObject
    // uses it when it decides the copy already there is no good and a new one has to be created.
    public Object remove(Object oldInstance) {
        Expression exp = this.links.remove(oldInstance);
        return this.valueOf(exp);
    }

    // The old object's new counterpart, or null if it has not been written yet.
    //
    // Strings return themselves: they are immutable, so a string's "copy" is the string. Without
    // this case, every literal of the graph would ask for a construction of its own.
    public Object get(Object oldInstance) {
        Object r;
        if (oldInstance == null || oldInstance == this || oldInstance.getClass() == String.class) {
            r = oldInstance;
        } else {
            r = this.valueOf(this.links.get(oldInstance));
        }
        return r;
    }

    // The object's counterpart, writing it only if it does not have one yet.
    //
    // The "only if" is not an optimization: it is what cuts the recursion. When copying a property,
    // the default delegate asks to write the expression `old.getX()`, whose TARGET is the very
    // object being written. Without the guard, translating that expression writes the target again,
    // which copies its properties again, which ask for `getX()` again: any bean with one property
    // overflows the stack.
    private Object writeObject1(Object oldInstance) {
        Object o = this.get(oldInstance);
        if (o == null) {
            if (oldInstance instanceof Class) {
                // A Class is its own counterpart: classes are not rebuilt, the `forName` on the
                // other side returns this very object. Saying so here is what stops describing
                // `Class.forName("X")` --whose target is `Class.class`-- from forcing
                // `Class.class` to be described, which would be described with another
                // `Class.forName` over the same target. Without this, storing any object overflows
                // the stack.
                o = oldInstance;
            } else {
                this.writeObject(oldInstance);
                o = this.get(oldInstance);
            }
        }
        return o;
    }

    // It translates a call from the old world into the new one: every object appearing as target
    // or as argument is written first and replaced by its counterpart.
    private Statement cloneStatement(Statement oldExp) {
        Object newTarget = this.writeObject1(oldExp.getTarget());
        Object[] oldArgs = oldExp.getArguments();
        Object[] newArgs = new Object[oldArgs.length];
        for (int i = 0; i < oldArgs.length; i++) {
            newArgs[i] = this.writeObject1(oldArgs[i]);
        }
        return Statement.class.equals(oldExp.getClass())
            ? new Statement(newTarget, oldExp.getMethodName(), newArgs)
            : new Expression(newTarget, oldExp.getMethodName(), newArgs);
    }

    // A call with no value: it is run on the copy and discarded.
    public void writeStatement(Statement oldStm) {
        Statement newStm = this.cloneStatement(oldStm);
        if (oldStm.getTarget() != this) {
            try {
                newStm.execute();
            } catch (Exception e) {
                this.getExceptionListener().exceptionThrown(
                    new Exception("Encoder: discarding statement " + newStm, e));
            }
        }
    }

    // A call with a value: it is noted that the old value is produced by this expression, and then
    // the value is written -- which is what triggers the writing of its internal state.
    //
    // The `get(oldValue) != null` above cuts the recursion on the graph's cycles: an object that
    // already has a link is not described again.
    public void writeExpression(Expression oldExp) {
        Object oldValue = this.valueOf(oldExp);
        if (this.get(oldValue) == null) {
            this.links.put(oldValue, (Expression) this.cloneStatement(oldExp));
            this.writeObject(oldValue);
        }
    }

    // Evaluating an expression cannot return "it could not be done": the caller is already using it
    // as a value. The listener is told and it stops there.
    // It forgets the accumulated counterparts. XMLEncoder uses it when finishing a flush: each
    // written document starts from scratch, so an object appearing in two successive flushes is
    // described in full both times instead of coming out as a reference to an id from the previous
    // document.
    void clearLinks() {
        this.links.clear();
    }

    final Object valueOf(Expression exp) {
        Object r = null;
        if (exp != null) {
            try {
                r = exp.getValue();
            } catch (Exception e) {
                this.getExceptionListener().exceptionThrown(e);
                throw new RuntimeException("failed to evaluate: " + exp.toString());
            }
        }
        return r;
    }
}
