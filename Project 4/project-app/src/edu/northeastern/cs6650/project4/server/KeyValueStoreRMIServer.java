package edu.northeastern.cs6650.project4.server;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

import edu.northeastern.cs6650.project4.common.ServerResponseCode;
import edu.northeastern.cs6650.project4.common.Utils;
import edu.northeastern.cs6650.project4.interfaces.KeyValueStoreInterface;
import edu.northeastern.cs6650.project4.interfaces.KeyValuePaxosInterface;
import edu.northeastern.cs6650.project4.common.RequestType;
import edu.northeastern.cs6650.project4.common.RequestObject;
import edu.northeastern.cs6650.project4.common.PromiseObject;

/*
 * Class that implements an RMI server for key value storage. 
 *
 * Additionally, this class implements the KeyValuePaxosInterface which is used to implement the PAXOS algorithm.
 * 
 * This class implement the functions of different participants in the PAXOS algorithm. Namely, the proposer, acceptor and learner.
 * The Proposer sends a prepare message to all the acceptors. The acceptors respond with a promise message if they are ready to participate in consensus.
 * The proposer then sends an propose message to all the acceptors.The acceptors respond with an accept message if they accept to commit.
 * The proposer then sends a learn message to all the learners. The learners then learn the value.
 * 
 * The class also implement random failure of the acceptors and learners.
 */
public class KeyValueStoreRMIServer extends UnicastRemoteObject implements KeyValueStoreInterface, KeyValuePaxosInterface {

    private static final Logger LOGGER = Logger.getLogger(KeyValueStoreRMIServer.class.getName());
    private static final int MAX_PAXOS_RETRIES = 3;
    private static final boolean RANDOM_FAILURE_MODE = true;
    
    private Map<String, String> keyValueStore;
    private Object keyValueStoreMutex;
    private String serverIPAddress;
    private int serverPort;
    private int myIndex;
    private int totalKeyValueServers;
    private KeyValuePaxosInterface keyValueStoreRMIServers[];
    private final ReadWriteLock readWriteLock; 
    private final Lock writeLock; 
    private final Lock readLock; 
    private Long maxProposalNumber;
    private Long lastAcceptedProposalNumber;
    private String lastAcceptedKey;
    private String lastAcceptedValue;

    public KeyValueStoreRMIServer(String serverIPAddress, int serverPort, int index, int totalKeyValueServers) throws RemoteException {
        super();
        this.serverIPAddress = serverIPAddress;
        this.serverPort = serverPort;
        this.myIndex = index;
        this.totalKeyValueServers = totalKeyValueServers;
        this.keyValueStoreRMIServers = new KeyValuePaxosInterface[totalKeyValueServers];
        readWriteLock = new ReentrantReadWriteLock(); 
        writeLock = readWriteLock.writeLock(); 
        readLock = readWriteLock.readLock(); 
        keyValueStoreMutex = new Object();
        maxProposalNumber = 0L;
        this.keyValueStore = new HashMap<String, String>();
    }

    /*
     * Initialize the two phase commit coordinator and the key value store servers
     */
    private void initializeRemoteObjects() {
        try {

            // Register the remote key value server object
            // Each server contains a reference to other servers that play the role of acceptors and learners
            for(int i = 0; i < totalKeyValueServers; i++) {
                if(i != myIndex) {
                    Registry participantRegistry = LocateRegistry.getRegistry(serverIPAddress, serverPort + i + 1);
                    keyValueStoreRMIServers[i] = (KeyValuePaxosInterface)participantRegistry.lookup(Utils.KEY_VALUE_STORE_RMI_SERVER_NAME);
                } else {
                    keyValueStoreRMIServers[i] = this; 
                }
            }
            
            LOGGER.info("All key value store servers and coordinate initialized on the participant : "+ String.valueOf(myIndex) + ".");
        } catch (Exception exp) {
            LOGGER.severe("The key value store server could not be initialized on the participant : "+ String.valueOf(myIndex) + ".");  
        }
    }

