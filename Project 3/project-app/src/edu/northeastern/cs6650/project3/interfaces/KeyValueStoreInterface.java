package edu.northeastern.cs6650.project3.interfaces;
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

import edu.northeastern.cs6650.project3.common.RequestType;
/*
 * This RMI interface declare methods for the three application operations
 * 
 * These methods are called by the client and implemented on the server.
 */
public interface KeyValueStoreInterface extends Remote {
    void putKeyValue(String key, String value) throws RemoteException, ExecutionException;
    void deleteKeyValue(String key) throws RemoteException, NoSuchElementException, ExecutionException;
    String getValue(String key) throws RemoteException, NoSuchElementException;
   
    boolean canCommit(RequestType requestType, String key, String value) throws RemoteException;
    boolean doCommit(RequestType requestType, String key, String value) throws RemoteException;
    boolean doAbort(RequestType requestType, String key, String value) throws RemoteException;
}
