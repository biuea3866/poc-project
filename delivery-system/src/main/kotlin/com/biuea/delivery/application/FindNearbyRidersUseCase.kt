package com.biuea.delivery.application

import com.biuea.delivery.domain.rider.RiderSearchDomainService
import org.springframework.stereotype.Service

/**
 * 가게 반경 안의 배차 후보 라이더를 찾는다.
 *
 * 인덱스 조회·신선도 판정·반경 확장은 모두 도메인 정책이라 도메인 서비스가 소유한다.
 * 여기서는 커맨드를 도메인 입력으로 넘기고 응답으로 변환하는 일만 한다.
 * 조회 전용이고 Redis 인덱스만 읽으므로 트랜잭션 경계를 두지 않는다.
 */
@Service
class FindNearbyRidersUseCase(
    private val riderSearchDomainService: RiderSearchDomainService,
) {
    fun execute(command: FindNearbyRidersCommand): FindNearbyRidersResponse =
        FindNearbyRidersResponse.of(
            riderSearchDomainService.searchNearby(command.storeCoordinate, command.vehicleType, command.limit),
        )
}
