/*
 * AndroidTactileFeedback.java
 * Copyright (c) 2018
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura,
 * Vitalijs Krumins, Antonio Grieco
 * *****************************************************
 * This file is part of the Social Signal Interpretation for Java (SSJ) framework
 * developed at the Lab for Human Centered Multimedia of the University of Augsburg.
 *
 * SSJ has been inspired by the SSI (http://openssi.net) framework. SSJ is not a
 * one-to-one port of SSI to Java, it is an approximation. Nor does SSJ pretend
 * to offer SSI's comprehensive functionality and performance (this is java after all).
 * Nevertheless, SSJ borrows a lot of programming patterns from SSI.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package hcm.ssj.feedback;

import android.content.Context;
import android.os.Vibrator;

import java.util.Arrays;

import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;

/**
 * Created by Antonio Grieco on 06.09.2017.
 */

public class AndroidTactileFeedback extends Feedback {
    public final Options options = new Options();
    private Vibrator vibrator = null;

    public AndroidTactileFeedback() {
        _name = "AndroidTactileFeedback";
        Log.d("Instantiated AndroidTactileFeedback " + this.hashCode());
    }

    @Override
    public Feedback.Options getOptions() {
        return options;
    }

    @Override
    public void enterFeedback() throws SSJFatalException {
        if (_evchannel_in == null || _evchannel_in.size() == 0) {
            throw new SSJFatalException("no input channels");
        }

        vibrator = (Vibrator) SSJApplication.getAppContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (!vibrator.hasVibrator()) {
            throw new SSJFatalException("device can't vibrate");
        }
    }

    @Override
    public void notifyFeedback(Event event) {
        Log.i("vibration on android: " + Arrays.toString(options.vibrationPattern.get()));
        vibrator.vibrate(options.vibrationPattern.get(), -1);
    }

    @Override
    public void flush() throws SSJFatalException {
        vibrator.cancel();
    }

    public class Options extends Feedback.Options {

        public final Option<long[]> vibrationPattern = new Option<>("vibrationPattern", new long[]{0, 500}, long[].class, "vibration pattern (starts with delay)");

        private Options() {
            super();
            addOptions();
        }

    }
}
