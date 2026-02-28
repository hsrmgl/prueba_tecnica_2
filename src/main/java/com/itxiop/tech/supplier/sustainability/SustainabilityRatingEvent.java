package com.itxiop.tech.supplier.sustainability;

import org.springframework.context.ApplicationEvent;

public class SustainabilityRatingEvent extends ApplicationEvent {

    private final int duns;

    private final String score;

    public SustainabilityRatingEvent(Object source, int duns, String score) {
        super(source);
        this.duns = duns;
        this.score = score;
    }

    public int getDuns() {
        return duns;
    }

    public String getScore() {
        return score;
    }

}
