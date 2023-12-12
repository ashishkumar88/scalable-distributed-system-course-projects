package edu.northeastern.cs6650.project4.common;
/**
 * Copyright 2023 Ashish Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import edu.northeastern.cs6650.project4.common.RequestType;
import java.io.Serializable;

/*
* This class is used to store the data which is sent by the client to the server.
*/
public class RequestObject implements Serializable {
    private String key;
    private String value;
    private RequestType requestType;

    public RequestObject(RequestType requestType, String key, String value) {
        this.key = key;
        this.value = value;
        this.requestType = requestType; 
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public void setValue(String value) {
        this.value = value;
    }
}