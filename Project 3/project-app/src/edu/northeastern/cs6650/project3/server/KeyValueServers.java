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

import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Logger;
import java.rmi.Naming;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;

import edu.northeastern.cs6650.project3.server.KeyValueStoreRMIServer;
import edu.northeastern.cs6650.project3.server.KeyValueStoreTwoPhaseCommitCoordinator;
import edu.northeastern.cs6650.project3.common.Utils;

/*
 * Implementation of the server application
 * 
 * Based on the arguments provided, a RMI registry server is create along with the RMI server
 * hosting the remote object. Before a client executes a method on the remote object, it performs
 * a lookup on the registry using the remote object name and the server returns a "reference" to the
 * remote object. The client calls these methods, the methods are executed by the remote object on this
 * server and results are returned. To terminate the server, press Control-C.
 */
public class KeyValueServers {
    
    public static void main(String[] arguments) {  

        if(arguments.length != 3) {
            System.err.println("Incorrect number of arguments. Correct usage: java -classpath classes edu.northeastern.cs6650.project3.server.Server <server ip> <server port> <number key value servers>.");
        } else {
            try {

                // Read the arguments
                String ipAddress = arguments[0];
                int registryPort = Integer.parseInt(arguments[1]);
                int numberOfKeyValueServers = Integer.parseInt(arguments[2]);
                
                // Set up the RMI registry server
                System.setProperty("java.rmi.server.hostname", ipAddress);
                Registry coordinatorRegistry = LocateRegistry.createRegistry(registryPort);
                
                // Set up the RMI server hosting the remote object
                KeyValueStoreTwoPhaseCommitCoordinator twoPhaseCoordinator = new KeyValueStoreTwoPhaseCommitCoordinator(ipAddress, registryPort, numberOfKeyValueServers);
                coordinatorRegistry.bind(Utils.KEY_VALUE_STORE_RMI_TWO_PHASE_COMMIT_COORDINATOR_NAME, twoPhaseCoordinator);
                
                KeyValueStoreRMIServer keyValueStoreRMIServers[] = new KeyValueStoreRMIServer[numberOfKeyValueServers];
                for(int i = 0; i < numberOfKeyValueServers; i++) {
                    Registry participantRegistry = LocateRegistry.createRegistry(registryPort + i + 1);
                    keyValueStoreRMIServers[i] = new KeyValueStoreRMIServer("localhost", registryPort, i, numberOfKeyValueServers);
                    participantRegistry.bind(Utils.KEY_VALUE_STORE_RMI_SERVER_NAME, keyValueStoreRMIServers[i]);
                }
                
                // Initialize the remote objects
                for(int i = 0; i < numberOfKeyValueServers; i++) {
                    keyValueStoreRMIServers[i].initializeRemoteObjects();
                }
                twoPhaseCoordinator.intializeKeyValueStoreServers();
            } catch (NumberFormatException nfe) {
                System.err.println("The port number should be an integer.");
            } catch(IllegalArgumentException iae) {
                System.err.println("The server type should be either tcp or udp.");
            } catch(Exception exp) {
                exp.printStackTrace();
                System.err.println("Problem encountered while starting the server.");
            }
        }
        
    }
}
