package pb;

import pa.Base;

public class Hija extends Base {

    static class Interna {

        private final Hija h;

        Interna(Hija h) {
            this.h = h;
        }

        void usar() {
            h.avisar();          // <-- nuestro javac lo rechaza; el del JDK lo acepta
        }
    }
}
