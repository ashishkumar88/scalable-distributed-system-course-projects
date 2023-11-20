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
import edu.northeastern.cs6650.project3.common.RequestType;

public interface KeyValueStoreTwoPhaseCommitCoordinatorInterface extends Remote {
    public boolean prepare(String key, String value) throws RemoteException;
    public boolean commit(String key, String value) throws RemoteException;
    public boolean abort(String key, String value) throws RemoteException;
}