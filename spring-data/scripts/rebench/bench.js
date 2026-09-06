import http from 'k6/http';
import { check } from 'k6';

// 파티셔닝 재검증용 단일 부하 스크립트. 엔드포인트마다 파일을 두면 조건이 어긋나므로
// 부하 프로파일을 한 곳에 고정하고 대상만 환경변수로 바꾼다.
//
// MODE
//   static : URL 고정 (실험 A)
//   randid : id 를 매 요청 난수로 바꾼다 (실험 B — 같은 행만 때려 버퍼 풀에 얹히는 것을 막는다)
//   window : idFrom/idTo 로 좁은 id 구간을 난수로 고른다 (실험 C)

const BASE = __ENV.TARGET_URL;
const MODE = __ENV.MODE || 'static';
const ID_MIN = parseInt(__ENV.ID_MIN || '1');
const ID_MAX = parseInt(__ENV.ID_MAX || '1');
const WINDOW = parseInt(__ENV.ID_WINDOW || '1000');

export const options = {
  scenarios: {
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: __ENV.RAMP || '20s', target: parseInt(__ENV.VUS || '50') },
        { duration: __ENV.STEADY || '40s', target: parseInt(__ENV.VUS || '50') },
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '5s',
    },
  },
  // 측정이 목적이므로 임계값을 두지 않는다. 임계값 위반으로 런이 중단되면 비교군이 비어버린다.
  thresholds: {},
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

function randInt(min, max) {
  return min + Math.floor(Math.random() * (max - min + 1));
}

export default function () {
  let url = BASE;
  if (MODE === 'randid') {
    url = `${BASE}&id=${randInt(ID_MIN, ID_MAX)}`;
  } else if (MODE === 'window') {
    const from = randInt(ID_MIN, ID_MAX - WINDOW);
    url = `${BASE}&idFrom=${from}&idTo=${from + WINDOW}`;
  }

  const res = http.get(url);
  check(res, { 'status 200': (r) => r.status === 200 });
}
