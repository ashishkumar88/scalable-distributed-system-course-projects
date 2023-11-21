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

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.Arrays;
import java.util.concurrent.locks.Lock; 
import java.util.concurrent.locks.ReadWriteLock; 
import java.util.concurrent.locks.ReentrantReadWriteLock; 
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ExecutionException;

import edu.northeastern.cs6650.project3.common.ServerResponseCode;
import edu.northeastern.cs6650.project3.common.Utils;
import edu.northeastern.cs6650.project3.interfaces.KeyValueStoreInterface;
import edu.northeastern.cs6650.project3.interfaces.KeyValueStoreTwoPhaseCommitCoordinatorInterface;
import edu.northeastern.cs6650.project3.common.RequestType;
import edu.northeastern.cs6650.project3.common.Pair;

/*
 * Class that implements an RMI server for key value storage. 
 *
 * Additionally this class implements a participant in the two phase commit protocol for the key value storage
 */
public class KeyValueStoreRMIServer extends UnicastRemoteObject implements KeyValueStoreInterface {

    private static final Logger LOGGER = Logger.getLogger(KeyValueStoreRMIServer.class.getName());

    protected Map<String, String> keyValueStore = new HashMap<String, String>();
    private Object keyValueStoreMutex;
    private String serverIPAddress;
    private int serverPort;
    private KeyValueStoreTwoPhaseCommitCoordinatorInterface twoPhaseCoordinator;
    private boolean isTwoPhaseCommitCoordinatorInitialized = false;
    private int myIndex;
    private int totalKeyValueServers;
    private KeyValueStoreInterface keyValueStoreRMIServers[];
    private final ReadWriteLock readWriteLock; 
    private final Lock writeLock; 
    private final Lock readLock; 
    private Pair temporaryKeyValueStore;

    public KeyValueStoreRMIServer(String serverIPAddress, int serverPort, int index, int totalKeyValueServers) throws RemoteException {
        super();
        this.serverIPAddress = serverIPAddress;
        this.serverPort = serverPort;
        this.myIndex = index;
        this.totalKeyValueServers = totalKeyValueServers;
        this.keyValueStoreRMIServers = new KeyValueStoreInterface[totalKeyValueServers];
        readWriteLock = new ReentrantReadWriteLock(); 
        writeLock = readWriteLock.writeLock(); 
        readLock = readWriteLock.readLock(); 
        keyValueStoreMutex = new Object();
    }

    /*
     * Initialize the two phase commit coordinator and the key value store servers
     */
    public void initializeRemoteObjects() {
        try {

            Registry coordinatorRegistry = LocateRegistry.getRegistry(serverIPAddress, serverPort);
            twoPhaseCoordinator = (KeyValueStoreTwoPhaseCommitCoordinatorInterface)coordinatorRegistry.lookup(Utils.KEY_VALUE_STORE_RMI_TWO_PHASE_COMMIT_COORDINATOR_NAME);
            isTwoPhaseCommitCoordinatorInitialized = true;
            
            // Register the remote key value server object
            for(int i = 0; i < totalKeyValueServers; i++) {
                if(i != myIndex) {
                    Registry participantRegistry = LocateRegistry.getRegistry(serverIPAddress, serverPort + i + 1);
                    keyValueStoreRMIServers[i] = (KeyValueStoreInterface)participantRegistry.lookup(Utils.KEY_VALUE_STORE_RMI_SERVER_NAME);
                } else {
                    keyValueStoreRMIServers[i] = this;
                }
            }

            LOGGER.info("All key value store servers and coordinate initialized on the participant : "+ String.valueOf(myIndex) + ".");
        } catch (Exception exp) {
            exp.printStackTrace();  
            isTwoPhaseCommitCoordinatorInitialized = false;
            LOGGER.severe("The two phase commit coordinator is not available.");
        }
    }

    /*
     * Process a request to put a key-value pair into the local key - value storage
     */
    public void putKeyValue(String key, String value) throws RemoteException, ExecutionException {

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
        
        writeLock.lock();
        boolean successful = this.twoPhaseCoordinator.initiateTwoPhaseCommit(RequestType.PUT, key, value); 
        writeLock.unlock();

        if(!successful) {
            throw new ExecutionException("The PUT transaction was unsuccessful.", new Throwable("The PUT transaction was unsuccessful."));
        } 
    }


    /*
     * Process a request to delete a key from the local key - value storage given a valid key
     */
    public void deleteKeyValue(String key) throws RemoteException, NoSuchElementException, ExecutionException {

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
        
        writeLock.lock();
        if(keyValueStore.containsKey(key)) {
            flag[0] = true;
            boolean successful = this.twoPhaseCoordinator.initiateTwoPhaseCommit(RequestType.DELETE, key, null); 
            if(!successful) {
                throw new ExecutionException("The DELETE transaction was unsuccessful.", new Throwable("The DELETE transaction was unsuccessful."));
            } 
        }
        writeLock.unlock();

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
        readLock.lock();
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
        readLock.unlock();

        if(valueString[0].equals("")) {
            throw new NoSuchElementException("This key does not exist in the system : "+ key + ".");
        }

        return valueString[0];
    }

    /*
    * Process a request to check if a key-value pair can be committed to the local key - value storage
    */
    public boolean canCommit(RequestType requestType, String key, String value) throws RemoteException {
        int randomInteger = ThreadLocalRandom.current().nextInt(1, 101);
        boolean mostlyTrue = true;
        
        // mostlyTrue = randomInteger < 90; // disable for simulation

        if(mostlyTrue) {
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
        } else {
            // Send acknowledgement to the coordinator
            return false;
        }
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
            new Thread(new Runnable() {
                public void run() {
                    synchronized (keyValueStoreMutex) {
                        keyValueStore.remove(key);
                        temporaryKeyValueStore = null;
                    }
                }
            }).start();
        }

        // simple and effective way to communicate haveCommitted to the coordinator
        twoPhaseCoordinator.haveCommitted(myIndex);
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
}
