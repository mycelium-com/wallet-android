package com.mycelium.wallet.activity;

import android.support.test.espresso.IdlingResource;
import android.view.View;

public class ViewVisibleIdlingResource implements IdlingResource {

  private final View view;
  private ResourceCallback resourceCallback;

  public ViewVisibleIdlingResource(View view, boolean shouldBeVisibleWhenIdle) {
    this.view = view;
  }

  @Override
  public String getName() {
    return ViewVisibleIdlingResource.class.getName() + ":" + view.getClass();
  }

  @Override
  public boolean isIdleNow() {

    boolean idle = view.getVisibility() == View.VISIBLE;
    if (idle) {
      resourceCallback.onTransitionToIdle();
    }
    return idle;
  }

  @Override
  public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
    this.resourceCallback = resourceCallback;
  }
}
