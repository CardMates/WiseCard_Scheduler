package com.wisecard.scheduler.grpc

import Promotion
import PromotionServiceGrpc
import com.wisecard.scheduler.scheduler.util.logger
import io.grpc.ManagedChannelBuilder
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.stereotype.Service

@Service
class PromotionServiceImpl(
    private val grpcProperties: GrpcProperties
) {
    fun sendPromotions(promotionList: Promotion.CardPromotionList) {
        val channel = ManagedChannelBuilder
            .forAddress(grpcProperties.host, grpcProperties.port)
            .usePlaintext()
            .build()
        val stub = PromotionServiceGrpc.newBlockingStub(channel)

        try {
            stub.receivePromotions(promotionList)
            logger.info("(${grpcProperties.host}:${grpcProperties.port})로 카드 프로모션 데이터 전송 완료")
        } catch (e: Exception) {
            logger.error("데이터 전송 중 오류 발생: ${e.message}")
        } finally {
            channel.shutdown()
        }
    }
}