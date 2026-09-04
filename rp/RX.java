import java.util.HashMap;
import java.util.Map;
public abstract class RX extends java.text.Format {
    public static class Field extends java.text.Format.Field {
        private static final Map<String, RX.Field> I = new HashMap<String, RX.Field>();
        protected Field(String name) {
            super(name);
            if (this.getClass() == RX.Field.class) { I.put(name, this); }
        }
        public static final RX.Field A = new RX.Field("a");
    }
    public static enum Style { SHORT, LONG }
}
