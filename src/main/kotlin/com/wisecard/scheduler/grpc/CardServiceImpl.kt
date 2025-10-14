package com.wisecard.scheduler.grpc

import Card
import com.wisecard.scheduler.scheduler.util.logger
import io.grpc.ManagedChannelBuilder
import org.springframework.stereotype.Service

@Service
class CardServiceImpl(
    private val grpcProperties: GrpcProperties
) {
    fun sendCardBenefits(cardBenefitList: Card.CardBenefitList) {
        val channel = ManagedChannelBuilder
            .forAddress(grpcProperties.host, grpcProperties.port)
            .usePlaintext()
            .build()
        val stub = CardDataServiceGrpc.newBlockingStub(channel)

        try {
            stub.saveCardData(cardBenefitList)
            logger.info("(${grpcProperties.host}:${grpcProperties.port})로 카드 혜택 데이터 전송 완료")
        } catch (e: Exception) {
            logger.error("데이터 전송 중 오류 발생: ${e.message}")
        } finally {
            channel.shutdown()
        }
    }
}