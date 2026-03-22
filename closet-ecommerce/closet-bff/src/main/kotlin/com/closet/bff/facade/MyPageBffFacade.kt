package com.closet.bff.facade

import com.closet.bff.client.MemberServiceClient
import com.closet.bff.client.OrderServiceClient
import com.closet.bff.dto.MyPageBffResponse
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class MyPageBffFacade(
    private val memberClient: MemberServiceClient,
    private val orderClient: OrderServiceClient,
) {
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    fun getMyPage(memberId: Long): MyPageBffResponse {
        val memberFuture = CompletableFuture.supplyAsync(
            { memberClient.getMember(memberId) },
            executor,
        )
        val ordersFuture = CompletableFuture.supplyAsync(
            { orderClient.getOrders(memberId, 0, 5) },
            executor,
        )
        val addressesFuture = CompletableFuture.supplyAsync(
            { memberClient.getAddresses(memberId) },
            executor,
        )

        CompletableFuture.allOf(memberFuture, ordersFuture, addressesFuture).join()

        return MyPageBffResponse(
            member = memberFuture.get().data!!,
            recentOrders = ordersFuture.get().data?.content ?: emptyList(),
            addresses = addressesFuture.get().data ?: emptyList(),
        )
    }
}
