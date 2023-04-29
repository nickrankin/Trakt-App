package com.nickrankin.traktapp.api.services.trakt.model;

import com.uwetrottmann.trakt5.entities.Airs;
import com.uwetrottmann.trakt5.entities.BaseEntity;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.enums.Status;

import org.threeten.bp.OffsetDateTime;

import java.util.List;

public class Show extends BaseEntity {

    public Integer year;
    public ShowIds ids;

    // extended info
    public OffsetDateTime first_aired;
    public Airs airs;
    public Integer runtime;
    public String certification;
    public String network;
    public String country;
    public String trailer;
    public String homepage;
    public Status status;
    public String language;
    public List<String> genres;
    public Integer aired_episodes;
}