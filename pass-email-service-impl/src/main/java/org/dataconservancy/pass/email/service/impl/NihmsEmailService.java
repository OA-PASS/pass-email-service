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
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

public class NihmsEmailService {
    private Logger LOG = LoggerFactory.getLogger(NihmsEmailService.class);

    private static final String SUCCESS_CRITERION = "Bulk submission submitted";
    private static final String JMS_MESSAGE_TRIGGER = "Job TaskId=";
    private static final String JMS_FALLBACK_MESSAGE_TRIGGER = " MSREFID";//leading space is important
    private static final String MESSAGE_ID_HEADER_KEY = "Message-ID";
    private static final String NIHMS_ID_KEY = "ID=";
    private static final String SUBJECT_SEARCH_STRING = "Bulk submission";

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
     * This method is responsible for retrieving the emails from the appropriate inbox, and extracting all
     * messages which need to be processed. Emails indicating successful submissions will be flagged as SEEN, so that
     * they will not be processed on the next run.
     *
     * @param protocol - the mail transport protocol used to connect
     * @param host     - the host to connect to
     * @param port     - the port on the host to connect to
     * @param userName - the name of the user on the mail account to be read
     * @param password - the password for the user of the mail account
     * @return the list of unseen emails which match our subject search string
     */
    public List<Message> getEmails(String protocol, String host, String port, String userName, String password) {
        Properties props = serverProperties(protocol, host, port);
        Session session = Session.getDefaultInstance(props);
        List<Message> messagesToBeProcessed = new ArrayList<>();

        try (Store store = session.getStore(protocol);
             Folder inbox = store.getFolder("INBOX")
        ) {
            store.connect(userName, password);
            inbox.open(Folder.READ_WRITE);

            SearchTerm searchTerm = new SubjectTerm(SUBJECT_SEARCH_STRING);
            Message[] messageArray = inbox.search(searchTerm);

            for (Message message : messageArray) {
                boolean isSuccess = message.getSubject().endsWith(SUCCESS_CRITERION);
                if (!message.getFlags().contains(Flags.Flag.SEEN)) {
                    if (isSuccess) {
                        message.setFlag(Flags.Flag.SEEN, isSuccess);
                    }
                    messagesToBeProcessed.add(message);
                    LOG.info("Message with massageId " +
                            getHeaderValue(message.getAllHeaders(), MESSAGE_ID_HEADER_KEY) +
                            " added to message processing list.");
                }
            }

        } catch (NoSuchProviderException e) {
            LOG.error("No such provider for protocol: " + protocol);
            e.printStackTrace();
        } catch (MessagingException e) {
            LOG.error("Unable to connect to the message store");
            e.printStackTrace();
        }
        return messagesToBeProcessed;
    }

    /**
     * Procss an email message by parsing it to generate one submission message for each submission mentioned in
     * the email.
     *
     * @param message the email message to process
     * @return a List of SubmissionMessages to be put in a message queue
     */
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
                //repair some escaped stuff to fix parsing issues
                bodyPart = bodyPart.replace("&gt;", ">").replace("&lt;", "<")
                        .replace("&quot;", "\"");
                Document doc = Jsoup.parse(bodyPart);
                Elements submissions = doc.select("td");
                for (Element submission : submissions) {//we may have several submissions in this email message
                    if (submission.text().contains(JMS_MESSAGE_TRIGGER)) {
                        submissionMessageList.add(formSubmissionMessage(message, submission.text()));
                    }
                }
            } else {//this is a plain text email message
                Scanner scanner = new Scanner((String) content);//we'll go line by line
                scanner.useDelimiter("\\r?\\n");
                String line;

                while (scanner.hasNext()) {//skip blank lines
                    line = scanner.next();
                    if (line.contains(JMS_MESSAGE_TRIGGER)) {
                        submissionMessageList.add(formSubmissionMessage(message, line));
                    } else if (line.contains(JMS_FALLBACK_MESSAGE_TRIGGER)) {//no taskId here, try to assemble some info
                        String out = line;
                        String put = "";
                        while(scanner.hasNext() && put.length() == 0) {//tack on next non-empty line
                            put = scanner.next();
                        }
                        submissionMessageList.add(formSubmissionMessage(message, String.join(" ", out, put)));
                    }
                }
                scanner.close();
            }

        } catch (MessagingException e) {
            LOG.error("Messaging Exception ", e);
        } catch (IOException e) {
           LOG.error("IO Exception ", e);
        }

        return submissionMessageList;
    }

    /**
     * A method to generate a Nihms submission message from an email message and an info string parsed from the email
     * This method may be called several times on the same email message if there are several submissions contained in the
     * email. The email is passed in to populate fields on the Nihms submission message to be created.
     *
     * @param message the email message being processed
     * @param info the info string parsed from the email
     * @return a new submission message correspomding to this email and info string
     * @throws MessagingException
     */
    private NihmsSubmissionMessage formSubmissionMessage(Message message, String info) throws MessagingException {
        NihmsSubmissionMessage sm = new NihmsSubmissionMessage();
        sm.setMessageId(getHeaderValue(message.getAllHeaders(), MESSAGE_ID_HEADER_KEY));
        sm.setSentDate(message.getSentDate());
        sm.setLatestReadDate(new Date());
        sm.setSubmitted(message.getSubject().endsWith(SUCCESS_CRITERION));

        if (info.contains(JMS_MESSAGE_TRIGGER)) {
            Scanner scanner = new Scanner(info);
            scanner.findInLine(JMS_MESSAGE_TRIGGER);
            if (scanner.hasNext()) {
                sm.setTaskId(scanner.next());
            }
            if (sm.isSubmitted()) {//let's get the ID
                //scanner reset() not necessary - this occurs after the first findInLine() above
                scanner.findInLine(NIHMS_ID_KEY);
                if (scanner.hasNext()) {
                    sm.setNihmsId(scanner.next());
                }
            }
            scanner.close();
        }

        /* there are three cases for setting the outcomeDescription
         *info string looks like
         * <ul>
         *     <li>Job taskId=<stuff> (success)</li>
         *     <li><stuff>Job TaskId=<more stuff> (nicely formed errors)</li>
         *     <li><stuff (with no "Job taskId")> (free form errors)</li>
         * </ul>
         * use <stuff> in any case
         *
         */

        if(info.startsWith(JMS_MESSAGE_TRIGGER)) {
            sm.setOutcomeDescription(info.substring(JMS_MESSAGE_TRIGGER.length()));
        } else if (info.contains(JMS_MESSAGE_TRIGGER)) {
            sm.setOutcomeDescription(info.substring(0,info.indexOf(JMS_MESSAGE_TRIGGER)));
        } else {//no taskId here, just give what we have in info
            sm.setOutcomeDescription(info);
        }

        return sm;
    }

    /**
     * a convenience method to grab a mail header value
     * @param headers the list of headers in the message
     * @param key the key for the header
     * @return the value for the header
     */
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
