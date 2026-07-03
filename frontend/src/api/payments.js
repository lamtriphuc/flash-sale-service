import apiClient from './client';

export const paymentApi = {
  mockConfirm: (orderId, result) =>
    apiClient.post(`/payments/${orderId}/mock-confirm`, { result }),
};