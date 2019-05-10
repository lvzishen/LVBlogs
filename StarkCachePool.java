package com.cleanerapp.filesgo.stark;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.ads.lib.AdUnitId;
import com.ads.lib.prop.AdCacheProp;
import com.ads.lib.prop.AdPositionIdProp;
import com.ads.lib.prop.AdStrategyProp;
import com.cleanerapp.filesgo.AppConfig;

import org.saturn.stark.openapi.CachePoolAdType;
import org.saturn.stark.openapi.StarkCachePoolParameter;
import org.saturn.stark.openapi.StarkSDK;

public class StarkCachePool {

    private static final String TAG = "StarkCachePool";
    private static final boolean DEBUG = AppConfig.DEBUG;

    public static void initStarkCachePool(Context context,String processName) {

        StarkCachePoolParameter nativeResultPoolParam = getCachePoolParameterBuilder(context,
                CachePoolAdType.AD_CACHE_POOL_NATIVE_ONLY, AdUnitId.PUBLIC_RESULT_PAGE_NATIVE_UNIT_ID, AdUnitId.AdPositionIdKey.KEY_PID_PUBLIC_RESULT_PAGE_NATIVE,processName);
        StarkCachePoolParameter nativeVirusPoolParam = getCachePoolParameterBuilder(context,
                CachePoolAdType.AD_CACHE_POOL_NATIVE_ONLY, AdUnitId.PUBLIC_RESULT_PAGE_VIRUS_NATIVE_UNIT_ID, AdUnitId.AdPositionIdKey.KEY_PID_PUBLIC_RESULT_PAGE_VIRUS_NATIVE,processName);
        StarkCachePoolParameter interResultPoolParam = getCachePoolParameterBuilder(context,
                CachePoolAdType.AD_CACHE_POOL_INTERSTITIAL, AdUnitId.PUBLIC_RESULT_PAGE_INTER_UNIT_ID, AdUnitId.AdPositionIdKey.KEY_PID_PUBLIC_RESULT_PAGE_INTER,processName);
        StarkCachePoolParameter interVirusPoolParam = getCachePoolParameterBuilder(context,
                CachePoolAdType.AD_CACHE_POOL_INTERSTITIAL, AdUnitId.PUBLIC_RESULT_PAGE_VIRUS_INTER_UNIT_ID, AdUnitId.AdPositionIdKey.KEY_PID_PUBLIC_RESULT_PAGE_VIRUS_INTER,processName);
        StarkCachePoolParameter nativeBannerPoolParam = getCachePoolParameterBuilder(context,
                // TODO: 2019/3/12  由于native banner类型的广告池不能与native类型的透池，与@云峰讨论之后决定此处使用AD_CACHE_POOL_BANNER类型，
                // TODO: 2019/3/12 如添加其他的banner类型的广告池，需要注意此处！！！
                CachePoolAdType.AD_CACHE_POOL_BANNER, AdUnitId.PUBLIC_NATIVE_BANNER_UNIT_ID, AdUnitId.AdPositionIdKey.KEY_PID_PUBLIC_NATIVE_BANNER,processName);

        StarkSDK.registerAdCachePool(
                nativeVirusPoolParam,
                nativeResultPoolParam,
                interVirusPoolParam,
                interResultPoolParam,
                nativeBannerPoolParam
        );

    }

    private static StarkCachePoolParameter getCachePoolParameterBuilder(Context context, CachePoolAdType cachePoolAdType, String unitId, String pIdKey,String processName) {

        AdPositionIdProp adPositionIdProp = AdPositionIdProp.getInstance(context);
        AdCacheProp adCacheProp = AdCacheProp.getInstance(context);
        AdStrategyProp adStrategyProp = AdStrategyProp.getInstance(context);

        String adPositionId = adPositionIdProp.getAdPositionIdByAdUnitId(unitId);
        if (DEBUG) {
            Log.d(TAG, "#getCachePoolParameterBuilder pIdKey = " + pIdKey + "; adPositionId = " + adPositionId);
            Log.d(TAG, "#getCachePoolParameterBuilder pIdKey = " + pIdKey + "; defaultStrategy = " + adStrategyProp.getDefaultStrategy(pIdKey));
            Log.d(TAG, "#setDefaultStrategy = " +adStrategyProp.getDefaultStrategy(pIdKey));
            Log.d(TAG, "#setRegisterProcess = " +processName);
            Log.d(TAG, "#setPrepareBanner = " + adCacheProp.isPrepareBanner(pIdKey));
            Log.d(TAG, "#setPrepareIcon = " + adCacheProp.isPrepareIcon(pIdKey));
            Log.d(TAG, "#setMuted = " + adCacheProp.isMuted(pIdKey));
            Log.d(TAG, "#setParallelCount = " + adCacheProp.getParallelCount(pIdKey));
            Log.d(TAG, "#setInventory = " + adCacheProp.getInventory(pIdKey));
            Log.d(TAG, "--------------------------------------------------------");
        }
        if (TextUtils.isEmpty(adPositionId)) {
            return null;
        }

        StarkCachePoolParameter.Builder starkCachePoolParameterBuilder = new StarkCachePoolParameter.Builder(cachePoolAdType, unitId, adPositionId)
                .setDefaultStrategy("ab:ca-app-pub-3940256099942544/2247696110")
                .setRegisterProcess(processName)
                .setPrepareBanner(adCacheProp.isPrepareBanner(pIdKey))
                .setPrepareIcon(adCacheProp.isPrepareIcon(pIdKey))
                .setMuted(adCacheProp.isMuted(pIdKey))
                .setParallelCount(adCacheProp.getParallelCount(pIdKey))
                .setInventory(adCacheProp.getInventory(pIdKey));

        StarkCachePoolParameter starkCachePoolParameter = starkCachePoolParameterBuilder.build();

        if (DEBUG) {
            Log.d(TAG, "#getCachePoolParameterBuilder " + starkCachePoolParameter);
        }

        return starkCachePoolParameter;
    }
}
