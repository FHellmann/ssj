/*
 * Landmark.java
 * Copyright (c) 2021
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

package hcm.ssj.ssd;

/**
 * Created by Michael Dietz on 12.01.2021.
 */
public class Landmark {
    public float x;
    public float y;
    public float z;
    public float visibility;

    public Landmark(float visibility) {
        this.visibility = visibility;
    }

    public Landmark(float x, float y) {
        this(x, y, 0.0f);
    }

    public Landmark(float x, float y, float z) {
        this(x, y, z, 1.0f);
    }

    public Landmark(float x, float y, float z, float visibility) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.visibility = visibility;
    }
}