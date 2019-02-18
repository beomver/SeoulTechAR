/*
 * Copyright (C) 2010- Peer internet solutions
 * 
 * This file is part of AR Navigator.
 * 
 * This program is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version. 
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details. 
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package ar.gui;

import android.graphics.Color;

import ar.DataView;
import ar.data.DataHandler;
import kunpeng.ar.lib.gui.PaintScreen;
import kunpeng.ar.lib.gui.ScreenObj;
import kunpeng.ar.lib.marker.Marker;

/** Takes care of the small radar in the top left corner and of its points
 * @author daniele
 *
 */
public class RadarPoints implements ScreenObj {
	/** The screen */
	public DataView view;
	/** The radar's range */
	float range;
	/** Radius in pixel on screen */
	public static float RADIUS = 80;
	/** Position on screen */
	static float originX = 0 , originY = 0;
	/** Color */
	static int radarColor = Color.argb(100, 0, 0, 200);
	
	public void paint(PaintScreen dw) {
		/** radius is in KM. */
		range = view.getRadius() * 1000;
		/** Draw the radar */
		dw.setFill(true);
		dw.setColor(Color.argb(100, 0, 0, 200));
		dw.paintCircle(originX + RADIUS, originY + RADIUS, RADIUS);
		dw.setFill(false);
		dw.setColor(Color.argb(250, 100, 100, 100));
		for(float i = 1.0f;i > 0;i -= 0.2)
		{
			dw.paintCircle(originX + RADIUS, originY + RADIUS, RADIUS*i);
		}
		dw.setFill(true);
		/** put the markers in it */
		float scale = range / RADIUS;

		DataHandler jLayer = view.getDataHandler();

		for (int i = 0; i < jLayer.getMarkerCount(); i++) {
			Marker pm = jLayer.getMarker(i);
			float x = pm.getLocationVector().x / scale;
			float y = pm.getLocationVector().z / scale;

			if (pm.isActive() && (x * x + y * y < RADIUS * RADIUS)) {
				dw.setFill(true);
				
				// For OpenStreetMap the color is changing based on the URL
				dw.setColor(Color.argb(150, 200, 0, 0));
				
				dw.paintRect(x + RADIUS - 1, y + RADIUS - 1, 4, 4);
			}
		}
	}

	/** Width on screen */
	public float getWidth() {
		return RADIUS * 2;
	}

	/** Height on screen */
	public float getHeight() {
		return RADIUS * 2;
	}
}

