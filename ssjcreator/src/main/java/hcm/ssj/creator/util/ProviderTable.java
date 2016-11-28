/*
 * ProviderTable.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura
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

package hcm.ssj.creator.util;

import android.app.Activity;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import hcm.ssj.core.Provider;
import hcm.ssj.core.Sensor;
import hcm.ssj.creator.R;
import hcm.ssj.creator.core.Linker;

/**
 * Create a table row which includes viable providers
 * Created by Frank Gaibler on 19.05.2016.
 */
public class ProviderTable
{
    /**
     * @param activity   Activity
     * @param mainObject Object
     * @param dividerTop boolean
     * @return TableRow
     */
    public static TableRow createTable(Activity activity, final Object mainObject, boolean dividerTop)
    {
        TableRow tableRow = new TableRow(activity);
        tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        if (dividerTop)
        {
            //add divider
            linearLayout.addView(Util.addDivider(activity));
        }
        TextView textViewName = new TextView(activity);
        textViewName.setText(R.string.str_providers);
        textViewName.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        textViewName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        linearLayout.addView(textViewName);
        //get possible providers
        final Object[] objects = getProvider(mainObject);
        //
        if (objects.length > 0)
        {
            for (int i = 0; i < objects.length; i++)
            {
                CheckBox checkBox = new CheckBox(activity);
                checkBox.setText(objects[i].getClass().getSimpleName());
                checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
                Object[] providers = Linker.getInstance().getProviders(mainObject);
                if (providers != null)
                {
                    for (Object provider : providers)
                    {
                        if (objects[i].equals(provider))
                        {
                            checkBox.setChecked(true);
                            break;
                        }
                    }
                }
                final int count = i;
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                {
                    final Object o = objects[count];

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                    {
                        if (isChecked)
                        {
                            Linker.getInstance().addProvider(mainObject, (Provider) o);
                        } else
                        {
                            Linker.getInstance().removeProvider(mainObject, (Provider) o);
                        }
                    }
                });
                linearLayout.addView(checkBox);
            }
        }
        tableRow.addView(linearLayout);
        return tableRow;
    }

    /**
     * @return Object[]
     */
    private static Object[] getProvider(Object mainObject)
    {
        //add possible providers
        Object[] sensProvCandidates = Linker.getInstance().getAll(Linker.Type.SensorProvider);
        ArrayList<Object> alCandidates = new ArrayList<>();
        //only add sensorProviders for sensors
        if (!(mainObject instanceof Sensor))
        {
            alCandidates.addAll(Arrays.asList(Linker.getInstance().getAll(Linker.Type.Transformer)));
            //remove itself
            for (int i = 0; i < alCandidates.size(); i++)
            {
                if (mainObject.equals(alCandidates.get(i)))
                {
                    alCandidates.remove(i);
                }
            }
        }
        alCandidates.addAll(0, Arrays.asList(sensProvCandidates));
        return alCandidates.toArray();
    }
}