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
package org.apache.logging.log4j.core.net.ssl;

import java.util.ArrayList;
import java.util.Random;

public class TLSSyslogTestUtil {
    public static final String ABC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String NUMBERS = "0123456789";
    public static final String WHITESPACES = " \t\n";
    public static final String CHARSET = ABC + NUMBERS;
    public static final String LEGACY_BSD_SYSLOG_CHARSET = ABC + NUMBERS + " \t";
    public static final String SYSLOG_CHARSET = ABC + NUMBERS + WHITESPACES;

    public static ArrayList<String> generateMessages(int numberOfMessages) {
        return generateMessages(numberOfMessages, CHARSET);
    }

    public static ArrayList<String> generateMessages(int numberOfMessages, TLSSyslogMessageFormat format) {
        switch (format) {
            case SYSLOG:
                return generateMessages(numberOfMessages, SYSLOG_CHARSET);
            case LEGACY_BSD:
                return generateMessages(numberOfMessages, LEGACY_BSD_SYSLOG_CHARSET);
            default:
                throw new IllegalArgumentException();
        }
    }

    private static ArrayList<String> generateMessages(int numberOfMessages, String charSet) {
        ArrayList<String> messageList = new ArrayList<String>(numberOfMessages);
        for (int i = 0; i < numberOfMessages; i++) {
            String message = createRandomMessage(charSet);
            messageList.add(message);
        }
        return messageList;
    }

    private static String createRandomMessage(String charset) {
        char[] chars = charset.toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < random.nextInt(10000); i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        String output = sb.toString();
        return output;
    }

    public static int getRandomInt(int max) {
        Random random = new Random();
        int n = random.nextInt(max);
        if (n < 0)
            return -n;
        return n;
    }
}

