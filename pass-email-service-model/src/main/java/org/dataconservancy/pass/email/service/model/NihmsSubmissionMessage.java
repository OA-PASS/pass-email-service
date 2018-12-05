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

package org.dataconservancy.pass.email.service.model;

import java.util.Date;

/**
 * A class to manage data pulled from an email message to build a JMS message
 */
public class NihmsSubmissionMessage {

    private String outcomeDescription;
    private boolean submitted;
    private Date sentDate;
    private Date latestReadDate;
    private String messageId;

    private String taskId;
    private String nihmsId;

    public String getOutcomeDescription() {
        return outcomeDescription;
    }

    public void setOutcomeDescription(String outcomeDescription) {
        this.outcomeDescription = outcomeDescription;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public Date getSentDate() {
        return sentDate;
    }

    public void setSentDate(Date sentDate) {
        this.sentDate = sentDate;
    }

    public Date getLatestReadDate() {
        return latestReadDate;
    }

    public void setLatestReadDate(Date latestReadDate) {
        this.latestReadDate = latestReadDate;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getNihmsId() {
        return nihmsId;
    }

    public void setNihmsId(String nihmsId) {
        this.nihmsId = nihmsId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NihmsSubmissionMessage that = (NihmsSubmissionMessage) o;

        if (outcomeDescription != null ? !outcomeDescription.equals(that.outcomeDescription) : that.outcomeDescription != null) return false;
        if (submitted != that.submitted) return false;
        if (sentDate != null ? !sentDate.equals(that.sentDate) : that.sentDate != null) return false;
        if (latestReadDate != null ? !latestReadDate.equals(that.latestReadDate) : that.latestReadDate != null) return false;
        if (messageId != null ? !messageId.equals(that.messageId) : that.messageId != null) return false;
        if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) return false;
        if (nihmsId != null ? nihmsId.equals(that.nihmsId) : that.nihmsId == null) return true;
        return false;
    }


    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + (outcomeDescription != null ? outcomeDescription.hashCode() : 0);
        result = 31 * result + (submitted ? 0: 1);
        result = 31 * result + (sentDate != null ? sentDate.hashCode() : 0);
        result = 31 * result + (latestReadDate != null ? latestReadDate.hashCode() : 0);
        result = 31 * result + (messageId != null ? messageId.hashCode() : 0);
        result = 31 * result + (taskId != null ? taskId.hashCode() : 0);
        result = 31 * result + (nihmsId != null ? nihmsId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {

        return "outcomeDescription: " +
                outcomeDescription +
                "\nsubmitted: " +
                submitted +
                "\nsentDate: " +
                sentDate.toString() +
                "\nlatestReadDate: " +
                latestReadDate.toString() +
                "\nmessageId: " +
                messageId +
                "\ntaskId: " +
                taskId +
                "\nnihmsId: " +
                nihmsId;
    }

}
