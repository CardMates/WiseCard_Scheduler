package com.wisecard.scheduler.application.mapper

import com.sub.grpc.CardCompanyOuterClass
import com.wisecard.scheduler.domain.card.CardCompany
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CardCompanyMapperTest {

    @Test
    fun `모든 CardCompany enum을 Proto로 정확히 매핑`() {
        assertThat(CardCompanyMapper.toProto(CardCompany.HANA))
            .isEqualTo(CardCompanyOuterClass.CardCompany.HANA)

        assertThat(CardCompanyMapper.toProto(CardCompany.SHINHAN))
            .isEqualTo(CardCompanyOuterClass.CardCompany.SHINHAN)

        assertThat(CardCompanyMapper.toProto(CardCompany.HYUNDAI))
            .isEqualTo(CardCompanyOuterClass.CardCompany.HYUNDAI)

        assertThat(CardCompanyMapper.toProto(CardCompany.KOOKMIN))
            .isEqualTo(CardCompanyOuterClass.CardCompany.KOOKMIN)

        assertThat(CardCompanyMapper.toProto(CardCompany.LOTTE))
            .isEqualTo(CardCompanyOuterClass.CardCompany.LOTTE)

        assertThat(CardCompanyMapper.toProto(CardCompany.SAMSUNG))
            .isEqualTo(CardCompanyOuterClass.CardCompany.SAMSUNG)
    }
}