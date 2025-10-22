// PHLogModel.java
package com.example.splashscreen;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class pHLogModel {

    private String poolId;
    private double currentPh;
    private double targetPh;
    private double poolVolume;
    private double dosageAmount;
    private String dosageUnit;
    private String chemicalName;
    @ServerTimestamp
    private Date timestamp;

    public pHLogModel() {
        // Required empty public constructor for Firestore
    }

    public pHLogModel(String poolId, double currentPh, double targetPh, double poolVolume, double dosageAmount, String dosageUnit, String chemicalName) {
        this.poolId = poolId;
        this.currentPh = currentPh;
        this.targetPh = targetPh;
        this.poolVolume = poolVolume;
        this.dosageAmount = dosageAmount;
        this.dosageUnit = dosageUnit;
        this.chemicalName = chemicalName;
    }

    // Getters and Setters

    public String getPoolId() {
        return poolId;
    }

    public void setPoolId(String poolId) {
        this.poolId = poolId;
    }

    public double getCurrentPh() {
        return currentPh;
    }

    public void setCurrentPh(double currentPh) {
        this.currentPh = currentPh;
    }

    public double getTargetPh() {
        return targetPh;
    }

    public void setTargetPh(double targetPh) {
        this.targetPh = targetPh;
    }

    public double getPoolVolume() {
        return poolVolume;
    }

    public void setPoolVolume(double poolVolume) {
        this.poolVolume = poolVolume;
    }

    public double getDosageAmount() {
        return dosageAmount;
    }

    public void setDosageAmount(double dosageAmount) {
        this.dosageAmount = dosageAmount;
    }

    public String getDosageUnit() {
        return dosageUnit;
    }

    public void setDosageUnit(String dosageUnit) {
        this.dosageUnit = dosageUnit;
    }

    public String getChemicalName() {
        return chemicalName;
    }

    public void setChemicalName(String chemicalName) {
        this.chemicalName = chemicalName;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}