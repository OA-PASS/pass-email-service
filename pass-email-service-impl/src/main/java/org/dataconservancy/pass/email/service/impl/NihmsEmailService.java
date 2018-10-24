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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;

public class NihmsEmailService {
    private Logger LOG = LoggerFactory.getLogger(NihmsEmailService.class);
    private static final String SUCCESS_CRITERION = "Bulk submission submitted";
    private static final String JMS_MESSAGE_TRIGGER = "Job TaskId=";

    private Properties serverProperties(String protocol, String host, String port) {
        Properties props = new Properties();
        props.put(String.format("mail.%s.host", protocol), host);
        props.put(String.format("mail.%s.port", protocol), port);
        props.setProperty(String.format("mail.%s.socketFactory.class", protocol), "javax.net.ssl.SSLSocketFactory");
        props.setProperty(String.format("mail.%s.socketFactory.fallback", protocol), "false");
        props.setProperty(String.format("mail.%s.socketFactory.port", protocol), String.valueOf(port));

        return props;
    }

    /**
     * @param protocol - the mail transport protocol used to connect
     * @param host     - the host to connect to
     * @param port     - the port on the host to connect to
     * @param userName - the name of the user on the mail account to be read
     * @param password - the password for the user of the mail account
     */
    public void getEmails(String protocol, String host, String port, String userName, String password) {
        Properties props = serverProperties(protocol, host, port);
        Session session = Session.getDefaultInstance(props);

        try (Store store = session.getStore(protocol);
             Folder inbox = store.getFolder("INBOX")
        ) {
            store.connect(userName, password);
            inbox.open(Folder.READ_WRITE);

            Message[] messageArray = inbox.getMessages();
            for (Message message : messageArray) {
                boolean isSuccessful = message.getSubject().endsWith(SUCCESS_CRITERION);
                if (!message.getFlags().contains(Flags.Flag.SEEN)) {
//TODO: process the NihmsSubmissionMessage objects for this message
                    }
                message.setFlag(Flags.Flag.SEEN, isSuccessful);
            }


        } catch (NoSuchProviderException e) {
            LOG.info("No such provider for protocol: " + protocol);
            e.printStackTrace();
        } catch (MessagingException e) {
            LOG.info("Unable to connect to the message store");
            e.printStackTrace();
        }
    }

    List<NihmsSubmissionMessage> processMessage(Message message) {
        List<NihmsSubmissionMessage> submissionMessageList = new ArrayList<>();
        try {
            Object content = message.getContent();
            if (content instanceof MimeMultipart) {//have html to parse
                MimeMultipart mmp = (MimeMultipart) content;
                String bodyPart = (String) mmp.getBodyPart(0).getContent();
                for (int i = 0; i < mmp.getCount(); i++) {
                    if (mmp.getBodyPart(i).getContentType().contains("text/html")) {
                        bodyPart = (String) mmp.getBodyPart(i).getContent();
                        break;
                    }
                }
                //repair some escaped stuff to fix parsing
                bodyPart = bodyPart.replace("&gt;", ">").replace("&lt;", "<")
                        .replace("&quot;", "\"");
                Document doc = Jsoup.parse(bodyPart);
                Elements submissions = doc.select("td");
                for (Element submission : submissions) {//we may have several submissions in this email message
                    if (submission.text().contains("TaskId")) {
                       submissionMessageList.add(formSubmissionMessage(message, submission.text()));
                    }
                }
            } else {//message content is plain text - we are in wild west mode
                String[] lines = ((String) content).split("\n");
                String out = null;
                int counter = 0;
                boolean haveHeuristicTrigger = false;
                for (String line : lines) {//we may have several submissions in this email message
                    if (haveHeuristicTrigger) {
                        counter++;
                    }
                    if (line.contains(JMS_MESSAGE_TRIGGER)) {
                        submissionMessageList.add(formSubmissionMessage(message, line));
                    } else if (line.contains(" MSREFID")) {//failure mode 6 - need a different trigger
                        out = line;
                        haveHeuristicTrigger = true;
                    } else if (counter == 2) {//still failure mode 6 - we will pick up the second line after the trigger
                        //(first is blank) and append it to the trigger line. this is the detail for the error.
                        out = String.join(" ", out,line);
                        submissionMessageList.add(formSubmissionMessage(message, out));
                        counter = 0;
                        haveHeuristicTrigger = false;
                    }
                }
            }
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
        return submissionMessageList;
    }

    private NihmsSubmissionMessage formSubmissionMessage(Message message, String info) throws MessagingException {
        NihmsSubmissionMessage sm = new NihmsSubmissionMessage();
        sm.setMessageId(getMessageID(message.getAllHeaders()));
        sm.setSentDate(message.getSentDate());
        sm.setLatestReadDate(new Date());
        sm.setSubmitted(message.getSubject().endsWith(SUCCESS_CRITERION));

        // setting the outcomeDescription is a little tricky - there are three cases:
        // info string looks like
        // <stuff>Job TaskId=<more stuff> (nicely formed errors)
        // Job TaskId=<stuff> (success)
        // <stuff with no "Job taskId"> (free form errors)
        if (info.contains(JMS_MESSAGE_TRIGGER)) {
            String[] rawMessage = info.split(JMS_MESSAGE_TRIGGER);
            String[] taskIdVein = rawMessage[rawMessage.length - 1].split(" ");//"mining" for taskId
            sm.setTaskId(taskIdVein[0]);//first token will be the taskId

            if (rawMessage[0].length() > 0 && rawMessage.length > 1) {//first case
                sm.setOutcomeDescription(rawMessage[0]);
                } else {
                    if (rawMessage.length > 1) {//second case
                        sm.setOutcomeDescription(rawMessage[1]);
                    }
                }
        } else {//no taskId here, just give what we have in info
            sm.setOutcomeDescription(info);
        }

        if (sm.isSubmitted()) {//let's get the ID
            String[] infoParts = info.split(" ");
            for (String s : infoParts) {
                if (s.contains("=")) {
                    String[] pair = s.split("=");
                    if (pair[0].equals("ID")) {
                        sm.setNihmsId(pair[1]);
                    }
                }
            }

        }
        return sm;
    }

    private String getMessageID (Enumeration<Header> headers) {
        while (headers.hasMoreElements()) {
            Header header = headers.nextElement();
            if(header.getName().equals("Message-ID")){
                return header.getValue();
            }
        }
        return null;
    }

}
