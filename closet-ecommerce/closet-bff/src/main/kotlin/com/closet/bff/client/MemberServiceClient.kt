package com.closet.bff.client

import com.closet.bff.dto.MemberResponse
import com.closet.bff.dto.ShippingAddressResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class MemberServiceClient(
    @Value("\${service.member.url}") private val baseUrl: String,
) {
    private val webClient: WebClient by lazy {
        WebClient.builder().baseUrl(baseUrl).build()
    }

    fun getMember(memberId: Long): Mono<MemberResponse> {
        return webClient.get()
            .uri("/members/{id}", memberId)
            .retrieve()
            .bodyToMono(MemberResponse::class.java)
    }

    fun getAddresses(memberId: Long): Mono<List<ShippingAddressResponse>> {
        return webClient.get()
            .uri("/members/{id}/addresses", memberId)
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<ShippingAddressResponse>>() {})
    }
}
