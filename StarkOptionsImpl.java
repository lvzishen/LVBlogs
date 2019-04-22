package com.cleanerapp.filesgo.stark;

import android.content.Context;
import android.util.Log;

import com.cleanerapp.filesgo.AppConfig;

import org.adoto.xut.AdotoUserTagKeys;
import org.adoto.xut.AdotoUserTagSDK;
import org.saturn.config.StarkRemoteConfig;
import org.saturn.stark.openapi.IAllowLoaderAdListener;
import org.saturn.stark.openapi.StarkAdType;

/**
 * Created by zhaozhiwen on 2018/4/13.
 */

public class StarkOptionsImpl implements IAllowLoaderAdListener {
    private static final String TAG = "StarkOptionsImpl";
    private static final boolean DEBUG = AppConfig.DEBUG;
    private Context mContext;

    public StarkOptionsImpl(Context c) {
        mContext = c;
    }

    @Override
    public boolean isAllowLoaderAd(String s, String s1, StarkAdType starkAdType) {
        String location = AdotoUserTagSDK.getUserTagKeyWordInfo(AdotoUserTagKeys.USER_TAG_KEY_CCS,"");

        if (DEBUG) {
            Log.i(TAG, "isAllowLoaderAd: " + location + "===有没有");
            return true;
        }

        return StarkRemoteConfig.adGeneralControlEnable(mContext, location);
      // return true;
    }
}
