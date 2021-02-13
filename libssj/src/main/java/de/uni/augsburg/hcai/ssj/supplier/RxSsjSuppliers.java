package de.uni.augsburg.hcai.ssj.supplier;

import android.content.Context;
import android.hardware.Sensor;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import de.uni.augsburg.hcai.ssj.RxSsjEvent;
import de.uni.augsburg.hcai.ssj.supplier.helper.RxAndroidSensorHelper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Function;

/**
 * This enum provides all available suppliers.
 *
 * @author Fabio Hellmann
 */
public enum RxSsjSuppliers implements RxSsjSupplier {
    ANDROID_ACCELEROMETER(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_ACCELEROMETER)),
    @RequiresApi(api = Build.VERSION_CODES.O)
    ANDROID_ACCELEROMETER_UNCALIBRATED(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_ACCELEROMETER_UNCALIBRATED)),
    ANDROID_AMBIENT_TEMPERATURE(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_AMBIENT_TEMPERATURE)),
    @RequiresApi(api = Build.VERSION_CODES.N)
    ANDROID_DEVICE_PRIVATE_BASE(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_DEVICE_PRIVATE_BASE)),
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    ANDROID_GAME_ROTATION_VECTOR(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_GAME_ROTATION_VECTOR)),
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    ANDROID_GEOMAGNETIC_ROTATION_VECTOR(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)),
    ANDROID_GRAVITY(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_GRAVITY)),
    ANDROID_GYROSCOPE(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_GYROSCOPE)),
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    ANDROID_GYROSCOPE_UNCALIBRATED(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_GYROSCOPE_UNCALIBRATED)),
    @RequiresApi(api = Build.VERSION_CODES.N)
    ANDROID_HEART_BEAT(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_HEART_BEAT)),
    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    ANDROID_HEART_RATE(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_HEART_RATE)),
    ANDROID_LIGHT(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_LIGHT)),
    ANDROID_LINEAR_ACCELERATION(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_LINEAR_ACCELERATION)),
    @RequiresApi(api = Build.VERSION_CODES.O)
    ANDROID_LOW_LATENCY_OFFBODY_DETECT(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)),
    ANDROID_MAGNETIC_FIELD(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_MAGNETIC_FIELD)),
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    ANDROID_MAGNETIC_FIELD_UNCALIBRATED(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED)),
    @RequiresApi(api = Build.VERSION_CODES.N)
    ANDROID_MOTION_DETECT(context -> RxAndroidSensorHelper.observeSensor(context, Sensor.TYPE_MOTION_DETECT));

    @NonNull
    private final Function<Context, Flowable<RxSsjEvent>> mFunction;

    RxSsjSuppliers(@NonNull final Function<Context, Flowable<RxSsjEvent>> function) {
        mFunction = function;
    }

    @Override
    public Flowable<RxSsjEvent> open(@NonNull Context context) throws Throwable {
        return mFunction.apply(context);
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
