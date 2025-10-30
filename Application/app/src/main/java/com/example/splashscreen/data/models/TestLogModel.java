package com.example.splashscreen.data.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class TestLogModel {

    // 1. CORE HEALTH METRICS
    private double ph;
    private double chlorine;
    private double alkalinity;
    private double stabilizer;

    // 2. LOG METADATA
    private String poolId;
    @ServerTimestamp
    private Date timestamp;
    private double poolVolume;

    // 3. PH CALCULATOR DATA
    private double targetPh;
    private double phDosageAmount;
    private String phDosageUnit;
    private String phChemicalName;

    // 4. CHLORINE CALCULATOR DATA
    private double targetChlorine;
    private double chlorineDosageAmount;
    private String chlorineDosageUnit;
    private String chlorineChemicalName;

    // 💥 5. ALKALINITY CALCULATOR DATA (NEW FIELDS ADDED)
    private double targetAlkalinity;
    private double alkDosageAmount;
    private String alkDosageUnit;
    private String alkChemicalName;


    // ------------------------------------
    // --- CONSTRUCTORS (FOR FIREBASE & CODE) ---
    // ------------------------------------

    public TestLogModel() {
    }

    // Existing constructor for PH (retained for backward compatibility)
    public TestLogModel(String poolId, double currentPh, double targetPh, double poolVolume,
                        double dosageAmount, String dosageUnit, String chemicalName) {
        this();

        this.poolId = poolId;
        this.ph = currentPh;
        this.poolVolume = poolVolume;

        this.targetPh = targetPh;
        this.phDosageAmount = dosageAmount;
        this.phDosageUnit = dosageUnit;
        this.phChemicalName = chemicalName;
    }

    public TestLogModel(String poolId, double ph, double chlorine, double alkalinity, double stabilizer) {
        this();

        this.poolId = poolId;
        this.ph = ph;
        this.chlorine = chlorine;
        this.alkalinity = alkalinity;
        this.stabilizer = stabilizer;
    }


    // ------------------------------------
    // --- GETTERS AND SETTERS ---
    // ------------------------------------

    // 1. CORE HEALTH METRICS
    public double getPh() { return ph; }
    public void setPh(double ph) { this.ph = ph; }

    public double getChlorine() { return chlorine; }
    public void setChlorine(double chlorine) { this.chlorine = chlorine; }

    public double getAlkalinity() { return alkalinity; }
    public void setAlkalinity(double alkalinity) { this.alkalinity = alkalinity; }

    public double getStabilizer() { return stabilizer; }
    public void setStabilizer(double stabilizer) { this.stabilizer = stabilizer; }

    // 2. LOG METADATA
    public String getPoolId() { return poolId; }
    public void setPoolId(String poolId) { this.poolId = poolId; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public double getPoolVolume() { return poolVolume; }
    public void setPoolVolume(double poolVolume) { this.poolVolume = poolVolume; }


    // 3. PH CALCULATOR DATA
    public double getTargetPh() { return targetPh; }
    public void setTargetPh(double targetPh) { this.targetPh = targetPh; }

    public double getPhDosageAmount() { return phDosageAmount; }
    public void setPhDosageAmount(double phDosageAmount) { this.phDosageAmount = phDosageAmount; }

    public String getPhDosageUnit() { return phDosageUnit; }
    public void setPhDosageUnit(String phDosageUnit) { this.phDosageUnit = phDosageUnit; }

    public String getPhChemicalName() { return phChemicalName; }
    public void setPhChemicalName(String phChemicalName) { this.phChemicalName = phChemicalName; }

    // 4. CHLORINE CALCULATOR DATA
    public double getTargetChlorine() { return targetChlorine; }
    public void setTargetChlorine(double targetChlorine) { this.targetChlorine = targetChlorine; }

    public double getChlorineDosageAmount() { return chlorineDosageAmount; }
    public void setChlorineDosageAmount(double chlorineDosageAmount) { this.chlorineDosageAmount = chlorineDosageAmount; }

    public String getChlorineDosageUnit() { return chlorineDosageUnit; }
    public void setChlorineDosageUnit(String chlorineDosageUnit) { this.chlorineDosageUnit = chlorineDosageUnit; }

    public String getChlorineChemicalName() { return chlorineChemicalName; }
    public void setChlorineChemicalName(String chlorineChemicalName) { this.chlorineChemicalName = chlorineChemicalName; }

    // 💥 5. ALKALINITY CALCULATOR DATA (NEW GETTERS/SETTERS)
    public double getTargetAlkalinity() { return targetAlkalinity; }
    public void setTargetAlkalinity(double targetAlkalinity) { this.targetAlkalinity = targetAlkalinity; }

    public double getAlkDosageAmount() { return alkDosageAmount; }
    public void setAlkDosageAmount(double alkDosageAmount) { this.alkDosageAmount = alkDosageAmount; }

    public String getAlkDosageUnit() { return alkDosageUnit; }
    public void setAlkDosageUnit(String alkDosageUnit) { this.alkDosageUnit = alkDosageUnit; }

    public String getAlkChemicalName() { return alkChemicalName; }
    public void setAlkChemicalName(String alkChemicalName) { this.alkChemicalName = alkChemicalName; }
}