/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.Level;

/**
 * This configuration defaults to no logging.
 */
public class NullConfiguration extends AbstractConfiguration {

    /**
     * Name of this configuration.
     */
    public static final String NULL_NAME = "Null";

    public NullConfiguration() {
        super(ConfigurationSource.NULL_SOURCE);

        setName(NULL_NAME);
        final LoggerConfig root = getRootLogger();
        root.setLevel(Level.OFF);
    }

}
