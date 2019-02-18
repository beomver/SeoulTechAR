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
package ar;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContextWrapper;
import android.content.Intent;
import android.widget.Toast;

import ar.mgr.location.LocationFinder;
import ar.mgr.location.LocationFinderFactory;
import ar.mgr.webcontent.WebContentManager;
import ar.mgr.webcontent.WebContentManagerFactory;
import kunpeng.ar.lib.MixContextInterface;
import kunpeng.ar.lib.render.Matrix;

/**
 * Cares about location management and about the data (source, inputstream)
 */
public class MixContext extends ContextWrapper implements MixContextInterface {

    // TAG for logging
    public static final String TAG = "AR";

    private ARView mixView;

    private Matrix rotationM = new Matrix();

    /** Responsible for all location tasks */
    private LocationFinder locationFinder;

    /** Responsible for Web Content */
    private WebContentManager webContentManager;

    /** Toast POPUP notification*/
    private Toast msgPopUp;

    public MixContext(ARView appCtx) {
        super(appCtx);
        mixView = appCtx;

        // TODO: RE-ORDER THIS SEQUENCE... IS NECESSARY?

        rotationM.toIdentity();
        getLocationFinder().switchOn();
        getLocationFinder().findLocation();
    }

    public String getStartUrl() {
        Intent intent = ((Activity) getActualMixView()).getIntent();
        if (intent.getAction() != null
                && intent.getAction().equals(Intent.ACTION_VIEW)) {
            return intent.getData().toString();
        } else {
            return "";
        }
    }

    public void getRM(Matrix dest) {
        synchronized (rotationM) {
            dest.set(rotationM);
        }
    }

    /**
     * Shows a webpage with the given url when clicked on a marker.
     */
    public void loadMixViewWebPage(String url) throws Exception {
        // TODO: CHECK INTERFACE METHOD
        getWebContentManager().loadWebPage(url, getActualMixView());
    }

    public void doResume(ARView mixView) {
        setActualMixView(mixView);
    }

    public void updateSmoothRotation(Matrix smoothR) {
        synchronized (rotationM) {
            rotationM.set(smoothR);
        }
    }

    public LocationFinder getLocationFinder() {
        if (this.locationFinder == null) {
            locationFinder = LocationFinderFactory.makeLocationFinder(this);
        }
        return locationFinder;
    }

    public WebContentManager getWebContentManager() {
        if (this.webContentManager == null) {
            webContentManager = WebContentManagerFactory
                    .makeWebContentManager(this);
        }
        return webContentManager;
    }

    public ARView getActualMixView() {
        synchronized (mixView) {
            return this.mixView;
        }
    }

    private void setActualMixView(ARView mv) {
        synchronized (mixView) {
            this.mixView = mv;
        }
    }

    public ContentResolver getContentResolver() {
        ContentResolver out = super.getContentResolver();
        if (super.getContentResolver() == null) {
            out = getActualMixView().getContentResolver();
        }
        return out;
    }

    /**
     * Cancel Toast POPUP notification
     */
    public void cancelMsgPopUp() {
        if (msgPopUp != null) {
            msgPopUp.cancel();
        }
    }

    /**
     * Toast POPUP notification
     *
     * @param string message
     */
    public void doMsgPopUp(final String string) {
        if (msgPopUp == null) {
            msgPopUp = Toast.makeText(this.mixView, string, Toast.LENGTH_LONG);
        } else {
            msgPopUp.setText(string);
            msgPopUp.setDuration(Toast.LENGTH_LONG);
        }
        msgPopUp.show();
    }

    /**
     * Toast POPUP notification
     */
    public void doMsgPopUp(int RidOfString) {
        doMsgPopUp(this.getString(RidOfString));
    }
}
