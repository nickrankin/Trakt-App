package com.nickrankin.traktapp.api.services.trakt.model;

import com.uwetrottmann.trakt5.entities.PersonIds;

import org.threeten.bp.LocalDate;

public class Person {
    public String name;
    public PersonIds ids;

    // extended info
    public String biography;
    public LocalDate birthday;
    public LocalDate death;
    public String birthplace;
    public String homepage;
    public Boolean series_regular;

}
