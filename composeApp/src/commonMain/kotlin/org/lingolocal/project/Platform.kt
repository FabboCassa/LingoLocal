package org.lingolocal.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform