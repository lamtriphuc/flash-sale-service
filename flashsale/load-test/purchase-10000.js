import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    flash_sale_spike: {
      executor: 'shared-iterations',
      vus: Number(__ENV.VUS || 1000),
      iterations: Number(__ENV.ITERATIONS || 10000),
      maxDuration: __ENV.MAX_DURATION || '60s',
    },
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const CAMPAIGN_ID = __ENV.CAMPAIGN_ID;
const ITEM_ID = __ENV.ITEM_ID;
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN;
const USER_TOKENS = (__ENV.USER_TOKENS || '')
  .split(',')
  .map((token) => token.trim())
  .filter(Boolean);

function tokenForIteration() {
  if (USER_TOKENS.length > 0) {
    return USER_TOKENS[__ITER % USER_TOKENS.length];
  }
  return ACCESS_TOKEN;
}

export default function () {
  const token = tokenForIteration();

  const response = http.post(
    `${BASE_URL}/api/v1/campaigns/${CAMPAIGN_ID}/orders`,
    JSON.stringify({
      itemId: Number(ITEM_ID),
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
    }
  );

  check(response, {
    'status is expected': (r) => [201, 202, 409, 503].includes(r.status),
  });
}
