package com.wyw.photodemo;

import android.app.Application;

/**
 * 项目名称：PhotoDemo
 * 类描述：
 * 创建人：伍跃武
 * 创建时间：2017/11/24 15:38
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        appInstance = this;
    }

    /**
     * 获得Application对象
     */
    private static App appInstance;

    public static App getInstance() {
        return appInstance;
    }
}
