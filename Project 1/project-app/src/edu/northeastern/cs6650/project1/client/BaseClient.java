package edu.northeastern.cs6650.project1.client;
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

import java.util.logging.Logger;

import edu.northeastern.cs6650.project1.common.RequestType;
import edu.northeastern.cs6650.project1.common.ServerResponseCode;

/*
 * Base abstract class for the TCP and UDP client classes
 */
public abstract class BaseClient {

    private static final Logger LOGGER = Logger.getLogger(BaseClient.class.getName());

    public abstract void makeServerRequest(StringBuffer requestBody);

    /*
     * Processes a response body received from a server
     * 
     * This is common for both the TCP and the UDP clients.
     */
    protected void processServerResponse(StringBuffer responseBody) {

        String[] lines = responseBody.toString().split("\n");

        // Response from a server in this application always has three lines
        if(lines.length != 3) {
            LOGGER.severe("Invalid response from the server.");
        } else {
            try {
                ServerResponseCode serverResponseCode = ServerResponseCode.parseString(lines[0]);
                switch(serverResponseCode) {
                    case SUCCESS:
                        LOGGER.info(lines[1]);
                        break;
                    case FAILED:
                    case BAD_REQUEST:
                        LOGGER.severe("Error processing request by the server : " + lines[1]);
                }
            } catch (Exception exp) {
                LOGGER.severe("Error processing response from the server.\n" + exp.getMessage());                
            }
        }
        
    }
}
