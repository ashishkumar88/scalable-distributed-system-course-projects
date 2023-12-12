package edu.northeastern.cs6650.project4.common;
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

import java.io.Serializable;

/*
* This class is used to store the promise object which is sent by the acceptor to the proposer.
*/
public class PromiseObject implements Serializable {
    private Long proposalNumber;
    private String lastAcceptedKey;
    private String lastAcceptedValue;
    private Long lastAcceptedProposalNumber;

    public PromiseObject(Long proposalNumber, String lastAcceptedKey, String lastAcceptedValue, Long lastAcceptedProposalNumber) {
        this.proposalNumber = proposalNumber;
        this.lastAcceptedKey = lastAcceptedKey;
        this.lastAcceptedValue = lastAcceptedValue;
        this.lastAcceptedProposalNumber = lastAcceptedProposalNumber;
    }

    public Long getProposalNumber() {
        return proposalNumber;
    }

    public String getLastAcceptedKey() {
        return lastAcceptedKey;
    }

    public String getLastAcceptedValue() {
        return lastAcceptedValue;
    }

    public Long getLastAcceptedProposalNumber() {
        return lastAcceptedProposalNumber;
    }

    public void setProposalNumber(Long proposalNumber) {
        this.proposalNumber = proposalNumber;
    }

    public void setLastAcceptedKey(String lastAcceptedKey) {
        this.lastAcceptedKey = lastAcceptedKey;
    }

    public void setLastAcceptedValue(String lastAcceptedValue) {
        this.lastAcceptedValue = lastAcceptedValue;
    }

    public void setLastAcceptedProposalNumber(Long lastAcceptedProposalNumber) {
        this.lastAcceptedProposalNumber = lastAcceptedProposalNumber;
    }
}