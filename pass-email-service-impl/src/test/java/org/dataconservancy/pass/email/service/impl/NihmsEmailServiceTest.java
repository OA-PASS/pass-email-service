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
import org.junit.Before;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import static java.util.Locale.US;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class NihmsEmailServiceTest {

    private final String[] messages = {"messages/NIHMS-failure-1.txt",
            "messages/NIHMS-failure-2.txt",
            "messages/NIHMS-failure-3.txt",
            "messages/NIHMS-failure-4.txt",
            "messages/NIHMS-failure-5.txt",
            "messages/NIHMS-failure-6.txt",
            "messages/NIHMS-success.txt"
    };

    private final NihmsSubmissionMessage expectedFailureMessage1 = new NihmsSubmissionMessage();
    private final NihmsSubmissionMessage expectedFailureMessage2 = new NihmsSubmissionMessage();
    private final NihmsSubmissionMessage expectedFailureMessage3 = new NihmsSubmissionMessage();
    private final NihmsSubmissionMessage expectedFailureMessage4 = new NihmsSubmissionMessage();
    private final NihmsSubmissionMessage expectedFailureMessage5 = new NihmsSubmissionMessage();
    private final NihmsSubmissionMessage expectedFailureMessage6 = new NihmsSubmissionMessage();
    private final NihmsSubmissionMessage expectedFailureMessage7 = new NihmsSubmissionMessage();
    private final NihmsSubmissionMessage expectedSuccessMessage1 = new NihmsSubmissionMessage();
    private final NihmsSubmissionMessage expectedSuccessMessage2 = new NihmsSubmissionMessage();
    
    private List<NihmsSubmissionMessage> expectedMessages = new ArrayList<>();

    @Before
    public void setup() {
        Date now = new Date();
        //in failure file a
        expectedFailureMessage1.setOutcomeDescription("Wrong request file name format: \\\\freezer\\ftp-nihms\\bulk-upload\\JHU_DC_TEST\\" +
                "upload\\2018-06-20\\nihms-native-2017-07_2018-06-20T011745.109_3d847c29-cb6b-4edd-8afc-b24aa574051d.tar.gzip");
        expectedFailureMessage1.setSubmitted(false);
        expectedFailureMessage1.setSentDate(makeDateFromString("Tue Jun 19 21:30:10 EDT 2018"));
        expectedFailureMessage1.setLatestReadDate(now);
        expectedFailureMessage1.setMessageId("<20180620013010.32F071A0010@mail2.ncbi.nlm.nih.gov>");
        expectedFailureMessage1.setTaskId("nihms-native-2017-07_2018-06-20T011745.109_3d847c29-cb6b-4edd-8afc-b24aa574051d.tar.gzip");
        expectedFailureMessage1.setNihmsId(null);
        expectedMessages.add(expectedFailureMessage1);

        //in failure file 2
        expectedFailureMessage2.setOutcomeDescription("Error (3, 4): The element 'journal-meta' has incomplete content. " +
                "List of possible elements expected: 'journal-id issn'.");
        expectedFailureMessage2.setSubmitted(false);
        expectedFailureMessage2.setSentDate(makeDateFromString("Wed Jun 20 16:15:17 EDT 2018"));
        expectedFailureMessage2.setLatestReadDate(now);
        expectedFailureMessage2.setMessageId("<20180620201517.E01801A0003@mail2.ncbi.nlm.nih.gov>");
        expectedFailureMessage2.setTaskId("nihms-native-2017-07_2018-06-20_20-06-12_6f8a0aa9-f330-4239-9b9e-6560435c0504");
        expectedFailureMessage2.setNihmsId(null);
        expectedMessages.add(expectedFailureMessage2);

        //in failure file 2
        expectedFailureMessage3.setOutcomeDescription("Error (3, 4): The element 'journal-meta' has incomplete content. " +
                "List of possible elements expected: 'journal-id issn'.");
        expectedFailureMessage3.setSubmitted(false);
        expectedFailureMessage3.setSentDate(makeDateFromString("Wed Jun 20 16:15:17 EDT 2018"));
        expectedFailureMessage3.setLatestReadDate(now);
        expectedFailureMessage3.setMessageId("<20180620201517.E01801A0003@mail2.ncbi.nlm.nih.gov>");
        expectedFailureMessage3.setTaskId("nihms-native-2017-07_2018-06-20_20-06-47_d65c9662-76d2-490b-a82f-2db05dc924fa");
        expectedFailureMessage3.setNihmsId(null);
        expectedMessages.add(expectedFailureMessage3);

        //in failure file 3
        expectedFailureMessage4.setOutcomeDescription("Error (3, 4): The element 'journal-meta' has incomplete content. " +
                "List of possible elements expected: 'journal-id issn'.");
        expectedFailureMessage4.setSubmitted(false);
        expectedFailureMessage4.setSentDate(makeDateFromString("Wed Jun 20 16:45:19 EDT 2018"));
        expectedFailureMessage4.setLatestReadDate(now);
        expectedFailureMessage4.setMessageId("<20180620204519.9BBEA1A0002@mail2.ncbi.nlm.nih.gov>");
        expectedFailureMessage4.setTaskId("nihms-native-2017-07_2018-06-20_20-06-25_d0ff66f3-f336-40c1-b8c1-57042c65057b");
        expectedFailureMessage4.setNihmsId(null);
        expectedMessages.add(expectedFailureMessage4);

        //in failure file 4
        expectedFailureMessage5.setOutcomeDescription("Cannot find manifest file manifest.txt. Check if it exists. " +
                "Also check if tar package was created incorrectly with files in a subdirectory instead of the root of the archive.");
        expectedFailureMessage5.setSubmitted(false);
        expectedFailureMessage5.setSentDate(makeDateFromString("Thu Jun 21 22:45:15 EDT 2018"));
        expectedFailureMessage5.setLatestReadDate(now);
        expectedFailureMessage5.setMessageId("<20180622024515.C2B881A0002@mail2.ncbi.nlm.nih.gov>");
        expectedFailureMessage5.setTaskId("nihms-native-2017-07_2018-06-20_20-06-25_d0ff66f3-f336-40c1-b8c1-57042c65057b");
        expectedFailureMessage5.setNihmsId(null);
        expectedMessages.add(expectedFailureMessage5);

        //in failure file 5
        expectedFailureMessage6.setOutcomeDescription("Error (4, 6): The required attribute 'pub-type' is missing.");
        expectedFailureMessage6.setSubmitted(false);
        expectedFailureMessage6.setSentDate(makeDateFromString("Fri Jun 22 10:45:12 EDT 2018"));
        expectedFailureMessage6.setLatestReadDate(now);
        expectedFailureMessage6.setMessageId("<20180622144512.4D62B340004@mail1.ncbi.nlm.nih.gov>");
        expectedFailureMessage6.setTaskId("nihms-native-2017-07_2018-06-22_14-06-29_5accc350-6e74-4870-b2d9-f361d3c2db99");
        expectedFailureMessage6.setNihmsId(null);
        expectedMessages.add(expectedFailureMessage6);

        //in failure file 6
        expectedFailureMessage7.setOutcomeDescription("We were unable to generate a PDF Receipt for MSREFID1861125. The following error was encountered: " +
                "File \"IMG_2917.MOV\" content cannot be extracted.");
        expectedFailureMessage7.setSubmitted(false);
        expectedFailureMessage7.setSentDate(makeDateFromString("Sun Jun 24 17:00:04 EDT 2018"));
        expectedFailureMessage7.setLatestReadDate(now);
        expectedFailureMessage7.setMessageId("<20180624210004.C5D04340004@mail1.ncbi.nlm.nih.gov>");
        expectedFailureMessage7.setTaskId(null);
        expectedFailureMessage7.setNihmsId(null);
        expectedMessages.add(expectedFailureMessage7);

        //in success file
        expectedSuccessMessage1.setOutcomeDescription("nihms-native-2017-07_2018-06-25_17-06-03_c0bc5281-884e-4a1d-bcb5-533e882cf355 " +
                "for Manuscript ID=969594 was submitted successfully.");
        expectedSuccessMessage1.setSubmitted(true);
        expectedSuccessMessage1.setSentDate(makeDateFromString("Mon Jun 25 13:46:42 EDT 2018"));
        expectedSuccessMessage1.setLatestReadDate(now);
        expectedSuccessMessage1.setMessageId("<20180625174642.E2F101A0002@mail2.ncbi.nlm.nih.gov>");
        expectedSuccessMessage1.setTaskId("nihms-native-2017-07_2018-06-25_17-06-03_c0bc5281-884e-4a1d-bcb5-533e882cf355");
        expectedSuccessMessage1.setNihmsId("969594");
        expectedMessages.add(expectedSuccessMessage1);

        //in success file
        expectedSuccessMessage2.setOutcomeDescription("nihms-native-2017-07_2018-06-25_17-06-10_b6bb04a6-de25-4623-8902-535634e77fb0 " +
                "for Manuscript ID=969595 was submitted successfully.");
        expectedSuccessMessage2.setSubmitted(true);
        expectedSuccessMessage2.setSentDate(makeDateFromString("Mon Jun 25 13:46:42 EDT 2018"));
        expectedSuccessMessage2.setLatestReadDate(now);
        expectedSuccessMessage2.setMessageId("<20180625174642.E2F101A0002@mail2.ncbi.nlm.nih.gov>");
        expectedSuccessMessage2.setTaskId("nihms-native-2017-07_2018-06-25_17-06-10_b6bb04a6-de25-4623-8902-535634e77fb0");
        expectedSuccessMessage2.setNihmsId("969595");
        expectedMessages.add(expectedSuccessMessage2);
    }

    private NihmsEmailService underTest = new NihmsEmailService();

    @Test
    public void testReadEmailFromFiles() throws MessagingException {
        List<NihmsSubmissionMessage> results;
        //first the files which have just one submission
        int[] singleSubmissionFiles = {0, 2, 3, 4, 5};//files 1, 3, 4, 5, 6
        for(int i : singleSubmissionFiles) {
            results = underTest.processMessage(getMessageFromFile(messages[i]));
            assertEquals(1, results.size());
            NihmsSubmissionMessage expected = expectedMessages.get(i==0?i:i+1);//offset because of file 2
            NihmsSubmissionMessage actual = results.get(0);
            compareMessages(expected, actual);
        }

        //test multiple failure file - file 2
        results = underTest.processMessage(getMessageFromFile(messages[1]));
        assertEquals(2,results.size());

        if(results.get(0).getTaskId().equals(expectedFailureMessage2.getTaskId())) {
            compareMessages(expectedFailureMessage2, results.get(0));
            compareMessages(expectedFailureMessage3, results.get(1));
        } else {
            compareMessages(expectedFailureMessage2, results.get(1));
            compareMessages(expectedFailureMessage3, results.get(0));
        }

        //test (multiple) success file
        results = underTest.processMessage(getMessageFromFile(messages[6]));
        assertEquals(2,results.size());

        if(results.get(0).getTaskId().equals(expectedSuccessMessage1.getTaskId())) {
            compareMessages(expectedSuccessMessage1, results.get(0));
            compareMessages(expectedSuccessMessage2, results.get(1));
        } else {
            compareMessages(expectedSuccessMessage1, results.get(1));
            compareMessages(expectedSuccessMessage2, results.get(0));
        }
    }

    private Message getMessageFromFile(String fileName) throws MessagingException {
        InputStream mailFileInputStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        return new MimeMessage(session, mailFileInputStream);
    }

    private Date makeDateFromString(String dateString) {
        String[] parts = dateString.split(" ");
        String[] timeParts = parts[3].split(":");

        String monthString = parts[1];
        int month = -1;
        switch(monthString) {
            case "Jan": month = Calendar.JANUARY;
                break;
            case "Feb": month = Calendar.FEBRUARY;
                break;
            case "Mar": month = Calendar.MARCH;
                break;
            case "Apr": month = Calendar.APRIL;
                break;
            case "May": month = Calendar.MAY;
                break;
            case "Jun": month = Calendar.JUNE;
                break;
            case "Jul": month = Calendar.JULY;
                break;
            case "Aug": month = Calendar.AUGUST;
                break;
            case "Sep": month = Calendar.SEPTEMBER;
                break;
            case "Oct": month = Calendar.OCTOBER;
                break;
            case "Nov": month = Calendar.NOVEMBER;
                break;
            case "Dec": month = Calendar.DECEMBER;
        }

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT-04:00"), US);//we are on EDT in these messages
        //noinspection MagicConstant
        calendar.set(Integer.parseInt(parts[5]), month, Integer.parseInt(parts[2]), Integer.parseInt(timeParts[0]),
                Integer.parseInt(timeParts[1]), Integer.parseInt(timeParts[2]));
        return calendar.getTime();
    }
    
    private void compareMessages(NihmsSubmissionMessage expected, NihmsSubmissionMessage actual) {
        assertTrue(expected.getOutcomeDescription() != null ? expected.getOutcomeDescription().equals(actual.getOutcomeDescription()):
                actual.getOutcomeDescription() == null);
        assertEquals(expected.isSubmitted(), actual.isSubmitted());
        assertEquals(expected.getSentDate().toString(), actual.getSentDate().toString());
        //5 seconds is close enough :-)
        assertTrue(actual.getLatestReadDate().getTime() - expected.getLatestReadDate().getTime() < 5000);
        assertTrue(expected.getMessageId() != null?  expected.getMessageId().equals(actual.getMessageId()): actual.getMessageId() == null);
        assertTrue(expected.getTaskId() != null ? expected.getTaskId().equals(actual.getTaskId()): actual.getTaskId() == null);
        assertTrue(expected.getNihmsId() != null ? expected.getNihmsId().equals(actual.getNihmsId()): actual.getNihmsId() == null);
    }
}

