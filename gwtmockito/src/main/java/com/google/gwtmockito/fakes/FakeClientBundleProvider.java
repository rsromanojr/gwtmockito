/*
 * Copyright 2013 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwtmockito.fakes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ResourceCallback;
import com.google.gwt.resources.client.ResourcePrototype;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;

/**
 * Provides fake implementations of {@link ClientBundle}s. Any methods in the
 * bundle returning {@link CssResource}s will GWT.create the {@link CssResource}
 * (which by default will cause it to be generated by
 * {@link FakeMessagesProvider}. Other types of resources will be generated to
 * return unique values from their getText() or getSafeUri() methods.
 *
 * @author ekuefler@google.com (Erik Kuefler)
 */
public class FakeClientBundleProvider implements FakeProvider<ClientBundle> {

  /**
   * Returns a new instance of the given type that implements methods as
   * described in the class description.
   *
   * @param type interface to be implemented by the returned type.
   */
  @Override
  public ClientBundle getFake(Class<?> type) {
    return (ClientBundle) Proxy.newProxyInstance(
        FakeClientBundleProvider.class.getClassLoader(),
        new Class<?>[] {type},
        new InvocationHandler() {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
            Class<?> returnType = method.getReturnType();
            if (CssResource.class.isAssignableFrom(returnType)) {
              return GWT.create(returnType);
            } else {
              return createFakeResource(returnType, method.getName());
            }
          }
        });
  }
  
  /**
   * Creates a fake resource class that returns its own name where possible.
   */
  @SuppressWarnings("unchecked") // safe since the proxy implements type
  private <T> T createFakeResource(Class<T> type, final String name) {
    return (T) Proxy.newProxyInstance(
        FakeClientBundleProvider.class.getClassLoader(),
        new Class<?>[] {type},
        new InvocationHandler() {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
            Class<?> returnType = method.getReturnType();
            if (returnType == String.class) {
              return name;
            } else if (returnType == SafeHtml.class) {
              return SafeHtmlUtils.fromTrustedString(name);
            } else if (returnType == SafeUri.class) {
              return UriUtils.fromTrustedString(name);
            } else if (returnType == boolean.class) {
              return false;
            } else if (returnType == int.class) {
              return 0;
            } else if (method.getParameterTypes()[0] == ResourceCallback.class) {
              // Read the underlying resource type out of the generic parameter
              // in the method's argument
              Class<?> resourceType = 
                  (Class<?>) 
                  ((ParameterizedType) args[0].getClass().getGenericInterfaces()[0])
                  .getActualTypeArguments()[0];
              ((ResourceCallback<ResourcePrototype>) args[0]).onSuccess(
                  (ResourcePrototype) createFakeResource(resourceType, name));
              return null;
            } else {
              throw new IllegalArgumentException(
                  "Unexpected return type for method " + method.getName());
            }
          }
        });
  }
}
