package com.sun.jdi.connect;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Una forma de conseguir una {@link com.sun.jdi.VirtualMachine}.
 *
 * <p>Hay tres, y son las tres subinterfaces: {@link LaunchingConnector} arranca la VM,
 * {@link AttachingConnector} se pega a una que ya corre, y {@link ListeningConnector} espera a que
 * la VM se conecte al depurador. La tercera existe porque a veces el que arranca primero es el
 * programa depurado --{@code -agentlib:jdwp=server=n}-- y entonces el depurador es el que escucha.
 *
 * <h2>Los argumentos, y por que son tan raros</h2>
 *
 * <p>Cada conector se configura con un mapa de {@link Argument} que el propio conector entrega
 * lleno de valores por omision: el cliente pide {@link #defaultArguments()}, cambia lo que quiera y
 * lo devuelve. Es al reves de lo habitual --el que llama no arma el mapa, lo recibe-- y es a
 * proposito: cada conector tiene argumentos distintos, y un depurador generico tiene que poder
 * mostrar un formulario para uno que no conoce. Por eso cada `Argument` trae su etiqueta, su
 * descripcion, si es obligatorio y como validarse.
 *
 * <p>Las cuatro subinterfaces de `Argument` --cadena, entero, booleano y eleccion-- son las cuatro
 * clases de control que ese formulario necesita saber dibujar.
 */
public interface Connector {

    /** El nombre corto del conector, por ejemplo {@code "com.sun.jdi.SocketAttach"}. */
    String name();

    /** Una descripcion legible, para mostrarle al usuario. */
    String description();

    /** El transporte por el que este conector habla. */
    Transport transport();

    /**
     * Un mapa nuevo de argumentos, con los valores por omision puestos.
     *
     * <p>Es una copia: modificarlo no afecta al conector, y hay que devolverselo al conectar.
     */
    Map<String, Argument> defaultArguments();

    /**
     * Un argumento de configuracion de un {@link Connector}.
     *
     * <p>Es {@link Serializable} para que un depurador pueda guardar una configuracion de conexion
     * y volver a abrirla.
     */
    interface Argument extends Serializable {

        /** El nombre con el que el argumento aparece en el mapa. */
        String name();

        /** Una etiqueta corta, para poner al lado del control. */
        String label();

        /** Una explicacion, para la ayuda. */
        String description();

        /** El valor actual, como texto. */
        String value();

        /**
         * Fija el valor.
         *
         * <p>No valida: {@link #isValid} es aparte, para que una interfaz pueda mostrar un valor a
         * medio escribir sin rechazarlo caracter por caracter.
         */
        void setValue(String value);

        /** Si ese texto seria un valor aceptable para este argumento. */
        boolean isValid(String value);

        /** Si el argumento tiene que tener valor antes de conectar. */
        boolean mustSpecify();
    }

    /** Un {@link Argument} cuyo valor es texto libre. */
    interface StringArgument extends Argument {

        /**
         * Si ese texto sirve.
         *
         * <p>Para un argumento de texto, cualquier cadena sirve: la implementacion de siempre
         * devuelve `true`.
         */
        boolean isValid(String value);
    }

    /** Un {@link Argument} cuyo valor es un entero acotado. */
    interface IntegerArgument extends Argument {

        /**
         * Fija el valor.
         *
         * <p>Un valor fuera de {@link #min()}..{@link #max()} se acepta igual: validar es aparte,
         * por lo mismo que en {@link Argument#setValue}.
         */
        void setValue(int value);

        /** Si ese texto es un entero dentro del rango. */
        boolean isValid(String value);

        /** Si ese entero esta dentro del rango. */
        boolean isValid(int value);

        /** Ese entero escrito como lo escribiria este argumento. */
        String stringValueOf(int value);

        /** El valor actual como entero. */
        int intValue();

        /** El maximo aceptable. */
        int max();

        /** El minimo aceptable. */
        int min();
    }

    /** Un {@link Argument} cuyo valor es `true` o `false`. */
    interface BooleanArgument extends Argument {

        /** Fija el valor. */
        void setValue(boolean value);

        /** Si ese texto es uno de los dos valores que este argumento reconoce. */
        boolean isValid(String value);

        /** Ese booleano escrito como lo escribiria este argumento. */
        String stringValueOf(boolean value);

        /** El valor actual como booleano. */
        boolean booleanValue();
    }

    /** Un {@link Argument} cuyo valor sale de una lista cerrada. */
    interface SelectedArgument extends Argument {

        /** Los valores posibles. */
        List<String> choices();

        /** Si ese texto es uno de los de {@link #choices()}. */
        boolean isValid(String value);
    }
}
