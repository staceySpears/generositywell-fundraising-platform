package com.kenzie.appserver.repositories.model;

import com.kenzie.appserver.service.model.User;
import com.kenzie.appserver.service.model.Volunteer;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDate;
import java.util.List;

/**
 * DynamoDB-mapped record for a fundraising event.
 *
 * <p>An event is a physical or virtual volunteer activity linked to a campaign.
 * {@code status} holds an {@link com.kenzie.appserver.service.model.EventStatus} name.
 * {@code organizer} is the User who created the event, serialised via {@link UserTypeConverter}.
 * {@code volunteers} is the list of RSVPs, serialised via {@link VolunteerTypeConverter}.
 */
@DynamoDbBean
public class FundraisingEventRecord {

    private String id;
    private String campaignId;
    private String name;
    private String description;
    private String location;
    private LocalDate eventDate;
    private LocalDate registrationDeadline;
    private Integer capacity;
    private User organizer;
    private List<Volunteer> volunteers;
    private String status; // EventStatus name()

    public FundraisingEventRecord() {}

    /** @return the event's unique ID (partition key) */
    @DynamoDbPartitionKey
    public String getId() { return id; }
    /** @param id the event ID */
    public void setId(String id) { this.id = id; }

    /** @return the ID of the campaign this event belongs to */
    public String getCampaignId() { return campaignId; }
    /** @param campaignId the campaign ID */
    public void setCampaignId(String campaignId) { this.campaignId = campaignId; }

    /** @return the event display name */
    public String getName() { return name; }
    /** @param name the event display name */
    public void setName(String name) { this.name = name; }

    /** @return the event description */
    public String getDescription() { return description; }
    /** @param description the event description */
    public void setDescription(String description) { this.description = description; }

    /** @return the event location (address or virtual URL) */
    public String getLocation() { return location; }
    /** @param location the event location */
    public void setLocation(String location) { this.location = location; }

    /** @return the date of the event */
    public LocalDate getEventDate() { return eventDate; }
    /** @param eventDate the event date */
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    /** @return the last date volunteers may RSVP */
    public LocalDate getRegistrationDeadline() { return registrationDeadline; }
    /** @param registrationDeadline the RSVP deadline */
    public void setRegistrationDeadline(LocalDate registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    /** @return the maximum number of CONFIRMED volunteers; {@code null} means unlimited */
    public Integer getCapacity() { return capacity; }
    /** @param capacity the volunteer capacity */
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    /** @return the event organizer; stored via {@link UserTypeConverter} */
    @DynamoDbConvertedBy(UserTypeConverter.class)
    public User getOrganizer() { return organizer; }
    /** @param organizer the event organizer */
    public void setOrganizer(User organizer) { this.organizer = organizer; }

    /** @return the list of RSVPs; stored via {@link VolunteerTypeConverter} */
    @DynamoDbConvertedBy(VolunteerTypeConverter.class)
    public List<Volunteer> getVolunteers() { return volunteers; }
    /** @param volunteers the list of RSVPs */
    public void setVolunteers(List<Volunteer> volunteers) { this.volunteers = volunteers; }

    /** @return the event status name (see {@link com.kenzie.appserver.service.model.EventStatus}) */
    public String getStatus() { return status; }
    /** @param status the event status name */
    public void setStatus(String status) { this.status = status; }
}
