package edu.northeastern.cs6650.project1.server;
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

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import edu.northeastern.cs6650.project1.common.ServerResponseCode;
import edu.northeastern.cs6650.project1.common.Utils;

/*
 * Base abstract class for the TCP and UDP server classes
 */
public abstract class BaseServer {

    private static final Logger LOGGER = Logger.getLogger(BaseServer.class.getName());

    public abstract void spin();
    
    protected Map<String, String> keyValueStore = new HashMap<String, String>();

    /*
     * Process a request body sent by either a TCP or UDP client
     */
    protected String processRequest(StringBuffer requestBody, String clientAddress, int clientPort) throws IllegalArgumentException, NoSuchElementException {

        String requestBodyAsString = requestBody.toString();

        if(requestBodyAsString.startsWith("GET", 0)) {
            return  processGETRequest(requestBodyAsString, clientAddress, clientPort);
        } else if(requestBodyAsString.startsWith("PUT", 0)) {
            return processPUTRequest(requestBodyAsString, clientAddress, clientPort);
        } else if(requestBodyAsString.startsWith("DELETE", 0)) {
            return processDELETERequest(requestBodyAsString, clientAddress, clientPort);
        } else {
            throw new IllegalArgumentException("Request type is not supported.");
        }

    }

    /*
     * Given a request body, verify that it contains the right number of lines and return the lines as a string array
     */
    protected String[] verifyAndGetRequestLines(String requestBody, int expectedNumberOfLines) throws IllegalArgumentException {
        String[] lines = requestBody.split("\n");

        if(lines.length != expectedNumberOfLines) {
            throw new IllegalArgumentException("The request body is invalid.");
        }

        return lines;
    }

    /*
     * Process a request to get a value from the local key - value storage given a valid key
     */
    protected String processGETRequest(String requestBody, String clientAddress, int clientPort) throws IllegalArgumentException, NoSuchElementException {
        String lines[] = verifyAndGetRequestLines(requestBody, 3);

        if(!Utils.isKeyValid(lines[1])) {
            throw new IllegalArgumentException("The key is empty or invalid.");
        }

        LOGGER.info(clientAddress + ":" + String.valueOf(clientPort) + "::" + "The key received for GET request is : " + lines[1] + ".");

        // Check if local key value storage contains a key, fetch and return the corresponding value
        // If key does not exist, throw a NoSuchElementException
        if(keyValueStore.containsKey(lines[1])) {
            String valueString = keyValueStore.get(lines[1]);

            return "GET request was successfully processed. The value is : " + valueString; 
        } else {
            throw new NoSuchElementException("This key does not exist in the system : "+ lines[1] + ".");
        }
    }

    /*
     * Process a request to put a key-value pair into the local key - value storage
     */
    protected String processPUTRequest(String requestBody, String clientAddress, int clientPort) {
        String lines[] = verifyAndGetRequestLines(requestBody, 4);

        if(!(Utils.isKeyValid(lines[1]) || Utils.isValueValid(lines[2]))) {
            throw new IllegalArgumentException("The key or value or both are invalid.");
        }

        LOGGER.info(clientAddress + ":" + String.valueOf(clientPort) + "::" + "The key received for PUT request is : " + lines[1] + ".\nThe value received for PUT request is : " + lines[2] + ".");

        keyValueStore.put(lines[1], lines[2]);
        return "PUT request was successfully processed.";
    }

    /*
     * Process a request to delete a key from the local key - value storage given a valid key
     */
    protected String processDELETERequest(String requestBody, String clientAddress, int clientPort) {
        String lines[] = verifyAndGetRequestLines(requestBody, 3);

        if(!Utils.isKeyValid(lines[1])) {
            throw new IllegalArgumentException("The key is empty or invalid.");
        }

        LOGGER.info(clientAddress + ":" + String.valueOf(clientPort) + "::" + "The key received for DELETE request is : " + lines[1]);

        // Check if local key value storage contains a key, then delete the key
        // If key does not exist, throw a NoSuchElementException
        if(keyValueStore.containsKey(lines[1])) {
            keyValueStore.remove(lines[1]);

            return "DELETE request was successfully processed."; 
        } else {
            throw new NoSuchElementException("This key does not exist in the system : "+ lines[1] + ".");
        }
    }
    
}
