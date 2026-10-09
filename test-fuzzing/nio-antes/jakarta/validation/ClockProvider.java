package jakarta.validation;

import java.time.Clock;

// KajiLibrary's jakarta.validation.ClockProvider — supplies the clock used for time-based constraints.
public interface ClockProvider {

    Clock getClock();
}
