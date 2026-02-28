package com.itxiop.tech.supplier.country;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonAutoDetect(getterVisibility = Visibility.ANY, isGetterVisibility = Visibility.ANY)
class Country {

    private String name;

    private boolean isBanned;

    Country(String name, boolean isBanned) {
        this.name = name;
        this.isBanned = isBanned;
    }

    String getName() {
        return name;
    }

    boolean getIsBanned() {
        return isBanned;
    }

}
