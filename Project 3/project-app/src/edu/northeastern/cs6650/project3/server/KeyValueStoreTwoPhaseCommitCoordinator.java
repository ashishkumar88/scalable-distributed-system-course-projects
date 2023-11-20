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

import edu.northeastern.cs6650.project3.interfaces.KeyValueStoreTwoPhaseCommitCoordinatorInterface;
import edu.northeastern.cs6650.project3.interfaces.KeyValueStoreInterface;
import edu.northeastern.cs6650.project3.common.Utils;
import edu.northeastern.cs6650.project3.common.RequestType;

public class KeyValueStoreTwoPhaseCommitCoordinator extends UnicastRemoteObject implements KeyValueStoreTwoPhaseCommitCoordinatorInterface {

    private static final Logger LOGGER = Logger.getLogger(KeyValueStoreTwoPhaseCommitCoordinator.class.getName());
    private int numberOfKeyValueServers;
    private Registry serverRegistry;
    private KeyValueStoreInterface keyValueStoreRMIServers[];
    private boolean keyValueStoreRMIServersInitialized;
    private String serverIPAddress;
    private int serverPort;

    public KeyValueStoreTwoPhaseCommitCoordinator(String serverIPAddress, int serverPort, int numberOfKeyValueServers) throws RemoteException {
        super();
        this.numberOfKeyValueServers = numberOfKeyValueServers;
        this.serverIPAddress = serverIPAddress; 
        this.serverPort = serverPort;
        keyValueStoreRMIServers = new KeyValueStoreInterface[numberOfKeyValueServers];
        keyValueStoreRMIServersInitialized = false;
    }

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

    public boolean prepare(String key, String value) throws RemoteException {
        return true;
    }

    public boolean commit(String key, String value) throws RemoteException {
        return true;
    }

    public boolean abort(String key, String value) throws RemoteException {
        return true;
    }
}