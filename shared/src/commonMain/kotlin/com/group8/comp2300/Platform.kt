package com.group8.comp2300

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform