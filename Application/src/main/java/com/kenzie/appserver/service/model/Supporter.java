package com.kenzie.appserver.service.model;

/**
 * Represents an individual who has donated to or supported a fundraising campaign.
 * {@code amountInCents} and {@code donationDate} are populated when a donation is
 * recorded via the authenticated direct-donate path; they are {@code null} for legacy
 * supporter entries created before donation tracking was added.
 */
public class Supporter {

    public String id;
    public String name;
    public String email;
    /** Cumulative donation total for this donor on this campaign, in cents. */
    public Long amountInCents;
    /** ISO date (yyyy-MM-dd) of the most recent donation recorded for this donor. */
    public String donationDate;

    public Supporter(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public Supporter() {}

    /** @return the supporter's unique ID */
    public String getId() {
        return id;
    }

    /** @param id the supporter's unique ID */
    public void setId(String id) {
        this.id = id;
    }

    /** @return the supporter's display name */
    public String getName() {
        return name;
    }

    /** @param name the supporter's display name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the supporter's email address */
    public String getEmail() {
        return email;
    }

    /** @param email the supporter's email address */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return the donor's cumulative total on this campaign in cents, or {@code null} if not tracked */
    public Long getAmountInCents() {
        return amountInCents;
    }

    /** @param amountInCents cumulative donation total in cents */
    public void setAmountInCents(Long amountInCents) {
        this.amountInCents = amountInCents;
    }

    /** @return ISO date of most recent donation, or {@code null} if not tracked */
    public String getDonationDate() {
        return donationDate;
    }

    /** @param donationDate ISO date string (yyyy-MM-dd) */
    public void setDonationDate(String donationDate) {
        this.donationDate = donationDate;
    }
}
