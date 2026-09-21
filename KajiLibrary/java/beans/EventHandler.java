package java.beans;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

// It turns an event into a call on another object, without writing the listener's class: "when this
// event arrives, take this property off it and pass it to this method of that object".
//
// The three names it carries are the three parts of that sentence:
//   - `listenerMethodName`: which listener method it reacts to (null = all of them).
//   - `eventPropertyName`: what to take off the event. It may be a dotted path --"source.text" takes
//     `getSource()` and then `getText()`. null means "pass it nothing".
//   - `action`: what to call on the target. It may be a dotted path too, and the last segment is the
//     method or the property to write.
//
// The three static `create(...)` are the face that gets used: they wrap this InvocationHandler in a
// proxy implementing the listener interface asked for, so the caller can hand it to an
// `addFooListener` without writing the class. They depend on `java.lang.reflect.Proxy`, which
// **now is** in this tree; an earlier version of this class declared them omitted for want of it and
// that note went stale.
public class EventHandler implements InvocationHandler {

    private Object target;
    private String action;
    private String eventPropertyName;
    private String listenerMethodName;

    public EventHandler(Object target, String action, String eventPropertyName, String listenerMethodName) {
        if (target == null) {
            throw new NullPointerException("target must be non-null");
        }
        if (action == null) {
            throw new NullPointerException("action must be non-null");
        }
        this.target = target;
        this.action = action;
        this.eventPropertyName = eventPropertyName;
        this.listenerMethodName = listenerMethodName;
    }

    public Object getTarget() {
        return this.target;
    }

    public String getAction() {
        return this.action;
    }

    public String getEventPropertyName() {
        return this.eventPropertyName;
    }

    // Which listener method it reacts to. null means all of them.
    public String getListenerMethodName() {
        return this.listenerMethodName;
    }

    // The call arriving from the listener. If it is not the method this handler reacts to, the
    // least the interface expects is answered and nothing is done.
    //
    // Object's three methods go apart and not as "the listener's method": with
    // `listenerMethodName == null` this handler reacts to EVERYTHING, and a `hashCode()` on the
    // proxy --which is what any collection the listener is stored in does-- would end up running the
    // action. They are answered by the proxy's identity, which is what a listener with no state of
    // its own is.
    public Object invoke(Object proxy, Method method, Object[] arguments) {
        Object result = null;
        if (method != null) {
            String name = method.getName();
            if (method.getDeclaringClass() == Object.class) {
                if (name.equals("hashCode")) {
                    result = Integer.valueOf(System.identityHashCode(proxy));
                } else if (name.equals("equals")) {
                    result = Boolean.valueOf(proxy == arguments[0]);
                } else if (name.equals("toString")) {
                    result = proxy.getClass().getName() + '@'
                        + Integer.toHexString(System.identityHashCode(proxy));
                }
            } else if (this.listenerMethodName == null || this.listenerMethodName.equals(name)) {
                result = this.apply(arguments);
            }
        }
        return result;
    }

    private Object apply(Object[] arguments) {
        Object result = null;
        try {
            // What to pass the target: whatever eventPropertyName says about the event, or
            // nothing.
            Object[] args;
            if (this.eventPropertyName == null) {
                args = new Object[0];
            } else {
                Object event = arguments != null && arguments.length > 0 ? arguments[0] : null;
                args = new Object[] { this.followPath(event, this.eventPropertyName) };
            }

            // The action may be a path too: it is walked to the second-to-last segment and the
            // last one is what gets called.
            Object target = this.target;
            String last = this.action;
            int dot = this.action.lastIndexOf('.');
            if (dot >= 0) {
                target = this.followPath(this.target, this.action.substring(0, dot));
                last = this.action.substring(dot + 1);
            }
            result = this.callOn(target, last, args);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    // It walks "a.b.c" applying each segment as a read property.
    private Object followPath(Object base, String path) throws Exception {
        Object current = base;
        int from = 0;
        while (from <= path.length() && current != null) {
            int dot = path.indexOf('.', from);
            String segment = dot < 0 ? path.substring(from) : path.substring(from, dot);
            if (segment.length() > 0) {
                current = this.readProperty(current, segment);
            }
            from = dot < 0 ? path.length() + 1 : dot + 1;
        }
        return current;
    }

    // It reads a property by trying `getX`, `isX` and, lastly, a method called the same.
    private Object readProperty(Object o, String name) throws Exception {
        String cap = PropertyDescriptor.capitalize(name);
        Method m = PropertyDescriptor.findMethod(o.getClass(), "get" + cap, 0);
        if (m == null) {
            m = PropertyDescriptor.findMethod(o.getClass(), "is" + cap, 0);
        }
        if (m == null) {
            m = PropertyDescriptor.findMethod(o.getClass(), name, 0);
        }
        if (m == null) {
            throw new NoSuchMethodException("No property " + name + " on " + o.getClass().getName());
        }
        return m.invoke(o);
    }

    // It calls the target's method, accepting both the plain name and the `setX` form.
    private Object callOn(Object target, String name, Object[] args) throws Exception {
        Method chosen = this.findCompatible(target.getClass(), name, args);
        if (chosen == null) {
            String cap = "set" + PropertyDescriptor.capitalize(name);
            chosen = this.findCompatible(target.getClass(), cap, args);
        }
        if (chosen == null) {
            throw new NoSuchMethodException("No method " + name + " on " + target.getClass().getName());
        }
        return chosen.invoke(target, args);
    }

    // A listener of `listenerInterface` that, on any of its methods, calls `action` on the target
    // without passing it anything from the event.
    public static <T> T create(Class<T> listenerInterface, Object target, String action) {
        return makeProxy(listenerInterface, target, action, null, null);
    }

    // The same, but passing the target whatever `eventPropertyName` takes off the event.
    public static <T> T create(Class<T> listenerInterface, Object target, String action,
            String eventPropertyName) {
        return makeProxy(listenerInterface, target, action, eventPropertyName, null);
    }

    // The same, and it also reacts only to the listener method that is named.
    public static <T> T create(Class<T> listenerInterface, Object target, String action,
            String eventPropertyName, String listenerMethodName) {
        return makeProxy(listenerInterface, target, action, eventPropertyName, listenerMethodName);
    }

    // The loader asked of the proxy is the interface's and not the context's: the class that is
    // manufactured has to SEE `listenerInterface` in order to implement it, and the only loader that
    // is certain of is the one that loaded it.
    private static <T> T makeProxy(Class<T> listenerInterface, Object target, String action,
            String eventPropertyName, String listenerMethodName) {
        if (listenerInterface == null) {
            throw new NullPointerException("listenerInterface must be non-null");
        }
        EventHandler eh = new EventHandler(target, action, eventPropertyName, listenerMethodName);
        Object proxy = Proxy.newProxyInstance(listenerInterface.getClassLoader(),
            new Class<?>[] { listenerInterface }, eh);
        return listenerInterface.cast(proxy);
    }

    private Method findCompatible(Class<?> c, String name, Object[] args) {
        Method chosen = null;
        Method[] ms = c.getMethods();
        for (int i = 0; i < ms.length; i++) {
            if (chosen == null
                    && ms[i].getName().equals(name)
                    && Statement.accept(ms[i].getParameterTypes(), args)) {
                chosen = ms[i];
            }
        }
        return chosen;
    }
}
