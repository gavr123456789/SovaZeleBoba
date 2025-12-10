package com.gavr123456789.github.greeklearncompose

class JVMPlatform {
    val name: String = "Java ${System.getProperty("java.version")}"
}

fun getPlatform() = JVMPlatform()