package java.beans;

import java.beans.beancontext.BeanContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

/**
 * Loose utilities about beans: which mode the environment runs in, and how to bring a bean in by
 * name.
 *
 * <h2>How a bean is brought in, and why in that order</h2>
 *
 * <p>{@link #instantiate} looks **first for a `.ser`** with the bean's name and, if it is there,
 * deserializes it; only if it is not there does it load the class and call its no-argument
 * constructor. The order is not arbitrary: a bean stored in a `.ser` comes with its properties
 * already set --that is the whole point of having stored it-- and constructing it from scratch would
 * give a different object from the one that was asked for, with the default values instead of the
 * ones somebody configured.
 *
 * <h2>The form with `AppletInitializer`</h2>
 *
 * <p>The fourth form of `instantiate` exists for the beans that are applets: besides constructing
 * them it prepares them as a browser would, with {@link AppletInitializer#initialize} before entering
 * the context and {@link AppletInitializer#activate} afterwards. Here that path is never walked,
 * because a {@link java.applet.Applet} cannot be constructed with no screen; the form is here in full
 * all the same, and for a bean that is not an applet it does exactly the same as the three-argument
 * one.
 */
public class Beans {

    private static boolean designTime;
    private static boolean guiAvailable = true;

    /** A `Beans`. The class is entirely static; the constructor is here because the JDK declares
     * it. */
    public Beans() {
    }

    /**
     * Brings a bean in by name.
     *
     * @throws IOException if the `.ser` exists and could not be read
     * @throws ClassNotFoundException if the class was not found, or if it could not be constructed
     */
    public static Object instantiate(ClassLoader cls, String beanName)
            throws IOException, ClassNotFoundException {
        return Beans.instantiate(cls, beanName, null);
    }

    /**
     * Brings a bean in by name and puts it into that context.
     *
     * <p>The bean enters the context **after** being constructed, which is the only possible way: a
     * context validates and reports its additions, and it cannot do that over a half-made object.
     *
     * @param beanContext the context that is going to host it, or `null` for hosting it in none
     * @throws IOException if the `.ser` exists and could not be read
     * @throws ClassNotFoundException if the class was not found, or if it could not be constructed
     */
    public static Object instantiate(ClassLoader cls, String beanName, BeanContext beanContext)
            throws IOException, ClassNotFoundException {
        if (beanName == null) {
            throw new NullPointerException("beanName");
        }
        Object bean = Beans.fromSerializedForm(cls, beanName);
        if (bean == null) {
            bean = Beans.fromClass(cls, beanName);
        }
        if (beanContext != null) {
            beanContext.add(bean);
        }
        return bean;
    }

    /**
     * Brings a bean in by name, puts it into that context and, if it is an applet, prepares it.
     *
     * <p>The order is a browser's: the applet is initialized **before** entering the context, so
     * that on entering it already has its stub in place, and it is activated **afterwards**, because
     * activating means "get going", and getting going without being hosted has nowhere to show
     * itself. A bean that is not an applet ignores the initializer.
     *
     * @param initializer whoever prepares the applet, or `null` for not preparing it
     * @throws IOException if the `.ser` exists and could not be read
     * @throws ClassNotFoundException if the class was not found, or if it could not be constructed
     */
    public static Object instantiate(ClassLoader cls, String beanName, BeanContext beanContext,
            AppletInitializer initializer) throws IOException, ClassNotFoundException {
        if (beanName == null) {
            throw new NullPointerException("beanName");
        }
        Object bean = Beans.fromSerializedForm(cls, beanName);
        if (bean == null) {
            bean = Beans.fromClass(cls, beanName);
        }
        boolean applet = bean instanceof java.applet.Applet && initializer != null;
        if (applet) {
            initializer.initialize((java.applet.Applet) bean, beanContext);
        }
        if (beanContext != null) {
            beanContext.add(bean);
        }
        if (applet) {
            initializer.activate((java.applet.Applet) bean);
        }
        return bean;
    }

    // The bean stored in `<name with / >.ser`, or `null` if there is no such resource.
    //
    // A resource that exists but cannot be read is NOT treated as if it did not exist: the
    // IOException is propagated. The difference matters -- falling back to constructing the class
    // from scratch on a broken `.ser` would return a bean with the default values and nobody would
    // hear that the stored state had been lost.
    private static Object fromSerializedForm(ClassLoader cls, String beanName)
            throws IOException, ClassNotFoundException {
        String resource = beanName.replace('.', '/') + ".ser";
        InputStream in = cls == null ? ClassLoader.getSystemResourceAsStream(resource)
                : cls.getResourceAsStream(resource);
        if (in == null) {
            return null;
        }
        ObjectInputStream ois = new ObjectInputStream(in);
        try {
            return ois.readObject();
        } finally {
            ois.close();
        }
    }

    private static Object fromClass(ClassLoader cls, String beanName)
            throws ClassNotFoundException {
        Class<?> c = cls == null ? Class.forName(beanName) : Class.forName(beanName, true, cls);
        try {
            return c.newInstance();
        } catch (ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            // The JDK wraps the instantiation failure in ClassNotFoundException: from outside, a
            // bean that cannot be constructed is indistinguishable from one that is not there.
            throw new ClassNotFoundException(beanName + ": " + e);
        }
    }

    /**
     * Whether `bean` can be seen as `targetType`.
     *
     * <p>The answer is the type system's and nothing else. The JDK leaves the door open for a bean to
     * offer "views" of itself under another type; no standard implementation uses it, and nor does
     * this one.
     */
    public static boolean isInstanceOf(Object bean, Class<?> targetType) {
        return bean != null && targetType != null && targetType.isInstance(bean);
    }

    /** The bean seen as `targetType`. A bean's only possible view is the bean itself. */
    public static Object getInstanceOf(Object bean, Class<?> targetType) {
        return bean;
    }

    /** Whether the environment runs in design mode. */
    public static boolean isDesignTime() {
        return Beans.readDesignTime();
    }

    /** Sets the design mode. */
    public static void setDesignTime(boolean isDesignTime) {
        Beans.writeDesignTime(isDesignTime);
    }

    /** Whether a graphical interface is available. */
    public static boolean isGuiAvailable() {
        return Beans.readGuiAvailable();
    }

    /** Sets whether a graphical interface is available. */
    public static void setGuiAvailable(boolean isGuiAvailable) {
        Beans.writeGuiAvailable(isGuiAvailable);
    }

    // The four flags are read and written under the class's lock. They are static and global to the
    // process: a thread changing them has to make that visible to the rest, and without
    // synchronizing there is nothing that guarantees it.
    private static synchronized boolean readDesignTime() {
        return Beans.designTime;
    }

    private static synchronized void writeDesignTime(boolean v) {
        Beans.designTime = v;
    }

    private static synchronized boolean readGuiAvailable() {
        return Beans.guiAvailable;
    }

    private static synchronized void writeGuiAvailable(boolean v) {
        Beans.guiAvailable = v;
    }
}
