package java.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// A bean's simple property: a name, a type, and up to two methods --the one that reads and the one
// that writes. Either of the two may be missing: with no writer it is read-only, with no reader it
// is write-only, and both are legitimate properties.
//
// The type is NOT declared, it is worked out: it comes from the reader's return or from the writer's
// single parameter. When both are there they have to agree, and if they do not agree the property is
// not valid -- it is the check that stops `setMismatched(int)` from being hooked to
// `getMismatched():String`.
//
// On `bound`: the constructor that receives the bean's class turns it on only if that class has
// addPropertyChangeListener. It is checked against the real JDK (a bean without that method gives
// bound=false, one with it gives bound=true) and it is not Introspector's doing: it happens in the
// constructor. `constrained`, on the other hand, is NOT worked out from addVetoableChangeListener
// -- also checked.
public class PropertyDescriptor extends FeatureDescriptor {

    private Class<?> propertyType;
    private Method readMethod;
    private Method writeMethod;
    private boolean bound;
    private boolean constrained;
    private Class<?> propertyEditorClass;

    // The bean's class, so that the other accessor can be resolved when only one has come in.
    private Class<?> class0;

    // It looks for `isName`/`getName` and `setName` in the class.
    public PropertyDescriptor(String propertyName, Class<?> beanClass) throws IntrospectionException {
        this(propertyName, beanClass,
             "is" + capitalize(propertyName),
             "set" + capitalize(propertyName));
    }

    // The same, but with the method names given. A null name means "this property has no such
    // accessor".
    public PropertyDescriptor(String propertyName, Class<?> beanClass,
                              String readMethodName, String writeMethodName)
            throws IntrospectionException {
        if (beanClass == null) {
            throw new IntrospectionException("Target Bean class is null");
        }
        if (propertyName == null || propertyName.length() == 0) {
            throw new IntrospectionException("bad property name");
        }
        this.setName(propertyName);
        this.class0 = beanClass;

        String readName = readMethodName;
        if (readName != null && readName.length() == 0) {
            readName = null;
        }
        String writeName = writeMethodName;
        if (writeName != null && writeName.length() == 0) {
            writeName = null;
        }

        Method r = null;
        if (readName != null) {
            r = findMethod(beanClass, readName, 0);
            if (r == null) {
                // The first attempt assumes boolean (`isX`); if it is not there, `getX` is tried.
                // That the error message keeps the ORIGINAL name is what the JDK does.
                String alternative = "get" + capitalize(propertyName);
                r = findMethod(beanClass, alternative, 0);
            }
            if (r == null) {
                throw new IntrospectionException("Method not found: " + readName);
            }
        }
        if (r != null) {
            this.setReadMethod(r);
        }

        if (writeName != null) {
            Method w = findMethod(beanClass, writeName, 1);
            if (w == null && readName == null) {
                throw new IntrospectionException("Method not found: " + writeName);
            }
            if (w != null) {
                this.setWriteMethod(w);
            }
        }

        this.bound = findMethod(beanClass, "addPropertyChangeListener", 1) != null;
    }

    // With the methods already in hand. Either of the two may be null.
    public PropertyDescriptor(String propertyName, Method readMethod, Method writeMethod)
            throws IntrospectionException {
        if (propertyName == null || propertyName.length() == 0) {
            throw new IntrospectionException("bad property name");
        }
        this.setName(propertyName);
        this.setReadMethod(readMethod);
        this.setWriteMethod(writeMethod);
    }

    // Introspector's internal constructor, which validated everything already while discovering
    // the methods and does not need it validated again.
    PropertyDescriptor(String propertyName, Method readMethod, Method writeMethod, boolean unchecked) {
        this.setName(propertyName);
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
        if (readMethod != null) {
            this.propertyType = readMethod.getReturnType();
        } else if (writeMethod != null) {
            this.propertyType = writeMethod.getParameterTypes()[0];
        }
    }

    // The property's type, or null when there is neither a non-indexed reader nor a non-indexed
    // writer -- which is exactly the case of a purely indexed property.
    public synchronized Class<?> getPropertyType() {
        return this.propertyType;
    }

    // So that IndexedPropertyDescriptor can leave it null without repeating the deduction.
    void setType(Class<?> t) {
        this.propertyType = t;
    }

    public synchronized Method getReadMethod() {
        return this.readMethod;
    }

