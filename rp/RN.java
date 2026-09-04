import java.util.HashMap;
import java.util.Map;
public class RN extends RF {
    public static class Field extends RF.Field {
        private static final Map<String, RN.Field> I = new HashMap<String, RN.Field>();
        protected Field(String n) {
            super(n);
            if (this.getClass() == RN.Field.class) { I.put(n, this); }
        }
        public static final RN.Field A = new RN.Field("a");
    }
}
