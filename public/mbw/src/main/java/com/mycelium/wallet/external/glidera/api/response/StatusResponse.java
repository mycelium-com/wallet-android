package com.mycelium.wallet.external.glidera.api.response;

public class StatusResponse extends GlideraResponse {
    private boolean userCanTransact;
    private boolean userCanBuy;
    private boolean userCanSell;
    private boolean userBankAccountIsSetup;
    private boolean userEmailIsSetup;
    private boolean userPhoneIsSetup;
    private boolean userSsnIsSetup;
    private boolean userOowIsSetup;
    private State personalInfoState;
    private State bankAccountState;
    private String country;
    private String[] nextSteps;
    private boolean tier1SetupComplete;
    private boolean tier2SetupComplete;
    private boolean tier2TransactionVolumeRequirementComplete;
    private boolean tier2AccountAgeRequirementComplete;
    private Boolean userAdditionalInfoRequired;
    private Boolean userAdditionalInfoIsSetup;
    private String userPictureIdState;

    public boolean isUserCanTransact() {
        return userCanTransact;
    }

    public void setUserCanTransact(boolean userCanTransact) {
        this.userCanTransact = userCanTransact;
    }

    public boolean isUserCanBuy() {
        return userCanBuy;
    }

    public void setUserCanBuy(boolean userCanBuy) {
        this.userCanBuy = userCanBuy;
    }

    public boolean isUserCanSell() {
        return userCanSell;
    }

    public void setUserCanSell(boolean userCanSell) {
        this.userCanSell = userCanSell;
    }

    public boolean isUserBankAccountIsSetup() {
        return userBankAccountIsSetup;
    }

    public void setUserBankAccountIsSetup(boolean userBankAccountIsSetup) {
        this.userBankAccountIsSetup = userBankAccountIsSetup;
    }

    public boolean isUserEmailIsSetup() {
        return userEmailIsSetup;
    }

    public void setUserEmailIsSetup(boolean userEmailIsSetup) {
        this.userEmailIsSetup = userEmailIsSetup;
    }

    public boolean isUserPhoneIsSetup() {
        return userPhoneIsSetup;
    }

    public void setUserPhoneIsSetup(boolean userPhoneIsSetup) {
        this.userPhoneIsSetup = userPhoneIsSetup;
    }

    public boolean isUserSsnIsSetup() {
        return userSsnIsSetup;
    }

    public void setUserSsnIsSetup(boolean userSsnIsSetup) {
        this.userSsnIsSetup = userSsnIsSetup;
    }

    public boolean isUserOowIsSetup() {
        return userOowIsSetup;
    }

    public void setUserOowIsSetup(boolean userOowIsSetup) {
        this.userOowIsSetup = userOowIsSetup;
    }

    public State getPersonalInfoState() {
        return personalInfoState;
    }

    public void setPersonalInfoState(State personalInfoState) {
        this.personalInfoState = personalInfoState;
    }

    public State getBankAccountState() {
        return bankAccountState;
    }

    public void setBankAccountState(State bankAccountState) {
        this.bankAccountState = bankAccountState;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String[] getNextSteps() {
        return nextSteps;
    }

    public void setNextSteps(String[] nextSteps) {
        this.nextSteps = nextSteps;
    }

    public boolean isTier1SetupComplete() {
        return tier1SetupComplete;
    }

    public void setTier1SetupComplete(boolean tier1SetupComplete) {
        this.tier1SetupComplete = tier1SetupComplete;
    }

    public boolean isTier2SetupComplete() {
        return tier2SetupComplete;
    }

    public void setTier2SetupComplete(boolean tier2SetupComplete) {
        this.tier2SetupComplete = tier2SetupComplete;
    }

    public boolean isTier2TransactionVolumeRequirementComplete() {
        return tier2TransactionVolumeRequirementComplete;
    }

    public void setTier2TransactionVolumeRequirementComplete(boolean tier2TransactionVolumeRequirementComplete) {
        this.tier2TransactionVolumeRequirementComplete = tier2TransactionVolumeRequirementComplete;
    }

    public boolean isTier2AccountAgeRequirementComplete() {
        return tier2AccountAgeRequirementComplete;
    }

    public void setTier2AccountAgeRequirementComplete(boolean tier2AccountAgeRequirementComplete) {
        this.tier2AccountAgeRequirementComplete = tier2AccountAgeRequirementComplete;
    }

    public Boolean getUserAdditionalInfoRequired() {
        return userAdditionalInfoRequired;
    }

    public void setUserAdditionalInfoRequired(Boolean userAdditionalInfoRequired) {
        this.userAdditionalInfoRequired = userAdditionalInfoRequired;
    }

    public Boolean getUserAdditionalInfoIsSetup() {
        return userAdditionalInfoIsSetup;
    }

    public void setUserAdditionalInfoIsSetup(Boolean userAdditionalInfoIsSetup) {
        this.userAdditionalInfoIsSetup = userAdditionalInfoIsSetup;
    }

    public String getUserPictureIdState() {
        return userPictureIdState;
    }

    public void setUserPictureIdState(String userPictureIdState) {
        this.userPictureIdState = userPictureIdState;
    }
}
