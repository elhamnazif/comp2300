package com.group8.comp2300.util

fun hashPin(pin: String): String = sha256("vita-pin-salt$pin")
