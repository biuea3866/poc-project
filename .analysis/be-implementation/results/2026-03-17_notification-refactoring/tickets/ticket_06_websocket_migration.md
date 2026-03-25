# [GRT-4006] WebSocket Gateway (netty-socketio) 마이그레이션

## 개요
- PRD: https://doodlin.atlassian.net/wiki/x/SICjdg
- Phase: 1 (서비스 구축)
- 예상 공수: 5d
- 의존성: GRT-4001

**범위:** Node.js alert-server의 Socket.io WebSocket 기능 → Spring Boot + netty-socketio 이관. FE 코드 변경 0, Socket.io v4 프로토콜 호환성 보장

## 작업 내용

### 1. netty-socketio 서버 설정

```kotlin
@Configuration
class SocketIoConfig(
    @Value("\${socketio.host}") private val host: String,
    @Value("\${socketio.port}") private val port: Int
) {
    @Bean
    fun socketIoServer(): SocketIOServer {
        val config = com.corundumstudio.socketio.Configuration().apply {
            hostname = host
            this.port = port
            setOrigin("*")
            pingTimeout = 60000
            pingInterval = 25000
            // Socket.io v4 호환
            setTransports(Transport.WEBSOCKET, Transport.POLLING)
        }
        return SocketIOServer(config)
    }

    @Bean
    fun socketIoServerLifecycle(server: SocketIOServer): SmartLifecycle {
        return object : SmartLifecycle {
            private var running = false
            override fun start() { server.start(); running = true }
            override fun stop() { server.stop(); running = false }
            override fun isRunning() = running
        }
    }
}
```

### 2. JWT 인증

```kotlin
@Component
class SocketIoAuthHandler(
    private val jwtProvider: JwtProvider
) : AuthorizationListener {

    override fun isAuthorized(data: HandshakeData): Boolean {
        val token = data.getSingleUrlParam("token")
            ?: data.httpHeaders.get("Authorization")?.removePrefix("Bearer ")
            ?: return false

        return try {
            val claims = jwtProvider.validateAndParse(token)
            // userId, workspaceId를 handshake 데이터에 저장
            data.urlParams["userId"] = listOf(claims.userId.toString())
            data.urlParams["workspaceId"] = listOf(claims.workspaceId.toString())
            true
        } catch (e: Exception) {
            log.warn("WebSocket auth failed: ${e.message}")
            false
        }
    }
}

@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val secret: String
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))
    }

    fun validateAndParse(token: String): JwtClaims {
        val claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).body
        return JwtClaims(
            userId = claims["userId"] as Long,
            workspaceId = claims["workspaceId"] as Long
        )
    }
}
```

### 3. Redis Pub/Sub 멀티인스턴스 동기화

```kotlin
@Configuration
class RedisSocketIoConfig(
    private val redisTemplate: StringRedisTemplate
) {
    companion object {
        const val CHANNEL_NOTIFICATION = "socketio:notification"
    }

    @Bean
    fun redisMessageListenerContainer(
        connectionFactory: RedisConnectionFactory,
        notificationSubscriber: RedisNotificationSubscriber
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        container.addMessageListener(notificationSubscriber, ChannelTopic(CHANNEL_NOTIFICATION))
        return container
    }
}

@Component
class RedisNotificationPublisher(
    private val redisTemplate: StringRedisTemplate
) {
    fun publish(message: SocketNotificationMessage) {
        redisTemplate.convertAndSend(
            RedisSocketIoConfig.CHANNEL_NOTIFICATION,
            objectMapper.writeValueAsString(message)
        )
    }
}

@Component
class RedisNotificationSubscriber(
    private val socketIoServer: SocketIOServer
) : MessageListener {

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val notification = objectMapper.readValue(message.body, SocketNotificationMessage::class.java)
        val room = "workspace:${notification.workspaceId}:user:${notification.userId}"
        socketIoServer.getRoomOperations(room).sendEvent("alert.added", notification.payload)
    }
}
```

### 4. joinAlertChannel / leaveAlertChannel 이벤트

```kotlin
@Component
class SocketIoEventHandler(
    private val socketIoServer: SocketIOServer
) {
    @PostConstruct
    fun init() {
        socketIoServer.addConnectListener { client ->
            val userId = client.handshakeData.getSingleUrlParam("userId")?.toLong() ?: return@addConnectListener
            val workspaceId = client.handshakeData.getSingleUrlParam("workspaceId")?.toLong() ?: return@addConnectListener
            val room = "workspace:$workspaceId:user:$userId"
            client.joinRoom(room)
            log.info("Client connected: sessionId=${client.sessionId}, room=$room")
        }

        socketIoServer.addDisconnectListener { client ->
            log.info("Client disconnected: sessionId=${client.sessionId}")
        }

        // FE 호환: joinAlertChannel 이벤트 처리
        socketIoServer.addEventListener("joinAlertChannel", JoinChannelRequest::class.java) { client, data, ack ->
            val room = "workspace:${data.workspaceId}:user:${data.userId}"
            client.joinRoom(room)
            ack?.sendAckData("joined")
        }

        // FE 호환: leaveAlertChannel 이벤트 처리
        socketIoServer.addEventListener("leaveAlertChannel", LeaveChannelRequest::class.java) { client, data, ack ->
            val room = "workspace:${data.workspaceId}:user:${data.userId}"
            client.leaveRoom(room)
            ack?.sendAckData("left")
        }
    }
}

data class JoinChannelRequest(val workspaceId: Long, val userId: Long)
data class LeaveChannelRequest(val workspaceId: Long, val userId: Long)
```

