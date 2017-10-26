/*
 * MainActivity.java
 * Copyright (c) 2017
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

package hcm.ssj.creator;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.Toast;

import com.microsoft.band.tiles.TileButtonEvent;
import com.microsoft.band.tiles.TileEvent;

import java.util.ArrayList;

import hcm.ssj.core.ExceptionHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.Monitor;
import hcm.ssj.core.Pipeline;
import hcm.ssj.creator.core.BandComm;
import hcm.ssj.creator.core.PipelineBuilder;
import hcm.ssj.creator.core.SSJDescriptor;
import hcm.ssj.creator.dialogs.AddDialog;
import hcm.ssj.creator.dialogs.FileDialog;
import hcm.ssj.creator.dialogs.Listener;
import hcm.ssj.creator.main.AnnotationTab;
import hcm.ssj.creator.main.TabHandler;
import hcm.ssj.creator.util.DemoHandler;
import hcm.ssj.creator.util.Util;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener
{
    private static boolean ready = true;
    private boolean firstStart = false;
    private static final int REQUEST_DANGEROUS_PERMISSIONS = 108;
    //tabs
    private TabHandler tabHandler;

    private boolean actionButtonsVisible = false;

	private LinearLayout sensorLayout;
	private LinearLayout sensorChannelLayout;
	private LinearLayout transformerLayout;
	private LinearLayout consumerLayout;
	private LinearLayout eventHandlerLayout;

	private Animation showButton;
	private Animation hideButton;
	private Animation showLayout;
	private Animation hideLayout;

	private FloatingActionButton fab;

    private BroadcastReceiver msBandReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {

            android.util.Log.i("SSJCreator", "received tile event");
            TileButtonEvent data = intent.getParcelableExtra(TileEvent.TILE_EVENT_DATA);

            if (!data.getPageID().equals(BandComm.pageId))
                return;

            //toggle button
            AnnotationTab anno = tabHandler.getAnnotation();
            if (anno == null)
                return;

            anno.toggleAnnoButton(anno.getBandAnnoButton(), data.getElementID() == BandComm.BTN_YES);
        }
    };

    /**
     *
     */
    private void init()
    {
    	// Initialize action button layouts with their corresponding event listeners.
		initAddComponentButtons();
		initActionButtonLayouts();
		initFloatingActionButton();

        //init tabs
        tabHandler = new TabHandler(MainActivity.this);
        //handle permissions
        checkPermissions();
        //set exception handler
        setExceptionHandler();

        //register receiver for ms band events
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.microsoft.band.action.ACTION_TILE_BUTTON_PRESSED");
        registerReceiver(msBandReceiver, filter);
    }

    /**
     *
     */
    private void setExceptionHandler()
    {
        ExceptionHandler exceptionHandler = new ExceptionHandler()
        {
            @Override
            public void handle(final String location, final String msg, final Throwable t)
            {
                Monitor.notifyMonitor();
                Handler handler = new Handler(Looper.getMainLooper());
                Runnable runnable = new Runnable()
                {
                    public void run()
                    {

                        String text = location + ": " + msg;

                        Throwable cause = t;
                        while(cause != null)
                        {
                            text += "\ncaused by: " + cause.getMessage();
                            cause = cause.getCause();
                        }

                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.str_error)
                                .setMessage(text)
                                .setPositiveButton(R.string.str_ok, null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                };
                handler.post(runnable);
            }
        };
        Pipeline.getInstance().setExceptionHandler(exceptionHandler);
    }

    /**
     *
     */
    private void checkPermissions()
    {
        if (Build.VERSION.SDK_INT >= 23)
        {
            //dangerous permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BODY_SENSORS,
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_DANGEROUS_PERMISSIONS);
            }
        }
    }

    /**
     * @param view View
     */
    public void buttonPressed(View view)
    {
        switch (view.getId())
        {
            case R.id.id_imageButton:
            {
                handlePipe();
                break;
            }
        }
    }

    /**
     * Start or stop pipe
     */
    private void handlePipe()
    {
        if (ready)
        {
            ready = false;
            new Thread()
            {
                @Override
                public void run()
                {
                    //change button text
                    changeImageButton(android.R.drawable.ic_popup_sync, false);
                    //save framework options
                    Pipeline pipeline = Pipeline.getInstance();
                    //remove old content
                    pipeline.clear();
                    pipeline.resetCreateTime();
                    //add components
                    try
                    {
                        PipelineBuilder.getInstance().buildPipe();
                    } catch (Exception e)
                    {
                        Log.e(getString(R.string.err_buildPipe), e);
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Toast.makeText(getApplicationContext(), R.string.err_buildPipe, Toast.LENGTH_LONG).show();
                            }
                        });
                        ready = true;
                        changeImageButton(android.R.drawable.ic_media_play, true);
                        return;
                    }
                    //change button text
                    changeImageButton(android.R.drawable.ic_media_pause, true);
                    //notify tabs
                    tabHandler.preStart();
                    //start framework
                    pipeline.start();

                    //run
                    if(pipeline.isRunning())
                        Monitor.waitMonitor();

                    //stop framework
                    try
                    {
                        tabHandler.preStop();
                        pipeline.stop();
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    ready = true;
                    //change button text
                    changeImageButton(android.R.drawable.ic_media_play, true);
                }
            }.start();
        } else
        {
            Monitor.notifyMonitor();
        }
    }

    /**
     * @param idImage int
     */
    private void changeImageButton(final int idImage, final boolean enabled)
    {
        final ImageButton imageButton = (ImageButton) findViewById(R.id.id_imageButton);
        if (imageButton != null)
        {
            imageButton.post(new Runnable()
            {
                public void run()
                {
                    imageButton.setImageResource(idImage);
                    imageButton.setEnabled(enabled);
                }
            });
        }
    }


    /**
     * @param resource int
     * @param list     ArrayList
     */
    private void showAddDialog(int resource, ArrayList<Class> list)
    {
        final AddDialog addDialog = new AddDialog();
        addDialog.setTitleMessage(resource);
        addDialog.setOption(list);
        Listener listener = new Listener()
        {
            @Override
            public void onPositiveEvent(Object[] o)
            {
                addDialog.removeListener(this);
                actualizeContent(Util.AppAction.ADD, o != null ? o[0] : null);
            }

            @Override
            public void onNegativeEvent(Object[] o)
            {
                addDialog.removeListener(this);
            }
        };
        addDialog.addListener(listener);
        addDialog.show(getSupportFragmentManager(), MainActivity.this.getClass().getSimpleName());
    }

    /**
     * @param title   int
     * @param type    FileDialog.Type
     * @param message int
     */
    private void showFileDialog(final int title, final FileDialog.Type type, final int message)
    {
        if (firstStart)
            DemoHandler.copyFiles(MainActivity.this);

        final FileDialog fileDialog = new FileDialog();
        fileDialog.setTitleMessage(title);
        fileDialog.setType(type);
        fileDialog.show(getSupportFragmentManager(), MainActivity.this.getClass().getSimpleName());
        Listener listener = new Listener()
        {
            @Override
            public void onPositiveEvent(Object[] o)
            {
                fileDialog.removeListener(this);
                if (type == FileDialog.Type.LOAD)
                {
                    actualizeContent(Util.AppAction.LOAD, o != null ? o[0] : null);
                } else if (type == FileDialog.Type.SAVE)
                {
                    actualizeContent(Util.AppAction.SAVE, o != null ? o[0] : null);
                }
            }

            @Override
            public void onNegativeEvent(Object[] o)
            {
                if (o != null)
                {
                    Log.e(getResources().getString(message));
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(message)
                            .setPositiveButton(R.string.str_ok, null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
                fileDialog.removeListener(this);
            }
        };
        fileDialog.addListener(listener);
    }

    /**
     * @param appAction Util.AppAction
     * @param o         Object
     */
    private void actualizeContent(Util.AppAction appAction, Object o)
    {
        tabHandler.actualizeContent(appAction, o);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        actualizeContent(Util.AppAction.DISPLAYED, null);
        if (!ready)
        {
            changeImageButton(android.R.drawable.ic_media_pause, true);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        unregisterReceiver(msBandReceiver);

        tabHandler.cleanUp();
        Pipeline framework = Pipeline.getInstance();
        if (framework.isRunning())
        {
            framework.stop();
        }
        PipelineBuilder.getInstance().clear();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        startTutorial();
        setContentView(R.layout.activity_main);

        loadAnimations();
        init();

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close);
		drawer.addDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		final TabHost tabHost = (TabHost) findViewById(R.id.id_tabHost);
		tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
			@Override
			public void onTabChanged(String s)
			{
				int currentTabId = tabHost.getCurrentTab();
				FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

				// Show floating action button only if canvas tab is selected.
				if (currentTabId == 0)
				{
					fab.show();
				}
				else
				{
					if (actionButtonsVisible)
					{
						hideActionButtons();
					}
					fab.hide();
				}
			}
		});
    }

    /**
     * Close drawer if open otherwise go to app home screen.
     */
    @Override
    public void onBackPressed()
    {
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			moveTaskToBack(true);
		}
    }

    private void startTutorial()
    {
        //declare a new thread to do a preference check
        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

                //  check if the activity has started before on this app version...
                String name = "LAST_VERSION";
                String ssjVersion = Pipeline.getVersion();
                firstStart = !getPrefs.getString(name, "").equalsIgnoreCase(ssjVersion);

                if (firstStart)
                {
                    //launch app intro
                    Intent i = new Intent(MainActivity.this, TutorialActivity.class);
                    startActivity(i);

                    //save current version in preferences so the next time this won't run again
                    SharedPreferences.Editor e = getPrefs.edit();
                    e.putString(name, ssjVersion);
                    e.apply();
                }
            }
        });
        //start the thread
        t.start();
    }

	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item)
	{
		int itemId = item.getItemId();

		if (itemId == R.id.action_framework)
		{
			Intent intent = new Intent(getApplicationContext(), OptionsActivity.class);
			startActivity(intent);
		}
		else if (itemId == R.id.action_save)
		{
			showFileDialog(R.string.str_save, FileDialog.Type.SAVE, R.string.str_saveError);
		}
		else if (itemId == R.id.action_load)
		{
			showFileDialog(R.string.str_load, FileDialog.Type.LOAD, R.string.str_loadError);
		}
		else if (itemId == R.id.action_delete)
		{
			showFileDialog(R.string.str_delete, FileDialog.Type.DELETE, R.string.str_deleteError);
		}

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);

		return false;
	}

	/**
	 * Initialize floating action button to show/hide SSJ component selection buttons.
	 */
	private void initFloatingActionButton()
	{
		fab = (FloatingActionButton) findViewById(R.id.fab);

		fab.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				if (actionButtonsVisible)
				{
					hideActionButtons();
				}
				else
				{
					showActionButtons();
				}
			}
		});
	}

	/**
	 * Initialize all linear layouts that contain action buttons for SSJ component selection
	 * and their corresponding text labels.
	 */
	private void initActionButtonLayouts()
	{
		sensorLayout = (LinearLayout) findViewById(R.id.sensor_layout);
		sensorChannelLayout = (LinearLayout) findViewById(R.id.sensor_channel_layout);
		transformerLayout = (LinearLayout) findViewById(R.id.transformers_layout);
		consumerLayout = (LinearLayout) findViewById(R.id.consumer_layout);
		eventHandlerLayout = (LinearLayout) findViewById(R.id.event_handler_layout);
	}

	/**
	 * Initialize all action buttons for SSJ component selection and add corresponding event
	 * listeners.
	 */
	private void initAddComponentButtons()
	{
		FloatingActionButton addSensor = (FloatingActionButton) findViewById(R.id.action_sensors);
		addSensor.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				showAddDialog(R.string.str_sensors, SSJDescriptor.getInstance().sensors);
			}
		});

		FloatingActionButton addProvider = (FloatingActionButton) findViewById(R.id.action_providers);
		addProvider.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				showAddDialog(R.string.str_sensor_channels, SSJDescriptor.getInstance().sensorChannels);
			}
		});

		FloatingActionButton addTransformer = (FloatingActionButton) findViewById(R.id.action_transformers);
		addTransformer.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				showAddDialog(R.string.str_transformers, SSJDescriptor.getInstance().transformers);
			}
		});

		FloatingActionButton addConsumer = (FloatingActionButton) findViewById(R.id.action_consumers);
		addConsumer.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				showAddDialog(R.string.str_consumers, SSJDescriptor.getInstance().consumers);
			}
		});

		FloatingActionButton addEventHandler = (FloatingActionButton) findViewById(R.id.action_eventhandlers);
		addEventHandler.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				showAddDialog(R.string.str_eventhandlers, SSJDescriptor.getInstance().eventHandlers);
			}
		});
	}

	/**
	 * Load animations that toggle visibility of action buttons.
	 */
	private void loadAnimations()
	{
		showButton = AnimationUtils.loadAnimation(MainActivity.this,
												  R.anim.show_button);
		hideButton = AnimationUtils.loadAnimation(MainActivity.this,
												  R.anim.hide_button);
		showLayout = AnimationUtils.loadAnimation(MainActivity.this,
												  R.anim.show_layout);
		hideLayout = AnimationUtils.loadAnimation(MainActivity.this,
												  R.anim.hide_layout);
	}

	/**
	 * Animate appearance of action buttons.
	 */
	private void showActionButtons()
	{
		sensorLayout.setVisibility(View.VISIBLE);
		sensorLayout.startAnimation(showLayout);

		sensorChannelLayout.setVisibility(View.VISIBLE);
		sensorChannelLayout.startAnimation(showLayout);

		transformerLayout.setVisibility(View.VISIBLE);
		transformerLayout.startAnimation(showLayout);

		consumerLayout.setVisibility(View.VISIBLE);
		consumerLayout.startAnimation(showLayout);

		eventHandlerLayout.setVisibility(View.VISIBLE);
		eventHandlerLayout.startAnimation(showLayout);

		fab.startAnimation(showButton);
		actionButtonsVisible = true;
	}

	/**
	 * Animate hiding of action buttons.
	 */
	private void hideActionButtons()
	{
		sensorLayout.setVisibility(View.GONE);
		sensorLayout.startAnimation(hideLayout);

		sensorChannelLayout.setVisibility(View.GONE);
		sensorChannelLayout.startAnimation(hideLayout);

		transformerLayout.setVisibility(View.GONE);
		transformerLayout.startAnimation(hideLayout);

		consumerLayout.setVisibility(View.GONE);
		consumerLayout.startAnimation(hideLayout);

		eventHandlerLayout.setVisibility(View.GONE);
		eventHandlerLayout.startAnimation(hideLayout);

		fab.startAnimation(hideButton);
		actionButtonsVisible = false;
	}
}
