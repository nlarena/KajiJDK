package com.sun.java.accessibility.util;

/**
 * Los identificadores de cada familia de eventos, para poder hablar de "un tipo de evento" como
 * dato.
 *
 * <h2>Por que numeros y no clases</h2>
 *
 * <p>Porque estos monitores registran oyentes <strong>por tipo</strong> en una sola lista, y una
 * clave numerica hace que agregar, sacar y repartir sea una comparacion de enteros. Con
 * {@code Class} habria que resolver jerarquias en cada despacho.
 *
 * <p>Es la razon de que exista {@link AccessibilityListenerList}: una lista de pares
 * (tipo, oyente) en un solo arreglo.
 *
 * <p>Las once primeras constantes son de AWT y las diecisiete siguientes de Swing, que es el mismo
 * corte que separa a {@link AWTEventMonitor} de {@link SwingEventMonitor}.
 */
public class EventID {

    /** Un boton o similar se activo. */
    public static final int ACTION = 0;
    /** Cambio una barra de desplazamiento. */
    public static final int ADJUSTMENT = 1;
    /** Un componente se movio, cambio de tamano o de visibilidad. */
    public static final int COMPONENT = 2;
    /** Un contenedor gano o perdio un hijo. */
    public static final int CONTAINER = 3;
    /** Cambio el foco. */
    public static final int FOCUS = 4;
    /** Se selecciono o deselecciono un item. */
    public static final int ITEM = 5;
    /** Teclado. */
    public static final int KEY = 6;
    /** Botones del mouse. */
    public static final int MOUSE = 7;
    /** Movimiento del mouse; va aparte porque llega muchisimo mas seguido. */
    public static final int MOTION = 8;
    /** Cambio un campo de texto. */
    public static final int TEXT = 9;
    /** Una ventana se abrio, cerro, minimizo. */
    public static final int WINDOW = 10;

    /** Cambio la cadena de ancestros de un componente. */
    public static final int ANCESTOR = 11;
    /** Se movio el cursor de texto. */
    public static final int CARET = 12;
    /** Termino la edicion de una celda. */
    public static final int CELLEDITOR = 13;
    /** Cambio de estado generico. */
    public static final int CHANGE = 14;
    /** Cambio el modelo de columnas de una tabla. */
    public static final int COLUMNMODEL = 15;
    /** Cambio un documento de texto. */
    public static final int DOCUMENT = 16;
    /** Cambio el contenido de una lista. */
    public static final int LISTDATA = 17;
    /** Cambio la seleccion de una lista. */
    public static final int LISTSELECTION = 18;
    /** Un menu se abrio o se cerro. */
    public static final int MENU = 19;
    /** Un menu contextual se abrio o se cerro. */
    public static final int POPUPMENU = 20;
    /** Cambio el modelo de una tabla. */
    public static final int TABLEMODEL = 21;
    /** Un nodo de arbol se expandio o se contrajo. */
    public static final int TREEEXPANSION = 22;
    /** Cambio el modelo de un arbol. */
    public static final int TREEMODEL = 23;
    /** Cambio la seleccion de un arbol. */
    public static final int TREESELECTION = 24;
    /** Se hizo algo deshacible. */
    public static final int UNDOABLEEDIT = 25;
    /** Cambio una propiedad. */
    public static final int PROPERTYCHANGE = 26;
    /** Se va a cambiar una propiedad, y se puede vetar. */
    public static final int VETOABLECHANGE = 27;
    /** Una ventana interna cambio de estado. */
    public static final int INTERNALFRAME = 28;

    public EventID() {
    }
}
