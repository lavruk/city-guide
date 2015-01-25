package com.lavruk.cityguide;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.lavruk.cityguide.net.Api;
import com.lavruk.cityguide.tools.OkHttpStack;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;

@Module(injects = {
        MainActivity.class,
        PlacesFragment.class,
        Api.class
})
public final class MainModule {

    private final App app;

    public MainModule(App app) {
        this.app = app;
    }

    @Provides @Singleton Application provideApplication() {
        return app;
    }

    @Provides @Singleton public EventBus provideEventBus() {
        return EventBus.getDefault();
    }

    @Provides @Singleton public Api provideApi(Application app) {
        return new Api(app);
    }

    @Provides @Singleton public RequestQueue provideRequestQueue(Application app) {
        return Volley.newRequestQueue(app, new OkHttpStack());
    }

}
