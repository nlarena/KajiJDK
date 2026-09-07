package java.beans;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

// The built-in persistence delegates. It is not a JDK class: over there the same thing lives in
// `com.sun.beans.metadata`/`MetaData`, which is internal and forms no part of the contract. Here it
// is gathered in one place so that `Encoder.getPersistenceDelegate` is a readable table.
//
// Each one answers a shape of object the default delegate would not know how to remake: an array has
// no no-argument constructor, an enum is not constructed but looked up, a Class is not instantiated,
// and a collection keeps its contents in no public field.
final class Delegates {

    private Delegates() {
    }

    // What the encoder does when something fails and nobody registered a listener: report and carry
    // on. Carrying on is deliberate --a graph with one unreadable property is stored all the same,
    // without that property.
    static final ExceptionListener DEFAULT_LISTENER = new DefaultListener();

    static final PersistenceDelegate NULL = new ForNull();
    static final PersistenceDelegate STRING = new ForString();
    static final PersistenceDelegate PRIMITIVE = new ForPrimitive();
    static final PersistenceDelegate ARRAY = new ForArray();
    static final PersistenceDelegate ENUM = new ForEnum();
    static final PersistenceDelegate CLASS = new ForClass();
    static final PersistenceDelegate FIELD = new ForField();
    static final PersistenceDelegate METHOD = new ForMethod();
    static final PersistenceDelegate COLLECTION = new ForCollection();
    static final PersistenceDelegate MAP = new ForMap();
    static final PersistenceDelegate DEFAULT = new DefaultPersistenceDelegate();

    // The order matters: an enum is also an object with properties, and an array also has
    // `getClass`. It is tried from the most specific to the most general.
    static PersistenceDelegate forType(Class<?> type) {
        PersistenceDelegate d;
        if (type == null) {
            d = NULL;
        } else if (type == String.class) {
            d = STRING;
        } else if (Statement.primitiveOfWrapper(type) != null) {
            d = PRIMITIVE;
        } else if (type.isArray()) {
            d = ARRAY;
        } else if (Enum.class.isAssignableFrom(type)) {
            d = ENUM;
        } else if (type == Class.class) {
            d = CLASS;
        } else if (Field.class.isAssignableFrom(type)) {
            d = FIELD;
        } else if (Method.class.isAssignableFrom(type)) {
            d = METHOD;
        } else if (Collection.class.isAssignableFrom(type)) {
            d = COLLECTION;
        } else if (Map.class.isAssignableFrom(type)) {
            d = MAP;
        } else {
            d = DEFAULT;
        }
        return d;
    }

    private static final class DefaultListener implements ExceptionListener {
        public void exceptionThrown(Exception e) {
            System.err.println(e);
            System.err.println("Continuing ...");
        }
    }

    // null is neither constructed nor initialized: writing it is doing nothing. The XML's `<null/>`
    // is put there by the encoder when printing, not by this delegate.
    private static final class ForNull extends PersistenceDelegate {
        protected Expression instantiate(Object oldInstance, Encoder out) {
            return null;
        }

        public void writeObject(Object oldInstance, Encoder out) {
        }

        protected void initialize(Class<?> type, Object o, Object n, Encoder out) {
        }
    }

    // A string is its own value: `Encoder.get` already returns it as it stands, so there is nothing
    // to remake. Without this delegate it would be introspected as a bean, which does not break
    // anything but walks half of the String class to emit nothing.
    private static final class ForString extends PersistenceDelegate {
        protected Expression instantiate(Object oldInstance, Encoder out) {
            return null;
        }

        public void writeObject(Object oldInstance, Encoder out) {
        }
    }

    // Wrappers: they are remade from their text, `new Integer("7")`. And they are equal by value, so
    // whichever one is already there will do if it is worth the same.
    private static final class ForPrimitive extends PersistenceDelegate {
        protected boolean mutatesTo(Object oldInstance, Object newInstance) {
            return newInstance != null && oldInstance != null
                && oldInstance.getClass() == newInstance.getClass()
                && oldInstance.equals(newInstance);
        }

        protected Expression instantiate(Object oldInstance, Encoder out) {
            return new Expression(oldInstance, oldInstance.getClass(), "new",
                                  new Object[] { oldInstance.toString() });
        }
    }

    // An array is created with `Array.newInstance(type, length)` and filled element by element. The
    // target has to be literally `Array.class`: XMLEncoder recognizes that shape and prints it as
    // `<array class=... length=.../>` instead of as a call.
    private static final class ForArray extends PersistenceDelegate {
        protected boolean mutatesTo(Object oldInstance, Object newInstance) {
            return newInstance != null
                && oldInstance.getClass() == newInstance.getClass()
                && Statement.arrayLength(oldInstance) == Statement.arrayLength(newInstance);
        }

        protected Expression instantiate(Object oldInstance, Encoder out) {
            return new Expression(oldInstance, Array.class, "newInstance",
                new Object[] { oldInstance.getClass().getComponentType(),
                               Integer.valueOf(Statement.arrayLength(oldInstance)) });
        }

        protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
            int n = Statement.arrayLength(oldInstance);
            for (int i = 0; i < n; i++) {
                Object index = Integer.valueOf(i);
                Expression old = new Expression(oldInstance, "get", new Object[] { index });
                Expression fresh = new Expression(newInstance, "get", new Object[] { index });
                try {
                    Object oldValue = old.getValue();
                    Object newValue = fresh.getValue();
                    out.writeExpression(old);
                    if (!Objects.equals(newValue, out.get(oldValue))) {
                        DefaultPersistenceDelegate.emitCall(oldInstance, "set",
                            new Object[] { index, oldValue }, out);
                    }
                } catch (Exception e) {
                    out.getExceptionListener().exceptionThrown(e);
                }
            }
        }
    }

    // An enum constant is not constructed: it is looked up by name. And two constants are the same
    // one only if they are the same object, which is all the guarantee an enum gives.
    private static final class ForEnum extends PersistenceDelegate {
        protected boolean mutatesTo(Object oldInstance, Object newInstance) {
            return oldInstance == newInstance;
        }

        protected Expression instantiate(Object oldInstance, Encoder out) {
            Enum<?> e = (Enum<?>) oldInstance;
            return new Expression(oldInstance, e.getDeclaringClass(), "valueOf",
                                  new Object[] { e.name() });
        }
    }

    // Classes are recovered by name. The primitive ones have no name `forName` would accept: they
    // are taken from their wrapper's `TYPE` field, which is where the JDK keeps them.
    private static final class ForClass extends PersistenceDelegate {
        protected boolean mutatesTo(Object oldInstance, Object newInstance) {
            return oldInstance.equals(newInstance);
        }

        protected Expression instantiate(Object oldInstance, Encoder out) {
            Class<?> c = (Class<?>) oldInstance;
            Expression e = null;
            if (c.isPrimitive()) {
                try {
                    Class<?> wrapper = Statement.wrapperOf(c);
                    Field field = wrapper.getDeclaredField("TYPE");
                    e = new Expression(oldInstance, field, "get", new Object[] { null });
                } catch (Exception ex) {
                    e = null;
                }
            }
            // Mind the recursion this line hides: `Class.forName("X")` is a call whose TARGET is
            // `Class.class`, which is itself a Class. If the encoder had to describe that target, for
            // `Class.class` it would ask for `Class.forName("java.lang.Class")` --whose target is
            // `Class.class` again-- and the graph bites its own tail.
            //
            // The one that cuts the knot is `Encoder.writeObject1`, which treats a Class as its own
            // counterpart and therefore never asks for it to be described. It is true: a class is not
            // reconstructed, `forName` returns the same old object. The JDK cuts elsewhere --it makes
            // `Class.class` come out of `String.class.getClass()`-- and here it cannot, because
            // `Method.invoke` over `Object.getClass` topples this VM (see XMLEncoder's header). With
            // the cut in writeObject1 no special case is needed here.
            if (e == null) {
                e = new Expression(oldInstance, Class.class, "forName", new Object[] { c.getName() });
            }
            return e;
        }
    }

    // A Field turns up in the graph because the default delegate describes the public fields with
    // `field.get(object)`. It is recovered by asking its class for it.
    private static final class ForField extends PersistenceDelegate {
        protected boolean mutatesTo(Object oldInstance, Object newInstance) {
            return oldInstance.equals(newInstance);
        }

        protected Expression instantiate(Object oldInstance, Encoder out) {
            Field f = (Field) oldInstance;
            return new Expression(oldInstance, f.getDeclaringClass(), "getField",
                                  new Object[] { f.getName() });
        }
    }

    private static final class ForMethod extends PersistenceDelegate {
        protected boolean mutatesTo(Object oldInstance, Object newInstance) {
            return oldInstance.equals(newInstance);
        }

        protected Expression instantiate(Object oldInstance, Encoder out) {
            Method m = (Method) oldInstance;
            return new Expression(oldInstance, m.getDeclaringClass(), "getMethod",
                                  new Object[] { m.getName(), m.getParameterTypes() });
        }
    }

    // A collection's contents are in no property: they have to be enumerated. Whatever the new
    // object brings out of the box is emptied and what the old one has is added, in order.
    private static final class ForCollection extends DefaultPersistenceDelegate {
        protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
            Collection<?> old = (Collection<?>) oldInstance;
            Collection<?> fresh = (Collection<?>) newInstance;
            if (fresh != null && fresh.size() != 0) {
                emitCall(oldInstance, "clear", new Object[0], out);
            }
            Iterator<?> it = old.iterator();
            while (it.hasNext()) {
                emitCall(oldInstance, "add", new Object[] { it.next() }, out);
            }
        }
    }

    // A map is walked by key and compared value by value, like a bean's properties: that way a map
    // already carrying the right entries does not have them written again.
    private static final class ForMap extends DefaultPersistenceDelegate {
        protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
            Map<?, ?> old = (Map<?, ?>) oldInstance;
            Map<?, ?> fresh = (Map<?, ?>) newInstance;
            if (fresh != null) {
                Object[] keys = fresh.keySet().toArray();
                for (int i = 0; i < keys.length; i++) {
                    if (!old.containsKey(keys[i])) {
                        emitCall(oldInstance, "remove", new Object[] { keys[i] }, out);
                    }
                }
            }
            Iterator<?> it = old.keySet().iterator();
            while (it.hasNext()) {
                Object key = it.next();
                Expression eOld = new Expression(oldInstance, "get", new Object[] { key });
                Expression eFresh = new Expression(newInstance, "get", new Object[] { key });
                try {
                    Object oldValue = eOld.getValue();
                    Object newValue = eFresh.getValue();
                    out.writeExpression(eOld);
                    if (!Objects.equals(newValue, out.get(oldValue))) {
                        emitCall(oldInstance, "put", new Object[] { key, oldValue }, out);
                    }
                } catch (Exception e) {
                    out.getExceptionListener().exceptionThrown(e);
                }
            }
        }
    }
}
