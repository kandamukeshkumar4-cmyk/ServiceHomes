package com.servicehomes.api.search.application.dto;

public record SearchSuggestionResponse(
    String text,
    String type,
    String subtitle
) {
    public static SearchSuggestionResponse ofLocation(String city, String country) {
        return new SearchSuggestionResponse(city, "location", city + ", " + country);
    }

    public static SearchSuggestionResponse ofTitle(String title, String location) {
        return new SearchSuggestionResponse(title, "listing", location);
    }
}
