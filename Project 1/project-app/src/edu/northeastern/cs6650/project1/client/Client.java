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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.logging.Logger;

import edu.northeastern.cs6650.project1.client.TCPClient;
import edu.northeastern.cs6650.project1.client.UDPClient;
import edu.northeastern.cs6650.project1.client.BaseClient;
import edu.northeastern.cs6650.project1.common.Utils;
import edu.northeastern.cs6650.project1.common.ServerType;
import edu.northeastern.cs6650.project1.common.RequestType;

/*
 * A factory that creates a TCP or a UDP client object
 */
class ClientFactory {
    public static BaseClient createClient(String serverIPAddress, int serverPort, ServerType serverType) {

        if(serverType == ServerType.TCP) {
            return new TCPClient(serverIPAddress, serverPort);
        } else if(serverType == ServerType.UDP) {
            return new UDPClient(serverIPAddress, serverPort);
        }
        return null;

    }
}

/*
 * The main client application class
 * 
 * Based on the arguments provided, either a TCP or a UDP client is created and 
 * started. The client listens for a user to input the type of operation to perform
 * on the server and the arguments for that operation. The cloent application is 
 * terminated on the press of Control-C.
 */
public class Client 
{
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

    /*
     * Reads a string typed by an user on a terminal
     */
    private static String readUserInput() throws IOException{

        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        String userInput = null;
        userInput = consoleReader.readLine();        
        return userInput;

    }
    public static void main(String[] arguments)
    {
        if(arguments.length != 3) {
            LOGGER.severe("Incorrect number of arguments. Correct usage: java -classpath classes edu.northeastern.cs6650.project1.client.Client <server ip address> <server port number> <server type>.");
        } else {
            try {

                // Fetch user arguments
                String serverIPAddress = arguments[0];
                int serverPort = Integer.parseInt(arguments[1]);
                ServerType serverType = ServerType.valueOf(arguments[2].toUpperCase());

                // Create a client based on the server type
                BaseClient client = ClientFactory.createClient(serverIPAddress, serverPort, serverType);
                
                // Prepopulate with values
                for(int i = 0; i < 5; i++) {
                    StringBuffer requestBody = Utils.createRequestBody(RequestType.PUT, "key" + String.valueOf(i), "value" + String.valueOf(i));
                    client.makeServerRequest(requestBody);
                }

                // Required operations
                for(int i = 0; i < 5; i++) {
                    StringBuffer requestBody = Utils.createRequestBody(RequestType.PUT, "key1" + String.valueOf(i), "value1" + String.valueOf(i));
                    client.makeServerRequest(requestBody);
                    
                    requestBody = Utils.createRequestBody(RequestType.GET, "key1" + String.valueOf(i));
                    client.makeServerRequest(requestBody);
                    
                    requestBody = Utils.createRequestBody(RequestType.DELETE, "key1" + String.valueOf(i));
                    client.makeServerRequest(requestBody);
                }

                while(true) {
                    System.out.print("Type a request to make (PUT/GET/DELETE) : ");                    
                    String userInput = Client.readUserInput();
                    
                    if(userInput != null && RequestType.parseString(userInput) != RequestType.NONE) {
                        RequestType requestType = RequestType.parseString(userInput);
                        switch(requestType) {
                            case GET:
                                System.out.print("Type a key to get : ");     
                                String key = Client.readUserInput();

                                if(!Utils.isKeyValid(key)) {
                                    LOGGER.severe("Key is invalid. Please try again.");
                                } else {
                                    StringBuffer requestBody = Utils.createRequestBody(requestType, key);
                                    client.makeServerRequest(requestBody);
                                }

                                break;
                            case PUT:
                                System.out.print("Type a key to put : ");     
                                key = Client.readUserInput();
                                System.out.print("Type a value to put : ");     
                                String value = Client.readUserInput();

                                if(!(Utils.isKeyValid(key) && Utils.isValueValid(value))) {
                                    LOGGER.severe("Key or value or both are invalid. Please try again.");
                                } else {
                                    StringBuffer requestBody = Utils.createRequestBody(requestType, key, value);
                                    client.makeServerRequest(requestBody);
                                }

                                break;
                            case DELETE:
                                System.out.print("Type a key to delete : ");     
                                key = Client.readUserInput();

                                if(!Utils.isKeyValid(key)) {
                                    LOGGER.severe("Key is invalid. Please try again.");
                                } else {
                                    StringBuffer requestBody = Utils.createRequestBody(requestType, key);
                                    client.makeServerRequest(requestBody);
                                }

                                break;  
                        }
                    } else {
                        LOGGER.severe("Invalid request type. Please try again.");
                    }
                }
            } catch (NumberFormatException nfe) {
                LOGGER.severe("The port number should be an integer.");
            } catch(IllegalArgumentException iae) {
                LOGGER.severe("The server type should be either tcp or udp.");
            } catch(SocketTimeoutException ioe) {
                LOGGER.severe("Connection attempt to server timed out.");
            } catch(IOException ioe) {
                LOGGER.severe("Error in reading user input.");
            }
        }
    }
}
