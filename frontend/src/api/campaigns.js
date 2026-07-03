import apiClient from './client';

const toFormData = (file) => {
  const fd = new FormData();
  fd.append('file', file);
  return fd;
};

export const campaignApi = {
  // Campaign CRUD
  getAllCampaigns: () => apiClient.get('/campaigns'),
  createCampaign: (data) => apiClient.post('/campaigns', data),
  getCampaign: (id) => apiClient.get(`/campaigns/${id}`),
  
  // Items
  getItems: (campaignId) => apiClient.get(`/campaigns/${campaignId}/items`),
  addItem: (campaignId, data) => apiClient.post(`/campaigns/${campaignId}/items`, data),
  
  // Orders
  createOrder: (campaignId, data) => apiClient.post(`/campaigns/${campaignId}/orders`, data),
  getOrders: (campaignId) => apiClient.get(`/campaigns/${campaignId}/orders`),
  
  // Thumbnail
  uploadThumbnail: (campaignId, file) =>
    apiClient.post(`/admin/campaigns/${campaignId}/thumbnail`, toFormData(file), {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
};
