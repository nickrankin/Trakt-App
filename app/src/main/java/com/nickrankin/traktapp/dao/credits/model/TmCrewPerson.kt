package com.nickrankin.traktapp.dao.credits.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.trakt5.enums.Type

enum class CrewType {DIRECTING, PRODUCING, WRITING}
@Entity(tableName = "crew_people")
data class TmCrewPerson(@PrimaryKey override val id: String, override val person_trakt_id: Int, override val trakt_id: Int, override val tmdb_id: Int?, override val title: String?,
                        override  val year: Int?, override val ordering: Int, val name: String?, override val type: Type, val job: String, val crewType: CrewType): CreditPerson