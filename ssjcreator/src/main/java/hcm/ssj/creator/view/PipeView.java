/*
 * PipeView.java
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

package hcm.ssj.creator.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import hcm.ssj.core.Component;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.Provider;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.Transformer;
import hcm.ssj.creator.core.PipelineBuilder;
import hcm.ssj.creator.main.TwoDScrollView;
import hcm.ssj.creator.util.ConnectionType;
import hcm.ssj.creator.util.Util;
import hcm.ssj.feedback.Feedback;
import hcm.ssj.feedback.FeedbackCollection;
import hcm.ssj.ml.IModelHandler;
import hcm.ssj.ml.Model;

/**
 * Draws a pipe<br>
 * Created by Frank Gaibler on 29.04.2016.
 */
public class PipeView extends ViewGroup {
    //layout
    private final static int PORTRAIT_NUMBER_OF_BOXES = 20;
    private final static int LANDSCAPE_NUMBER_OF_BOXES = 2 * PORTRAIT_NUMBER_OF_BOXES; //@todo adjust to different screen sizes (e.g. show all boxes on tablet)
    private final int iGridWidthNumberOfBoxes = 50; //chosen box number
    private final int iGridHeightNumberOfBoxes = 50; //chosen box number
    //elements
    private ArrayList<ComponentView> componentViewsSensor = new ArrayList<>();
    private ArrayList<ComponentView> componentViewsSensorChannel = new ArrayList<>();
    private ArrayList<ComponentView> componentViewsTransformer = new ArrayList<>();
    private ArrayList<ComponentView> componentViewsConsumer = new ArrayList<>();
    private ArrayList<ComponentView> componentViewsEventHandler = new ArrayList<>();
    private ArrayList<ComponentView> componentViewsModel = new ArrayList<>();
    //connections
    private final ArrayList<ConnectionView> streamConnectionViews = new ArrayList<>();
    private final ArrayList<ConnectionView> eventConnectionViews = new ArrayList<>();
    private final ArrayList<ConnectionView> modelConnectionViews = new ArrayList<>();
    //colors
    private Paint paintElementGrid;
    private Paint paintElementShadow;
    private int iOrientation = Configuration.ORIENTATION_UNDEFINED;
    //grid
    private GridLayout gridLayout;
    private int iGridBoxSize = 0; //box size depends on screen width
    private int iGridPadWPix = 0; //left and right padding to center grid
    private int iGridPadHPix = 0; //top and bottom padding to center grid
    private int iSizeWidth = 0; //draw size width
    private int iSizeHeight = 0; //draw size height
    //listeners
    private final HashSet<PipeListener> hsPipeListener = new HashSet<>();

    /**
     * @param context Context
     */
    public PipeView(Context context) {
        super(context);
        init(context);
    }

    /**
     * @param context Context
     * @param attrs   AttributeSet
     */
    public PipeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public static boolean isValidConnection(Component src, Component dst, ConnectionType type) {
        if (type == ConnectionType.EVENTCONNECTION && !(src instanceof Model)) {
            return true;
        }
        if (type == ConnectionType.MODELCONNECTION &&
                ((src instanceof Model && dst instanceof IModelHandler)
                        || (src instanceof IModelHandler && dst instanceof Model))) {
            return true;
        }
        if (src instanceof Sensor && dst instanceof SensorChannel) {
            return true;
        }
        if (src instanceof SensorChannel && (dst instanceof Transformer || dst instanceof Consumer)) {
            return true;
        }
        return src instanceof Transformer && (dst instanceof Transformer || dst instanceof Consumer);
    }

    private void init(Context context) {
        Log.i("Init PipeView");
        //children should not be clipped
        setClipToPadding(false);
        //create grid
        gridLayout = new GridLayout(iGridWidthNumberOfBoxes, iGridHeightNumberOfBoxes);
        //add drag listener
        setOnDragListener(new PipeOnDragListener(context));
        //initiate colors
        paintElementGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintElementGrid.setStyle(Paint.Style.STROKE);
        paintElementGrid.setColor(Color.GRAY);
        //
        paintElementShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintElementShadow.setStyle(Paint.Style.FILL);
        paintElementShadow.setColor(Color.LTGRAY);

        // TODO: Adjust box count to DPI
        // DisplayMetrics metrics = getResources().getDisplayMetrics();
        // Log.i("Metrics W: " + metrics.widthPixels + " H: " + metrics.heightPixels + " D: " + metrics.density + " DPI: " + metrics.densityDpi + " xDPI: " + metrics.xdpi + " yDPI: " + metrics.ydpi);
    }

