package java.util;

import java.io.Serializable;
import java.util.stream.Stream;

// Una moneda ISO 4217: su codigo de tres letras, su codigo numerico y cuantos decimales usa.
//
// Es la respuesta a "cuantos decimales tiene esta plata", que es la unica pregunta que el formateo
// de numeros de `java.text` le hace. Yen y won no tienen ninguno; dinar kuwaiti tiene tres. Un
// formateador que asuma dos se equivoca en ambos extremos.
//
// **Instancias unicas**: `getInstance` devuelve siempre el mismo objeto para el mismo codigo, asi
// que `==` funciona. Es del contrato del JDK, no una optimizacion.
//
// A KajiLibrary subset, y conviene tenerlo claro antes de usarla:
//
//   - La tabla es de las **59 monedas** de abajo, no de las ~180 de la norma. El JDK trae la tabla
//     entera en un archivo binario; replicarla es un problema de datos, no de codigo. Un codigo
//     bien formado que no este en la tabla se rechaza con IllegalArgumentException, que es lo que
//     el JDK hace con un codigo que no conoce.
//   - `getSymbol()` y `getDisplayName()` devuelven el **codigo**. El JDK los saca de los bundles
//     de locale, que aca no existen — la misma decision que ya tomo `TimeZone.getDisplayName`. Es
//     menos amable y nunca miente sobre que moneda es.
//   - `getInstance(Locale)` cubre los paises de las monedas de la tabla.
public final class Currency implements Serializable {

    // La tabla, en tres arreglos paralelos: `CODIGOS[i]` tiene el codigo numerico `NUMERICOS[i]`
    // y usa `DECIMALES[i]` decimales.
    //
    // Paralelos y no un `Object[]` de tripletes: no hay boxeo, no hay casts y no hay aritmetica
    // de indices de por medio.
    //
    // La forma con `Object[]` mezclado fue lo que destapo el finding #289 —el inicializador no
    // boxeaba los enteros y emitia un `aastore` con un `int` crudo—, pero eso ya esta arreglado.
    // Esta forma se conserva porque es mejor por si misma, no por el defecto.
    //
    // Los decimales son lo que tiene que ser exacto. Casi todas usan 2; las que no son las que
    // rompen a quien asume: 0 para las que no fraccionan (JPY, KRW, CLP, ISK, VND, UGX, RWF, XOF,
    // XAF, PYG, VUV, KMF, DJF, GNF) y 3 para los dinares (BHD, IQD, JOD, KWD, LYD, OMR, TND).
    private static final String[] CODIGOS = {
        "AED", "ARS", "AUD", "BGN", "BHD", "BRL", "CAD", "CHF", "CLP", "CNY",
        "COP", "CZK", "DJF", "DKK", "EGP", "EUR", "GBP", "GNF", "HKD", "HUF",
        "IDR", "ILS", "INR", "IQD", "ISK", "JOD", "JPY", "KMF", "KRW", "KWD",
        "LYD", "MXN", "MYR", "NOK", "NZD", "OMR", "PEN", "PHP", "PLN", "PYG",
        "RON", "RUB", "RWF", "SAR", "SEK", "SGD", "THB", "TND", "TRY", "TWD",
        "UAH", "UGX", "USD", "UYU", "VND", "VUV", "XAF", "XOF", "ZAR",
    };

    private static final int[] NUMERICOS = {
        784,  32,  36, 975,  48, 986, 124, 756, 152, 156,
        170, 203, 262, 208, 818, 978, 826, 324, 344, 348,
        360, 376, 356, 368, 352, 400, 392, 174, 410, 414,
        434, 484, 458, 578, 554, 512, 604, 608, 985, 600,
        946, 643, 646, 682, 752, 702, 764, 788, 949, 901,
        980, 800, 840, 858, 704, 548, 950, 952, 710,
    };

    private static final int[] DECIMALES = {
          2,   2,   2,   2,   3,   2,   2,   2,   0,   2,
          2,   2,   0,   2,   2,   2,   2,   0,   2,   2,
          2,   2,   2,   3,   0,   3,   0,   0,   0,   3,
          3,   2,   2,   2,   2,   3,   2,   2,   2,   0,
          2,   2,   0,   2,   2,   2,   2,   3,   2,   2,
          2,   0,   2,   2,   0,   0,   0,   0,   2,
    };