    // It accepts the reader method if it is a real getter: no parameters and returning something.
    // If there was a writer already, the types have to add up.
    public synchronized void setReadMethod(Method readMethod) throws IntrospectionException {
        if (readMethod == null) {
            this.readMethod = null;
            if (this.writeMethod == null) {
                this.propertyType = null;
            }
        } else {
            if (readMethod.getParameterTypes().length != 0) {
                throw new IntrospectionException("bad read method arg count");
            }
            Class<?> t = readMethod.getReturnType();
            if (t == void.class) {
                throw new IntrospectionException("read method returns void");
            }
            if (this.propertyType != null && this.propertyType != t) {
                throw new IntrospectionException("type mismatch between read and write methods");
            }
            this.readMethod = readMethod;
            this.propertyType = t;
            if (this.class0 == null) {
                this.class0 = readMethod.getDeclaringClass();
            }
        }
    }

    public synchronized Method getWriteMethod() {
        return this.writeMethod;
    }

    // It accepts the writer method if it takes exactly one argument, and if that argument is of the
    // type the property already has.
    public synchronized void setWriteMethod(Method writeMethod) throws IntrospectionException {
        if (writeMethod == null) {
            this.writeMethod = null;
            if (this.readMethod == null) {
                this.propertyType = null;
            }
        } else {
            Class<?>[] args = writeMethod.getParameterTypes();
            if (args.length != 1) {
                throw new IntrospectionException("bad write method arg count");
            }
            if (this.propertyType != null && this.propertyType != args[0]) {
                throw new IntrospectionException("type mismatch between read and write methods");
            }
            this.writeMethod = writeMethod;
            this.propertyType = args[0];
            if (this.class0 == null) {
                this.class0 = writeMethod.getDeclaringClass();
            }
        }
    }

    // Whether changing it fires a PropertyChangeEvent.
    public boolean isBound() {
        return this.bound;
    }

    public void setBound(boolean bound) {
        this.bound = bound;
    }

    // Whether a listener may veto the change.
    public boolean isConstrained() {
        return this.constrained;
    }

    public void setConstrained(boolean constrained) {
        this.constrained = constrained;
    }

    public void setPropertyEditorClass(Class<?> propertyEditorClass) {
        this.propertyEditorClass = propertyEditorClass;
    }

    public Class<?> getPropertyEditorClass() {
        return this.propertyEditorClass;
    }

    // It instantiates the declared editor. The constructor that receives the bean --the one the
    // editors needing context use-- is tried first, and it falls back to the zero-argument one.
    public PropertyEditor createPropertyEditor(Object bean) {
        PropertyEditor ed = null;
        if (this.propertyEditorClass != null) {
            try {
                Constructor<?> c = null;
                try {
                    c = this.propertyEditorClass.getConstructor(Object.class);
                } catch (Exception withoutThatCtor) {
                    c = null;
                }
                Object o;
                if (c != null) {
                    o = c.newInstance(bean);
                } else {
                    o = this.propertyEditorClass.newInstance();
                }
                ed = (PropertyEditor) o;
            } catch (Exception e) {
                ed = null;
            }
        }
        return ed;
    }

    public boolean equals(Object obj) {
        boolean same = this == obj;
        if (!same && obj instanceof PropertyDescriptor) {
            PropertyDescriptor other = (PropertyDescriptor) obj;
            same = sameMethods(this.getReadMethod(), other.getReadMethod())
                 && sameMethods(this.getWriteMethod(), other.getWriteMethod())
                 && this.getPropertyType() == other.getPropertyType()
                 && this.getPropertyEditorClass() == other.getPropertyEditorClass()
                 && this.bound == other.bound
                 && this.constrained == other.constrained;
        }
        return same;
    }

    public int hashCode() {
        int h = 7;
        h = 37 * h + (this.getName() == null ? 0 : this.getName().hashCode());
        h = 37 * h + (this.propertyType == null ? 0 : this.propertyType.hashCode());
        return h;
    }

    private static boolean sameMethods(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    // --- helpers shared with the rest of the package ----------------------------------

    // "name" -> "Name". It is not decapitalize backwards: here raising the first letter is
    // enough.
    static String capitalize(String s) {
        String r = s;
        if (s != null && s.length() > 0) {
            r = s.substring(0, 1).toUpperCase() + s.substring(1);
        }
        return r;
    }

    // The first public and NON-static method with that name and that number of arguments.
    // Introspector ignores the static ones, and it is checked against the real JDK: a public
    // `getStatic()` produces no property at all.
    static Method findMethod(Class<?> c, String name, int argCount) {
        Method hit = null;
        if (c != null && name != null) {
            Method[] ms = c.getMethods();
            for (int i = 0; i < ms.length; i++) {
                Method m = ms[i];
                if (hit == null
                        && m.getName().equals(name)
                        && m.getParameterTypes().length == argCount
                        && !Modifier.isStatic(m.getModifiers())) {
                    hit = m;
                }
            }
        }
        return hit;
    }
}
