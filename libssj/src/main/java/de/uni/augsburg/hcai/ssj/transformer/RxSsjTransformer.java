package de.uni.augsburg.hcai.ssj.transformer;

import de.uni.augsburg.hcai.ssj.RxSsjEvent;
import io.reactivex.rxjava3.core.FlowableTransformer;

/**
 * An interface for all transformer classes.
 *
 * @author Fabio Hellmann
 */
public interface RxSsjTransformer extends FlowableTransformer<RxSsjEvent, RxSsjEvent> {
    /**
     * Get the name of the transformer.
     *
     * @return the name.
     */
    String getName();

    /**
     * Get the id of the transformer.
     *
     * @return the id.
     */
    int getId();
}
