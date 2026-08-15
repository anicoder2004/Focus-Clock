package com.anisoft.focusclock

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform