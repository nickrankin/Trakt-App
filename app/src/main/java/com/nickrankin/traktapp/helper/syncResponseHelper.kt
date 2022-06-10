package com.nickrankin.traktapp.helper

import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Type

enum class Response { ADDED_OK, DELETED_OK, EXISTING,  NOT_FOUND, ERROR }

fun getSyncResponse(syncResponse: SyncResponse?, type: Type): Response {

    if(syncResponse == null) {
        return Response.ERROR
    }

    var response = Response.NOT_FOUND

    when(type) {
        Type.MOVIE -> {
            if((syncResponse.added?.movies ?: 0) > 0) {
                response = Response.ADDED_OK
            } else if((syncResponse.deleted?.movies ?: 0) > 0) {
                response = Response.DELETED_OK
            } else if((syncResponse.existing?.movies ?: 0) > 0) {
                response = Response.EXISTING
            }
        }
        Type.SHOW -> {
            if((syncResponse.added?.shows ?: 0) > 0) {
                response = Response.ADDED_OK
            } else if((syncResponse.deleted?.shows ?: 0) > 0) {
                response = Response.DELETED_OK
            } else if((syncResponse.existing?.shows ?: 0) > 0) {
                response = Response.EXISTING
            }
        }
        Type.EPISODE -> {
            if((syncResponse.added?.episodes ?: 0) > 0) {
                response = Response.ADDED_OK
            } else if((syncResponse.deleted?.episodes ?: 0) > 0) {
                response = Response.DELETED_OK
            } else if((syncResponse.existing?.episodes ?: 0) > 0) {
                response = Response.EXISTING
            }
        }
        Type.PERSON -> {
            if((syncResponse.added?.people ?: 0) > 0) {
                response = Response.ADDED_OK
            } else if((syncResponse.deleted?.people ?: 0) > 0) {
                response = Response.DELETED_OK
            } else if((syncResponse.existing?.people ?: 0) > 0) {
                response = Response.EXISTING
            }
        }
        else -> {}

    }

    return response
}