package com.sun.management;

import javax.management.openmbean.CompositeData;

/**
 * Una opcion de la VM: su valor actual, si se puede cambiar, y <strong>de donde salio</strong>.
 *
 * <h2>Por que el origen es el dato importante</h2>
 *
 * <p>Porque el valor solo no alcanza para entender nada. Una opcion que vale lo mismo que el
 * default puede haberla puesto el usuario en la linea de comandos, o puede que nadie la haya
 * tocado; y una que vale algo raro puede ser una decision explicita o
 * {@link Origin#ERGONOMIC ergonomia} — la VM ajustandose sola al hardware que encontro.
 *
 * <p>Distinguir esos casos es lo que separa "esta mal configurado" de "la VM decidio esto y hay que
 * entender por que". Es la razon de que {@link Origin} tenga ocho valores y no dos.
 *
 * @since 1.6
 */
public class VMOption {

    private final String name;
    private final String value;
    private final boolean writeable;
    private final Origin origin;

    /**
     * Una opcion.
     *
     * @param name el nombre
     * @param value el valor actual, como texto
     * @param writeable si se puede cambiar con la VM andando
     * @param origin de donde salio el valor
     * @throws NullPointerException si el nombre, el valor o el origen son {@code null}
     */
    public VMOption(final String name, final String value, final boolean writeable,
            final Origin origin) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }
        if (origin == null) {
            throw new NullPointerException("origin");
        }
        this.name = name;
        this.value = value;
        this.writeable = writeable;
        this.origin = origin;
    }

    /**
     * El nombre.
     *
     * @return el nombre
     */
    public String getName() {
        return name;
    }

    /**
     * El valor actual, como texto.
     *
     * <p>Siempre texto, aunque la opcion sea numerica o booleana: son cientos de opciones con tipos
     * distintos y no hay ninguna clase que las cubra a todas.
     *
     * @return el valor
     */
    public String getValue() {
        return value;
    }

    /**
     * De donde salio el valor.
     *
     * @return el origen
     */
    public Origin getOrigin() {
        return origin;
    }

    /**
     * Si se puede cambiar con la VM ya andando.
     *
     * <p>La mayoria no: dimensionan estructuras que se arman al arrancar. Solo las marcadas
     * {@code manageable} en la VM aceptan cambios en caliente.
     *
     * @return si es escribible
     */
    public boolean isWriteable() {
        return writeable;
    }

    /** {@inheritDoc} */
    public String toString() {
        return "VM option: " + name + " value: " + value + " " + " origin: " + origin
                + " " + (writeable ? "(read-write)" : "(read-only)");
    }

    /**
     * Reconstruye una opcion desde su forma abierta.
     *
     * <p>Es lo que hace falta del lado del cliente cuando la opcion viajo por una conexion JMX: lo
     * que llega es un {@link CompositeData} generico y esto lo vuelve a convertir en el objeto.
     *
     * @param cd la forma abierta, o {@code null}
     * @return la opcion, o {@code null} si {@code cd} era {@code null}
     * @throws IllegalArgumentException si {@code cd} no tiene la forma de una {@code VMOption}
     */
    public static VMOption from(final CompositeData cd) {
        if (cd == null) {
            return null;
        }
        if (!cd.containsKey("name") || !cd.containsKey("value") || !cd.containsKey("origin")
                || !cd.containsKey("writeable")) {
            throw new IllegalArgumentException(
                    "el CompositeData no tiene la forma de una VMOption");
        }
        return new VMOption((String) cd.get("name"), (String) cd.get("value"),
                ((Boolean) cd.get("writeable")).booleanValue(),
                Origin.valueOf((String) cd.get("origin")));
    }

    /** De donde salio el valor de una opcion. */
    public enum Origin {
        /** Nadie la toco: es el valor con el que viene la VM. */
        DEFAULT,
        /** De la linea de comandos, al arrancar. */
        VM_CREATION,
        /** De una variable de entorno. */
        ENVIRON_VAR,
        /** De un archivo de configuracion. */
        CONFIG_FILE,
        /** La cambio una interfaz de gestion con la VM andando. */
        MANAGEMENT,
        /** La eligio la VM sola, segun el hardware que encontro. */
        ERGONOMIC,
        /** La puso una herramienta que se conecto al proceso ya arrancado. */
        ATTACH_ON_DEMAND,
        /** De otro lado. */
        OTHER
    }
}
