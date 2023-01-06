package com.hbrc.jira_watcher.doto

import com.hbrc.jira_watcher.entity.Message

interface MessageDto {
    data class Insert(
        val type: String,
        val json: String
    ) {
        fun toEntity(): Message = Message(
            type = type,
            json = json
        )
    }
}