package org.example.prjbrowser.model;

import java.util.function.Consumer;

public class CookieBridge {
    private Consumer<String> onCookieReceived;

    public CookieBridge(Consumer<String> callback) {
        this.onCookieReceived = callback;
    }

    public void setCookie(String cookieString) {
        if (onCookieReceived != null) {
            onCookieReceived.accept(cookieString);
        }
    }
}
