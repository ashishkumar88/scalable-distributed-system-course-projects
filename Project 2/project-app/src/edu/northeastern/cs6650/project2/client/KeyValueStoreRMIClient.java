package edu.northeastern.cs6650.project2.client;
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

import edu.northeastern.cs6650.project2.interfaces.KeyValueStoreInterface;
import edu.northeastern.cs6650.project2.common.RequestType;
import edu.northeastern.cs6650.project2.common.Utils;

/*
 * Implementation of the RMI client 
 * 
 * This class calls different methods on the remote object to execute the GET, PUT and DELETE
 * operations.
 */
public class KeyValueStoreRMIClient {

    private static final Logger LOGGER = Logger.getLogger(KeyValueStoreRMIClient.class.getName());
    private Registry serverRegistry;
    private KeyValueStoreInterface serverInterface;
    private boolean interfaceInitialized = false;

    public KeyValueStoreRMIClient(String serverIPAddress, int serverPort) {
        try {
            serverRegistry = LocateRegistry.getRegistry(serverIPAddress, serverPort);
            initializeRemoteObject();
        } catch (Exception rexp) {
            LOGGER.severe("Remote interface could not be initialized. Retry after server is available.");
        }
    }

    /*
     * Intialize the remote object
     */
    private void initializeRemoteObject() {
        try {
            serverInterface = (KeyValueStoreInterface)serverRegistry.lookup(Utils.KEY_VALUE_STORE_RMI_SERVER_NAME);
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
    public void makePutRequest(String key, String value) throws RemoteException {

        // try initializing the remote object in case the server was not previously available
        if (!interfaceInitialized) {
            initializeRemoteObject();
        }

        if(interfaceInitialized) {
            serverInterface.putKeyValue(key, value);
        } 
    }

    /*
     * Executes a GET request
     * 
     * This inturn calls the a method on the remote object to execute GET operation
     */
    public String makeGetRequest(String key) throws RemoteException, NoSuchElementException {
        
        // try initializing the remote object in case the server was not previously available
        if (!interfaceInitialized) {
            initializeRemoteObject();
        }

        if (interfaceInitialized) {
            return serverInterface.getValue(key);
        } else {
            return null;
        }
    }

    /*
     * Executes a DELETE request
     * 
     * This inturn calls the a method on the remote object to execute DELETE operation
     */
    public void makeDeleteRequest(String key) throws RemoteException, NoSuchElementException {
        
        // try initializing the remote object in case the server was not previously available
        if (!interfaceInitialized) {
            initializeRemoteObject();
        }

        if (interfaceInitialized) {
            serverInterface.deleteKeyValue(key);
        }
    }
}
