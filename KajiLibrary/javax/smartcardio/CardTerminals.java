package javax.smartcardio;

import java.util.List;

/**
 * KajiLibrary's javax.smartcardio.CardTerminals -- los lectores que hay.
 *
 * <p>Se consigue con {@link TerminalFactory#terminals}. Lo que aporta sobre una lista comun es que
 * sabe <b>esperar cambios</b>: que aparezca un lector, que metan una tarjeta.
 *
 * <h2>{@link State#CARD_INSERTION} y {@link State#CARD_REMOVAL}</h2>
 *
 * <p>Estos dos no son estados sino <b>transiciones desde la ultima consulta</b>. Un lector aparece en
 * {@code CARD_INSERTION} una sola vez, en la primera consulta despues de que le pusieron la tarjeta;
 * en la siguiente ya esta solo en {@code CARD_PRESENT}. Tratarlos como estados hace que el programa se
 * pierda insercciones o las cuente dos veces.
 */
public abstract class CardTerminals {

    /** Para las subclases. */
    protected CardTerminals() {
    }

    /**
     * Que lectores pedir. Ver la nota de la clase sobre los dos ultimos.
     */
    public enum State {

        /** Todos. */
        ALL,

        /** Los que tienen tarjeta. */
        CARD_PRESENT,

        /** Los que no. */
        CARD_ABSENT,

        /** Los que recibieron una desde la ultima consulta. */
        CARD_INSERTION,

        /** Los que la perdieron desde la ultima consulta. */
        CARD_REMOVAL,
    }

    /**
     * Todos los lectores.
     *
     * @throws CardException si no se pudieron listar
     */
    public List<CardTerminal> list() throws CardException {
        return list(State.ALL);
    }

    /**
     * Los que esten en ese estado.
     *
     * @throws NullPointerException si el estado es null
     * @throws CardException si no se pudieron listar
     */
    public abstract List<CardTerminal> list(State state) throws CardException;

    /**
     * El lector que se llama asi.
     *
     * @return null si no hay ninguno
     * @throws NullPointerException si el nombre es null
     */
    public CardTerminal getTerminal(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        try {
            List<CardTerminal> terminals = list();
            int i = 0;
            while (i < terminals.size()) {
                if (name.equals(terminals.get(i).getName())) {
                    return terminals.get(i);
                }
                i = i + 1;
            }
        } catch (CardException e) {
            return null;
        }
        return null;
    }

    /**
     * Espera para siempre a que algo cambie.
     *
     * @throws IllegalStateException si no hay lectores
     * @throws CardException si no se pudo esperar
     */
    public void waitForChange() throws CardException {
        waitForChange(0);
    }

    /**
     * Espera a que algo cambie.
     *
     * @param timeout milisegundos, o cero para esperar para siempre
     * @return si hubo un cambio; false si se acabo el tiempo
     * @throws IllegalStateException si no hay lectores
     * @throws IllegalArgumentException si el tiempo es negativo
     * @throws CardException si no se pudo esperar
     */
    public abstract boolean waitForChange(long timeout) throws CardException;
}
