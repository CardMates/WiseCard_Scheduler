package com.wisecard.scheduler.application.ports.out

interface BenefitsRefiner {
    fun refine(raw: String): String
}