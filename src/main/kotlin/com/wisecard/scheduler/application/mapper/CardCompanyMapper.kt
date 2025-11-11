package com.wisecard.scheduler.application.mapper

import com.sub.grpc.CardCompanyOuterClass
import com.wisecard.scheduler.domain.card.CardCompany

object CardCompanyMapper {
    fun toProto(company: CardCompany): CardCompanyOuterClass.CardCompany = when (company) {
        CardCompany.HANA -> CardCompanyOuterClass.CardCompany.HANA
        CardCompany.SHINHAN -> CardCompanyOuterClass.CardCompany.SHINHAN
        CardCompany.HYUNDAI -> CardCompanyOuterClass.CardCompany.HYUNDAI
        CardCompany.KOOKMIN -> CardCompanyOuterClass.CardCompany.KOOKMIN
        CardCompany.LOTTE -> CardCompanyOuterClass.CardCompany.LOTTE
        CardCompany.SAMSUNG -> CardCompanyOuterClass.CardCompany.SAMSUNG
    }
}