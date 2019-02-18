/*
 * Copyright (C) 2012- Peer internet solutions & Finalist IT Group
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
package ar.mgr.location;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import ar.MixContext;

class LocationObserver implements LocationListener {
	
	private LocationMgrImpl myController;

	public LocationObserver(LocationMgrImpl myController) {
		super();
		this.myController=myController;
	}

	public void onLocationChanged(Location location) {
		Log.d(MixContext.TAG, "Normal Location Changed: " + location.getProvider()
						+ " lat: " + location.getLatitude() + " lon: "
						+ location.getLongitude() + " alt: "
						+ location.getAltitude() + " acc: "
						+ location.getAccuracy());
		try {
			Log.v(MixContext.TAG, "Location Changed: " + location.getProvider()
							+ " lat: " + location.getLatitude() + " lon: "
							+ location.getLongitude() + " alt: "
							+ location.getAltitude() + " acc: "
							+ location.getAccuracy());
			myController.setPosition(location);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

	@Override
	public void onProviderEnabled(String provider) {

	}

	@Override
	public void onProviderDisabled(String provider) {

	}
}
