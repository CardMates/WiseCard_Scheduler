package com.wisecard.scheduler.grpc

import Card
import CardServiceGrpc
import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
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

    override fun getCardBenefits(
        request: Empty,
        responseObserver: StreamObserver<Card.CardBenefitList>
    ) {
        responseObserver.onNext(cardBenefitList)
        responseObserver.onCompleted()
    }
}