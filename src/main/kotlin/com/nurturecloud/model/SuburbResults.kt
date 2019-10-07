package com.nurturecloud.model

import java.math.BigDecimal

data class SuburbResults(
        val suburb: String,
        val postCode: Int,
        val distance: BigDecimal)
