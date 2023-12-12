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

/*
 * Enum containing values for different server response codes sent by the server
 */
public enum ServerResponseCode {
    SUCCESS(200),
    BAD_REQUEST(400),
    FAILED(500),
    NONE(-1);

    private final int code;

    ServerResponseCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }    

    public static ServerResponseCode parseString(String code) {

        for (ServerResponseCode type : ServerResponseCode.values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }

        return NONE;
    }
}
