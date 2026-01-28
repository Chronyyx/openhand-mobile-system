package com.mana.openhand_backend.identity.presentationlayer.payload;

public record ProfileRequest(String name, String phoneNumber, String preferredLanguage, String gender, Integer age) {
}
