package com.sun.java.accessibility.util;

import java.util.EventListener;

/**
 * Una lista de oyentes que guarda, junto a cada uno, <strong>de que tipo</strong> es.
 *
 * <h2>Por que un arreglo de pares y no un mapa</h2>
 *
 * <p>Porque el arreglo se recorre en el despacho, que es lo que pasa muchisimas veces, y agregar o
 * sacar pasa muy pocas. Un {@code Map<Class, List>} seria mas comodo de escribir y mas lento de
 * recorrer: una indireccion por tipo en cada evento.
 *
 * <p>El formato es el de {@code javax.swing.event.EventListenerList}: posiciones pares el tipo,
 * impares el oyente. Feo de leer y muy barato de recorrer.
 *
 * <h2>Por que se copia el arreglo al modificar</h2>
 *
 * <p>Porque el despacho ocurre en el hilo de eventos y el registro en cualquier otro. Copiar en vez
 * de mutar hace que quien esta recorriendo siga con el arreglo que tenia — sin bloquear en el camino
 * caliente, y sin la excepcion de modificacion concurrente que traeria una lista mutable.
 *
 * <p>Es la razon de que {@link #getListenerList} devuelva el arreglo interno y de que la
 * documentacion del JDK diga que <strong>no hay que modificarlo</strong>: prestarlo es lo que evita
 * una copia por evento.
 */
public class AccessibilityListenerList {

    private static final Object[] VACIO = new Object[0];

    /** Pares (tipo, oyente); ver la nota de la clase sobre el formato. */
    protected transient Object[] listenerList = VACIO;

    public AccessibilityListenerList() {
    }

    /**
     * El arreglo de pares, prestado.
     *
     * <p>No modificarlo: es el que estan recorriendo los despachos en curso.
     */
    public Object[] getListenerList() {
        return this.listenerList;
    }

    /** Cuantos oyentes hay, de todos los tipos. */
    public int getListenerCount() {
        return this.listenerList.length / 2;
    }

    /** Cuantos hay de ese tipo. */
    public int getListenerCount(Class<? extends EventListener> t) {
        int n = 0;
        Object[] lista = this.listenerList;
        for (int i = 0; i < lista.length; i += 2) {
            if (t == (Class<?>) lista[i]) {
                n++;
            }
        }
        return n;
    }

    /**
     * Agrega un oyente de ese tipo.
     *
     * <p>Se puede agregar el mismo dos veces, y entonces recibe cada evento dos veces. Es lo que
     * hace el JDK: deduplicar obligaria a recorrer la lista en cada alta y cambiaria el
     * comportamiento de quien se registra a proposito dos veces.
     */
    public synchronized void add(Class<? extends EventListener> t, EventListener l) {
        if (l == null) {
            return;
        }
        if (!t.isInstance(l)) {
            throw new IllegalArgumentException(
                    "el oyente no es del tipo " + t.getName());
        }
        Object[] nuevo = new Object[this.listenerList.length + 2];
        System.arraycopy(this.listenerList, 0, nuevo, 0, this.listenerList.length);
        nuevo[this.listenerList.length] = t;
        nuevo[this.listenerList.length + 1] = l;
        this.listenerList = nuevo;
    }

    /**
     * Saca <strong>una</strong> ocurrencia de ese oyente con ese tipo.
     *
     * <p>Una y no todas, para ser simetrico con {@link #add}: quien lo agrego dos veces tiene que
     * sacarlo dos veces.
     */
    public synchronized void remove(Class<? extends EventListener> t, EventListener l) {
        if (l == null) {
            return;
        }
        if (!t.isInstance(l)) {
            throw new IllegalArgumentException(
                    "el oyente no es del tipo " + t.getName());
        }
        // Se busca desde el final: lo mas recien agregado es lo que mas se saca.
        int indice = -1;
        for (int i = this.listenerList.length - 2; i >= 0; i -= 2) {
            if (this.listenerList[i] == t && this.listenerList[i + 1].equals(l)) {
                indice = i;
                break;
            }
        }
        if (indice < 0) {
            return;
        }
        Object[] nuevo = new Object[this.listenerList.length - 2];
        System.arraycopy(this.listenerList, 0, nuevo, 0, indice);
        if (indice < nuevo.length) {
            System.arraycopy(this.listenerList, indice + 2, nuevo, indice,
                    nuevo.length - indice);
        }
        this.listenerList = nuevo.length == 0 ? VACIO : nuevo;
    }

    public String toString() {
        Object[] lista = this.listenerList;
        StringBuilder sb = new StringBuilder();
        sb.append("EventListenerList: ");
        sb.append(String.valueOf(lista.length / 2)).append(" listeners: ");
        for (int i = 0; i <= lista.length - 2; i += 2) {
            sb.append(" type ").append(((Class<?>) lista[i]).getName());
            sb.append(" listener ").append(String.valueOf(lista[i + 1]));
        }
        return sb.toString();
    }
}
