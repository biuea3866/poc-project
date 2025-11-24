# 스프링 로컬 캐시 예제 프로젝트
## 개요
스프링 로컬 캐시를 활용하기 위한 예제 프로젝트

## 목적
로컬 캐시 사용 전후의 메트릭을 비교하여 사용했을 때의 장단점을 확인합니다.

## 환경
* kotlin + spring boot
* caffeine cache
* mysql

## 요구사항
* Caffeine 캐시를 이용하여 로컬 캐시를 구현합니다.
* 하나의 애플리케이션에 총 2가지의 예제를 구성합니다.
  * write, read 패턴을 적용하여 점진적으로 캐시가 쌓이는 구조로 예제를 작성합니다.
  * 처음 애플리케이션 부트 시 한번에 cache가 로드되고, 1분 후에 TTL이 만료되어 cache penetration이 발생합니다. (2분 동안 api 호출)
* 도메인은 주문 도메인으로 합니다.
* metric 지표를 볼 수 있게 docker-compose로 인프라를 구성하고, 간단하게 마크 다운 형식의 리포트 파일을 작성합니다.
  * 애플리케이션의 cpu, 메모리 사용률
  * mysql의 cpu, 메모리 사용률
  * 이들을 같은 그래프에서 비교하여 로컬 캐시를 사용했을 때의 cpu, 메모리 사용률 차이를 볼 수 있습니다.
  * metric은 grafana를 이용합니다. 
* 부하 테스트 도구는 k6를 사용합니다.
* 주문 도메인 데이터는 100만 건을 대상으로 합니다.
* 프로젝트는 feature/flag_project/cache-practice로 구성하고, 해당 파일(local_cache_context.md)을 cache-practice로 옮깁니다.
    
## 기대 결과
* 모든 예제에 대한 metric 지표를 통해 메모리, cpu, 처리 시간(latency)등을 한 눈에 파악합니다.