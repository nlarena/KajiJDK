package javax.smartcardio;

import java.util.List;

/**
 * KajiLibrary's javax.smartcardio.CardTerminals -- the readers there are.
 *
 * <p>It is obtained with {@link TerminalFactory#terminals}. What it adds over an ordinary list is
 * that it knows how to <b>wait for changes</b>: a reader appearing, a card being inserted.
 *
 * <h2>{@link State#CARD_INSERTION} and {@link State#CARD_REMOVAL}</h2>
 *
 * <p>These two are not states but <b>transitions since the last query</b>. A reader appears in
 * {@code CARD_INSERTION} only once, in the first query after the card was inserted; in the next one
 * it is only in {@code CARD_PRESENT}. Treating them as states makes the program miss insertions or
 * count them twice.
 */
public abstract class CardTerminals {

    /** For subclasses. */
    protected CardTerminals() {
    }

    /**
     * Which readers to ask for. See the class note about the last two.
     */
    public enum State {

        /** All of them. */
        ALL,

        /** The ones that have a card. */
        CARD_PRESENT,

        /** The ones that do not. */
        CARD_ABSENT,

        /** The ones that received one since the last query. */
        CARD_INSERTION,

        /** The ones that lost it since the last query. */
        CARD_REMOVAL,
    }

    /**
     * All the readers.
     *
     * @throws CardException if they could not be listed
     */
    public List<CardTerminal> list() throws CardException {
        return list(State.ALL);
    }

    /**
     * The ones in that state.
     *
     * @throws NullPointerException if the state is null
     * @throws CardException if they could not be listed
     */
    public abstract List<CardTerminal> list(State state) throws CardException;

    /**
     * The reader with that name.
     *
     * @return null if there is none
     * @throws NullPointerException if the name is null
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
     * Waits forever for something to change.
     *
     * @throws IllegalStateException if there are no readers
     * @throws CardException if it could not wait
     */
    public void waitForChange() throws CardException {
        waitForChange(0);
    }

    /**
     * Waits for something to change.
     *
     * @param timeout milliseconds, or zero to wait forever
     * @return whether there was a change; false if the time ran out
     * @throws IllegalStateException if there are no readers
     * @throws IllegalArgumentException if the time is negative
     * @throws CardException if it could not wait
     */
    public abstract boolean waitForChange(long timeout) throws CardException;
}
