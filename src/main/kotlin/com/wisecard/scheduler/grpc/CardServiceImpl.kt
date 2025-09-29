package com.wisecard.scheduler.grpc

import Card
import CardServiceGrpc
import io.grpc.ManagedChannelBuilder
import net.devh.boot.grpc.server.service.GrpcService
import java.io.File

@GrpcService
class CardServiceImpl : CardServiceGrpc.CardServiceImplBase() {

    private val cardBenefitList: Card.CardBenefitList by lazy {
        val file = File("proto/card_benefits.pb")
        if (!file.exists()) {
            throw IllegalStateException("card_benefits.pb 파일이 현재 디렉토리에 없습니다! (${file.absolutePath})")
        }
        Card.CardBenefitList.parseFrom(file.inputStream())
    }

    fun sendCardBenefits(clientAddress: String = "localhost", clientPort: Int = 50052) {
        val channel = ManagedChannelBuilder
            .forAddress(clientAddress, clientPort)
            .usePlaintext() // 실제 환경에서는 TLS 사용 권장
            .build()
        val stub = CardServiceGrpc.newBlockingStub(channel)

        try {
            stub.receiveCardBenefits(cardBenefitList)
            println("클라이언트($clientAddress:$clientPort)로 카드 혜택 데이터 전송 완료")
        } catch (e: Exception) {
            println("클라이언트로 데이터 전송 중 오류 발생: ${e.message}")
        } finally {
            channel.shutdown()
        }
    }
}