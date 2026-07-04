package com.biuea.chat.domain.routing

/**
 * 그룹 채팅의 서버 단위 팬아웃 계산. 수신자를 접속 서버별로 묶어,
 * 서버당 한 번만 전달하도록 한다. 위치를 못 찾은(오프라인) 수신자는 제외한다.
 */
object RoomFanout {
    fun groupByServer(
        recipientIds: Collection<Long>,
        sessionLocator: SessionLocator,
    ): Map<String, List<Long>> =
        recipientIds
            .mapNotNull { userId -> sessionLocator.locate(userId)?.let { server -> server to userId } }
            .groupBy({ it.first }, { it.second })
}
