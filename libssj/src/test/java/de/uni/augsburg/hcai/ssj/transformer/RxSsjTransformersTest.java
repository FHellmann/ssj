package de.uni.augsburg.hcai.ssj.transformer;

import org.junit.Test;

import de.uni.augsburg.hcai.ssj.RxSsjEvent;
import de.uni.augsburg.hcai.ssj.supplier.RxAndroidSuppliers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class RxSsjTransformersTest {
    @Test
    public void testMin() {
        final RxSsjEvent input = new RxSsjEvent(
                RxAndroidSuppliers.TYPE_ACCELEROMETER,
                -1,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        final RxSsjEvent expected = new RxSsjEvent(
                RxAndroidSuppliers.TYPE_ACCELEROMETER,
                -1,
                0);
        final TestSubscriber<RxSsjEvent> subscriber = new TestSubscriber<>();
        Flowable.fromArray(input)
                .compose(RxMathTransformers.MIN)
                .subscribe(subscriber);
        subscriber.assertComplete();
        subscriber.assertNoErrors();
        subscriber.assertValueCount(1);
        subscriber.assertValue(expected);
    }

    @Test
    public void testMax() {
        final RxSsjEvent input = new RxSsjEvent(
                RxAndroidSuppliers.TYPE_ACCELEROMETER,
                -1,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        final RxSsjEvent expected = new RxSsjEvent(
                RxAndroidSuppliers.TYPE_ACCELEROMETER,
                -1,
                9);
        final TestSubscriber<RxSsjEvent> subscriber = new TestSubscriber<>();
        Flowable.fromArray(input)
                .compose(RxMathTransformers.MAX)
                .subscribe(subscriber);
        subscriber.assertComplete();
        subscriber.assertNoErrors();
        subscriber.assertValueCount(1);
        subscriber.assertValue(expected);
    }

    @Test
    public void testInvert() {
        final RxSsjEvent input = new RxSsjEvent(
                RxAndroidSuppliers.TYPE_ACCELEROMETER,
                -1,
                0f, .1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f, .9f);
        final RxSsjEvent expected = new RxSsjEvent(
                RxAndroidSuppliers.TYPE_ACCELEROMETER,
                -1,
                1f, .9f, .8f, .7f, .6f, .5f, .4f, .3f, .2f, .1f);
        final TestSubscriber<RxSsjEvent> subscriber = new TestSubscriber<>();
        Flowable.fromArray(input)
                .compose(RxMathTransformers.INVERT)
                .subscribe(subscriber);
        subscriber.assertComplete();
        subscriber.assertNoErrors();
        subscriber.assertValueCount(1);
        subscriber.assertValue(expected);
    }

    @Test
    public void testAverage() {
        final RxSsjEvent input = new RxSsjEvent(
                RxAndroidSuppliers.TYPE_ACCELEROMETER,
                -1,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        final RxSsjEvent expected = new RxSsjEvent(
                RxAndroidSuppliers.TYPE_ACCELEROMETER,
                -1,
                4.5f);
        final TestSubscriber<RxSsjEvent> subscriber = new TestSubscriber<>();
        Flowable.fromArray(input)
                .compose(RxMathTransformers.AVERAGE_MEAN)
                .subscribe(subscriber);
        subscriber.assertComplete();
        subscriber.assertNoErrors();
        subscriber.assertValueCount(1);
        subscriber.assertValue(expected);
    }

    @Test
    public void testCount() {
        final RxSsjEvent input = new RxSsjEvent(
                RxAndroidSuppliers.TYPE_ACCELEROMETER,
                -1,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        final RxSsjEvent expected = new RxSsjEvent(
                RxAndroidSuppliers.TYPE_ACCELEROMETER,
                -1,
                10);
        final TestSubscriber<RxSsjEvent> subscriber = new TestSubscriber<>();
        Flowable.fromArray(input)
                .compose(RxMathTransformers.COUNT)
                .subscribe(subscriber);
        subscriber.assertComplete();
        subscriber.assertNoErrors();
        subscriber.assertValueCount(1);
        subscriber.assertValue(expected);
    }
}