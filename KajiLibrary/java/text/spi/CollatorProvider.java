package java.text.spi;

import java.text.Collator;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

/**
 * KajiLibrary's java.text.spi.CollatorProvider -- como se ordenan las cadenas de un idioma.
 *
 * <p>Un solo metodo, y detras de el esta el ordenamiento cultural, que no tiene nada que ver con
 * comparar puntos de codigo. Tres ejemplos de por que:
 *
 * <ul>
 *   <li>En español la n con virgulilla va <b>despues</b> de la n, no al final del alfabeto donde la
 *       pone su punto de codigo.
 *   <li>En sueco la a con circulo va <b>al final</b>, despues de la z.
 *   <li>En aleman la a con dieresis se ordena como "ae" en una guia telefonica y como "a" en un
 *       diccionario: el <b>mismo</b> idioma con dos ordenamientos, que es para lo que sirven las
 *       extensiones Unicode del local.
 * </ul>
 *
 * <p>Por eso un {@code String.compareTo} nunca sirve para mostrarle una lista ordenada a una
 * persona, y por eso este proveedor existe.
 */
public abstract class CollatorProvider extends LocaleServiceProvider {

    protected CollatorProvider() {
    }

    /** El comparador cultural de ese local. */
    public abstract Collator getInstance(Locale locale);
}
