package com.lavruk.cityguide;

import android.app.Application;
import android.content.Context;

import java.util.Arrays;
import java.util.List;

import dagger.ObjectGraph;
import timber.log.Timber;
import timber.log.Timber.DebugTree;

public class App extends Application {

    private ObjectGraph mObjectGraph;

    @Override public void onCreate() {
        super.onCreate();
        mObjectGraph = ObjectGraph.create(getModules().toArray());

        if (BuildConfig.DEBUG) {
            Timber.plant(new DebugTree());
        }
    }

    public void inject(Object o) {
        mObjectGraph.inject(o);
    }

    protected List<Object> getModules() {
        return Arrays.<Object>asList(new MainModule(this));
    }

    public static void inject(Context context, Object object) {
        ((App) context.getApplicationContext()).inject(object);
    }
}
