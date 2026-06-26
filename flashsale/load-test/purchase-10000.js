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
const API_KEY = __ENV.API_KEY;
const CAMPAIGN_ID = __ENV.CAMPAIGN_ID;
const ITEM_ID = __ENV.ITEM_ID;

export default function () {
  const userId = `user-${__ITER}`;

  const response = http.post(
    `${BASE_URL}/api/v1/campaigns/${CAMPAIGN_ID}/orders`,
    JSON.stringify({
      itemId: Number(ITEM_ID),
      userId,
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': API_KEY,
      },
    }
  );

  check(response, {
    'status is 201 or 409': (r) => [201, 409].includes(r.status),
  });
}
