package com.nickrankin.traktapp.api.services.trakt.model;

import com.uwetrottmann.trakt5.entities.Crew;

import java.util.List;

public class Credits {

    public List<CastMember> cast;
    public List<CastMember> guest_stars;

    public Crew crew;
}
