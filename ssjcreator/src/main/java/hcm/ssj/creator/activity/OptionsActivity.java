/*
 * OptionsActivity.java
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

package hcm.ssj.creator.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.creator.R;
import hcm.ssj.creator.core.PipelineBuilder;
import hcm.ssj.creator.util.ArrayAdapterWithNull;
import hcm.ssj.creator.util.OptionTable;
import hcm.ssj.creator.util.ProviderTable;
import hcm.ssj.ml.IModelHandler;
import hcm.ssj.ml.Model;
import hcm.ssj.ml.Trainer;

public class OptionsActivity extends AppCompatActivity
{
	public static Object object;
	private Object innerObject;

	/**
	 *
	 */
	private void init()
	{
		innerObject = object;
		object = null;
		Option[] options;
		if (innerObject == null)
		{
			//change title
			setTitle(R.string.str_options_pipe);
			options = PipelineBuilder.getOptionList(Pipeline.getInstance());
		}
		else
		{
			//change title
			setTitle(((hcm.ssj.core.Component) innerObject).getComponentName());
			options = PipelineBuilder.getOptionList(innerObject);
		}
		//stretch columns
		TableLayout tableLayout = (TableLayout) findViewById(R.id.id_tableLayout);
		tableLayout.setStretchAllColumns(true);
		//add frame size and delta for transformer and consumer
		if (innerObject != null
				&& (innerObject instanceof Transformer
				|| innerObject instanceof Consumer))
		{
			//add frame size and delta
			TableRow frameSizeTableRow = createTextView(true);
			TableRow deltaTableRow = createTextView(false);
			tableLayout.addView(frameSizeTableRow);
			tableLayout.addView(deltaTableRow);

			if (innerObject instanceof Consumer)
			{
				boolean triggeredByEvent = PipelineBuilder.getInstance().getEventTrigger(innerObject) != null;
				setEnabledRecursive(frameSizeTableRow, !triggeredByEvent);
				setEnabledRecursive(deltaTableRow, !triggeredByEvent);
				tableLayout.addView(createConsumerTextView(innerObject, frameSizeTableRow, deltaTableRow));
			}
		}
		//add options
		if (options != null && options.length > 0)
		{
			tableLayout.addView(OptionTable.createTable(this, options, innerObject));
		}
		//add possible providers for sensor, transformer or consumer
		if (innerObject != null && innerObject instanceof Sensor)
		{
			//add possible stream providers
			TableRow streamTableRow = ProviderTable.createStreamTable(this, innerObject,
																	  (innerObject instanceof Transformer || innerObject instanceof Consumer)
																			  || (innerObject instanceof Sensor && options != null && options.length > 0), R.string.str_stream_output);
			if (streamTableRow != null)
			{
				tableLayout.addView(streamTableRow);
			}
		}
		if (innerObject != null && (innerObject instanceof Transformer || innerObject instanceof Consumer))
		{
			//add possible stream providers
			TableRow streamTableRow = ProviderTable.createStreamTable(this, innerObject,
																	  (innerObject instanceof Transformer || innerObject instanceof Consumer)
																			  || (innerObject instanceof Sensor && options != null && options.length > 0), R.string.str_stream_input);
			if (streamTableRow != null)
			{
				tableLayout.addView(streamTableRow);
			}
		}
		if (innerObject != null && (innerObject instanceof IModelHandler || innerObject instanceof Model))
		{
			//add possible model connections
			TableRow modelTableRow = ProviderTable.createModelTable(this, innerObject,true, (innerObject instanceof IModelHandler ? R.string.str_model_conn : R.string.str_modelhandler_conn));
			if (modelTableRow != null)
			{
				tableLayout.addView(modelTableRow);
			}
		}
		if (innerObject != null && !(innerObject instanceof Model))
		{
			//add possible event providers
			// Do not add event provider for managed feedback
			if(!PipelineBuilder.getInstance().isManagedFeedback(innerObject))
			{
				TableRow eventTableRow = ProviderTable.createEventTable(this, innerObject,
																		(innerObject instanceof Transformer || innerObject instanceof Consumer)
																				|| (innerObject instanceof Sensor && options != null && options.length > 0));
				if (eventTableRow != null)
				{
					tableLayout.addView(eventTableRow);
				}
			}
		}
	}

	/**
	 * @param isFrameSize boolean
	 * @return TableRow
	 */
	private TableRow createTextView(final boolean isFrameSize)
	{
		TableRow tableRow = new TableRow(this);
		tableRow.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
		LinearLayout linearLayout = new LinearLayout(this);
		linearLayout.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
		linearLayout.setOrientation(LinearLayout.HORIZONTAL);
		linearLayout.setWeightSum(1.0f);
		//
		EditText editText = new EditText(this);
		editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
		editText.setEms(10);
		editText.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f));
		editText.setHint(R.string.str_frameSizeSmallest);
		editText.setText(isFrameSize
								 ? ((PipelineBuilder.getInstance().getFrameSize(innerObject) != null) ? String.valueOf(PipelineBuilder.getInstance().getFrameSize(innerObject)) : null)
								 : String.valueOf(PipelineBuilder.getInstance().getDelta(innerObject)));
		editText.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
			}

			@Override
			public void afterTextChanged(Editable s)
			{
				if (isFrameSize)
				{
					try
					{
						if (s == null || s.length() == 0)
						{
							PipelineBuilder.getInstance().setFrameSize(innerObject, null);
						}
						else
						{
							double d = Double.parseDouble(s.toString());
							if (d > 0)
							{
								PipelineBuilder.getInstance().setFrameSize(innerObject, d);
							}
						}
					}
					catch (NumberFormatException ex)
					{
						Log.w("Invalid input for frameSize double: " + s.toString());
					}
				}
				else
				{
					try
					{
						double d = Double.parseDouble(s.toString());
						if (d >= 0)
						{
							PipelineBuilder.getInstance().setDelta(innerObject, d);
						}
					}
					catch (NumberFormatException ex)
					{
						Log.w("Invalid input for delta double: " + s.toString());
					}
				}
			}
		});
		linearLayout.addView(editText);
		//
		TextView textView = new TextView(this);
		textView.setText(isFrameSize ? R.string.str_frameSize : R.string.str_delta);
		textView.setTextAppearance(this, android.R.style.TextAppearance_Medium);
		textView.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f));
		linearLayout.addView(textView);
		tableRow.addView(linearLayout);
		return tableRow;
	}

	/**
	 * @param frameSizeTableRow
	 * @param deltaTableRow
	 * @return
	 */
	private TableRow createConsumerTextView(Object mainObject, final TableRow frameSizeTableRow, final TableRow deltaTableRow)
	{
		TableRow tableRow = new TableRow(this);
		tableRow.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
		LinearLayout linearLayout = new LinearLayout(this);
		linearLayout.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
		linearLayout.setOrientation(LinearLayout.HORIZONTAL);
		linearLayout.setWeightSum(1.0f);

		TextView textView = new TextView(this);
		textView.setText(R.string.str_eventrigger);
		textView.setTextAppearance(this, android.R.style.TextAppearance_Medium);
		linearLayout.addView(textView);

		final Spinner eventTriggerList = new Spinner(this);
		ArrayList<Object> items = new ArrayList<>();
		items.add(null); //default element
		if(mainObject instanceof Trainer)
			items.add(PipelineBuilder.getInstance().getAnnotation()); //annotation channel
		items.addAll(Arrays.asList(PipelineBuilder.getInstance().getPossibleEventConnections(mainObject)));
		eventTriggerList.setAdapter(new ArrayAdapterWithNull(this, android.R.layout.simple_spinner_item, items, "<none>"));

		//preselect item
		Object selection = PipelineBuilder.getInstance().getEventTrigger(mainObject);
		if(selection == null)
		{
			eventTriggerList.setSelection(0);
		}
		else
		{
			for (int i = 0; i < items.size(); i++)
			{
				if (items.get(i) != null && items.get(i).equals(selection))
				{
					eventTriggerList.setSelection(i);
					break;
				}
			}
		}

		eventTriggerList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				setEnabledRecursive(frameSizeTableRow, position == 0);
				setEnabledRecursive(deltaTableRow, position == 0);

				Object src = parent.getItemAtPosition(position);
				PipelineBuilder.getInstance().setEventTrigger(innerObject, src);
			}

			@Override
			public void onNothingSelected(AdapterView parent)
			{
			}
		});
		linearLayout.addView(eventTriggerList);

		tableRow.addView(linearLayout);
		return tableRow;
	}

	private void setEnabledRecursive(final View view, final boolean enabled)
	{
		if (view instanceof ViewGroup)
		{
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
			{
				setEnabledRecursive(((ViewGroup) view).getChildAt(i), enabled);
			}
		}
		else
		{
			view.post(new Runnable()
			{
				@Override
				public void run()
				{
					view.setEnabled(enabled);
				}
			});
		}
	}

	/**
	 * @param savedInstanceState Bundle
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_options);
		init();
	}
}