    /**
     * @param appAction Util.AppAction
     * @param o         Object
     */
    public final void recalculate(Util.AppAction appAction, Object o) {
        switch (appAction) {
            case SAVE: {
                SaveLoad.save(o, componentViewsSensorChannel, componentViewsSensor,
                        componentViewsTransformer, componentViewsConsumer,
                        componentViewsEventHandler, componentViewsModel);
                break;
            }
            case LOAD: {
                createElements();
                gridLayout.clear();
                changeElementPositions(SaveLoad.load(o));
                placeElements();
                informListeners();
                break;
            }
            case CLEAR:
                gridLayout.clear(); //fallthrough
            case ADD: //fallthrough
            case DISPLAYED:
                createElements();
                placeElements();
                informListeners();
                break;
            default:
                break;
        }
    }

    /**
     * Inform listeners about changed components.<br>
     * Mainly used for informing tab holder about new or deleted painters or writers.<br>
     */
    protected void informListeners() {
        for (PipeListener pipeListener : hsPipeListener) {
            pipeListener.viewChanged();
        }
    }

    /**
     * @param pipeListener PipeListener
     */
    public final void addViewListener(PipeListener pipeListener) {
        hsPipeListener.add(pipeListener);
    }

    /**
     * @param pipeListener PipeListener
     */
    public final void removeViewListener(PipeListener pipeListener) {
        hsPipeListener.remove(pipeListener);
    }

    /**
     *
     */
    protected void createElements() {
        // clear views
        removeAllViews();

        //add connections
        streamConnectionViews.clear();
        for (int i = 0; i < PipelineBuilder.getInstance().getNumberOfStreamConnections(); i++) {
            ConnectionView connectionView = new ConnectionView(getContext());
            connectionView.setConnectionType(ConnectionType.STREAMCONNECTION);
            streamConnectionViews.add(connectionView);
            addView(streamConnectionViews.get(i));
        }
        eventConnectionViews.clear();
        for (int i = 0; i < PipelineBuilder.getInstance().getNumberOfEventConnections(); i++) {
            ConnectionView connectionView = new ConnectionView(getContext());
            connectionView.setConnectionType(ConnectionType.EVENTCONNECTION);
            eventConnectionViews.add(connectionView);
            addView(eventConnectionViews.get(i));
        }
        modelConnectionViews.clear();
        for (int i = 0; i < PipelineBuilder.getInstance().getNumberOfModelConnections(); i++) {
            ConnectionView connectionView = new ConnectionView(getContext());
            connectionView.setConnectionType(ConnectionType.MODELCONNECTION);
            modelConnectionViews.add(connectionView);
            addView(modelConnectionViews.get(i));
        }

        //add providers
        componentViewsSensorChannel = fillList(componentViewsSensorChannel, PipelineBuilder.Type.SensorChannel);
        //add sensors
        componentViewsSensor = fillList(componentViewsSensor, PipelineBuilder.Type.Sensor);
        //add transformers
        componentViewsTransformer = fillList(componentViewsTransformer, PipelineBuilder.Type.Transformer);
        //add consumers
        componentViewsConsumer = fillList(componentViewsConsumer, PipelineBuilder.Type.Consumer);
        //add eventhandler
        componentViewsEventHandler = fillList(componentViewsEventHandler, PipelineBuilder.Type.EventHandler);
        //add models
        componentViewsModel = fillList(componentViewsModel, PipelineBuilder.Type.Model);

    }

    /**
     * @param alView ArrayList
     * @param type   Linker.Type
     */
    private ArrayList<ComponentView> fillList(ArrayList<ComponentView> alView, PipelineBuilder.Type type) {
        //get all pipe components of specific type
        Object[] objects = PipelineBuilder.getInstance().getAll(type);
        //copy to new list to delete unused components
        ArrayList<ComponentView> alInterim = new ArrayList<>();
        for (Object object : objects) {
            //check of components already exist in list
            boolean found = false;
            for (ComponentView v : alView) {
                if (v.getElement().equals(object)) {
                    found = true;
                    v.setStreamConnectionHashes(PipelineBuilder.getInstance().getStreamConnectionHashes(object));
                    v.setEventConnectionHashes(PipelineBuilder.getInstance().getEventConnectionHashes(object));
                    v.setModelConnectionHashes(PipelineBuilder.getInstance().getModelConnectionHashes(object));
                    alInterim.add(v);
                    break;
                }
            }
            //create new ComponentView if not
            if (!found) {
                ComponentView view = new ComponentView(getContext(), object);
                view.setStreamConnectionHashes(PipelineBuilder.getInstance().getStreamConnectionHashes(object));
                view.setEventConnectionHashes(PipelineBuilder.getInstance().getEventConnectionHashes(object));
                view.setModelConnectionHashes(PipelineBuilder.getInstance().getModelConnectionHashes(object));
                alInterim.add(view);
            }
        }
        //replace old list
        alView = alInterim;
        //add views
        for (View view : alView) {
            addView(view);
        }
        return alView;
    }

