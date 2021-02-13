package de.uni.augsburg.hcai.ssj.consumer;

import androidx.annotation.NonNull;

import de.uni.augsburg.hcai.ssj.RxSsjEvent;
import io.reactivex.rxjava3.functions.Consumer;
import timber.log.Timber;

/**
 * This enum provides all available consumers.
 *
 * @author Fabio Hellmann
 */
public enum RxLoggerConsumers implements RxSsjConsumer {
    /**
     * A simple logger that logs all incoming events to the console as INFO.
     */
    LOG_INFO(event -> Timber.i(event.toString())),
    /**
     * A simple logger that logs all incoming events to the console as DEBUG.
     */
    LOG_DEBUG(event -> Timber.d(event.toString())),
    /**
     * A simple logger that logs all incoming events to the console as WARNING.
     */
    LOG_WARN(event -> Timber.w(event.toString())),
    /**
     * A simple logger that logs all incoming events to the console as ERROR.
     */
    LOG_ERROR(event -> Timber.e(event.toString()));

    @NonNull
    private final Consumer<RxSsjEvent> mConsumer;

    RxLoggerConsumers(@NonNull final Consumer<RxSsjEvent> consumer) {
        mConsumer = consumer;
    }

    @Override
    public void accept(RxSsjEvent rxSensorEvent) throws Throwable {
        mConsumer.accept(rxSensorEvent);
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public int getId() {
        return ordinal();
    }
}