### 5. Socket.io v4 프로토콜 호환 검증

기존 FE에서 사용하는 패턴:
```javascript
// FE 기존 코드 (변경 없이 동작해야 함)
const socket = io(SOCKET_URL, {
    query: { token: accessToken },
    transports: ['websocket', 'polling']
});
socket.emit('joinAlertChannel', { workspaceId, userId });
socket.on('alert.added', (data) => { /* 알림 수신 처리 */ });
```

호환 보장 사항:
- `query` 파라미터로 token 전달 지원
- `joinAlertChannel`, `leaveAlertChannel` 이벤트명 동일
- `alert.added` 이벤트명 동일
- 페이로드 구조 동일 (기존 Node.js 서버와 같은 JSON 구조)

### 수정 파일 목록

| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting-notification-service | infrastructure | src/.../infrastructure/websocket/config/SocketIoConfig.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/websocket/auth/SocketIoAuthHandler.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/websocket/auth/JwtProvider.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/websocket/handler/SocketIoEventHandler.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/websocket/redis/RedisSocketIoConfig.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/websocket/redis/RedisNotificationPublisher.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/websocket/redis/RedisNotificationSubscriber.kt | 신규 |
| greeting-notification-service | infrastructure | src/.../infrastructure/websocket/dto/*.kt | 신규 |

## 영향 범위

- greeting-alert-server (Node.js): 대체 대상. Phase 3에서 replica=0 처리.
- FE: WebSocket 연결 URL만 변경 (Ingress로 라우팅), 코드 변경 없음
- Redis: socketio:notification 채널 추가

## 테스트 케이스

| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| TC-06-01 | WebSocket 연결 성공 | 유효한 JWT | Socket.io 클라이언트 connect | 연결 성공, room join 완료 |
| TC-06-02 | JWT 인증 실패 | 만료된/잘못된 JWT | Socket.io 클라이언트 connect | 연결 거부 |
| TC-06-03 | joinAlertChannel 이벤트 | 연결된 클라이언트 | joinAlertChannel emit | 해당 room에 join |
| TC-06-04 | alert.added 수신 | room에 join된 클라이언트 | 해당 room에 알림 발송 | 클라이언트가 alert.added 이벤트 수신 |
| TC-06-05 | Redis Pub/Sub 멀티인스턴스 | 2개 인스턴스, 사용자가 인스턴스 A에 연결 | 인스턴스 B에서 알림 publish | 인스턴스 A의 사용자가 수신 |
| TC-06-06 | Socket.io v4 호환 | 기존 FE Socket.io 클라이언트 | 동일 코드로 연결/이벤트 | 정상 동작 (프로토콜 호환) |
| TC-06-07 | disconnect 처리 | 연결된 클라이언트 | disconnect | room에서 자동 제거, 리소스 해제 |
| TC-06-08 | 동시 접속 부하 | 100개 동시 연결 | 모든 클라이언트에 메시지 발송 | 전체 수신 성공, 메모리 안정 |

## 기대 결과 (AC)

- [ ] netty-socketio 서버 기동 (9092 포트)
- [ ] JWT 인증 동작 (HS256, base64 키)
- [ ] joinAlertChannel / leaveAlertChannel 이벤트 정상 처리
- [ ] alert.added 이벤트 발송 및 수신 정상
- [ ] Redis Pub/Sub 멀티인스턴스 동기화 정상
- [ ] 기존 FE Socket.io 클라이언트와 호환 (코드 변경 0)
- [ ] 100 동시 접속 부하 테스트 통과

## 체크리스트

- [ ] netty-socketio 라이브러리 버전 확인 (Socket.io v4 지원 여부)
- [ ] 미지원 시 대안 검토 (Spring WebSocket + SockJS, 또는 socket.io-server-java)
- [ ] FE 팀과 호환성 사전 확인 (개발 환경에서 FE 코드 무변경 연동 테스트)
- [ ] Kubernetes readiness probe에 WebSocket 포트 포함
- [ ] 메모리 튜닝 (동시 접속 수 × 세션 메모리)
- [ ] CORS 설정 확인
