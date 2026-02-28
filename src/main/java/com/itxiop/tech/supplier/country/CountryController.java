package com.itxiop.tech.supplier.country;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CountryController {

    private static final String KNOWN = "abcdefghijklmnopqrstuvwxyz";
    private static final String BANNED = "nopqrstuvwxyz";

    Logger logger = LoggerFactory.getLogger(CountryController.class);

    @GetMapping("/countries/{country}")
    ResponseEntity<Country> getCountry(@PathVariable String country) {
        logger.info("hit /countries/{}", country);

        ResponseEntity<Country> responseEntity;

        if (!isKnownCountry(country)) {
            responseEntity = ResponseEntity.notFound().build();
        } else {
            Country info = new Country(country, isBannedCountry(country));
            responseEntity = ResponseEntity.ok().body(info);
        }

        return responseEntity;
    }

    private static String countryCode(String country) {
        return country.toLowerCase().substring(0, 1);
    }

    private boolean isKnownCountry(String country) {
        return null != country && 2 == country.length() && KNOWN.contains(countryCode(country));
    }

    private boolean isBannedCountry(String country) {
        return BANNED.contains(countryCode(country));
    }

}
