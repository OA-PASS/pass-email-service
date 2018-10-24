/*
 * Copyright 2018 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataconservancy.pass.email.service.impl;

import org.dataconservancy.pass.email.service.model.NihmsSubmissionMessage;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;


public class NihmsEmailServiceTest {

    private final String[] messages = {"messages/NIHMS-failure-1.txt",
            "messages/NIHMS-failure-2.txt",
            "messages/NIHMS-failure-3.txt",
            "messages/NIHMS-failure-4.txt",
            "messages/NIHMS-failure-5.txt",
            "messages/NIHMS-failure-6.txt",
            "messages/NIHMS-success.txt"
    };



    private NihmsEmailService underTest = new NihmsEmailService();

    @Test
    public void testReadEmailFromFile() throws MessagingException {
        List<NihmsSubmissionMessage> results;
        for (String fileName : messages) {
            System.out.println("********" + fileName);
            results = underTest.processMessage(getMessageFromFile(fileName));
            for (NihmsSubmissionMessage sm : results) {
                System.out.println(sm.toString());
                System.out.println("");
            }
        }

    }

    private Message getMessageFromFile(String fileName) throws MessagingException {
        InputStream mailFileInputStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        return new MimeMessage(session, mailFileInputStream);
    }
}
