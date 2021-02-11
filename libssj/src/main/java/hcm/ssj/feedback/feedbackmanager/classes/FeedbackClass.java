/*
 * FeedbackClass.java
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

package hcm.ssj.feedback.feedbackmanager.classes;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

import hcm.ssj.core.Log;
import hcm.ssj.core.event.Event;
import hcm.ssj.feedback.FeedbackManager;
import hcm.ssj.feedback.feedbackmanager.actions.Action;
import hcm.ssj.feedback.feedbackmanager.conditions.Condition;


/**
 * Created by Johnny on 01.12.2014.
 */
public abstract class FeedbackClass {
    protected Type type;
    protected Context context;
    protected Condition condition = null;
    protected Action action = null;
    protected int level = 0;
    protected FeedbackClass.Valence valence;
    protected FeedbackManager.Options options;
    private final ArrayList<FeedbackListener> listeners = new ArrayList<>();

    public static FeedbackClass create(XmlPullParser xml, Context context, FeedbackManager.Options options) {
        FeedbackClass f = null;

        if (xml.getAttributeValue(null, "type").equalsIgnoreCase("visual"))
            f = new Visual(context, options);
        else if (xml.getAttributeValue(null, "type").equalsIgnoreCase("tactile"))
            f = new Tactile(context, options);
        else if (xml.getAttributeValue(null, "type").equalsIgnoreCase("audio"))
            f = new Auditory(context, options);
        else
            throw new UnsupportedOperationException("feedback type " + xml.getAttributeValue(null, "type") + " not yet implemented");

        f.load(xml, context);
        return f;
    }

    public int getLevel() {
        return level;
    }

    public Valence getValence() {
        return valence;
    }

    public void release() {
        action.release();
    }

    public Condition getCondition() {
        return condition;
    }

    public Action getAction() {
        return action;
    }

    /*
     * called every frame by the manager
     */
    public void update() {
    }

    public void process(Event event) {
        if (!condition.checkEvent(event))
            return;

        if (action != null && execute(action)) {
            action.lastExecutionTime = System.currentTimeMillis();
        }

        // Notify event listeners
        callPostFeedback(event, action, condition.parseEvent(event));
    }

    private void callPostFeedback(final hcm.ssj.core.event.Event ssjEvent, final Action ev, final float value) {
        for (final FeedbackListener listener : listeners) {
            new Thread(new Runnable() {
                public void run() {
                    listener.onPostFeedback(ssjEvent, ev, value);
                }
            }).start();
        }
    }

    public abstract boolean execute(Action action);

    protected void load(XmlPullParser xml, Context context) {
        try {
            xml.require(XmlPullParser.START_TAG, null, "feedback");

            String level_str = xml.getAttributeValue(null, "level");
            if (level_str != null)
                level = Integer.parseInt(level_str);

            String valence_str = xml.getAttributeValue(null, "valence");
            if (valence_str != null)
                valence = FeedbackClass.Valence.valueOf(valence_str);

            while (xml.next() != XmlPullParser.END_DOCUMENT) {
                if (xml.getEventType() == XmlPullParser.START_TAG && xml.getName().equalsIgnoreCase("condition")) {
                    condition = Condition.create(xml, context);
                } else if (xml.getEventType() == XmlPullParser.START_TAG && xml.getName().equalsIgnoreCase("action")) {
                    action = Action.create(type, xml, context);
                } else if (xml.getEventType() == XmlPullParser.END_TAG && xml.getName().equalsIgnoreCase("feedback"))
                    break; //jump out once we reach end tag
            }
        } catch (IOException | XmlPullParserException e) {
            Log.e("error parsing config file", e);
        }
    }

    public void addFeedbackListener(FeedbackListener listener) {
        listeners.add(listener);
    }

    public long getLastExecutionTime() {
        return action.lastExecutionTime;
    }

    public enum Type {
        Visual,
        Tactile,
        Audio
    }

    public enum Valence {
        Unknown,
        Desirable,
        Undesirable
    }
}
