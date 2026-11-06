package org.jellyfin.mobile.api;

import android.content.Context;
import java.io.IOException;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.interaction.VolleyHttpClient;
import org.jellyfin.apiclient.interaction.http.HttpRequest;
import org.jellyfin.apiclient.interaction.http.IAsyncHttpClient;
import org.jellyfin.apiclient.logging.ILogger;
import org.jellyfin.mobile.R;
import org.jellyfin.mobile.playback.NetworkGate;

/** Also guard legacy/background SDK calls before a request is queued. */
public final class GuardedHttpClient implements IAsyncHttpClient {
    private final Context context;
    private final VolleyHttpClient delegate;
    public GuardedHttpClient(Context context,ILogger logger) {
        this.context=context.getApplicationContext();
        delegate=new VolleyHttpClient(logger,this.context);
    }
    @Override public void Send(HttpRequest request,Response<String> response) {
        if(!NetworkGate.allows(context)) {
            if(response!=null)response.onError(new IOException(context.getString(R.string.gen5w_network_required)));
            return;
        }
        delegate.Send(request,response);
    }
}
