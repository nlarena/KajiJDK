package java.awt.im.spi;

import java.awt.AWTException;
import java.awt.Image;
import java.util.Locale;

/**
 * La ficha de un {@link InputMethod}: lo que se puede saber de el **sin cargarlo**.
 *
 * <p>Esa es toda la razon de que exista. Un sistema puede tener muchos metodos de entrada
 * instalados, y armar el menu para elegirlos no puede costar cargar y arrancar cada uno: seria
 * pagar por todos para usar uno. El descriptor es liviano --nombre, icono, idiomas-- y el metodo se
 * crea recien cuando alguien lo elige.
 *
 * <p>Se descubre por {@link java.util.ServiceLoader}: un metodo de entrada se instala declarando su
 * descriptor como proveedor de este servicio.
 */
public interface InputMethodDescriptor {

    /**
     * Los idiomas que este metodo de entrada soporta.
     *
     * <p>Un idioma en la lista no promete que este disponible ahora --puede depender de un
     * diccionario que se instala aparte-- pero si que
     * {@link InputMethod#setLocale} tiene sentido para el.
     *
     * @throws AWTException si no se pudo averiguar
     */
    Locale[] getAvailableLocales() throws AWTException;

    /**
     * Si la lista de idiomas puede cambiar mientras el programa corre.
     *
     * <p>Con `true`, el marco de trabajo vuelve a preguntar en vez de quedarse con lo primero que
     * le dijeron: es el caso de un metodo cuyos diccionarios se pueden instalar sobre la marcha.
     */
    boolean hasDynamicLocaleList();

    /**
     * El nombre a mostrar, en el idioma en que se lo quiere mostrar.
     *
     * @param inputLocale el idioma que se escribiria con el, o `null` si no importa
     * @param displayLanguage el idioma en el que se quiere el nombre
     */
    String getInputMethodDisplayName(Locale inputLocale, Locale displayLanguage);

    /**
     * El icono, de 16x16, o `null` si no tiene.
     *
     * @param inputLocale el idioma que se escribiria con el, o `null` si no importa
     */
    Image getInputMethodIcon(Locale inputLocale);

    /**
     * Crea el metodo de entrada.
     *
     * <p>Es el unico punto caro, y por eso esta separado del resto de la interfaz.
     *
     * @throws Exception lo que sea que falle al crearlo; el marco de trabajo lo reporta y sigue con
     *     los demas metodos instalados
     */
    InputMethod createInputMethod() throws Exception;
}
