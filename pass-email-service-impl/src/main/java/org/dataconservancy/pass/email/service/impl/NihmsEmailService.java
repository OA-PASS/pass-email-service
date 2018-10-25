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
import java.util.Scanner;

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
    private static final String JMS_FALLBACK_MESSAGE_TRIGGER = " MSREFID";

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
                boolean isSuccess = message.getSubject().endsWith(SUCCESS_CRITERION);
                if (!message.getFlags().contains(Flags.Flag.SEEN)) {
//TODO: process the NihmsSubmissionMessage objects for this message
                    }
                message.setFlag(Flags.Flag.SEEN, isSuccess);
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
            } else {
                Scanner scanner = new Scanner((String) content);//we'll go line by line
                String line;

                while (scanner.hasNextLine()) {//skip blank lines
                    line = scanner.nextLine();
                    if (line.contains(JMS_MESSAGE_TRIGGER)) {
                        submissionMessageList.add(formSubmissionMessage(message, line));
                    } else if (line.contains(JMS_FALLBACK_MESSAGE_TRIGGER)) {//no taskId here, try to assemble some info
                        String out = line;
                        if (scanner.hasNextLine()) {//we need two lines for these error messages
                            if (scanner.nextLine().length() == 0 && scanner.hasNextLine()) {
                                out = String.join(" ", out, scanner.nextLine());
                            }
                        }
                        submissionMessageList.add(formSubmissionMessage(message, out));
                    }
                }
                scanner.close();
            }

        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return submissionMessageList;
    }

    private NihmsSubmissionMessage formSubmissionMessage(Message message, String info) throws MessagingException {
        NihmsSubmissionMessage sm = new NihmsSubmissionMessage();
        sm.setMessageId(getHeaderValue(message.getAllHeaders(), "Message-ID"));
        sm.setSentDate(message.getSentDate());
        sm.setLatestReadDate(new Date());
        sm.setSubmitted(message.getSubject().endsWith(SUCCESS_CRITERION));

        if (info.contains(JMS_MESSAGE_TRIGGER)) {
            Scanner scanner = new Scanner(info);
            scanner.findInLine(JMS_MESSAGE_TRIGGER);
            if(scanner.hasNext()) {
                sm.setTaskId(scanner.next());
            }
            scanner.close();
        }

        // setting the outcomeDescription is a little tricky - there are three cases:
        // info string looks like
        // Job TaskId=<stuff> (success)
        // <stuff>Job TaskId=<more stuff> (nicely formed errors)
        // <stuff with no "Job taskId"> (free form errors)
        // use <stuff> in any case
        if(info.startsWith(JMS_MESSAGE_TRIGGER)) {
            sm.setOutcomeDescription(info.substring(JMS_MESSAGE_TRIGGER.length()));
        } else if (info.contains(JMS_MESSAGE_TRIGGER)) {
            sm.setOutcomeDescription(info.substring(0,info.indexOf(JMS_MESSAGE_TRIGGER)));
        } else {//no taskId here, just give what we have in info
            sm.setOutcomeDescription(info);
        }

        if (sm.isSubmitted()) {//let's get the ID
            Scanner scanner = new Scanner(info);
            scanner.findInLine("ID=");
            if(scanner.hasNext()) {
                sm.setNihmsId(scanner.next());
            }
            scanner.close();
        }

        return sm;
    }

    private String getHeaderValue (Enumeration<Header> headers, String key) {
        while (headers.hasMoreElements()) {
            Header header = headers.nextElement();
            if(header.getName().equals(key)){
                return header.getValue();
            }
        }
        return null;
    }

}
