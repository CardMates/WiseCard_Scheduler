package com.wisecard.scheduler.grpc

import Card
import CardServiceGrpc
import io.grpc.ManagedChannelBuilder
import net.devh.boot.grpc.server.service.GrpcService

@GrpcService
class CardServiceImpl : CardServiceGrpc.CardServiceImplBase() {

    fun sendCardBenefits(cardBenefitList: Card.CardBenefitList, address: String = "localhost", port: Int = 50052) {
        val channel = ManagedChannelBuilder
            .forAddress(address, port)
            .usePlaintext()
            .build()
        val stub = CardServiceGrpc.newBlockingStub(channel)

        try {
            stub.receiveCardBenefits(cardBenefitList)
            println("($address:$port)로 카드 혜택 데이터 전송 완료")
        } catch (e: Exception) {
            println("데이터 전송 중 오류 발생: ${e.message}")
        } finally {
            channel.shutdown()
        }
    }
}