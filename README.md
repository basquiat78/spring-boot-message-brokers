# Redis Sentinel Setup

지금까지 잘 진행해 왔다면 이제는 `Redis`를 `Sentinel`로 구성을 해보자.

기존에 도커 컴포즈로 실행한 `Redis`는 `docker compose down`으로 내린다.

그리고 `redis`폴더에 `cluster`폴더내에 관련 설정 파일을 만들어 두었다.

뭔가 복잡한 거 같지만 `master node`를 앞에 두고 2개의 `replica`가 `master node`를 바라보게 하는 것이다.

이때 포트 정보는 기존에 로컬에서 돌고 있는 `Redis`와 충돌나지 않도록 기존에 진행해오면서 사용한 포트 6400을 그대로 사용하기 위해 세팅을 해 논 상태이다.

이것은 본인의 로컬에 따라서 기존 포트인 6379로 세팅을 해도 무방하다.

`sentinel.conf`에는 설정 정보에 상세한 주석을 달아놨으니 해당 정보를 참조하면 된다.

도커 컴포즈를 통해 센티넬을 구성을 띄우고 다음 명령어를 통해서 제대로 떠 있는지 확인해 보자.


```shell
$>docker exec -it redis-sentinel-1 redis-cli -p 26379 sentinel master basquiat
$>docker exec -it redis-sentinel-2 redis-cli -p 26379 sentinel master basquiat

1) "name"
 2) "basquiat"
 3) "ip"
 4) "redis-master"
 5) "port"
 6) "6379"
 7) "runid"
 8) "216426c2ed70931e9ecedc706bfe878573f6e3c9"
 9) "flags"
10) "master"
11) "link-pending-commands"
12) "0"
13) "link-refcount"
14) "1"
15) "last-ping-sent"
16) "0"
17) "last-ok-ping-reply"
18) "592"
19) "last-ping-reply"
20) "592"
21) "down-after-milliseconds"
22) "5000"
23) "info-refresh"
24) "6656"
25) "role-reported"
26) "master"
27) "role-reported-time"
28) "56837"
29) "config-epoch"
30) "0"
31) "num-slaves"
32) "2"
33) "num-other-sentinels"
34) "2"
35) "quorum"
36) "2"
37) "failover-timeout"
38) "60000"
39) "parallel-syncs"
40) "1"

What's next:
    Try Docker Debug for seamless, persistent debugging tools in any container or image → docker debug redis-sentinel-1
    Learn more at https://docs.docker.com/go/debug-cli/
medium@mediumui-MacBookPro cluster % docker exec -it redis-sentinel-2 redis-cli -p 26379 sentinel master basquiat
 1) "name"
 2) "basquiat"
 3) "ip"
 4) "redis-master"
 5) "port"
 6) "6379"
 7) "runid"
 8) "216426c2ed70931e9ecedc706bfe878573f6e3c9"
 9) "flags"
10) "master"
11) "link-pending-commands"
12) "0"
13) "link-refcount"
14) "1"
15) "last-ping-sent"
16) "0"
17) "last-ok-ping-reply"
18) "223"
19) "last-ping-reply"
20) "223"
21) "down-after-milliseconds"
22) "5000"
23) "info-refresh"
24) "5516"
25) "role-reported"
26) "master"
27) "role-reported-time"
28) "65661"
29) "config-epoch"
30) "0"
31) "num-slaves"
32) "2"
33) "num-other-sentinels"
34) "2"
35) "quorum"
36) "2"
37) "failover-timeout"
38) "60000"
39) "parallel-syncs"
40) "1"
```
각 `replica`설정값들이 제대로 뜨는 것을 확인하면 센티넬 구성은 준비가 되었다.

# Sentinel 구성을 왜 한겨??

보통 센티넬 구성은 `Redis`를 `in-Memory DB`로 사용하기 위해 구성하는 경우이다.

그래서 한대로 구성한 `Redis`가 문제가 생기면 벌어질 참사를 막기 위해 사용하게 된다.

한대의 마스터 노드가 메인으로 사용되면서 데이터를 다른 레플리카 노드와 동기화를 하게 된다.

그러다가 마스터 노드가 어떤 이유로 장애가 발생하게 되면 빠르게 레플리카중 한대를 마스터로 승격시킨다.

근데 이렇게 구성하게 되면 우리가 기존에 `RLock`을 이용한 분산락에도 문제가 발생하게 된다.

이유가 뭘까?

# Master Node가 바꼈네????

그렇다.

문제가 없이 돌아간다면 마스터로 설정한 노드가 변경될 일이 없다.

그리고 지금은 로컬이기 때문에 사실 마스터가 변경된다 해도 `IP`는 그대로 일 수 있다.

