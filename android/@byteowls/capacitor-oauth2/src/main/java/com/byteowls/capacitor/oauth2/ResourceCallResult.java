package com.byteowls.capacitor.oauth2;

import com.getcapacitor.JSObject;

/**
 * @author m.oberwasserlechner@byteowls.com
 */
public class ResourceCallResult {

    private boolean error;
    private String errorMsg;
    private JSObject response;

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public JSObject getResponse() {
        return response;
    }

    public void setResponse(JSObject response) {
        this.response = response;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
