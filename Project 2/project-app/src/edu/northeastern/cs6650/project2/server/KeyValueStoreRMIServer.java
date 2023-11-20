package edu.northeastern.cs6650.project2.server;
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
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.server.UnicastRemoteObject;

import edu.northeastern.cs6650.project2.common.ServerResponseCode;
import edu.northeastern.cs6650.project2.common.Utils;
import edu.northeastern.cs6650.project2.interfaces.KeyValueStoreInterface;

/*
 * Class that implements an RMI server for key value storage
 */
public class KeyValueStoreRMIServer extends UnicastRemoteObject implements KeyValueStoreInterface {

    private static final Logger LOGGER = Logger.getLogger(KeyValueStoreRMIServer.class.getName());

    protected Map<String, String> keyValueStore = new HashMap<String, String>();
    Object keyValueStoreMutex = new Object();

    public KeyValueStoreRMIServer() throws RemoteException {
        super();
    }

    /*
     * Process a request to put a key-value pair into the local key - value storage
     */
    public void putKeyValue(String key, String value) throws RemoteException {

        String clientAddress = null;
        try {
            clientAddress = getClientHost();
        } catch (Exception exp) {
            LOGGER.severe("The client IP address is not available.");
        }
        
        if(!(Utils.isKeyValid(key) || Utils.isValueValid(value))) {
            throw new IllegalArgumentException("The key or value or both are invalid.");
        }

        LOGGER.info(clientAddress + "::" + "The key received for PUT request is : " + key + ".\nThe value received for PUT request is : " + value + ".");
        
        new Thread(new Runnable() {
            public void run() {
                synchronized (keyValueStoreMutex) {
                    keyValueStore.put(key, value);
                }
            }
        }).start();
    }


    /*
     * Process a request to delete a key from the local key - value storage given a valid key
     */
    public void deleteKeyValue(String key) throws RemoteException, NoSuchElementException {

        String clientAddress = null;
        try {
            clientAddress = getClientHost();
        } catch (Exception exp) {
            LOGGER.severe("The client IP address is not available.");
        }
        
        if(!Utils.isKeyValid(key)) {
            throw new IllegalArgumentException("The key is empty or invalid.");
        }

        LOGGER.info(clientAddress + "::" + "The key received for DELETE request is : " + key);

        // Check if local key value storage contains a key, then delete the key
        // If key does not exist, throw a NoSuchElementException
        final boolean flag[] = { false };
        Thread runner = new Thread(new Runnable() {
            public void run() {
                synchronized (keyValueStoreMutex) {
                    if(keyValueStore.containsKey(key)) {
                        keyValueStore.remove(key);
                        flag[0] = true;
                    } 
                }
            }
        });
        
        runner.start();
        try {
            runner.join();
        } catch (Exception exp) {
            LOGGER.severe("The server was interrupted.");
        }

        if(!flag[0]) {
            throw new NoSuchElementException("This key does not exist in the system : "+ key + ".");
        }
    }

    /*
     * Process a request to get a value from the local key - value storage given a valid key
     */
    public String getValue(String key) throws RemoteException, NoSuchElementException {

        String clientAddress = null;
        try {
            clientAddress = getClientHost();
        } catch (Exception exp) {
            LOGGER.severe("The client IP address is not available.");
        }

        if(!Utils.isKeyValid(key)) {
            throw new IllegalArgumentException("The key is empty or invalid.");
        }

        LOGGER.info(clientAddress + "::" + "The key received for GET request is : " + key + ".");

        // Check if local key value storage contains a key, fetch and return the corresponding value
        // If key does not exist, throw a NoSuchElementException
        final String[] valueString = {""};
        
        // run the synchronized block in a thread
        Thread runner = new Thread(new Runnable() {
            public void run() {
                synchronized (keyValueStoreMutex) {
                    if(keyValueStore.containsKey(key)) {
                        valueString[0] = keyValueStore.get(key);
                    } 
                }
            }
        });

        runner.start();
        try {
            runner.join();
        } catch (Exception exp) {
            LOGGER.severe("The server was interrupted.");
        }

        if(valueString[0].equals("")) {
            throw new NoSuchElementException("This key does not exist in the system : "+ key + ".");
        }

        return valueString[0];
    }
    
}
