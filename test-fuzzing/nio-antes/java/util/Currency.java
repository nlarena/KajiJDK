package java.util;

import java.io.Serializable;
import java.util.stream.Stream;

// An ISO 4217 currency: its three-letter code, its numeric code and how many decimals it uses.
//
// It is the answer to "how many decimals does this money have", which is the only question
// `java.text`'s number formatting asks it. Yen and won have none; the Kuwaiti dinar has three. A
// formatter that assumes two is wrong at both ends.
//
// **Unique instances**: `getInstance` always returns the same object for the same code, so `==`
// works. It is from the JDK's contract, not an optimisation.
//
// A KajiLibrary subset, and it is worth being clear about before using it:
//
//   - The table is of the **59 currencies** below, not the ~180 of the standard. The JDK carries the
//     whole table in a binary file; replicating it is a data problem, not a code one. A well-formed
//     code that is not in the table is rejected with IllegalArgumentException, which is what the JDK
//     does with a code it does not know.
//   - `getSymbol()` and `getDisplayName()` return the **code**. The JDK takes them from the locale
//     bundles, which are not here — the same decision `TimeZone.getDisplayName` already took. It is
//     less friendly and it never lies about which currency it is.
//   - `getInstance(Locale)` covers the countries of the table's currencies.
public final class Currency implements Serializable {

    // The table, in three parallel arrays: `CODES[i]` has the numeric code `NUMERIC_CODES[i]` and uses
    // `DECIMALS[i]` decimals.
    //
    // Parallel and not an `Object[]` of triples: there is no boxing, no casts and no index arithmetic
    // in between.
    //
    // The mixed `Object[]` form is what uncovered finding #289 —the initialiser did not box the
    // integers and emitted an `aastore` with a raw `int`— but that is fixed. This form is kept
    // because it is better in itself, not because of the defect.
    //
    // The decimals are what has to be exact. Almost all use 2; the ones that do not are the ones that
    // break whoever assumes: 0 for those that do not fraction (JPY, KRW, CLP, ISK, VND, UGX, RWF,
    // XOF, XAF, PYG, VUV, KMF, DJF, GNF) and 3 for the dinars (BHD, IQD, JOD, KWD, LYD, OMR, TND).
    private static final String[] CODES = {
        "AED", "ARS", "AUD", "BGN", "BHD", "BRL", "CAD", "CHF", "CLP", "CNY",
        "COP", "CZK", "DJF", "DKK", "EGP", "EUR", "GBP", "GNF", "HKD", "HUF",
        "IDR", "ILS", "INR", "IQD", "ISK", "JOD", "JPY", "KMF", "KRW", "KWD",
        "LYD", "MXN", "MYR", "NOK", "NZD", "OMR", "PEN", "PHP", "PLN", "PYG",
        "RON", "RUB", "RWF", "SAR", "SEK", "SGD", "THB", "TND", "TRY", "TWD",
        "UAH", "UGX", "USD", "UYU", "VND", "VUV", "XAF", "XOF", "ZAR",
    };

    private static final int[] NUMERIC_CODES = {
        784,  32,  36, 975,  48, 986, 124, 756, 152, 156,
        170, 203, 262, 208, 818, 978, 826, 324, 344, 348,
        360, 376, 356, 368, 352, 400, 392, 174, 410, 414,
        434, 484, 458, 578, 554, 512, 604, 608, 985, 600,
        946, 643, 646, 682, 752, 702, 764, 788, 949, 901,
        980, 800, 840, 858, 704, 548, 950, 952, 710,
    };

    private static final int[] DECIMALS = {
          2,   2,   2,   2,   3,   2,   2,   2,   0,   2,
          2,   2,   0,   2,   2,   2,   2,   0,   2,   2,
          2,   2,   2,   3,   0,   3,   0,   0,   0,   3,
          3,   2,   2,   2,   2,   3,   2,   2,   2,   0,
          2,   2,   0,   2,   2,   2,   2,   3,   2,   2,
          2,   0,   2,   2,   0,   0,   0,   0,   2,
    };

    // ISO 3166 country -> currency code, in pairs. It covers the countries of the table above.
    private static final String[] COUNTRIES = {
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

    // The instances already handed out, so `==` works. The key is the code.
    private static final HashMap<String, Currency> CACHE = new HashMap<String, Currency>();

    private final String currencyCode;
    private final int numericCode;
    private final int defaultFractionDigits;

    private Currency(String currencyCode, int numericCode, int defaultFractionDigits) {
        this.currencyCode = currencyCode;
        this.numericCode = numericCode;
        this.defaultFractionDigits = defaultFractionDigits;
    }

    // The currency of the given ISO 4217 code.
    //
    // Always the SAME instance for the same code. An unknown code —or one that is not three letters—
    // is IllegalArgumentException, not null: asking for a currency that does not exist is the
    // caller's error, and returning null would move it to the first use.
    public static Currency getInstance(String currencyCode) {
        if (currencyCode == null) {
            throw new NullPointerException();
        }
        synchronized (CACHE) {
            Currency cached = CACHE.get(currencyCode);
            if (cached != null) {
                return cached;
            }
            int i = 0;
            while (i < CODES.length) {
                if (CODES[i].equals(currencyCode)) {
                    Currency c = new Currency(currencyCode, NUMERIC_CODES[i], DECIMALS[i]);
                    CACHE.put(currencyCode, c);
                    return c;
                }
                i = i + 1;
            }
        }
        throw new IllegalArgumentException(currencyCode);
    }

    // The currency of `locale`'s country.
    public static Currency getInstance(Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        String country = locale.getCountry();
        if (country.length() != 2) {
            throw new IllegalArgumentException(
                "The country of the argument locale is not a supported ISO 3166 country code.");
        }
        int i = 0;
        while (i < COUNTRIES.length) {
            if (COUNTRIES[i].equals(country)) {
                return getInstance(COUNTRIES[i + 1]);
            }
            i = i + 2;
        }
        throw new IllegalArgumentException(
            "The country of the argument locale is not a supported ISO 3166 country code.");
    }

    // Every currency this library knows.
    public static Set<Currency> getAvailableCurrencies() {
        HashSet<Currency> out = new HashSet<Currency>();
        int i = 0;
        while (i < CODES.length) {
            out.add(getInstance(CODES[i]));
            i = i + 1;
        }
        return out;
    }

    // The same, as a stream.
    public static Stream<Currency> availableCurrencies() {
        Set<Currency> all = getAvailableCurrencies();
        Object[] a = new Object[all.size()];
        int i = 0;
        Iterator<Currency> it = all.iterator();
        while (it.hasNext()) {
            a[i] = it.next();
            i = i + 1;
        }
        return (Stream<Currency>) Stream.of(a);
    }

    // The three-letter ISO 4217 code.
    public String getCurrencyCode() {
        return this.currencyCode;
    }

    // The symbol in the default locale. A KajiLibrary subset: it returns the code.
    public String getSymbol() {
        return this.currencyCode;
    }

    // The symbol in the given locale. A KajiLibrary subset: it returns the code.
    public String getSymbol(Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        return this.currencyCode;
    }

    // How many decimals this currency uses: 2 for almost all, 0 for the yen, 3 for the dinars.
    public int getDefaultFractionDigits() {
        return this.defaultFractionDigits;
    }

    // The numeric ISO 4217 code.
    public int getNumericCode() {
        return this.numericCode;
    }

    // The numeric code with three digits, padded with zeros ("032" for the Argentine peso).
    public String getNumericCodeAsString() {
        String s = "" + this.numericCode;
        while (s.length() < 3) {
            s = "0" + s;
        }
        return s;
    }

    // The name in the default locale. A KajiLibrary subset: it returns the code.
    public String getDisplayName() {
        return this.currencyCode;
    }

    // The name in the given locale. A KajiLibrary subset: it returns the code.
    public String getDisplayName(Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        return this.currencyCode;
    }

    // The ISO 4217 code, which is what the JDK prints.
    public String toString() {
        return this.currencyCode;
    }
}
