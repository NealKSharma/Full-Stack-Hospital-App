package com.project.saintcyshospital;

import java.io.Serializable;

public class Patient implements Serializable {
    public final long id;
    public final String name;
    public final String mrn;
    public final String dob;
    public final String gender;

    public Patient(long id, String name, String mrn, String dob, String gender) {
        this.id = id;
        this.name = name;
        this.mrn = mrn;
        this.dob = dob;
        this.gender = gender;
    }

    private static String clean(String s) {
        if (s == null) return "—";
        String t = s.trim();
        if (t.isEmpty() || t.equalsIgnoreCase("null")) return "—";
        return t;
    }

    @Override
    public String toString() {
        String left = clean(name);
        String right = clean(mrn);
        return left + " — " + right;
    }
}
