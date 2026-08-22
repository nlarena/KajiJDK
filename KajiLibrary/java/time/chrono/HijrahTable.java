package java.time.chrono;

// KajiLibrary's Umm al-Qura tabular data for the Hijrah calendar — the part of HijrahChronology
// that is DATA, not arithmetic.
//
// The Islamic calendar is lunar: a month is a real lunar cycle, so it runs 29 or 30 days and the
// pattern does NOT repeat on any short cycle. There is no formula; there is a table. The Umm
// al-Qura variant (the civil calendar of Saudi Arabia) tabulates years 1300-1600 AH, and
// outside that range no Hijrah date is defined — which is why the range is a hard error rather
// than an extrapolation.
//
// STORAGE: one 12-bit mask per year, bit (m-1) set when month m has 30 days. Everything else is
// derived, which was verified against the JDK before compressing:
//   - length of month m  = 29 + bit
//   - length of the year = 348 + popcount(mask)   (12 x 29 = 348)
//   - first day of year  = the anchor, plus every preceding year's length
// So the whole table is 301 ints instead of three parallel arrays.
//
// The data was extracted by running the JDK's own HijrahChronology, not transcribed from a
// reference: see the generator in the session scratchpad.
final class HijrahTable {

    // Bit (m-1) is set when month m of that year has 30 days. Index 0 is year 1300 AH.
    private static final int[] MONTH_MASKS = {
        1365, 683, 2359, 694, 1398, 876, 2901, 2730, 2390, 1182, 2397, 698,
        1461, 938, 2891, 2710, 1326, 685, 1389, 2906, 1874, 3877, 3722, 3350,
        2646, 2741, 1716, 3497, 2962, 2853, 1611, 2715, 858, 1753, 1492, 3493,
        3402, 2709, 1334, 2421, 756, 1769, 1748, 1705, 1333, 605, 1213, 2490,
        948, 2921, 2858, 2645, 1197, 2653, 730, 1753, 3754, 3732, 3370, 3158,
        1198, 2669, 1386, 3413, 3402, 2707, 1323, 2651, 1338, 1717, 3753, 3410,
        3369, 2645, 1197, 1389, 2794, 1764, 3793, 3490, 2730, 2394, 730, 1465,
        2994, 1892, 1737, 1365, 683, 1243, 2746, 1460, 3497, 3410, 2725, 2349,
        621, 2285, 730, 2773, 2725, 2635, 1175, 2359, 694, 2421, 3433, 3410,
        3221, 2347, 603, 1243, 2517, 1490, 3493, 3402, 2709, 1357, 2733, 938,
        3026, 3012, 2953, 2709, 1325, 1453, 2922, 1748, 3529, 3474, 2726, 2390,
        686, 1389, 874, 2901, 2730, 2381, 1181, 2397, 698, 1461, 1450, 3413,
        2714, 2350, 622, 1373, 2778, 1748, 1701, 2855, 2637, 1197, 1389, 2906,
        1876, 3913, 3730, 3366, 2646, 854, 1717, 2986, 2962, 2853, 1675, 2715,
        1370, 2778, 1460, 3497, 2898, 2714, 1334, 630, 1397, 2802, 1748, 1705,
        1365, 685, 1213, 2490, 1396, 2921, 2898, 2709, 1325, 2653, 1242, 2777,
        1714, 3733, 3626, 3222, 2350, 2733, 1386, 3429, 3402, 3349, 1579, 3163,
        1338, 1717, 3506, 3428, 3369, 2645, 1197, 2413, 2794, 1768, 3793, 3492,
        3402, 2666, 730, 1465, 2930, 2920, 1745, 1621, 1195, 2395, 698, 1461,
        3497, 3410, 3238, 2382, 1134, 2397, 1242, 2773, 2730, 2637, 1179, 2359,
        1206, 2421, 3434, 3410, 2725, 2379, 683, 1371, 2777, 1490, 3525, 3474,
        2853, 1365, 2741, 1460, 2985, 1954, 1861, 1427, 2731, 1238, 2518, 1490,
        2981, 2890, 2709, 1197, 349, 733, 2522, 1460, 1449, 1325, 603, 2231,
        374, 1389, 2922, 2762, 2710, 1323, 347, 699, 1462, 3498, 2964, 3398,
        2701, 1325, 2717, 1370, 1877, 1865, 3859, 3658, 2710, 1366, 1717, 2986,
        2964
    };

