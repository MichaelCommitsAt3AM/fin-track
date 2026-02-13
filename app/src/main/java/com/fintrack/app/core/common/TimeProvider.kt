package com.fintrack.app.core.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

interface TimeProvider {
    fun currentDate(): LocalDate
    fun currentDateTime(): LocalDateTime
    fun currentTimeMillis(): Long
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun currentDate(): LocalDate = LocalDate.now()
    override fun currentDateTime(): LocalDateTime = LocalDateTime.now()
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
