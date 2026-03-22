package com.closet.bff.client

import com.closet.bff.dto.AddAddressRequest
import com.closet.bff.dto.LoginRequest
import com.closet.bff.dto.LoginResponse
import com.closet.bff.dto.MemberResponse
import com.closet.bff.dto.RegisterRequest
import com.closet.bff.dto.ShippingAddressResponse
import com.closet.bff.dto.UpdateAddressRequest
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

    fun register(request: RegisterRequest): Mono<MemberResponse> {
        return webClient.post()
            .uri("/members/register")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(MemberResponse::class.java)
    }

    fun login(request: LoginRequest): Mono<LoginResponse> {
        return webClient.post()
            .uri("/members/login")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(LoginResponse::class.java)
    }

    fun addAddress(memberId: Long, request: AddAddressRequest): Mono<ShippingAddressResponse> {
        return webClient.post()
            .uri("/members/{id}/addresses", memberId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ShippingAddressResponse::class.java)
    }

    fun updateAddress(memberId: Long, addressId: Long, request: UpdateAddressRequest): Mono<ShippingAddressResponse> {
        return webClient.put()
            .uri("/members/{memberId}/addresses/{addressId}", memberId, addressId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ShippingAddressResponse::class.java)
    }

    fun deleteAddress(memberId: Long, addressId: Long): Mono<Void> {
        return webClient.delete()
            .uri("/members/{memberId}/addresses/{addressId}", memberId, addressId)
            .retrieve()
            .bodyToMono(Void::class.java)
    }

    fun setDefaultAddress(memberId: Long, addressId: Long): Mono<ShippingAddressResponse> {
        return webClient.patch()
            .uri("/members/{memberId}/addresses/{addressId}/default", memberId, addressId)
            .retrieve()
            .bodyToMono(ShippingAddressResponse::class.java)
    }
}
