package com.itxiop.tech.supplier.sustainability;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonAutoDetect(getterVisibility = Visibility.ANY, isGetterVisibility = Visibility.ANY)
public class SustainabilityRating {

    private int duns;

    private String score;

    SustainabilityRating() {
    }

    int getDuns() {
        return duns;
    }

    void setDuns(int duns) {
        this.duns = duns;
    }

    String getScore() {
        return score;
    }

    void setScore(String score) {
        this.score = score;
    }

}
