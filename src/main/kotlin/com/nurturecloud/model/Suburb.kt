package com.nurturecloud.model

import java.math.BigDecimal

data class Suburb(
        val Pcode: Int,
        val Locality: String,
        val State: String,
        val Comments: String? = "",
        val Category: String? = "",
        val Longitude: BigDecimal? = null,
        val Latitude: BigDecimal? = null)