    /**
     *
     */
    protected void placeElements() {
        //elements
        int initHeight = 0;
        int divider = 5;
        setLayouts(componentViewsSensor, initHeight);
        initHeight += divider;
        setLayouts(componentViewsSensorChannel, initHeight);
        initHeight += divider;
        setLayouts(componentViewsTransformer, initHeight);
        initHeight += divider;
        setLayouts(componentViewsConsumer, initHeight);
        initHeight += divider;
        setLayouts(componentViewsEventHandler, initHeight);
        setLayouts(componentViewsModel, initHeight);

        //connections
        for (ConnectionView connectionView : streamConnectionViews) {
            connectionView.layout(0, 0, iSizeWidth, iSizeHeight);
        }
        for (ConnectionView connectionView : eventConnectionViews) {
            connectionView.layout(0, 0, iSizeWidth, iSizeHeight);
        }
        for (ConnectionView connectionView : modelConnectionViews) {
            connectionView.layout(0, 0, iSizeWidth, iSizeHeight);
        }

        int streamConnections = 0;
        int eventConnections = 0;
        int modelConnections = 0;
        for (ComponentView componentViewSensor : componentViewsSensor) {
            int[] streamHashes = componentViewSensor.getStreamConnectionHashes();
            streamConnections = checkStreamConnections(streamHashes, streamConnections, componentViewSensor, componentViewsSensorChannel, false);
            int[] eventHashes = componentViewSensor.getEventConnectionHashes();
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewSensor, componentViewsSensor, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewSensor, componentViewsSensorChannel, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewSensor, componentViewsTransformer, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewSensor, componentViewsConsumer, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewSensor, componentViewsEventHandler, true);
        }
        for (ComponentView componentViewSensorChannel : componentViewsSensorChannel) {
            int[] eventHashes = componentViewSensorChannel.getEventConnectionHashes();
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewSensorChannel, componentViewsSensor, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewSensorChannel, componentViewsSensorChannel, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewSensorChannel, componentViewsTransformer, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewSensorChannel, componentViewsConsumer, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewSensorChannel, componentViewsEventHandler, true);
        }
        for (ComponentView componentViewTransformer : componentViewsTransformer) {
            int[] streamHashes = componentViewTransformer.getStreamConnectionHashes();
            streamConnections = checkStreamConnections(streamHashes, streamConnections, componentViewTransformer, componentViewsSensorChannel, true);
            streamConnections = checkStreamConnections(streamHashes, streamConnections, componentViewTransformer, componentViewsTransformer, true);
            int[] eventHashes = componentViewTransformer.getEventConnectionHashes();
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewTransformer, componentViewsSensor, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewTransformer, componentViewsSensorChannel, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewTransformer, componentViewsTransformer, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewTransformer, componentViewsConsumer, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewTransformer, componentViewsEventHandler, true);
        }
        for (ComponentView componentViewConsumer : componentViewsConsumer) {
            int[] streamHashes = componentViewConsumer.getStreamConnectionHashes();
            streamConnections = checkStreamConnections(streamHashes, streamConnections, componentViewConsumer, componentViewsSensorChannel, true);
            streamConnections = checkStreamConnections(streamHashes, streamConnections, componentViewConsumer, componentViewsTransformer, true);
            int[] eventHashes = componentViewConsumer.getEventConnectionHashes();
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewConsumer, componentViewsSensor, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewConsumer, componentViewsSensorChannel, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewConsumer, componentViewsTransformer, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewConsumer, componentViewsConsumer, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewConsumer, componentViewsEventHandler, true);
        }
        for (ComponentView componentViewEventHandler : componentViewsEventHandler) {
            int[] eventHashes = componentViewEventHandler.getEventConnectionHashes();
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewEventHandler, componentViewsSensor, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewEventHandler, componentViewsSensorChannel, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewEventHandler, componentViewsTransformer, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewEventHandler, componentViewsConsumer, true);
            eventConnections = checkEventConnections(eventHashes, eventConnections, componentViewEventHandler, componentViewsEventHandler, true);
        }
        for (ComponentView componentViewModel : componentViewsModel) {
            int[] connectionHashes = componentViewModel.getModelConnectionHashes();
            modelConnections = checkModelConnections(connectionHashes, modelConnections, componentViewModel, componentViewsSensor);
            modelConnections = checkModelConnections(connectionHashes, modelConnections, componentViewModel, componentViewsSensorChannel);
            modelConnections = checkModelConnections(connectionHashes, modelConnections, componentViewModel, componentViewsTransformer);
            modelConnections = checkModelConnections(connectionHashes, modelConnections, componentViewModel, componentViewsConsumer);
            modelConnections = checkModelConnections(connectionHashes, modelConnections, componentViewModel, componentViewsEventHandler);
        }
    }

    /**
     * @param alPoints ArrayList<Point
     */
    private void changeElementPositions(ArrayList<Point> alPoints) {
        if (alPoints != null) {
            int count = 0;
            count = changeElementPositions(alPoints, componentViewsSensorChannel, count);
            count = changeElementPositions(alPoints, componentViewsSensor, count);
            count = changeElementPositions(alPoints, componentViewsTransformer, count);
            count = changeElementPositions(alPoints, componentViewsConsumer, count);
            count = changeElementPositions(alPoints, componentViewsEventHandler, count);
            changeElementPositions(alPoints, componentViewsModel, count);
        }
    }

    /**
     * @param alPoints        ArrayList<Point>
     * @param alComponentView ArrayList<ComponentView>
     * @param count           int
     */
    private int changeElementPositions(ArrayList<Point> alPoints, ArrayList<ComponentView> alComponentView, int count) {
        for (int i = 0; i < alComponentView.size() && count < alPoints.size(); i++, count++) {
            alComponentView.get(i).setGridX(alPoints.get(count).x);
            alComponentView.get(i).setGridY(alPoints.get(count).y);
        }
        return count;
    }

    /**
     * @param hashes              int[]
     * @param connections         int
     * @param destination         View
     * @param componentViews      ArrayList
     * @param standardOrientation boolean
     * @return int
     */
    private int checkStreamConnections(int[] hashes, int connections, ComponentView destination, ArrayList<ComponentView> componentViews, boolean standardOrientation) {
        if (hashes != null) {
            for (int hash : hashes) {
                for (ComponentView componentView : componentViews) {
                    if (hash == componentView.getElementHash()) {
                        ConnectionView connectionView = streamConnectionViews.get(connections);
                        //arrow from child to parent (e.g. transformer to consumer)
                        if (standardOrientation) {
                            connectionView.drawConnectionViews(componentView, destination, iGridBoxSize);
                            connectionView.invalidate();
                        } else
                        //arrow from parent to child (e.g. sensor to sensorChannel)
                        {
                            connectionView.drawConnectionViews(destination, componentView, iGridBoxSize);
                            connectionView.invalidate();
                        }
                        connections++;
                        break;
                    }
                }
            }
        }
        return connections;
    }

    /**
     * @param hashes              int[]
     * @param connections         int
     * @param destination         View
     * @param componentViews      ArrayList
     * @param standardOrientation boolean
     * @return int
     */
    private int checkEventConnections(int[] hashes, int connections, ComponentView destination, ArrayList<ComponentView> componentViews, boolean standardOrientation) {
        if (destination.getElement() instanceof Feedback && PipelineBuilder.getInstance().isManagedFeedback(destination.getElement())) {
            return connections;
        }
        if (hashes != null) {
            for (int hash : hashes) {
                for (ComponentView componentView : componentViews) {
                    if (hash == componentView.getElementHash()) {
                        ConnectionView connectionView = eventConnectionViews.get(connections);
                        //arrow from child to parent (e.g. transformer to consumer)
                        if (standardOrientation) {
                            connectionView.drawConnectionViews(componentView, destination, iGridBoxSize);
                            connectionView.invalidate();
                        } else
                        //arrow from parent to child (e.g. sensor to sensorChannel)
                        {
                            connectionView.drawConnectionViews(destination, componentView, iGridBoxSize);
                            connectionView.invalidate();
                        }
                        connections++;

                        if (PipelineBuilder.getInstance().isManagedFeedback(componentView.getElement())) {
                            connectionView.setVisibility(GONE);
                        } else {
                            setVisibility(VISIBLE);
                        }

                        break;
                    }
                }
            }
        }
        return connections;
    }

    /**
     * @param hashes         int[]
     * @param connections    int
     * @param destination    View
     * @param componentViews ArrayList
     * @return int
     */
    private int checkModelConnections(int[] hashes, int connections, ComponentView destination, ArrayList<ComponentView> componentViews) {
        if (hashes != null) {
            for (int hash : hashes) {
                for (ComponentView componentView : componentViews) {
                    if (hash == componentView.getElementHash()) {
                        ConnectionView connectionView = modelConnectionViews.get(connections);
                        connectionView.drawConnectionViews(componentView, destination, iGridBoxSize);
                        connectionView.invalidate();
                        connections++;
                        break;
                    }
                }
            }
        }
        return connections;
    }

    /**
     * @param views      ArrayList
     * @param initHeight int
     */
    private void setLayouts(ArrayList<ComponentView> views, int initHeight) {
        for (ComponentView view : views) {
            // Prevent managed feedback to be drawn
            if (view.getElement() instanceof Feedback) {
                if (PipelineBuilder.getInstance().isManagedFeedback(view.getElement())) {
                    view.setVisibility(GONE);
                    continue;
                } else {
                    view.setVisibility(VISIBLE);
                }
            }

            if (view.isPositioned()) {
                placeElementView(view);
            } else {
                boolean placed = false;
                //place elements as chess grid
                for (int j = initHeight; !placed && j < iGridHeightNumberOfBoxes; j += 2) {
                    for (int i = j % 2; !placed && i < iGridWidthNumberOfBoxes; i += 4) {
                        if (gridLayout.isGridFree(i, j)) {
                            view.setGridX(i);
                            view.setGridY(j);
                            placeElementView(view);
                            placed = true;
                        }
                    }
                }
                //try from zero if placement didn't work
                for (int j = 0; !placed && j < iGridHeightNumberOfBoxes && j < initHeight; j++) {
                    for (int i = 0; !placed && i < iGridWidthNumberOfBoxes; i++) {
                        if (gridLayout.isGridFree(i, j)) {
                            view.setGridX(i);
                            view.setGridY(j);
                            placeElementView(view);
                            placed = true;
                        }
                    }
                }
                if (!placed) {
                    Log.e("Too many elements in view. Could not place all.");
                }
            }
        }
    }

    /**
     * @param view ElementView
     */
    protected void placeElementView(ComponentView view) {
        gridLayout.setGridValue(view.getGridX(), view.getGridY(), true);
        int xPos = view.getGridX() * iGridBoxSize + iGridPadWPix;
        int yPos = view.getGridY() * iGridBoxSize + iGridPadHPix;
        int componentSize = iGridBoxSize * 2;
        view.layout(xPos, yPos, xPos + componentSize, yPos + componentSize);
    }

    /**
     * @return int
     */
    protected int getGridBoxSize() {
        return iGridBoxSize;
    }

    /**
     * Translates one pixel axis position to grid coordinate
     *
     * @param pos float
     * @return int
     */
    protected int getGridCoordinate(float pos) {
        int i = (int) (pos / iGridBoxSize + 0.5f) - 1;
        return i < 0 ? 0 : i;
    }

    /**
     * @param object Object
     * @param x      int
     * @param y      int
     * @return boolean
     */
    protected boolean checkCollisionConnection(Object object, int x, int y) {
        boolean result;
        if (object instanceof Sensor) {
            result = addCollisionConnection(object, x, y, componentViewsSensorChannel, false);
        } else {
            result = addCollisionConnection(object, x, y, componentViewsSensorChannel, true);
        }

        if (!result && object instanceof IModelHandler) {
            result = addCollisionConnection(object, x, y, componentViewsModel, true);
        }
        if (!result) {
            result = addCollisionConnection(object, x, y, componentViewsSensor, true);
        }
        if (!result) {
            result = addCollisionConnection(object, x, y, componentViewsTransformer, true);
        }
        if (!result) {
            result = addCollisionConnection(object, x, y, componentViewsConsumer, true);
        }
        if (!result) {
            result = addCollisionConnection(object, x, y, componentViewsEventHandler, true);
        }

        return result;
    }

    /**
     * @param object         Object
     * @param x              int
     * @param y              int
     * @param componentViews ArrayList
     * @param standard       boolean
     * @return boolean
     */
    private boolean addCollisionConnection(Object object, int x, int y, ArrayList<ComponentView> componentViews, boolean standard) {
        for (ComponentView componentView : componentViews) {
            int colX = componentView.getGridX();
            int colY = componentView.getGridY();
            if ((colX == x || colX == x - 1 || colX == x + 1) && (colY == y || colY == y - 1 || colY == y + 1)) {
                if (object instanceof Feedback && componentView.getElement() instanceof FeedbackCollection) {
                    PipelineBuilder.getInstance().addFeedbackToCollectionContainer(
                            (FeedbackCollection) componentView.getElement(),
                            (Feedback) object,
                            0,
                            FeedbackCollection.LevelBehaviour.Neutral);
                    // Unposition managed feedback
                    for (ComponentView view : componentViewsEventHandler) {
                        if (view.getElement().equals(object)) {
                            view.setGridX(-1);
                            view.setGridY(-1);
                        }
                    }
                    this.informListeners();
                } else if (isValidConnection((Component) object, (Component) componentView.getElement(), ConnectionType.STREAMCONNECTION)) {
                    if (standard) {
                        PipelineBuilder.getInstance().addStreamConnection(componentView.getElement(), (Provider) object);
                    } else {
                        PipelineBuilder.getInstance().addStreamConnection(object, (Provider) componentView.getElement());
                    }
                } else if (isValidConnection((Component) object, (Component) componentView.getElement(), ConnectionType.MODELCONNECTION)) {
                    PipelineBuilder.getInstance().addModelConnection((Component) object, (Component) componentView.getElement());
                } else if (isValidConnection((Component) object, (Component) componentView.getElement(), ConnectionType.EVENTCONNECTION)) {
                    PipelineBuilder.getInstance().addEventConnection(componentView.getElement(), (Component) object);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * @return GridLayout
     */
    protected GridLayout getGrid() {
        return gridLayout;
    }

    /**
     * @param canvas Canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();

        calculateLayout();

        int left = iGridPadWPix;
        int right = iSizeWidth - iGridPadWPix;
        int top = iGridPadHPix;
        int bottom = iSizeHeight - iGridPadHPix;
        for (int i = 0; i < iGridWidthNumberOfBoxes + 1; i++) {
            canvas.drawLine(iGridPadWPix + i * iGridBoxSize, top,
                    iGridPadWPix + i * iGridBoxSize, bottom, paintElementGrid);
        }
        for (int i = 0; i < iGridHeightNumberOfBoxes + 1; i++) {
            canvas.drawLine(left, iGridPadHPix + i * iGridBoxSize,
                    right, iGridPadHPix + i * iGridBoxSize, paintElementGrid);
        }
        for (int i = 0; i < gridLayout.getWidth(); i++) {
            for (int j = 0; j < gridLayout.getHeight(); j++) {
                if (gridLayout.getValue(i, j)) {
                    float xS = iGridBoxSize * i + iGridPadWPix;
                    float yS = iGridBoxSize * j + iGridPadHPix;
                    float xE = iGridBoxSize * i + iGridPadWPix + iGridBoxSize;
                    float yE = iGridBoxSize * j + iGridPadHPix + iGridBoxSize;
                    canvas.drawRect(xS, yS, xE, yE, paintElementShadow);
                }
            }
        }
        canvas.restore();
    }

    /**
     * @param w    int
     * @param h    int
     * @param oldw int
     * @param oldh int
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            invalidate();
        }
    }

    /**
     * @param changed boolean
     * @param l       int
     * @param t       int
     * @param r       int
     * @param b       int
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Log.i("OnLayout l: " + l + " t: " + t + " r: " + r + " b: " + b + " " + System.currentTimeMillis());
    }

    private void calculateLayout() {
        int orientation = getResources().getConfiguration().orientation;

        // Only change grid box size on orientation change
        if (iOrientation != orientation) {
            iOrientation = orientation;
            // Reset scroll
            ViewParent viewParent = getParent();
            if (viewParent != null && viewParent instanceof TwoDScrollView) {
                ((TwoDScrollView) viewParent).setScrollX(0);
                ((TwoDScrollView) viewParent).setScrollY(0);
            }
            // Get displayed screen size
            Rect rectSizeDisplayed = new Rect();
            getGlobalVisibleRect(rectSizeDisplayed);
            int width = rectSizeDisplayed.width();
            int height = rectSizeDisplayed.height();

            // Log.i("Width: " + width + " Height: " + height + " " + System.currentTimeMillis());

            iGridBoxSize = width > height
                    ? height / PORTRAIT_NUMBER_OF_BOXES
                    : width / PORTRAIT_NUMBER_OF_BOXES;

            if (iGridBoxSize <= 0) {
                iGridBoxSize = 50;
            }
            calcDerivedSizes(width, height);
            //check if display wouldn't be filled
            if (iSizeWidth < width) {
                iGridBoxSize = width / (iGridWidthNumberOfBoxes);
            } else if (iSizeHeight < height) {
                iGridBoxSize = height / (iGridHeightNumberOfBoxes);
            }
            calcDerivedSizes(width, height);
            //set size in handler to force correct size and scroll view behaviour
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable runnable = new Runnable() {
                public void run() {
                    setMinimumHeight(iSizeHeight);
                    setMinimumWidth(iSizeWidth);
                    //place elements anew
                    placeElements();
                }
            };
            handler.postDelayed(runnable, 50);
        }
    }

    /**
     * @param width  int
     * @param height int
     */
    private void calcDerivedSizes(int width, int height) {
        iGridPadWPix = width % iGridBoxSize / 2;
        iGridPadHPix = height % iGridBoxSize / 2;
        iSizeWidth = iGridBoxSize * iGridWidthNumberOfBoxes + (2 * iGridPadWPix);
        iSizeHeight = iGridBoxSize * iGridHeightNumberOfBoxes + (2 * iGridPadHPix);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        boolean returnValue = false;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof ComponentView) {
                // Check if motionEvent occured on CompontenView
                if (motionEvent.getX() >= child.getX() &&
                        motionEvent.getX() <= child.getX() + child.getWidth() &&
                        motionEvent.getY() >= child.getY() &&
                        motionEvent.getY() <= child.getY() + child.getHeight()) {
                    return false;
                }
            }
        }
        List<ConnectionView> connectionViewList = new ArrayList<>();
        connectionViewList.addAll(streamConnectionViews);
        connectionViewList.addAll(eventConnectionViews);
        for (ConnectionView connectionView : connectionViewList) {
            if (connectionView.isOnPath(motionEvent)) {
                toggleConnectionType(connectionView);
                returnValue = true;
            }
        }

        return returnValue;
    }

    private void toggleConnectionType(ConnectionView connectionView) {
        ComponentView start = connectionView.getStartComponentView();
        ComponentView destination = connectionView.getDestinationComponentView();

        if (start == null || destination == null) {
            return;
        }

        Component startComponent = PipelineBuilder.getInstance().getComponentForHash(start.getElementHash());
        Component destinationComponent = PipelineBuilder.getInstance().getComponentForHash(destination.getElementHash());

        // Check if the toggled connection would be valid.
        if (!isValidConnection(startComponent,
                destinationComponent,
                connectionView.getConnectionType() == ConnectionType.EVENTCONNECTION ? ConnectionType.STREAMCONNECTION : ConnectionType.EVENTCONNECTION)) {
            return;
        }

        //Swap start and destination if sensor is involved, because then it's drawn the other way round.
        switch (connectionView.getConnectionType()) {
            case STREAMCONNECTION:
                if (startComponent instanceof Sensor) {
                    PipelineBuilder.getInstance().removeStreamConnection(startComponent, (Provider) destinationComponent);
                } else {
                    PipelineBuilder.getInstance().removeStreamConnection(destinationComponent, (Provider) startComponent);
                }
                PipelineBuilder.getInstance().addEventConnection(destinationComponent, startComponent);
                break;

            case EVENTCONNECTION:
                PipelineBuilder.getInstance().removeEventConnection(destinationComponent, startComponent);
                if (startComponent instanceof Sensor) {
                    PipelineBuilder.getInstance().addStreamConnection(startComponent, (Provider) destinationComponent);
                } else {
                    PipelineBuilder.getInstance().addStreamConnection(destinationComponent, (Provider) startComponent);
                }
                break;
            default:
                throw new RuntimeException();
        }

        this.recalculate(Util.AppAction.DISPLAYED, null);
    }
}