    /*
     * Process a request to put a key-value RequestObject into the local key - value storage
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
        boolean successful = this.initiatePAXOS(new RequestObject(RequestType.PUT, key, value));
        writeLock.unlock();

        if(!successful) {
            throw new ExecutionException("The PUT transaction was unsuccessful.", new Throwable("The PUT transaction was unsuccessful."));
        } else {
            LOGGER.info("The PUT transaction was successful. The key is : " + key + ". The value is : " + value + ".");
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
            boolean successful = this.initiatePAXOS(new RequestObject(RequestType.DELETE, key, null));
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

        // paxos non required for read operations
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
    * This method implements the PAXOS algorithm. It is used to achieve consensus among the participants.
    * The PAXOS implementation is based on the following tutorial: https://people.cs.rutgers.edu/~pxk/417/notes/paxos.html
    */    
    private boolean initiatePAXOS(RequestObject request) {

        // Phase 1a: Proposer(Prepare)
        final Long proposalNumber = System.currentTimeMillis();
            
        for(int retryCount = 0; retryCount < MAX_PAXOS_RETRIES; retryCount++) {
            initializeRemoteObjects();

            List<Integer> serversPromised = new ArrayList<Integer>();
            for(int i = 0; i < totalKeyValueServers; i++) {
                try {
                    PromiseObject promise = keyValueStoreRMIServers[i].prepare(proposalNumber);
                    if(promise != null) {
                        serversPromised.add(i);

                        // if the proposal number is less than the last accepted proposal number, then set the value of the request to the last accepted value
                        // this is done to ensure that the value of the request is not lost
                        if(request.getRequestType() == RequestType.PUT && promise.getLastAcceptedProposalNumber() != null && promise.getLastAcceptedProposalNumber() > proposalNumber && promise.getLastAcceptedKey() != null && promise.getLastAcceptedKey().equals(request.getKey())) {
                            request.setValue(promise.getLastAcceptedValue());
                        }
                    }
                } catch (Exception exp) {
                    LOGGER.severe("The prepare message could not be sent to the participant : " + String.valueOf(i) + ".");
                }
            }

            // if majority of the participants have accepted the proposal, then send accept message to all participants
            // Phase 2a: Proposer(Propose)
            List<Integer> serversAccepted = new ArrayList<Integer>();
            if(serversPromised.size() > (totalKeyValueServers / 2)) {
                for(int i : serversPromised) {
                    try {
                        boolean success = keyValueStoreRMIServers[i].propose(proposalNumber, request);
                        if(success) {
                            serversAccepted.add(i);
                        } 
                    } catch (Exception exp) {
                        LOGGER.severe("The propose message could not be sent to the participant : " + String.valueOf(i) + ".");
                    }
                }
            } else {
                continue;
            }

            // if majority of the participants have accepted the proposal, then send learn message to all participants
            int learnersSuccess = 0;
            if(serversAccepted.size() > (totalKeyValueServers / 2)) {
                for(int i : serversAccepted) {
                    try {
                        keyValueStoreRMIServers[i].learn(proposalNumber, request);
                        learnersSuccess++;
                    } catch (Exception exp) {
                        LOGGER.severe("The learn message could not be sent to the participant : " + String.valueOf(i) + ".");
                    }
                }

                if(learnersSuccess > (totalKeyValueServers / 2)) {
                    LOGGER.info("The learn operation was successful.");
                    return true;
                }
            } 
        }
        return false;
    }
    
    // Phase 1b: Acceptor(Promise)
    public PromiseObject prepare(Long proposalId) throws RemoteException {

        int randomNumber = (int)(Math.random()*100);
        if(RANDOM_FAILURE_MODE && randomNumber < 10) { // 10% chance of failure
            throw new RemoteException("The acceptor failed to prepare.");
        }

        if(proposalId.compareTo(maxProposalNumber) > 0) {
            maxProposalNumber = proposalId;
            PromiseObject promise = new PromiseObject(proposalId, lastAcceptedKey, lastAcceptedValue, lastAcceptedProposalNumber);
            return promise; // simple approach to send promise to the proposer
        } else {
            return null;
        }
    }

    // Phase 2b: Acceptor(Accept)
    public boolean propose(Long proposalId, RequestObject request) throws RemoteException {

        int randomNumber = (int)(Math.random()*100);
        if(RANDOM_FAILURE_MODE && randomNumber < 10) { // 10% chance of failure
            throw new RemoteException("The acceptor failed to accept the proposal.");
        }

        if(proposalId.equals(maxProposalNumber)) {
            lastAcceptedProposalNumber = proposalId;
            lastAcceptedKey = request.getKey();
            lastAcceptedValue = request.getValue();
            return true;
        }
        return false; 
    }

    /*
    * Phase 3: Learner(Learn). This essentially commits the value to the key value store in case of PUT
    * and deletes the value from the key value store in case of DELETE.
    */
    public void learn(Long proposalId, RequestObject request) throws RemoteException {

        int randomNumber = (int)(Math.random()*100);
        if(RANDOM_FAILURE_MODE && randomNumber < 10) { // 10% chance of failure
            throw new RemoteException("The learner failed to accept the commit.");
        }
        
        if(request.getRequestType() == RequestType.PUT) {
            new Thread(new Runnable() {
                public void run() {
                    synchronized (keyValueStoreMutex) {
                        keyValueStore.put(request.getKey(), request.getValue());
                    }
                }
            }).start();
        } else if(request.getRequestType() == RequestType.DELETE) {
            if(keyValueStore.containsKey(request.getKey())) {
                new Thread(new Runnable() {
                    public void run() {
                        synchronized (keyValueStoreMutex) {
                            keyValueStore.remove(request.getKey());
                        }
                    }
                }).start();
            }
        }
    }
}
