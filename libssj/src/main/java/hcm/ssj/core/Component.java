/*
 * Component.java
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

package hcm.ssj.core;

import java.util.ArrayList;

import hcm.ssj.core.option.OptionList;

/**
 * Created by Johnny on 05.03.2015.
 */
public abstract class Component implements Runnable {
    public int threadPriority = Cons.THREAD_PRIORIIY_HIGH;
    protected String _name = "Component";
    protected boolean _terminate = false;
    protected boolean _safeToKill = false;
    protected boolean _isSetup = false;
    protected ArrayList<EventChannel> _evchannel_in = null;
    protected EventChannel _evchannel_out = null;

    public void close() {
        Pipeline frame = Pipeline.getInstance();
        Log.i(_name + " shutting down");

        _terminate = true;

        if (_evchannel_in != null)
            for (EventChannel ch : _evchannel_in)
                ch.close();

        if (_evchannel_out != null) _evchannel_out.close();

        double time = frame.getTime();
        while (!_safeToKill) {
            try {
                Thread.sleep(Cons.SLEEP_IN_LOOP);
            } catch (InterruptedException e) {
                Log.w("thread interrupt");
            }

            if (frame.getTime() > time + frame.options.waitThreadKill.get()) {
                Log.w(_name + " force-killed thread");
                forcekill();
                break;
            }
        }
        Log.i(_name + " shut down completed");
    }

    public void forcekill() {
        Thread.currentThread().interrupt();
    }

    public String getComponentName() {
        return _name;
    }

    void addEventChannelIn(EventChannel channel) {
        if (_evchannel_in == null)
            _evchannel_in = new ArrayList<>();

        _evchannel_in.add(channel);
    }

    public EventChannel getEventChannelOut() {
        if (_evchannel_out == null)
            _evchannel_out = new EventChannel();

        return _evchannel_out;
    }

    void setEventChannelOut(EventChannel channel) {
        _evchannel_out = channel;
    }

    /**
     * Resets internal state of components, does not alter references with framework or other components
     * Called by the framework on start-up
     */
    public void reset() {
        _terminate = false;
        _safeToKill = false;

        if (_evchannel_in != null)
            for (EventChannel ch : _evchannel_in)
                ch.reset();

        if (_evchannel_out != null) _evchannel_out.reset();
    }

    /**
     * Clears component, may alter references with framework or other components
     * Called on framework clear()
     */
    public void clear() {
        if (_evchannel_in != null) {
            for (EventChannel ch : _evchannel_in)
                ch.clear();

            _evchannel_in.clear();
        }
    }

    public abstract OptionList getOptions();

    public boolean isSetup() {
        return _isSetup;
    }
}
