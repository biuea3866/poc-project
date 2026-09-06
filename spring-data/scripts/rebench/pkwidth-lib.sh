#!/bin/bash
# 격리·복구 공통 함수. run-ablation.sh 가 source 한다.
#
# 이 머신에서는 다른 작업이 Testcontainers 를 돌리고 있고, 컨테이너가 죽은 뒤 외부 정리에
# 휩쓸려 사라지는 일이 두 번 있었다. 그래서 세 가지를 건다.
#   1. 네임드 볼륨 — 컨테이너가 제거돼도 데이터는 남는다. 재시드가 필요 없다.
#   2. restart 정책 — 죽으면 스스로 살아나 "정지된 시체" 상태로 남지 않는다.
#   3. 메모리 상한 + 낮춘 버퍼 풀 — 공유 Docker VM(8.3GB)에서 OOM 표적이 되지 않게 한다.
CT=mysql-pkw
VOL=pkw-mysql-data
PORT=3321
DB=partition_pkwidth

ensure_container() {
  if docker ps --format '{{.Names}}' | grep -qx "$CT"; then return 0; fi
  docker rm -f "$CT" >/dev/null 2>&1
  docker run -d --name "$CT" \
    --label owner=partition-bench \
    --restart unless-stopped \
    --memory=3g --memory-swap=3g \
    -v "$VOL":/var/lib/mysql \
    -e MYSQL_ROOT_PASSWORD=root -p ${PORT}:3306 \
    mysql:8.0 \
    --innodb-buffer-pool-size=1G --innodb-log-file-size=256M \
    --innodb-flush-log-at-trx-commit=2 --innodb-flush-method=O_DIRECT \
    --log-bin=OFF --log-bin-trust-function-creators=1 >/dev/null
  local i=0
  until docker exec "$CT" mysqladmin ping -uroot -proot --silent >/dev/null 2>&1; do
    sleep 3; i=$((i+1)); [ $i -gt 60 ] && return 1
  done
  # DB 를 지정하지 않고 확인한다 — partition_ablation 은 이 함수가 끝난 뒤에 만들어진다.
  until docker exec -i "$CT" mysql -uroot -proot -N -B -e "SELECT 1" >/dev/null 2>&1; do sleep 3; done
  return 0
}

# 컨테이너가 사라졌으면 되살린 뒤 재시도한다.
m() { docker exec -i "$CT" mysql -uroot -proot -N -B "$DB" "$@" 2>/dev/null; }
mq() { local q="$1"; local r; r=$(m -e "$q"); if [ -z "$r" ] && ! docker ps --format '{{.Names}}' | grep -qx "$CT"; then ensure_container; r=$(m -e "$q"); fi; echo "$r"; }
