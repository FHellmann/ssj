package de.uni.augsburg.hcai.ssj.consumer;

import de.uni.augsburg.hcai.ssj.RxSsjEvent;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * An interface for all sensor consumer classes.
 *
 * @author Fabio Hellmann
 */
public interface RxSsjConsumer extends Consumer<RxSsjEvent> {
    /**
     * Get the name of the consumer.
     *
     * @return the name.
     */
    String getName();

    /**
     * Get the id of the consumer.
     *
     * @return the id.
     */
    int getId();
}
