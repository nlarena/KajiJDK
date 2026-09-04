// Prueba de comportamiento de javax.management.monitor y javax.security.sasl.
//
// `run()` devuelve -1 si pasa todo, o el indice del primer caso que falla. Los numeros no se
// reciclan.
//
// No hace falta un `runKaji()`: nada de lo que se afirma aca depende de que existan proveedores.
// Los monitores se prueban parados --su configuracion y sus validaciones-- porque observar de verdad
// necesita un agente con MBeans, y eso lo cubre JmxTest.

import java.util.Enumeration;

import javax.management.MBeanNotificationInfo;
import javax.management.ObjectName;
import javax.management.monitor.CounterMonitor;
import javax.management.monitor.GaugeMonitor;
import javax.management.monitor.MonitorNotification;
import javax.management.monitor.MonitorSettingException;
import javax.management.monitor.StringMonitor;

import javax.security.sasl.AuthenticationException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslException;

public class MonSaslTest {

    public static int run() {
        // --- javax.management.monitor: valores por omision ---
        CounterMonitor counter = new CounterMonitor();
        if (counter.getGranularityPeriod() != 10000L) return 0;
        if (counter.isActive()) return 1;
        if (counter.getObservedAttribute() != null) return 2;
        if (counter.getObservedObject() != null) return 3;
        if (counter.getObservedObjects().length != 0) return 4;
        // El umbral arranca en null y el inicial en 0: no son lo mismo.
        if (counter.getThreshold() != null) return 5;
        if (counter.getInitThreshold().longValue() != 0L) return 6;
        if (counter.getOffset().longValue() != 0L) return 7;
        if (counter.getModulus().longValue() != 0L) return 8;
        if (counter.getNotify()) return 9;
        if (counter.getDifferenceMode()) return 10;
        if (counter.getDerivedGauge() != null) return 11;
        if (counter.getDerivedGaugeTimeStamp() != 0L) return 12;

        GaugeMonitor gauge = new GaugeMonitor();
        if (gauge.getHighThreshold().longValue() != 0L) return 13;
        if (gauge.getLowThreshold().longValue() != 0L) return 14;
        if (gauge.getNotifyHigh()) return 15;
        if (gauge.getNotifyLow()) return 16;
        if (gauge.getDifferenceMode()) return 17;
        if (gauge.getDerivedGauge() != null) return 18;

        StringMonitor string = new StringMonitor();
        // Arranca en la cadena vacia, no en null.
        if (!"".equals(string.getStringToCompare())) return 19;
        if (string.getNotifyMatch()) return 20;
        if (string.getNotifyDiffer()) return 21;
        if (string.getDerivedGauge() != null) return 22;

        // --- las validaciones ---
        if (!rejects(() -> counter.setGranularityPeriod(0))) return 23;
        if (!rejects(() -> counter.setGranularityPeriod(-1))) return 24;
        counter.setGranularityPeriod(5);
        if (counter.getGranularityPeriod() != 5L) return 25;
        if (!rejects(() -> counter.setObservedAttribute(null))) return 26;
        if (!rejects(() -> counter.addObservedObject(null))) return 27;
        if (!rejects(() -> counter.setObservedObject(null))) return 28;
        if (!rejects(() -> counter.setThreshold(null))) return 29;
        if (!rejects(() -> counter.setThreshold(Integer.valueOf(-1)))) return 30;
        // Un umbral con coma se acepta: la validacion es solo que no sea negativo.
        counter.setThreshold(Double.valueOf(1.5));
        if (!rejects(() -> counter.setOffset(null))) return 31;
        if (!rejects(() -> counter.setOffset(Integer.valueOf(-1)))) return 32;
        if (!rejects(() -> counter.setModulus(null))) return 33;
        if (!rejects(() -> counter.setModulus(Integer.valueOf(-1)))) return 34;
        if (!rejects(() -> string.setStringToCompare(null))) return 35;

        // Los dos umbrales del medidor van juntos y tienen tres reglas.
        if (!rejects(() -> gauge.setThresholds(null, null))) return 36;
        if (!rejects(() -> gauge.setThresholds(Integer.valueOf(1), Integer.valueOf(5)))) return 37;
        if (!rejects(() -> gauge.setThresholds(Integer.valueOf(5), Double.valueOf(1.0)))) return 38;
        gauge.setThresholds(Integer.valueOf(5), Integer.valueOf(1));
        if (gauge.getHighThreshold().longValue() != 5L) return 39;
        if (gauge.getLowThreshold().longValue() != 1L) return 40;
        // Iguales si se aceptan: la banda puede tener ancho cero.
        gauge.setThresholds(Integer.valueOf(3), Integer.valueOf(3));
        if (gauge.getHighThreshold().longValue() != 3L) return 41;

        // --- el estado por observado se crea al dar de alta, no en la primera lectura ---
        ObjectName probe;
        try {
            probe = new ObjectName("x:type=probe");
        } catch (Exception e) {
            return 123;
        }
        CounterMonitor fresh = new CounterMonitor();
        fresh.addObservedObject(probe);
        // Recien agregado ya hay valor y hay marca de tiempo: "no lei nada de este" y "este no
        // existe" son dos respuestas distintas.
        if (!Integer.valueOf(0).equals(fresh.getDerivedGauge())) return 124;
        if (fresh.getDerivedGaugeTimeStamp() <= 0) return 125;
        if (fresh.getThreshold().longValue() != 0L) return 126;
        // Un objeto que no esta observado no tiene umbral.
        try {
            if (fresh.getThreshold(new ObjectName("x:type=nadie")) != null) return 127;
        } catch (Exception e) {
            return 128;
        }
        GaugeMonitor freshGauge = new GaugeMonitor();
        freshGauge.addObservedObject(probe);
        if (!Integer.valueOf(0).equals(freshGauge.getDerivedGauge())) return 129;
        if (freshGauge.getDerivedGaugeTimeStamp() <= 0) return 130;
        // Al darlo de baja se olvida todo lo suyo.
        fresh.removeObservedObject(probe);
        if (fresh.getDerivedGauge() != null) return 131;
        if (fresh.getThreshold() != null) return 132;

        // --- la lista de observados ---
        ObjectName first;
        ObjectName second;
        try {
            first = new ObjectName("x:type=a");
            second = new ObjectName("x:type=b");
        } catch (Exception e) {
            return 42;
        }
        counter.addObservedObject(first);
        if (counter.getObservedObjects().length != 1) return 43;
        if (!counter.containsObservedObject(first)) return 44;
        if (counter.getObservedObject() != first) return 45;
        // Agregar el mismo dos veces no lo duplica.
        counter.addObservedObject(first);
        if (counter.getObservedObjects().length != 1) return 46;
        counter.addObservedObject(second);
        if (counter.getObservedObjects().length != 2) return 47;
        // El getter en singular devuelve el primero, no el ultimo.
        if (counter.getObservedObject() != first) return 48;
        counter.removeObservedObject(first);
        if (counter.getObservedObjects().length != 1) return 49;
        if (counter.getObservedObject() != second) return 50;
        // Sacar algo que no esta no es un error.
        counter.removeObservedObject(first);
        if (counter.getObservedObjects().length != 1) return 51;
        // El setter en singular reemplaza la lista entera.
        counter.setObservedObject(first);
        if (counter.getObservedObjects().length != 1) return 52;
        if (counter.getObservedObject() != first) return 53;

        // --- lo que cada monitor declara que manda ---
        MBeanNotificationInfo[] counterInfo = counter.getNotificationInfo();
        if (counterInfo.length != 1) return 54;
        if (!"javax.management.monitor.MonitorNotification".equals(counterInfo[0].getName())) {
            return 55;
        }
        if (counterInfo[0].getNotifTypes().length != 6) return 56;
        if (!has(counterInfo[0].getNotifTypes(), MonitorNotification.THRESHOLD_VALUE_EXCEEDED)) {
            return 57;
        }
        // El de contadores no anuncia los disparos del medidor.
        if (has(counterInfo[0].getNotifTypes(), MonitorNotification.THRESHOLD_HIGH_VALUE_EXCEEDED)) {
            return 58;
        }
        MBeanNotificationInfo[] gaugeInfo = gauge.getNotificationInfo();
        if (gaugeInfo[0].getNotifTypes().length != 7) return 59;
        if (!has(gaugeInfo[0].getNotifTypes(), MonitorNotification.THRESHOLD_HIGH_VALUE_EXCEEDED)) {
            return 60;
        }
        if (!has(gaugeInfo[0].getNotifTypes(), MonitorNotification.THRESHOLD_LOW_VALUE_EXCEEDED)) {
            return 61;
        }
        MBeanNotificationInfo[] stringInfo = string.getNotificationInfo();
        // El de cadenas no anuncia THRESHOLD_ERROR: no tiene umbral que pueda estar mal.
        if (stringInfo[0].getNotifTypes().length != 6) return 62;
        if (has(stringInfo[0].getNotifTypes(), MonitorNotification.THRESHOLD_ERROR)) return 63;
        if (!has(stringInfo[0].getNotifTypes(),
                 MonitorNotification.STRING_TO_COMPARE_VALUE_MATCHED)) return 64;

        // --- los tipos de aviso son parte del protocolo ---
        if (!"jmx.monitor.error.mbean".equals(MonitorNotification.OBSERVED_OBJECT_ERROR)) return 65;
        if (!"jmx.monitor.error.attribute"
                .equals(MonitorNotification.OBSERVED_ATTRIBUTE_ERROR)) return 66;
        if (!"jmx.monitor.error.type"
                .equals(MonitorNotification.OBSERVED_ATTRIBUTE_TYPE_ERROR)) return 67;
        if (!"jmx.monitor.error.threshold".equals(MonitorNotification.THRESHOLD_ERROR)) return 68;
        if (!"jmx.monitor.error.runtime".equals(MonitorNotification.RUNTIME_ERROR)) return 69;
        if (!"jmx.monitor.counter.threshold"
                .equals(MonitorNotification.THRESHOLD_VALUE_EXCEEDED)) return 70;
        if (!"jmx.monitor.gauge.high"
                .equals(MonitorNotification.THRESHOLD_HIGH_VALUE_EXCEEDED)) return 71;
        if (!"jmx.monitor.gauge.low"
                .equals(MonitorNotification.THRESHOLD_LOW_VALUE_EXCEEDED)) return 72;
        if (!"jmx.monitor.string.matches"
                .equals(MonitorNotification.STRING_TO_COMPARE_VALUE_MATCHED)) return 73;
        if (!"jmx.monitor.string.differs"
                .equals(MonitorNotification.STRING_TO_COMPARE_VALUE_DIFFERED)) return 74;

        // MonitorSettingException es no comprobada: la lanza el hilo del monitor.
        if (!(new MonitorSettingException() instanceof RuntimeException)) return 75;
        if (!"mal".equals(new MonitorSettingException("mal").getMessage())) return 76;

        // Arrancar y parar no necesita nada configurado.
        counter.start();
        if (!counter.isActive()) return 77;
        counter.stop();
        if (counter.isActive()) return 78;

        // --- javax.security.sasl ---
        if (!"javax.security.sasl.qop".equals(Sasl.QOP)) return 79;
        if (!"javax.security.sasl.strength".equals(Sasl.STRENGTH)) return 80;
        if (!"javax.security.sasl.server.authentication".equals(Sasl.SERVER_AUTH)) return 81;
        if (!"javax.security.sasl.bound.server.name".equals(Sasl.BOUND_SERVER_NAME)) return 82;
        if (!"javax.security.sasl.maxbuffer".equals(Sasl.MAX_BUFFER)) return 83;
        if (!"javax.security.sasl.rawsendsize".equals(Sasl.RAW_SEND_SIZE)) return 84;
        if (!"javax.security.sasl.reuse".equals(Sasl.REUSE)) return 85;
        if (!"javax.security.sasl.policy.noplaintext".equals(Sasl.POLICY_NOPLAINTEXT)) return 86;
        if (!"javax.security.sasl.policy.noactive".equals(Sasl.POLICY_NOACTIVE)) return 87;
        if (!"javax.security.sasl.policy.nodictionary".equals(Sasl.POLICY_NODICTIONARY)) return 88;
        if (!"javax.security.sasl.policy.noanonymous".equals(Sasl.POLICY_NOANONYMOUS)) return 89;
        if (!"javax.security.sasl.policy.forward".equals(Sasl.POLICY_FORWARD_SECRECY)) return 90;
        if (!"javax.security.sasl.policy.credentials"
                .equals(Sasl.POLICY_PASS_CREDENTIALS)) return 91;
        if (!"javax.security.sasl.credentials".equals(Sasl.CREDENTIALS)) return 92;

        // Un mecanismo que nadie implementa da null, no una excepcion.
        try {
            if (Sasl.createSaslClient(new String[] {"NO-EXISTE"}, null, "ldap", "host", null, null)
                    != null) return 93;
            if (Sasl.createSaslServer("NO-EXISTE", "ldap", "host", null, null) != null) return 94;
        } catch (SaslException e) {
            return 95;
        }
        // La enumeracion existe siempre, aunque venga vacia.
        Enumeration<SaslClientFactory> factories = Sasl.getSaslClientFactories();
        if (factories == null) return 96;
        if (Sasl.getSaslServerFactories() == null) return 97;

        // SaslException es una IOException: va adentro de un protocolo.
        SaslException plain = new SaslException("detalle");
        if (!(plain instanceof java.io.IOException)) return 98;
        if (plain.getCause() != null) return 99;
        if (!"detalle".equals(plain.getMessage())) return 100;
        if (!"javax.security.sasl.SaslException: detalle".equals(plain.toString())) return 101;
        // Con causa, el toString la muestra entre corchetes.
        Throwable inner = new IllegalStateException("adentro");
        SaslException wrapped = new SaslException("detalle", inner);
        if (wrapped.getCause() != inner) return 102;
        if (!("javax.security.sasl.SaslException: detalle [Caused by "
                + inner.toString() + "]").equals(wrapped.toString())) return 103;
        // Una causa ya puesta no se puede reemplazar.
        try {
            wrapped.initCause(new RuntimeException("otra"));
            return 104;
        } catch (IllegalStateException expected) {
            // asi tiene que ser
        }
        if (!(new AuthenticationException("x") instanceof SaslException)) return 105;

        // --- AuthorizeCallback: autenticar no es autorizar ---
        AuthorizeCallback ac = new AuthorizeCallback("quien", "como");
        if (!"quien".equals(ac.getAuthenticationID())) return 106;
        if (!"como".equals(ac.getAuthorizationID())) return 107;
        if (ac.isAuthorized()) return 108;
        // Sin autorizar, el identificador es null aunque el de autorizacion exista.
        if (ac.getAuthorizedID() != null) return 109;
        // Poner el reescrito no autoriza por si solo.
        ac.setAuthorizedID("directo");
        if (ac.isAuthorized()) return 110;
        if (ac.getAuthorizedID() != null) return 111;
        ac.setAuthorized(true);
        if (!"directo".equals(ac.getAuthorizedID())) return 112;
        // Sin reescribir, el autorizado es el de autorizacion.
        AuthorizeCallback plainAc = new AuthorizeCallback("quien", "como");
        plainAc.setAuthorized(true);
        if (!"como".equals(plainAc.getAuthorizedID())) return 113;
        // Desautorizar lo vuelve a null: no queda un identificador huerfano.
        plainAc.setAuthorized(false);
        if (plainAc.getAuthorizedID() != null) return 114;

        // --- los callbacks de dominio son los de auth.callback, con otro tipo ---
        RealmCallback rc = new RealmCallback("dominio?");
        if (!"dominio?".equals(rc.getPrompt())) return 115;
        if (rc.getDefaultText() != null) return 116;
        RealmCallback withDefault = new RealmCallback("dominio?", "EJEMPLO");
        if (!"EJEMPLO".equals(withDefault.getDefaultText())) return 117;
        if (!(rc instanceof javax.security.auth.callback.TextInputCallback)) return 118;
        RealmChoiceCallback choice = new RealmChoiceCallback(
            "cual?", new String[] {"UNO", "DOS"}, 1, false);
        if (choice.getChoices().length != 2) return 119;
        if (choice.getDefaultChoice() != 1) return 120;
        if (choice.allowMultipleSelections()) return 121;
        if (!(choice instanceof javax.security.auth.callback.ChoiceCallback)) return 122;

        return -1;
    }

    /** Si esa operacion se rechaza con IllegalArgumentException. */
    static boolean rejects(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    /** Si ese arreglo contiene esa cadena. */
    static boolean has(String[] all, String wanted) {
        int i = 0;
        while (i < all.length) {
            if (wanted.equals(all[i])) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
