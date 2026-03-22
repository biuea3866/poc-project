package com.closet.bff.facade

import com.closet.bff.client.MemberServiceClient
import com.closet.bff.client.OrderServiceClient
import com.closet.bff.dto.MyPageBffResponse
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class MyPageBffFacade(
    private val memberClient: MemberServiceClient,
    private val orderClient: OrderServiceClient,
) {
    fun getMyPage(memberId: Long): MyPageBffResponse {
        val memberMono = memberClient.getMember(memberId)
        val ordersMono = orderClient.getOrders(memberId, 0, 5)
        val addressesMono = memberClient.getAddresses(memberId)

        val result = Mono.zip(memberMono, ordersMono, addressesMono).block()!!

        return MyPageBffResponse(
            member = result.t1,
            recentOrders = result.t2.content,
            addresses = result.t3,
        )
    }
}
