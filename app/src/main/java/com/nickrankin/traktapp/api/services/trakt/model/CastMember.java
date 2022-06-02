package com.nickrankin.traktapp.api.services.trakt.model;

import com.uwetrottmann.trakt5.entities.Movie;
import com.uwetrottmann.trakt5.entities.Show;

public class CastMember {
    public String character;
    public Movie movie;
    public Show show;
    public Person person;
}
