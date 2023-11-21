package edu.northeastern.cs6650.project3.server;
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
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

import edu.northeastern.cs6650.project3.interfaces.KeyValueStoreTwoPhaseCommitCoordinatorInterface;
import edu.northeastern.cs6650.project3.interfaces.KeyValueStoreInterface;
import edu.northeastern.cs6650.project3.common.Utils;
import edu.northeastern.cs6650.project3.common.Pair;
import edu.northeastern.cs6650.project3.common.RequestType;

/*
 * Class that implements the two phase commit protocol coordinator for key value storage. 
 *
 * Additionally this class contains a key value storage that is used by the coordinator to store the key value pairs
 */
public class KeyValueStoreTwoPhaseCommitCoordinator extends UnicastRemoteObject implements KeyValueStoreTwoPhaseCommitCoordinatorInterface {

    private static final Logger LOGGER = Logger.getLogger(KeyValueStoreTwoPhaseCommitCoordinator.class.getName());
    protected Map<String, String> keyValueStore = new HashMap<String, String>();
    private int numberOfKeyValueServers;
    private Registry serverRegistry;
    private KeyValueStoreInterface keyValueStoreRMIServers[];
    private boolean keyValueStoreRMIServersInitialized;
    private String serverIPAddress;
    private int serverPort;
    private Pair temporaryKeyValueStore;
    private static final long NUMBER_RETRIES = 4;
    private Object keyValueStoreMutex;
    private boolean[] haveCommitResponded;
    private boolean[] haveCommitResponse;

    public KeyValueStoreTwoPhaseCommitCoordinator(String serverIPAddress, int serverPort, int numberOfKeyValueServers) throws RemoteException {
        super();
        this.numberOfKeyValueServers = numberOfKeyValueServers;
        this.serverIPAddress = serverIPAddress; 
        this.serverPort = serverPort;
        keyValueStoreRMIServers = new KeyValueStoreInterface[numberOfKeyValueServers];
        haveCommitResponded = new boolean[numberOfKeyValueServers];
        haveCommitResponse = new boolean[numberOfKeyValueServers];
        keyValueStoreRMIServersInitialized = false;
        keyValueStoreMutex = new Object();
    }

    /*
    * Initialize the RMI servers for the key value store
    */
    public void intializeKeyValueStoreServers() {
        int keyValueStoreServerCount = keyValueStoreRMIServers.length;

        for(int i = 0; i < keyValueStoreServerCount; i++) {
            try {
                Registry participantRegistry = LocateRegistry.getRegistry(serverIPAddress, serverPort + i + 1);
                keyValueStoreRMIServers[i] = (KeyValueStoreInterface)participantRegistry.lookup(Utils.KEY_VALUE_STORE_RMI_SERVER_NAME);
                keyValueStoreRMIServersInitialized = true;
            } catch (Exception exp) {
                keyValueStoreRMIServersInitialized = false; // if any one of the servers is not available, then the initialization is not complete  
                LOGGER.severe("The key value store server " + i + " is not available.");
            }
        }

        LOGGER.info("All key value store servers are initialized on the coordinator.");
    }

