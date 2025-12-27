package com.project.saintcyshospital;

public class Medicine {
    public final String id;
    public final String name;
    public final String genericName;
    public final String dosage;
    public final int priceCents;
    public final String imageUrl;

    public Medicine(String id, String name, String genericName, String dosage, int priceCents, String imageUrl) {
        this.id = id;
        this.name = name;
        this.genericName = genericName;
        this.dosage = dosage;
        this.priceCents = priceCents;
        this.imageUrl = imageUrl;
    }
}