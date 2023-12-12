package edu.northeastern.cs6650.project4.interfaces;
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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

import edu.northeastern.cs6650.project4.common.RequestType;
import edu.northeastern.cs6650.project4.common.RequestObject;
import edu.northeastern.cs6650.project4.common.PromiseObject;

/*
 * This RMI interface declare methods for PAXOS operations
 * 
 * These methods are called by the client and implemented on the server.
 */
public interface KeyValuePaxosInterface extends Remote {

    // Paxos related methods
    PromiseObject prepare(Long proposalId) throws RemoteException;
    boolean propose(Long proposalId, RequestObject request) throws RemoteException;
    void learn(Long proposalId, RequestObject request) throws RemoteException;
}
