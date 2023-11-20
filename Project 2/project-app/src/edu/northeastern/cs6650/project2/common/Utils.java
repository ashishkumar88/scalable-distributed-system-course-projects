package edu.northeastern.cs6650.project2.common;
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

import edu.northeastern.cs6650.project2.common.RequestType;
import edu.northeastern.cs6650.project2.common.ServerResponseCode;

/*
 * Utility class containing common utility functions
 */
public class Utils {

    public static String END_OF_MESSAGE = "EOM";
    public static final int SERVER_TIMEOUT = 5000;
    public static final String KEY_VALUE_STORE_RMI_SERVER_NAME = "KeyValueStoreApp";


    public static boolean isKeyValid(String key) {

        if(key == null || key.trim() == "" || key.trim().length() == 0) {
            return false;
        }

        return true;

    }

    public static boolean isValueValid(String value) {

        if(value == null || value.trim() == "" || value.trim().length() == 0) {
            return false;
        }

        return true;

    }

    /*
     * Creates a request body for the GET and DELETE request types
     * 
     * This is common for both the TCP and UDP client
     */
    public static StringBuffer createRequestBody(RequestType requestType, String key) {

        StringBuffer requestBody = new StringBuffer();
        requestBody.append(requestType.name());
        requestBody.append("\n");
        requestBody.append(key);
        requestBody.append("\n");
        requestBody.append(END_OF_MESSAGE);
        return requestBody;

    }

    /*
     * Creates a request body the PUT request type
     * 
     * This is common for both the TCP and UDP client
     */
    public static StringBuffer createRequestBody(RequestType requestType, String key, String value) {

        StringBuffer requestBody = new StringBuffer();
        requestBody.append(requestType.name());
        requestBody.append("\n");
        requestBody.append(key);
        requestBody.append("\n");
        requestBody.append(value);
        requestBody.append("\n");
        requestBody.append(END_OF_MESSAGE);
        return requestBody;

    }

    /*
     * Creates a response body that is sent back to the client by the server
     * 
     * This is common for both the TCP and UDP client
     */
    public static StringBuffer createResponseBody(ServerResponseCode serverResponseCode, String message) {

        StringBuffer responseBody = new StringBuffer();
        responseBody.append(serverResponseCode.name());
        responseBody.append("\n");
        responseBody.append(message);
        responseBody.append("\n");
        responseBody.append(END_OF_MESSAGE);
        return responseBody;

    }
}