    /*
    * Process a request to initiate a two phase commit for a key-value pair to the local key - value storage
    *
    * This method performs the following steps:
    * 1. Send a canCommit request to all the key value store servers
    * 2. Send a canCommit request to itself.
    * 3. If all the servers respond with a canCommit, then send a doCommit request to all the key value store servers
    * 4. If any of the servers respond with a no to canCommit, then send a doAbort request to all the key value store servers
    */
    public boolean initiateTwoPhaseCommit(RequestType requestType, String key, String value) throws RemoteException {
        
        // reinitialize the haveCommitResponded and haveCommitResponse arrays
        for(int i = 0; i < this.numberOfKeyValueServers; i++) {
            haveCommitResponded[i] = false;
            haveCommitResponse[i] = false;
        }

        if(!keyValueStoreRMIServersInitialized) {
            intializeKeyValueStoreServers();
        }

        if(keyValueStoreRMIServersInitialized) {
            boolean[] canCommit = new boolean[this.numberOfKeyValueServers + 1]; 
            Arrays.fill(canCommit, Boolean.FALSE);
            
            // Send canCommit request to all the key value store servers
            for(int i = 0; i < this.numberOfKeyValueServers; i++) {
                for(int j = 0; j < NUMBER_RETRIES; j++) {
                    try {
                        canCommit[i] = keyValueStoreRMIServers[i].canCommit(requestType, key, value);
                        break;
                    } catch (Exception exp) {
                        LOGGER.severe("The key value store server " + i + " is not available. Retrying...");
                    }
                }
            }
            
            // Send canCommit request to itself
            canCommit[this.numberOfKeyValueServers] = this.canCommit(requestType, key, value);

            boolean allCanCommit = false;

            for(int i = 0; i < this.numberOfKeyValueServers + 1; i++) {
                if(canCommit[i]) {
                    allCanCommit = true;
                } else {
                    allCanCommit = false;
                    break;
                }
            }

            // If all the servers respond with a canCommit, then send a doCommit request to all the key value store servers
            if(allCanCommit) {
                for(int i = 0; i < this.numberOfKeyValueServers; i++) {
                    for(int j = 0; j < NUMBER_RETRIES; j++) {
                        try {
                            keyValueStoreRMIServers[i].doCommit(requestType, key, value); //TODO: take care of concurrency
                            break;
                        } catch (Exception exp) {
                            LOGGER.severe("The key value store server " + i + " is not available. Retrying...");
                        }
                    }
                }

                this.doCommit(requestType, key, value);
                
                boolean allHaveCommitResponded = false;
                while(allHaveCommitResponded) {
                    allHaveCommitResponded = true;
                    for(int i = 0; i < this.numberOfKeyValueServers; i++) {
                        if(!haveCommitResponded[i]) {
                            allHaveCommitResponded = false;
                            break;
                        }
                    }

                    if(allHaveCommitResponded) {
                        break;
                    }
                }

                boolean allHaveCommitted = true;
                for(int i = 0; i < this.numberOfKeyValueServers; i++) {
                    if(!haveCommitResponse[i]) {
                        allHaveCommitted = false;
                        break;
                    }
                }

                return true;
            } else { // If any of the servers respond with a no to canCommit, then send a doAbort request to all the key value store servers
                for(int i = 0; i < this.numberOfKeyValueServers; i++) {
                    for(int j = 0; j < NUMBER_RETRIES; j++) {
                        try {
                            keyValueStoreRMIServers[i].doAbort(requestType, key, value);
                            break;
                        } catch (Exception exp) {
                            LOGGER.severe("The key value store server " + i + " is not available. Retrying...");
                        }
                    }
                }

                this.doAbort(requestType, key, value);
                return false;
            }
        }

        return false;
    }

    /*
    * Process a request to check if a key-value pair can be committed to the local key - value storage
    */
    public boolean canCommit(RequestType requestType, String key, String value) throws RemoteException {
        // Prepare for the transaction
        new Thread(new Runnable() {
            public void run() {
                synchronized (keyValueStoreMutex) {
                    temporaryKeyValueStore = new Pair(requestType, key, value);
                }
            }
        }).start();

        // Send acknowledgement to the coordinator
        return true;
    }

    /*
    * Process a request to commit a key-value pair to the local key - value storage
    */
    public boolean doCommit(RequestType requestType, String key, String value) throws RemoteException {
        if(requestType == RequestType.PUT) {
            new Thread(new Runnable() {
                public void run() {
                    synchronized (keyValueStoreMutex) {
                        keyValueStore.put(key, value);
                        temporaryKeyValueStore = null;
                    }
                }
            }).start();
        } else if(requestType == RequestType.DELETE) {
            if(keyValueStore.containsKey(key)) {
                new Thread(new Runnable() {
                    public void run() {
                        synchronized (keyValueStoreMutex) {
                            keyValueStore.remove(key);
                            temporaryKeyValueStore = null;
                        }
                    }
                }).start();
            }
        }

        // simple and effective way to communicate haveCommitted to the coordinator
        return true;
    }

    /*
    * Process a request to abort a key-value pair to the local key - value storage
    */
    public boolean doAbort(RequestType requestType, String key, String value) throws RemoteException {
        new Thread(new Runnable() {
            public void run() {
                synchronized (keyValueStoreMutex) {
                    temporaryKeyValueStore = null;
                }
            }
        }).start();

        // simple and effective way to communicate haveCommitted to the coordinator
        return true;
    }

    public void haveCommitted(int index) throws RemoteException {
        new Thread(new Runnable() {
            public void run() {
                synchronized (keyValueStoreMutex) {
                    haveCommitResponded[index] = true;
                    haveCommitResponse[index] = true;
                }
            }
        }).start();
    }
}