하지만 실제 운영상에서는 `IP`가 그대로일까?

마스터 노드가 바뀌게 되면 분산락도 바라보는 노드가 변경되어야 한다.

하지만 지금 방식이라면 문제가 생긴 노드를 계속 바라보게 될텐데 이것 역시 참사가 발생한다.

그래서 세팅 자체도 바뀌게 된다.

기존 `yaml`을 먼저 변경한다.

```yaml
spring:
  data:
    redis:
      password:
      database: 0
      timeout: 2000
      sentinel:
        # sentinel.conf 에서 정한 alias, 별칭
        master: basquiat
        # 메인 노드를 첫번쨰로 두고
        # 나머지 replica를 배열로 설정한다.
        # 포트는 당연히 docker-compose.yml에 설정한 그 외부 포트로 설정한다.
        # 이 부분 역시 개인에 맞춰서 수정해야 한다.
        nodes:
          - 127.0.0.1:26400
          - 127.0.0.1:26401
          - 127.0.0.1:26402
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
```

기존 `ReddisonConfig`는 아래와 같다.

```kotlin
@Configuration
class RedissonConfig(
    @Value($$"${spring.data.redis.host:localhost}")
    private val redisHost: String,
    @Value($$"${spring.data.redis.port:6379}")
    private val redisPort: Int,
    @Value($$"${spring.data.redis.password:}")
    private val redisPassword: String?,
    @Value($$"${spring.data.redis.database:0}")
    private val redisDatabase: Int,
    @Value($$"${redisson.protocol:redis://}")
    private val protocol: String,
) {
    @Bean
    fun redissonClient(): RedissonClient {
        val config = Config()

        val redissonMapper = mapper.copy()

        config.apply {
            if (!redisPassword.isNullOrBlank()) {
                password = redisPassword
            }
            nettyThreads = 16
            codec = TypedJsonJacksonCodec(Any::class.java, redissonMapper)
        }

        config.useSingleServer().apply {
            address = "$protocol$redisHost:$redisPort"
            database = redisDatabase
            timeout = 3000
            connectTimeout = 5000
            connectionMinimumIdleSize = 10
            connectionPoolSize = 15
            retryAttempts = 3
            retryDelay = ConstantDelay(Duration.ofMillis(100))
        }
        return try {
            Redisson.create(config)
        } catch (e: Exception) {
            throw RuntimeException("Redisson 연결 실패! 주소: redis://$redisHost:$redisPort, 원인: ${e.message}", e)
        }
    }
}
```
이것은 기존 방식인데 보면 알겠지만 `useSingleServer`를 사용하고 있다.

이제는 현행에 맞춰서 설정한다.

```kotlin
@Configuration
class RedissonConfig(
    @Value($$"${spring.data.redis.sentinel.master:basquiat}")
    private val masterName: String,
    @Value($$"${spring.data.redis.sentinel.nodes:127.0.0.1:26400}")
    private val sentinelNodes: List<String>,
    @Value($$"${spring.data.redis.password:}")
    private val redisPassword: String?,
    @Value($$"${spring.data.redis.database:0}")
    private val redisDatabase: Int,
) {
    @Bean
    fun redissonClient(): RedissonClient {
        val config = Config()
        val redissonMapper = mapper.copy()

        config.apply {
            if (!redisPassword.isNullOrBlank()) {
                password = redisPassword
            }
            nettyThreads = 16
            codec = TypedJsonJacksonCodec(Any::class.java, redissonMapper)
        }

        // useSingleServer 대신 useSentinelServers을 사용하자.
        config.useSentinelServers().apply {
            setMasterName(masterName)

            sentinelNodes.forEach { node ->
                addSentinelAddress("redis://$node")
            }

            setDatabase(redisDatabase)

            setTimeout(3000)
            setConnectTimeout(5000)
            setMasterConnectionMinimumIdleSize(10)
            setMasterConnectionPoolSize(15)
            setSlaveConnectionMinimumIdleSize(10)
            setSlaveConnectionPoolSize(15)

            retryAttempts = 3
            retryDelay = ConstantDelay(Duration.ofMillis(100))
        }

        return try {
            Redisson.create(config)
        } catch (e: Exception) {
            throw RuntimeException("Redisson 센티넬 연결 실패! 마스터: $masterName, 원인: ${e.message}", e)
        }
    }
}
```

기존의 우리가 작업한 `Aspect`는 딱히 바꿀 필요는 없다.

다만 지금 상태로는 몇가지 문제가 있는데 현재 환경인 `macOs`에서는 `redis-master`에 대한 `dns`에러가 발생한다.

