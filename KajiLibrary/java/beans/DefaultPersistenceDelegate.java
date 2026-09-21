package java.beans;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

// The delegate used when nobody said otherwise: it serves any class honouring the bean contract
// --a no-argument constructor and get/set pairs.
//
// Its work is two questions:
//
//   1. How it is constructed. By default, `new Foo()`. If the class is immutable and its state goes
//      through the constructor, the names of the properties feeding it are passed to the
//      constructor: `new DefaultPersistenceDelegate(new String[] { "x", "y" })` produces
//      `new Point(getX(), getY())`.
//   2. What has to be adjusted afterwards. The public fields and the properties are walked, compared
//      against the freshly created object, and **only what differs is emitted**. That is why storing
//      a bean with everything at its default value produces no call at all.
//
// The `mutatesTo` also differs from the superclass's, but only for case 1 above: if the object is
// built from its properties and defines `equals`, two equal instances describe the same value and
// there is no need to construct another. For the ordinary case --a no-argument constructor, a
// mutable object-- the superclass's rule is used; see `mutatesTo`'s comment.
public class DefaultPersistenceDelegate extends PersistenceDelegate {

    private String[] constructor;

    public DefaultPersistenceDelegate() {
        this.constructor = new String[0];
    }

    public DefaultPersistenceDelegate(String[] constructorPropertyNames) {
        this.constructor = constructorPropertyNames == null ? new String[0] : constructorPropertyNames;
    }

    // If the class wrote its own `equals`, it is the one that knows when two instances are worth
    // the same. If it did not write one, `equals` is identity and says nothing useful.
    private static boolean defineEquals(Class<?> type) {
        boolean r = false;
        Method[] ms = type.getDeclaredMethods();
        for (int i = 0; i < ms.length; i++) {
            Class<?>[] ps = ms[i].getParameterTypes();
            if (!r && ms[i].getName().equals("equals") && ps.length == 1 && ps[0] == Object.class) {
                r = true;
            }
        }
        return r;
    }

    // Asking `equals` is only worth doing when the object is constructed from its properties, that
    // is, when this delegate has constructor names. The condition is no detail: if the object has a
    // no-argument constructor, it is mutable, and for a mutable one `equals` answers another
    // question --"are they worth the same NOW?"-- which is not the one being asked.
    //
    // Without the condition, a list with elements is never equal to the freshly created empty list,
    // so `writeObject` decides over and over that it has to be created again, and creates it again,
    // and again: storing any non-empty collection overflows the stack. For a mutable object the
    // right question is the superclass's --is it the same mould?-- and the differences are settled
    // afterwards by `initialize`.
    protected boolean mutatesTo(Object oldInstance, Object newInstance) {
        boolean r;
        if (this.constructor.length != 0 && oldInstance != null
                && defineEquals(oldInstance.getClass())) {
            r = oldInstance.equals(newInstance);
        } else {
            r = super.mutatesTo(oldInstance, newInstance);
        }
        return r;
    }

    protected Expression instantiate(Object oldInstance, Encoder out) {
        int n = this.constructor.length;
        Class<?> type = oldInstance.getClass();
        Object[] args = new Object[n];
        for (int i = 0; i < n; i++) {
            try {
                Method reader = readerOf(type, this.constructor[i]);
                args[i] = reader.invoke(oldInstance);
            } catch (Exception e) {
                out.getExceptionListener().exceptionThrown(e);
            }
        }
        return new Expression(oldInstance, type, "new", args);
    }

    private static Method readerOf(Class<?> type, String propertyName) throws Exception {
        if (propertyName == null) {
            throw new IllegalArgumentException("Property name is null");
        }
        PropertyDescriptor chosen = null;
        PropertyDescriptor[] pds = Introspector.getBeanInfo(type).getPropertyDescriptors();
        for (int i = 0; i < pds.length; i++) {
            if (chosen == null && propertyName.equals(pds[i].getName())) {
                chosen = pds[i];
            }
        }
        if (chosen == null) {
            throw new IllegalStateException("Could not find property by the name " + propertyName);
        }
        Method m = chosen.getReadMethod();
        if (m == null) {
            throw new IllegalStateException("Could not find getter for the property " + propertyName);
        }
        return m;
    }

    protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
        super.initialize(type, oldInstance, newInstance, out);
        // Only when the chain of superclasses reaches the object's real class: initialize is
        // called once per level and the state is copied whole in a single pass, not per level.
        if (oldInstance.getClass() == type) {
            this.copyState(type, oldInstance, newInstance, out);
        }
    }

    private void copyState(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
        this.copyFields(type, oldInstance, newInstance, out);
        BeanInfo info;
        try {
            info = Introspector.getBeanInfo(type);
        } catch (IntrospectionException e) {
            out.getExceptionListener().exceptionThrown(e);
            info = null;
        }
        if (info != null) {
            PropertyDescriptor[] pds = info.getPropertyDescriptors();
            for (int i = 0; i < pds.length; i++) {
                if (!isTransientProperty(pds[i])) {
                    try {
                        this.copyProperty(pds[i], oldInstance, newInstance, out);
                    } catch (Exception e) {
                        out.getExceptionListener().exceptionThrown(e);
                    }
                }
            }
        }
    }

    // A descriptor marked `transient` is left out on purpose: it is how a class says "this is not
    // stored". The mark is put there by the @Transient annotation or by a BeanInfo written by
    // hand.
    static boolean isTransientProperty(FeatureDescriptor d) {
        return Boolean.TRUE.equals(d.getValue("transient"));
    }

    // The public, mutable fields are state too. They are read with an Expression over the Field
    // itself so that the encoder knows how to reproduce the reading, not just its result.
    private void copyFields(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
        Field[] fs = type.getFields();
        for (int i = 0; i < fs.length; i++) {
            Field f = fs[i];
            int mod = f.getModifiers();
            if (!Modifier.isFinal(mod) && !Modifier.isStatic(mod) && !Modifier.isTransient(mod)) {
                try {
                    Expression old = new Expression(f, "get", new Object[] { oldInstance });
                    Expression fresh = new Expression(f, "get", new Object[] { newInstance });
                    Object oldValue = old.getValue();
                    Object newValue = fresh.getValue();
                    out.writeExpression(old);
                    if (!Objects.equals(newValue, out.get(oldValue))) {
                        out.writeStatement(new Statement(f, "set", new Object[] { oldInstance, oldValue }));
                    }
                } catch (Exception e) {
                    out.getExceptionListener().exceptionThrown(e);
                }
            }
        }
    }

    // The heart of the saving: the property is read on both objects and the `set` is only emitted
    // if the new one does not have that value yet.
    //
    // The comparison is against `out.get(oldValue)` and not against `oldValue` alone: what has to be
    // asked is whether the new object already points at the old value's COUNTERPART, not whether it
    // points at the old value itself --which lives in the other graph and is never going to be
    // there.
    private void copyProperty(PropertyDescriptor pd, Object oldInstance, Object newInstance,
                                 Encoder out) throws Exception {
        Method reader = pd.getReadMethod();
        Method writer = pd.getWriteMethod();
        if (reader != null && writer != null) {
            Expression old = new Expression(oldInstance, reader.getName(), new Object[0]);
            Expression fresh = new Expression(newInstance, reader.getName(), new Object[0]);
            Object oldValue = old.getValue();
            Object newValue = fresh.getValue();
            out.writeExpression(old);
            if (!Objects.equals(newValue, out.get(oldValue))) {
                out.writeStatement(new Statement(oldInstance, writer.getName(),
                                                 new Object[] { oldValue }));
            }
        }
    }

    // The shortcut the built-in delegates use to emit "call this on the old object".
    static void emitCall(Object instance, String methodName, Object[] args, Encoder out) {
        out.writeStatement(new Statement(instance, methodName, args));
    }
}
