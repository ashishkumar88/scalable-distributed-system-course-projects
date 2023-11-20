package edu.northeastern.cs6650.project3.client;
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

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.io.StringWriter;
import java.io.PrintWriter;

import edu.northeastern.cs6650.project3.client.KeyValueStoreRMIClient;
import edu.northeastern.cs6650.project3.common.Utils;
import edu.northeastern.cs6650.project3.common.RequestType;

/*
 * Implementation of the client applicatio
 * 
 * This client creates a RMI client that invokes methods corresponding to GET, PUT and DELETE remote objects
 * based on user INPUT after the program launch. At the time of launch, 4 arguments can be provided, of which
 * 2 are required and 2 are optional. The required and optional arguments are described below.
 * Required command line arguments:
 * 1. <server ip> - IP address of the server hosting the registry as well as implementation of the RMI methods
 * for this application.
 * 2. <server port> - Based port that the server was initialized with. For example, if the server program was launched
 * on port 5000, then the client should be launched with 5000 as the port number.
 * 
 * Optional command line arguments:
 * 3. true/false - The third argument when set to false will not prepopulate the server with data. Default behavior
 * is that the client prepopulates the server with data.
 * 4. true/false - he fourth argument when set to false will not run the required 5 operations. Default behavior
 * is that the client run the default 5 operations.
 *  
 */

import edu.northeastern.cs6650.project3.common.Utils;

public class Client 
{
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

    public static void main(String[] arguments)
    {
        if(arguments.length < 2) {
            LOGGER.severe("Incorrect number of arguments. Correct usage: java -classpath classes edu.northeastern.cs6650.project3.client.Client <server ip address> <server port number> false(optional) false(optional).");
        } else {
            try {

                // Fetch user arguments
                String serverIPAddress = arguments[0];
                int serverPort = Integer.parseInt(arguments[1]);
                boolean isPrepopulateRequired = true;
                boolean isRequiredOperationsRequired = true;

                // Create a client based on the server type
                KeyValueStoreRMIClient client = new KeyValueStoreRMIClient(serverIPAddress, serverPort);

                // Parse the remaining arguments
                if(arguments.length > 2) {
                    if(arguments[2].equals("false")) {
                        isPrepopulateRequired = false;
                    }
                }

                if(arguments.length > 3) {
                    if(arguments[3].equals("false")) {
                        isRequiredOperationsRequired = false;
                    }
                }
                
                // Perform the required 5 PUT, GET and DELETE operations if the 3rd argument is not set to false
                if(isRequiredOperationsRequired) {
                    for(int i = 0; i < 5; i++) {
                        client.makePutRequest("key"+ String.valueOf(i), "value"+ String.valueOf(i));
                        LOGGER.info(String.format("Making required PUT request with key, value = (key%d, value%d)", i, i));
                        String value = client.makeGetRequest("key"+ String.valueOf(i));
                        LOGGER.info(String.format("Made required GET request with key: key%d. Value is: %s.", i, value));
                        client.makeDeleteRequest("key"+ String.valueOf(i));
                        LOGGER.info(String.format("Made required DELETE request with key: key%d.", i));
                    }
                }
                
                // Prepopulate the dictionary with data if the 4th argument is not set to false
                if(isPrepopulateRequired) {
                    int totalPuts = 1000;
                    LOGGER.info("Launching "+ String.valueOf(totalPuts) +" put operation in different threads.");
                    
                    Runnable putTask = () -> { 
                        try {
                            client.makePutRequest("key" + String.valueOf(System.currentTimeMillis()), "value" + String.valueOf(System.currentTimeMillis())); 
                        } catch (Exception exp) {
                            StringWriter sw = new StringWriter();
                            exp.printStackTrace(new PrintWriter(sw));
                            String exceptionAsString = sw.toString();
                            LOGGER.severe("Server PUT request failed : "+ exceptionAsString);
                        }
                    };
                    
                    Thread[] putThreads = new Thread[totalPuts];
                    for(int i = 0; i < totalPuts; i++) {
                        putThreads[i] = new Thread(putTask);
                        putThreads[i].start();
                    }

                    /* Uncomment to wait for all the threads to complete
                    for(int i = 0; i < totalPuts; i++) {
                        try {
                            putThreads[i].join();
                        } catch (InterruptedException ie) {
                            LOGGER.severe("Thread "+ String.valueOf(i) +" interrupted.");
                        }
                    }
                    LOGGER.info("Completed "+ String.valueOf(totalPuts) +" put operation in different threads.");
                     */
                }
                
                while(true) {
                    System.out.print("Type a request to make (PUT/GET/DELETE/QUIT) : ");                    
                    String userInput = Utils.readUserInput();
                    
                    if(userInput != null && RequestType.parseString(userInput) != RequestType.NONE) {
                        RequestType requestType = RequestType.parseString(userInput);
                        switch(requestType) {
                            case GET: // Perform the GET operation
                                System.out.print("Type a key to get : ");     
                                String key = Utils.readUserInput();

                                if(!Utils.isKeyValid(key)) {
                                    LOGGER.severe("Key is invalid. Please try again.");
                                } else {
                                    try {
                                        String value = client.makeGetRequest(key);
                                        if(value != null) {
                                            LOGGER.info("Received value from server : "+ value +".");
                                        } 
                                    } catch(NoSuchElementException nsee) {
                                        LOGGER.severe(nsee.getMessage());
                                    }
                                }

                                break;
                            case PUT: // Perform the PUT operation
                                System.out.print("Type a key to put : ");     
                                key = Utils.readUserInput();
                                System.out.print("Type a value to put : ");     
                                String value = Utils.readUserInput();

                                if(!(Utils.isKeyValid(key) && Utils.isValueValid(value))) {
                                    LOGGER.severe("Key or value or both are invalid. Please try again.");
                                } else {
                                    boolean isDone = client.makePutRequest(key, value);
                                    if(isDone) {
                                        LOGGER.info("Key, value successfully stored on the server.");
                                    } else {
                                        LOGGER.severe("Key, value could not be stored on the server.");
                                    }
                                }

                                break;
                            case DELETE: // Perform the DELETE operation
                                System.out.print("Type a key to delete : ");     
                                key = Utils.readUserInput();

                                if(!Utils.isKeyValid(key)) {
                                    LOGGER.severe("Key is invalid. Please try again.");
                                } else {
                                    try {
                                        client.makeDeleteRequest(key);
                                        LOGGER.info("Key, value successfully deleted on the server.");
                                    } catch(NoSuchElementException nsee) {
                                        LOGGER.severe(nsee.getMessage());
                                    }
                                }

                                break;  
                            case QUIT: // Perform the QUIT operation
                                System.exit(0);
                        }
                    } else {
                        LOGGER.severe("Invalid request type. Please try again.");
                    }
                }
            } catch (NumberFormatException nfe) {
                LOGGER.severe("The port number should be an integer.");
            } catch(IllegalArgumentException iae) {
                LOGGER.severe("The server type should be either tcp or udp.");
            } catch(IOException ioe) {
                ioe.printStackTrace();
                LOGGER.severe("Error in reading user input.");
            } 
        }
    }
}