그래서 다음과 같이

```shell
$> sudo vi /etc/hosts

##
# Host Database
#
# localhost is used to configure the loopback interface
# when the system is booting.  Do not change this entry.
##
127.0.0.1       localhost
127.0.0.1       redis-master
255.255.255.255 broadcasthost
::1             localhost
```
다음과 같이 설정을 추가한다.

하지만 `netty`에서 계속 외부의 `IP`를 통해서 무언가를 처리할려고 한다.

```kotlin
@Configuration
class RedissonConfig(
    @Value($$"${spring.data.redis.sentinel.master:basquiat}")
    private val masterAlias: String,
    @Value($$"${spring.data.redis.sentinel.nodes:127.0.0.1:26400}")
    private val sentinelNodes: List<String>,
    @Value($$"${spring.data.redis.password:}")
    private val redisPassword: String?,
    @Value($$"${spring.data.redis.database:0}")
    private val redisDatabase: Int,
) {
    @Bean
    fun redissonClient(): RedissonClient {
        println("$masterAlias / $sentinelNodes")

        val config = Config()
        val redissonMapper = mapper.copy()

        config.apply {
            if (!redisPassword.isNullOrBlank()) {
                password = redisPassword
            }
            nettyThreads = 16
            codec = TypedJsonJacksonCodec(Any::class.java, redissonMapper)
        }

        // useSingleServer 대신 useSentinelServers을 사용하자.
        config.useSentinelServers().apply {
            setMasterName(masterAlias)

            sentinelNodes.forEach { node ->
                addSentinelAddress("redis://$node")
            }

            setDatabase(redisDatabase)

            // redis에서 센티널 노드 리스트를 확인하지 않도록 false 처리한다.
            setCheckSentinelsList(false)
            setNatMapper(
                object : NatMapper {
                    override fun map(address: RedisURI): RedisURI {
                        // master-replica 도커 내부 포트
                        if (address.port == 6379) {
                            return RedisURI(address.scheme, "127.0.0.1", 6379)
                        }
                        // 모든 걸 무시하고 원래 주소를 반환하도록 한다.
                        return address
                    }
                },
            )

            setTimeout(3000)
            setConnectTimeout(5000)
            setMasterConnectionMinimumIdleSize(10)
            setMasterConnectionPoolSize(15)
            setSlaveConnectionMinimumIdleSize(10)
            setSlaveConnectionPoolSize(15)

            retryAttempts = 3
            retryDelay = ConstantDelay(Duration.ofMillis(100))
        }

        return try {
            Redisson.create(config)
        } catch (e: Exception) {
            throw RuntimeException("Redisson 센티넬 연결 실패! 마스터: $masterAlias, 원인: ${e.message}", e)
        }
    }
}
```

여기서는 서버 실행시

```text
INFO 16420 --- [message-brokers-server] [           main] o.r.c.SentinelConnectionManager          : master: 127.0.0.1/127.0.0.1:6379 added
INFO 16420 --- [message-brokers-server] [           main] o.r.c.SentinelConnectionManager          : slave: 127.0.0.1/127.0.0.1:6379 added
INFO 16420 --- [message-brokers-server] [           main] o.r.c.SentinelConnectionManager          : slave: 127.0.0.1/127.0.0.1:6379 added
INFO 16420 --- [message-brokers-server] [isson-netty-1-7] o.r.c.SentinelConnectionManager          : sentinel: redis://127.0.0.1:26400 added
```
`master/slave`접근 로그가 뜨면 된다.

하지만 이후 외부 `IP`를 찾으려는 `netty`가 계속적으로 에러 로그를 발생시킨다.

실제로 문제는 없지만 로컬 환경으로 인해 벌어지는 문제이므로 다음과 같이 `yaml`에 `logging.level`을 설정하자.

```yaml
logging:
  level:
    org.redisson.connection.SentinelConnectionManager: "OFF"
    org.redisson.client.RedisClient: "OFF"
```

기존에 테스트해본 `LockTest`를 진행해 보자.

여기서는 `NATS`를 통해 호출을 하기 때문에 `yaml`에서 `NATS`를 사용하도록 설정하거나 해당 테스트 코드를 수정해서 직접 테스트를 해보는 것을 추천한다.

# Next Step

이것은 센티넬 구조에서의 분산락을 구현한 것이다.

하지만 금융권 레벨의 분산락을 구현하기 위해서는 `RedLock`으로 가야한다.

이것은 구조 자체가 바뀌기 때문에 다음 브랜치에서 알아보고자 한다.

[독립된 Redis Node를 이용한 RedLock](https://github.com/basquiat78/spring-boot-message-brokers/tree/09-redis-redlock)