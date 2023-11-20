package edu.northeastern.cs6650.project2.common;
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
 * Enum containing values for different request types supported by the server
 */
public enum RequestType {
    GET,
    PUT,
    DELETE,
    QUIT,
    NONE;

    public static RequestType parseString(String requestType) {

        for (RequestType type : RequestType.values()) {
            if (type.name().equals(requestType.toUpperCase())) {
                return type;
            }
        }

        return NONE;
    }
}