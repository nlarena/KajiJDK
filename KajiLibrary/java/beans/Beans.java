package java.beans;

import java.beans.beancontext.BeanContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

/**
 * Utilidades sueltas sobre beans: en qué modo corre el entorno, y cómo traer un bean por nombre.
 *
 * <h2>Cómo se trae un bean, y por qué en ese orden</h2>
 *
 * <p>{@link #instantiate} busca **primero un `.ser`** con el nombre del bean y, si está, lo
 * deserializa; recién si no está carga la clase y llama a su constructor sin argumentos. El orden no
 * es arbitrario: un bean guardado en un `.ser` viene con sus propiedades ya puestas —eso es todo el
 * punto de haberlo guardado— y construirlo de cero daría un objeto distinto del que se pidió, con
 * los valores por omisión en vez de los que alguien configuró.
 *
 * <h2>Lo que no está, y por qué</h2>
 *
 * <p>Falta una de las cuatro formas de `instantiate`: la que además recibe un `AppletInitializer`.
 * Ese tipo tiene dos métodos que reciben un `java.applet.Applet`, y no hay paquete `java.applet` en
 * este árbol. No se puede declarar con otro tipo sin que la firma mienta, así que queda afuera.
 */
public class Beans {

    private static boolean designTime;
    private static boolean guiAvailable = true;

    /** Un `Beans`. La clase es toda estática; el constructor está porque el JDK lo declara. */
    public Beans() {
    }

    /**
     * Trae un bean por nombre.
     *
     * @throws IOException si el `.ser` existe y no se pudo leer
     * @throws ClassNotFoundException si no se encontró la clase, o si no se pudo construir
     */
    public static Object instantiate(ClassLoader cls, String beanName)
            throws IOException, ClassNotFoundException {
        return Beans.instantiate(cls, beanName, null);
    }

    /**
     * Trae un bean por nombre y lo mete en ese contexto.
     *
     * <p>El bean entra al contexto **después** de estar construido, que es la única forma posible: un
     * contexto valida y avisa de sus altas, y no puede hacerlo sobre un objeto a medio hacer.
     *
     * @param beanContext el contexto que lo va a alojar, o `null` para no alojarlo en ninguno
     * @throws IOException si el `.ser` existe y no se pudo leer
     * @throws ClassNotFoundException si no se encontró la clase, o si no se pudo construir
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

    // El bean guardado en `<nombre con / >.ser`, o `null` si no hay tal recurso.
    //
    // Un recurso que existe pero no se puede leer NO se trata como si no existiera: se propaga la
    // IOException. La diferencia importa -- caer a construir la clase de cero ante un `.ser` roto
    // devolvería un bean con los valores por omisión y nadie se enteraría de que se perdió el estado
    // guardado.
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
            // El JDK envuelve el fallo de instanciación en ClassNotFoundException: desde afuera, un
            // bean que no se puede construir es indistinguible de uno que no está.
            throw new ClassNotFoundException(beanName + ": " + e);
        }
    }

    /**
     * Si `bean` puede verse como `targetType`.
     *
     * <p>La respuesta es la del sistema de tipos y nada más. El JDK deja abierta la puerta a que un
     * bean ofrezca "vistas" de sí mismo bajo otro tipo; ninguna implementación estándar la usa, y
     * acá tampoco.
     */
    public static boolean isInstanceOf(Object bean, Class<?> targetType) {
        return bean != null && targetType != null && targetType.isInstance(bean);
    }

    /** El bean visto como `targetType`. La única vista posible de un bean es el bean mismo. */
    public static Object getInstanceOf(Object bean, Class<?> targetType) {
        return bean;
    }

    /** Si el entorno corre en modo diseño. */
    public static boolean isDesignTime() {
        return Beans.readDesignTime();
    }

    /** Fija el modo diseño. */
    public static void setDesignTime(boolean isDesignTime) {
        Beans.writeDesignTime(isDesignTime);
    }

    /** Si hay interfaz gráfica disponible. */
    public static boolean isGuiAvailable() {
        return Beans.readGuiAvailable();
    }

    /** Fija si hay interfaz gráfica disponible. */
    public static void setGuiAvailable(boolean isGuiAvailable) {
        Beans.writeGuiAvailable(isGuiAvailable);
    }

    // Las cuatro banderas se leen y se escriben bajo el candado de la clase. Son estáticas y
    // globales al proceso: un hilo que las cambia tiene que hacerlo visible para los demás, y sin
    // sincronizar no hay nada que lo garantice.
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