    // Pais ISO 3166 -> codigo de moneda, en pares. Cubre los paises de la tabla de arriba.
    private static final String[] PAISES = {
        "AE", "AED", "AR", "ARS", "AT", "EUR", "AU", "AUD", "BE", "EUR",
        "BG", "BGN", "BH", "BHD", "BR", "BRL", "CA", "CAD", "CH", "CHF",
        "CL", "CLP", "CN", "CNY", "CO", "COP", "CZ", "CZK", "DE", "EUR",
        "DK", "DKK", "EG", "EGP", "ES", "EUR", "FI", "EUR", "FR", "EUR",
        "GB", "GBP", "GR", "EUR", "HK", "HKD", "HU", "HUF", "ID", "IDR",
        "IE", "EUR", "IL", "ILS", "IN", "INR", "IQ", "IQD", "IS", "ISK",
        "IT", "EUR", "JO", "JOD", "JP", "JPY", "KR", "KRW", "KW", "KWD",
        "LY", "LYD", "MX", "MXN", "MY", "MYR", "NL", "EUR", "NO", "NOK",
        "NZ", "NZD", "OM", "OMR", "PE", "PEN", "PH", "PHP", "PL", "PLN",
        "PT", "EUR", "PY", "PYG", "RO", "RON", "RU", "RUB", "SA", "SAR",
        "SE", "SEK", "SG", "SGD", "TH", "THB", "TN", "TND", "TR", "TRY",
        "TW", "TWD", "UA", "UAH", "US", "USD", "UY", "UYU", "VN", "VND",
        "ZA", "ZAR",
    };

    // Las instancias ya entregadas, para que `==` funcione. La clave es el codigo.
    private static final HashMap<String, Currency> CACHE = new HashMap<String, Currency>();

    private final String currencyCode;
    private final int numericCode;
    private final int defaultFractionDigits;

    private Currency(String currencyCode, int numericCode, int defaultFractionDigits) {
        this.currencyCode = currencyCode;
        this.numericCode = numericCode;
        this.defaultFractionDigits = defaultFractionDigits;
    }

    // La moneda del codigo ISO 4217 dado.
    //
    // Siempre la MISMA instancia para el mismo codigo. Un codigo desconocido —o que no sean tres
    // letras— es IllegalArgumentException, no null: pedir una moneda que no existe es un error del
    // llamador, y devolverle null lo movería al primer uso.
    public static Currency getInstance(String currencyCode) {
        if (currencyCode == null) {
            throw new NullPointerException();
        }
        synchronized (CACHE) {
            Currency ya = CACHE.get(currencyCode);
            if (ya != null) {
                return ya;
            }
            int i = 0;
            while (i < CODIGOS.length) {
                if (CODIGOS[i].equals(currencyCode)) {
                    Currency c = new Currency(currencyCode, NUMERICOS[i], DECIMALES[i]);
                    CACHE.put(currencyCode, c);
                    return c;
                }
                i = i + 1;
            }
        }
        throw new IllegalArgumentException(currencyCode);
    }

    // La moneda del pais de `locale`.
    public static Currency getInstance(Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        String pais = locale.getCountry();
        if (pais.length() != 2) {
            throw new IllegalArgumentException(
                "The country of the argument locale is not a supported ISO 3166 country code.");
        }
        int i = 0;
        while (i < PAISES.length) {
            if (PAISES[i].equals(pais)) {
                return getInstance(PAISES[i + 1]);
            }
            i = i + 2;
        }
        throw new IllegalArgumentException(
            "The country of the argument locale is not a supported ISO 3166 country code.");
    }

    // Todas las monedas que esta biblioteca conoce.
    public static Set<Currency> getAvailableCurrencies() {
        HashSet<Currency> out = new HashSet<Currency>();
        int i = 0;
        while (i < CODIGOS.length) {
            out.add(getInstance(CODIGOS[i]));
            i = i + 1;
        }
        return out;
    }

    // Lo mismo, como stream.
    public static Stream<Currency> availableCurrencies() {
        Set<Currency> todas = getAvailableCurrencies();
        Object[] a = new Object[todas.size()];
        int i = 0;
        Iterator<Currency> it = todas.iterator();
        while (it.hasNext()) {
            a[i] = it.next();
            i = i + 1;
        }
        return (Stream<Currency>) Stream.of(a);
    }

    // El codigo ISO 4217 de tres letras.
    public String getCurrencyCode() {
        return this.currencyCode;
    }

    // El simbolo en el locale por defecto. A KajiLibrary subset: devuelve el codigo.
    public String getSymbol() {
        return this.currencyCode;
    }

    // El simbolo en el locale dado. A KajiLibrary subset: devuelve el codigo.
    public String getSymbol(Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        return this.currencyCode;
    }

    // Cuantos decimales usa esta moneda: 2 para casi todas, 0 para el yen, 3 para los dinares.
    public int getDefaultFractionDigits() {
        return this.defaultFractionDigits;
    }

    // El codigo numerico ISO 4217.
    public int getNumericCode() {
        return this.numericCode;
    }

    // El codigo numerico con tres digitos, rellenado con ceros ("032" para el peso argentino).
    public String getNumericCodeAsString() {
        String s = "" + this.numericCode;
        while (s.length() < 3) {
            s = "0" + s;
        }
        return s;
    }

    // El nombre en el locale por defecto. A KajiLibrary subset: devuelve el codigo.
    public String getDisplayName() {
        return this.currencyCode;
    }

    // El nombre en el locale dado. A KajiLibrary subset: devuelve el codigo.
    public String getDisplayName(Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        return this.currencyCode;
    }

    // El codigo ISO 4217, que es lo que el JDK imprime.
    public String toString() {
        return this.currencyCode;
    }
}
