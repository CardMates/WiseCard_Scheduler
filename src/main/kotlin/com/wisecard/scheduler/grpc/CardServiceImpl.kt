package com.wisecard.scheduler.grpc

import Card
import CardServiceGrpc
import com.wisecard.scheduler.scheduler.util.logger
import io.grpc.ManagedChannelBuilder
import net.devh.boot.grpc.server.service.GrpcService

@GrpcService
class CardServiceImpl(
    private val grpcProperties: GrpcProperties
) : CardServiceGrpc.CardServiceImplBase() {

    fun sendCardBenefits(cardBenefitList: Card.CardBenefitList) {
        val channel = ManagedChannelBuilder
            .forAddress(grpcProperties.host, grpcProperties.port)
            .usePlaintext()
            .build()
        val stub = CardServiceGrpc.newBlockingStub(channel)

        try {
            stub.receiveCardBenefits(cardBenefitList)
            logger.info("(${grpcProperties.host}:${grpcProperties.port})로 카드 혜택 데이터 전송 완료")
        } catch (e: Exception) {
            logger.error("데이터 전송 중 오류 발생: ${e.message}")
        } finally {
            channel.shutdown()
        }
    }
}