package com.group8.comp2300.data.remote

class ApiException(val statusCode: Int, override val message: String, cause: Throwable? = null) :
    Exception(message, cause)