    // Cumulative first-epoch-day per year, built once. An array is used rather than `static final
    // long` constants because a static-final primitive reads back as 0 at runtime (finding #112);
    // an array is an object and its `<clinit>` runs normally.
    private static final long[] YEAR_START = HijrahTable.buildYearStarts();

    private HijrahTable() {
    }

    static int minYear() {
        return 1300;
    }

    static int maxYear() {
        return 1600;
    }

    private static long anchorEpochDay() {
        return -31826L;
    }

    private static long[] buildYearStarts() {
        long[] starts = new long[MONTH_MASKS.length + 1];
        long day = -31826L;
        int i = 0;
        while (i < MONTH_MASKS.length) {
            starts[i] = day;
            day = day + (long) HijrahTable.yearLengthAt(i);
            i = i + 1;
        }
        starts[MONTH_MASKS.length] = day;
        return starts;
    }

    private static int popCount12(int mask) {
        int count = 0;
        int bit = 0;
        while (bit < 12) {
            if ((mask & (1 << bit)) != 0) {
                count = count + 1;
            }
            bit = bit + 1;
        }
        return count;
    }

    private static int yearLengthAt(int index) {
        return 348 + HijrahTable.popCount12(MONTH_MASKS[index]);
    }

    static boolean isSupportedYear(int year) {
        return year >= HijrahTable.minYear() && year <= HijrahTable.maxYear();
    }

    static int lengthOfYear(int year) {
        HijrahTable.checkYear(year);
        return HijrahTable.yearLengthAt(year - HijrahTable.minYear());
    }

    // A Hijrah year is "leap" when it runs 355 days instead of 354 — the extra day always lands in
    // the twelfth month.
    static boolean isLeapYear(int year) {
        return HijrahTable.lengthOfYear(year) == 355;
    }

    static int lengthOfMonth(int year, int month) {
        HijrahTable.checkYear(year);
        int mask = MONTH_MASKS[year - HijrahTable.minYear()];
        int len = 29;
        if ((mask & (1 << (month - 1))) != 0) {
            len = 30;
        }
        return len;
    }

    static long epochDayOf(int year, int month, int dayOfMonth) {
        HijrahTable.checkYear(year);
        long day = YEAR_START[year - HijrahTable.minYear()];
        int m = 1;
        while (m < month) {
            day = day + (long) HijrahTable.lengthOfMonth(year, m);
            m = m + 1;
        }
        return day + (long) (dayOfMonth - 1);
    }

    // The inverse: epoch day back to (year, month, day). Linear from the year found by scanning the
    // cumulative table — the table is only 301 entries, so a scan is honest and simple.
    static int yearOfEpochDay(long epochDay) {
        if (epochDay < YEAR_START[0] || epochDay >= YEAR_START[MONTH_MASKS.length]) {
            throw new java.time.DateTimeException("Hijrah date out of range");
        }
        int i = 0;
        int found = HijrahTable.minYear();
        while (i < MONTH_MASKS.length) {
            if (epochDay < YEAR_START[i + 1]) {
                found = HijrahTable.minYear() + i;
                i = MONTH_MASKS.length;
            } else {
                i = i + 1;
            }
        }
        return found;
    }

    static int monthOfEpochDay(long epochDay) {
        int year = HijrahTable.yearOfEpochDay(epochDay);
        long rest = epochDay - YEAR_START[year - HijrahTable.minYear()];
        int month = 1;
        while (month <= 12) {
            int len = HijrahTable.lengthOfMonth(year, month);
            if (rest < (long) len) {
                return month;
            }
            rest = rest - (long) len;
            month = month + 1;
        }
        return 12;
    }

    static int dayOfEpochDay(long epochDay) {
        int year = HijrahTable.yearOfEpochDay(epochDay);
        long rest = epochDay - YEAR_START[year - HijrahTable.minYear()];
        int month = 1;
        while (month <= 12) {
            int len = HijrahTable.lengthOfMonth(year, month);
            if (rest < (long) len) {
                return (int) rest + 1;
            }
            rest = rest - (long) len;
            month = month + 1;
        }
        return 1;
    }

    private static void checkYear(int year) {
        if (!HijrahTable.isSupportedYear(year)) {
            throw new java.time.DateTimeException("Hijrah year out of range: " + year);
        }
    }
}
