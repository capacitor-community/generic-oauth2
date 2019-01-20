package com.byteowls.capacitor.oauth2.api;

public interface AccessTokenCallback {

  void onSuccess(String accessToken);

  void onCancel();

  void onError(Exception error);

}
