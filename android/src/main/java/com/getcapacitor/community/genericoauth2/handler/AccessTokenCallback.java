package com.getcapacitor.community.genericoauth2.handler;

public interface AccessTokenCallback {
    void onSuccess(String accessToken);

    void onCancel();

    void onError(Exception error);
}
