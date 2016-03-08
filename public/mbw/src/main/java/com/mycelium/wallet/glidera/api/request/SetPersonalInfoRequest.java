package com.mycelium.wallet.glidera.api.request;

import android.support.annotation.NonNull;

public class SetPersonalInfoRequest {
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final String birthDate;
    private final String address1;
    private final String address2;
    private final String city;
    private final String state;
    private final String zipCode;
    private final String ip;
    private final String occupation;
    private final String employerName;
    private final String employerDescription;
    private final String last4Ssn;

    /**
     * @param firstName           User's first name. Cannot be updated in Canada
     * @param middleName          User's middle name. Cannot be updated in Canada
     * @param lastName            User's last name. Cannot be updated in Canada
     * @param birthDate           User's date of birth (yyyy-mm-dd).
     * @param address1            User's Address
     * @param address2
     * @param city                City name
     * @param state               Two character state or province code (ex. WI)
     * @param zipCode             Zip or Postal Code. 5 digits in US (ex. 60126). 7 characters in Canada with a space after 3rd character
     *                            (ex. L3R 9Z4)
     * @param ip                  Required from web wallet partners
     * @param occupation          Required in Canada. User's occupation code from list of occupation codes
     * @param employerName        Mandatory in Canada if occupation is 'Other'
     * @param employerDescription Mandatory in Canada if occupation is 'Other'
     * @param last4Ssn            Required in United States. Last 4 digits of user's SSN
     */
    public SetPersonalInfoRequest(@NonNull String firstName, String middleName, @NonNull String
            lastName, @NonNull String birthDate, @NonNull String address1, String address2,
                                  @NonNull String city, @NonNull String state, @NonNull String
                                          zipCode, String ip, String occupation, String
                                          employerName, String employerDescription, String
                                          last4Ssn) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.address1 = address1;
        this.address2 = address2;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.ip = ip;
        this.occupation = occupation;
        this.employerName = employerName;
        this.employerDescription = employerDescription;
        this.last4Ssn = last4Ssn;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getIp() {
        return ip;
    }

    public String getOccupation() {
        return occupation;
    }

    public String getEmployerName() {
        return employerName;
    }

    public String getEmployerDescription() {
        return employerDescription;
    }

    public String getLast4Ssn() {
        return last4Ssn;
    }
}
