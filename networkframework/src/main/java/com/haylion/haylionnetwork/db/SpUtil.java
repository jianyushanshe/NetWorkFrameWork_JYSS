package com.haylion.haylionnetwork.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:wangjianming
 * Time:2018/11/15
 * Description:
 */
public class SpUtil {
    private SharedPreferences sp;
    private Editor editor;
    public static final String TAG = "PushDemoActivity";
    public static final String RESPONSE_METHOD = "method";
    public static final String RESPONSE_CONTENT = "content";
    public static final String RESPONSE_ERRCODE = "errcode";
    protected static final String ACTION_LOGIN = "com.baidu.pushdemo.action.LOGIN";
    public static final String ACTION_MESSAGE = "com.baiud.pushdemo.action.MESSAGE";
    public static final String ACTION_RESPONSE = "bccsclient.action.RESPONSE";
    public static final String ACTION_SHOW_MESSAGE = "bccsclient.action.SHOW_MESSAGE";
    protected static final String EXTRA_ACCESS_TOKEN = "access_token";
    public static final String EXTRA_MESSAGE = "frist_location";
    public static String logStringCache = "";

    public SpUtil(Context context, String fileName) {
        sp = context.getSharedPreferences(fileName, context.MODE_PRIVATE);
        editor = sp.edit();
    }

    // 保存值
    public void save(String key, Object value) {
        if (value instanceof Integer) {
            editor.putInt(key, (Integer) value).apply();
        } else if (value instanceof String) {
            editor.putString(key, (String) value).apply();
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value).apply();
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value).apply();
        }
    }

    // 获取值 参数：cla:获取数据的类型
    public Object getData(String key, Class cla, Object obj) {
        String strContent;
        int intContent;
        boolean booleanContent;
        long longContent;
        if (cla == Integer.class) {
            intContent = sp.getInt(key, (Integer) obj);
            return intContent;
        } else if (cla == String.class) {
            strContent = sp.getString(key, (String) obj);
            return strContent;
        } else if (cla == Boolean.class) {
            booleanContent = sp.getBoolean(key, (Boolean) obj);
            return booleanContent;
        } else if (cla == Long.class) {
            longContent = sp.getLong(key, (Long) obj);
            return longContent;
        } else {
            return null;
        }
    }

    // 获取ApiKey
    public static String getMetaValue(Context context, String metaKey) {
        Bundle metaData = null;
        String apiKey = null;
        if (context == null || metaKey == null) {
            return null;
        }
        try {
            ApplicationInfo ai = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(),
                            PackageManager.GET_META_DATA);
            if (null != ai) {
                metaData = ai.metaData;
            }
            if (null != metaData) {
                apiKey = metaData.getString(metaKey);
            }
        } catch (NameNotFoundException e) {

        }
        return apiKey;
    }

    // 用share preference来实现是否绑定的开关。在ionBind且成功时设置true，unBind且成功时设置false
    public static boolean hasBind(Context context) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(context);
        String flag = sp.getString("bind_flag", "");
        if ("ok".equalsIgnoreCase(flag)) {
            return true;
        }
        return false;
    }

    public static void setBind(boolean flag, Context context) {
        String flagStr = "not";
        if (flag)
            flagStr = "ok";
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(context);
        Editor editor = sp.edit();
        editor.putString("bind_flag", flagStr);
        editor.apply();
    }

    public static List<String> getTagsList(String originalText) {
        if (originalText == null || originalText.equals("")) {
            return null;
        }
        List<String> tags = new ArrayList<String>();
        int indexOfComma = originalText.indexOf(',');
        String tag;
        while (indexOfComma != -1) {
            tag = originalText.substring(0, indexOfComma);
            tags.add(tag);

            originalText = originalText.substring(indexOfComma + 1);
            indexOfComma = originalText.indexOf(',');
        }

        tags.add(originalText);
        return tags;
    }

    public static String getLogText(Context context) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(context);
        return sp.getString("log_text", "");
    }

    /**
     * @param context
     * @param text
     */
    public static void setLogText(Context context, String text) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(context);
        Editor editor = sp.edit();
        editor.putString("log_text", text);
        editor.apply();
    }


}
