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

import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.concurrent.ExecutionException;
import java.rmi.ConnectIOException;

import edu.northeastern.cs6650.project3.interfaces.KeyValueStoreInterface;
import edu.northeastern.cs6650.project3.common.RequestType;
import edu.northeastern.cs6650.project3.common.Utils;

/*
 * Implementation of the RMI client that can talk to multiple RMI servers
 * 
 * This class calls different methods on the remote object to execute the GET, PUT and DELETE
 * operations.
 */
public class KeyValueStoreRMIClient {

    private static final Logger LOGGER = Logger.getLogger(KeyValueStoreRMIClient.class.getName());
    private String serverIPAddress;
    private int serverPort;
    private KeyValueStoreInterface serverInterface;
    private boolean interfaceInitialized = false;
    private int serverIndex = -1;

    public KeyValueStoreRMIClient(String serverIPAddress, int serverPort) {
        try {

            this.serverIPAddress = serverIPAddress;
            this.serverPort = serverPort;

            // Initialize the remote object
            initializeRemoteObject(serverIndex);
        } catch (Exception rexp) {
            LOGGER.severe("Remote interface could not be initialized. Retry after server is available.");
        }
    }

    /*
     * Intialize the remote object
     */
    private void initializeRemoteObject(int serverIndex) {
        try {
            int countKeyValStoreServers = 0;
            boolean isServerAvailable = true;
            
            // determine how many key value store servers are running
            while(isServerAvailable) {
                try {
                    Registry serverRegistry = LocateRegistry.getRegistry(serverIPAddress, serverPort + 1 + countKeyValStoreServers);
                    String[] servers = serverRegistry.list();
                    if(servers.length == 1 && servers[0].startsWith(Utils.KEY_VALUE_STORE_RMI_SERVER_NAME)) {
                        countKeyValStoreServers++;
                    }
                } catch (Exception exp) {
                    isServerAvailable = false;
                }
            }

            if (countKeyValStoreServers == 0) {
                LOGGER.severe("No key value store server is running on the specified port.");
                return; // no server is available
            } else {
                LOGGER.info("Found "+ countKeyValStoreServers +" key value servers.");
            }

            // Select a server based on the index
            if(countKeyValStoreServers == 1) {
                System.out.print("Only one server is available. Connecting to server 1.");    
                serverIndex = 0;                
            } else {
                System.out.print("Select a number between 1 and " + countKeyValStoreServers + " to connect to a server or select 0 for random selection : ");                    
                String userInput = Utils.readUserInput();
                serverIndex = Integer.parseInt(userInput);
                if(serverIndex < 0 || serverIndex >= countKeyValStoreServers) {
                    LOGGER.severe("Invalid server index. Select a random server.");
                    serverIndex = (int)(Math.random() * countKeyValStoreServers) + 1;
                } else if(serverIndex == 0) {
                    serverIndex = (int)(Math.random() * countKeyValStoreServers) + 1;
                } 
                
                serverIndex--;
            }

            Registry serverRegistry = LocateRegistry.getRegistry(serverIPAddress, serverPort + 1 + serverIndex);
            serverInterface = (KeyValueStoreInterface)serverRegistry.lookup(Utils.KEY_VALUE_STORE_RMI_SERVER_NAME);
            LOGGER.info("Connected to server "+ String.valueOf(serverIndex + 1) + ".");   
            interfaceInitialized = true;
        } catch (Exception rexp) {
            LOGGER.severe("Remote interface could not be initialized. Retry after server is available.");
        }
    }

    /*
     * Executes a PUT request
     * 
     * This inturn calls the a method on the remote object to execute PUT operation
     */
    public boolean makePutRequest(String key, String value) throws RemoteException {
        // try initializing the remote object in case the server was not previously available
        if (!interfaceInitialized) {
            initializeRemoteObject(serverIndex);
        }

        if(interfaceInitialized) {
            try{
                serverInterface.putKeyValue(key, value);
                return true;
            } catch(ExecutionException eexp) {
                return false;
            } catch(ConnectIOException cioexp) {
                LOGGER.severe("Remote cannot be reached due to networking/firewall issues.");
                return false;
            }
        } 

        return false;
    }

    /*
     * Executes a GET request
     * 
     * This inturn calls the a method on the remote object to execute GET operation
     */
    public String makeGetRequest(String key) throws RemoteException, NoSuchElementException {
        
        // try initializing the remote object in case the server was not previously available
        if (!interfaceInitialized) {
            initializeRemoteObject(serverIndex);
        }

        if (interfaceInitialized) {
            try {
                return serverInterface.getValue(key);
            } catch(ConnectIOException cioexp) {
                LOGGER.severe("Remote cannot be reached due to networking/firewall issues.");
                return null;
            }
        } else {
            return null;
        }
    }

    /*
     * Executes a DELETE request
     * 
     * This inturn calls the a method on the remote object to execute DELETE operation
     */
    public boolean makeDeleteRequest(String key) throws RemoteException, NoSuchElementException {
        
        // try initializing the remote object in case the server was not previously available
        if (!interfaceInitialized) {
            initializeRemoteObject(serverIndex);
        }

        if (interfaceInitialized) {
            try{
                serverInterface.deleteKeyValue(key);
                return true;
            } catch(ExecutionException eexp) {
                return false;
            } catch(ConnectIOException cioexp) {
                LOGGER.severe("Remote cannot be reached due to networking/firewall/timeout issues.");
                return false;
            }
        }

        return false;
    }
}
