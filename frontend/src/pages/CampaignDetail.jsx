import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { campaignApi } from '../api/campaigns';
import Countdown from '../components/Countdown';
import PaymentModal from '../components/PaymentModal';
import useAuthStore from '../store/authStore';

const CampaignDetail = () => {
  const { campaignId } = useParams();
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const [campaign, setCampaign] = useState(null);
  const [items, setItems] = useState([]);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [purchasing, setPurchasing] = useState(null);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  // Payment modal state
  const [paymentModal, setPaymentModal] = useState(null);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const [campaignRes, itemsRes, ordersRes] = await Promise.all([
        campaignApi.getCampaign(campaignId),
        campaignApi.getItems(campaignId),
        campaignApi.getOrders(campaignId).catch(() => ({ data: [] })),
      ]);
      setCampaign(campaignRes.data);
      setItems(itemsRes.data);
      setOrders(ordersRes.data);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load campaign');
    } finally {
      setLoading(false);
    }
  }, [campaignId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handlePurchase = async (itemId) => {
    setPurchasing(itemId);
    setError(null);
    setSuccessMsg(null);
    try {
      const res = await campaignApi.createOrder(campaignId, { itemId });
      const order = res.data;

      // If order is PENDING_PAYMENT, show payment modal
      if (order.status === 'PENDING_PAYMENT') {
        const purchasedItem = items.find((i) => i.id === itemId);
        setPaymentModal({ order, item: purchasedItem });

        // Refresh items (stock might have decreased)
        const itemsRes = await campaignApi.getItems(campaignId);
        setItems(itemsRes.data);
      } else {
        setSuccessMsg(`Order created! Status: ${order.status}`);
        const [itemsRes, ordersRes] = await Promise.all([
          campaignApi.getItems(campaignId),
          campaignApi.getOrders(campaignId).catch(() => ({ data: [] })),
        ]);
        setItems(itemsRes.data);
        setOrders(ordersRes.data);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Purchase failed');
    } finally {
      setPurchasing(null);
    }
  };

  const handlePaymentSuccess = (updatedOrder) => {
    setPaymentModal(null);
    setSuccessMsg('Payment successful! Your order has been confirmed.');
    setOrders((prev) =>
      prev.map((o) => (o.id === updatedOrder.id ? updatedOrder : o))
    );
    setTimeout(() => navigate(`/campaigns/${campaignId}`), 1000);
  };

  const handlePaymentFailed = (updatedOrder) => {
    setPaymentModal(null);
    setError('Payment failed. Inventory has been restored. You can try again.');
    // Refresh items to get updated stock
    campaignApi.getItems(campaignId).then((res) => setItems(res.data));
    setOrders((prev) =>
      prev.map((o) => (o.id === updatedOrder.id ? updatedOrder : o))
    );
  };

  const handlePaymentClose = () => {
    setPaymentModal(null);
    // Refresh everything
    fetchData();
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="text-xl text-gray-600 animate-pulse">Loading campaign...</div>
      </div>
    );
  }

  if (error && !campaign) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="bg-white p-8 rounded-lg shadow-md text-center">
          <p className="text-red-600 mb-4">{error}</p>
          <button
            onClick={() => navigate('/campaigns')}
            className="bg-blue-600 text-white px-6 py-2 rounded-md hover:bg-blue-700"
          >
            Back to Campaigns
          </button>
        </div>
      </div>
    );
  }

  const now = new Date();
  const start = new Date(campaign.startTime);
  const end = new Date(campaign.endTime);
  const isOngoing = now >= start && now <= end;
  const isUpcoming = now < start;
  const isEnded = now > end;

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Navbar */}
      <nav className="bg-white shadow-md px-6 py-4">
        <div className="max-w-6xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate('/campaigns')}
              className="text-gray-600 hover:text-gray-800"
            >
              &larr; Back
            </button>
            <h1 className="text-xl font-bold text-gray-800">FlashDeal</h1>
          </div>
          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-600">
              {user?.fullName || user?.email}
            </span>
            {user?.role === 'ADMIN' && (
              <button
                onClick={() => navigate('/admin')}
                className="bg-purple-600 text-white px-4 py-1.5 rounded-md text-sm hover:bg-purple-700"
              >
                Admin Panel
              </button>
            )}
            <button
              onClick={handleLogout}
              className="bg-gray-200 text-gray-700 px-4 py-1.5 rounded-md text-sm hover:bg-gray-300"
            >
              Logout
            </button>
          </div>
        </div>
      </nav>

      {/* Campaign Header */}
      <div className="max-w-6xl mx-auto px-6 py-8">
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex items-center justify-between mb-4">
            <div>
              {campaign.thumbnailUrl && (
                <img
                  src={campaign.thumbnailUrl}
                  alt={campaign.name}
                  className="w-full h-48 object-cover rounded-lg mb-4"
                  loading="lazy"
                />
              )}
              <h2 className="text-2xl font-bold text-gray-800">{campaign.name}</h2>
              <p className="text-gray-500 text-sm mt-1">Code: {campaign.code}</p>
            </div>
            <span
              className={`text-sm font-medium px-3 py-1 rounded-full ${
                isOngoing
                  ? 'bg-green-100 text-green-700'
                  : isUpcoming
                  ? 'bg-blue-100 text-blue-700'
                  : 'bg-gray-100 text-gray-500'
              }`}
            >
              {isOngoing ? 'ONGOING' : isUpcoming ? 'UPCOMING' : 'ENDED'}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div>
              <span className="text-gray-500">Start:</span>{' '}
              <span className="text-gray-800">
                {new Date(campaign.startTime).toLocaleString()}
              </span>
            </div>
            <div>
              <span className="text-gray-500">End:</span>{' '}
              <span className="text-gray-800">
                {new Date(campaign.endTime).toLocaleString()}
              </span>
            </div>
            {isUpcoming && (
              <div className="md:col-span-2">
                <span className="text-gray-500">Starts in:</span>{' '}
                <Countdown targetTime={campaign.startTime} />
              </div>
            )}
            {isOngoing && (
              <div className="md:col-span-2">
                <span className="text-gray-500">Ends in:</span>{' '}
                <Countdown targetTime={campaign.endTime} />
              </div>
            )}
          </div>
        </div>

        {/* Messages */}
        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-6">
            {error}
          </div>
        )}
        {successMsg && (
          <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded mb-6">
            {successMsg}
          </div>
        )}

        {/* Items Grid */}
        <h3 className="text-xl font-semibold text-gray-800 mb-4">Flash Sale Items</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
          {items.map((item) => (
            <div
              key={item.id}
              className="bg-white rounded-lg shadow-md p-6"
            >
              <h4 className="text-lg font-semibold text-gray-800 mb-1">
                {item.itemName}
              </h4>
              <p className="text-sm text-gray-500 mb-3">Code: {item.itemCode}</p>

              <div className="flex items-center gap-2 mb-3">
                <span className="text-2xl font-bold text-red-600">
                  ${(item.salePrice / 100).toFixed(2)}
                </span>
                <span className="text-sm text-gray-400 line-through">
                  ${(item.originalPrice / 100).toFixed(2)}
                </span>
              </div>

              <div className="mb-4">
                <div className="flex justify-between text-sm mb-1">
                  <span className="text-gray-600">Remaining:</span>
                  <span
                    className={`font-medium ${
                      item.remainingQuantity <= 0
                        ? 'text-red-600'
                        : item.remainingQuantity < item.totalQuantity * 0.2
                        ? 'text-orange-600'
                        : 'text-green-600'
                    }`}
                  >
                    {item.remainingQuantity} / {item.totalQuantity}
                  </span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                    className={`h-2 rounded-full transition-all ${
                      item.remainingQuantity <= 0
                        ? 'bg-red-500'
                        : item.remainingQuantity < item.totalQuantity * 0.2
                        ? 'bg-orange-500'
                        : 'bg-green-500'
                    }`}
                    style={{
                      width: `${(item.remainingQuantity / item.totalQuantity) * 100}%`,
                    }}
                  />
                </div>
              </div>

              {isOngoing && item.active && item.remainingQuantity > 0 ? (
                <button
                  onClick={() => handlePurchase(item.id)}
                  disabled={purchasing === item.id}
                  className="w-full bg-red-600 text-white py-2 rounded-md hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-medium"
                >
                  {purchasing === item.id ? 'Processing...' : 'Buy Now'}
                </button>
              ) : (
                <button
                  disabled
                  className="w-full bg-gray-300 text-gray-500 py-2 rounded-md cursor-not-allowed font-medium"
                >
                  {!item.active
                    ? 'Unavailable'
                    : isEnded
                    ? 'Ended'
                    : 'Not Started'}
                </button>
              )}
            </div>
          ))}

          {items.length === 0 && (
            <div className="col-span-full text-center py-12 text-gray-500">
              No items in this campaign yet.
            </div>
          )}
        </div>

        {/* Orders History */}
        {orders.length > 0 && (
          <>
            <h3 className="text-xl font-semibold text-gray-800 mb-4">
              Your Orders
            </h3>
            <div className="bg-white rounded-lg shadow-md overflow-hidden">
              <table className="w-full text-sm">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-gray-600">Order ID</th>
                    <th className="px-4 py-3 text-left text-gray-600">Item</th>
                    <th className="px-4 py-3 text-left text-gray-600">Status</th>
                    <th className="px-4 py-3 text-left text-gray-600">Date</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {orders.map((order) => (
                    <tr key={order.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 font-mono text-xs">#{order.id}</td>
                      <td className="px-4 py-3">{order.itemCode}</td>
                      <td className="px-4 py-3">
                        <span
                          className={`px-2 py-1 rounded-full text-xs font-medium ${
                            order.status === 'CONFIRMED'
                              ? 'bg-green-100 text-green-700'
                              : order.status === 'PENDING_PAYMENT'
                              ? 'bg-yellow-100 text-yellow-700'
                              : order.status === 'CANCELLED'
                              ? 'bg-red-100 text-red-700'
                              : 'bg-gray-100 text-gray-500'
                          }`}
                        >
                          {order.status === 'CONFIRMED'
                            ? 'CONFIRMED'
                            : order.status === 'PENDING_PAYMENT'
                            ? 'PENDING PAYMENT'
                            : order.status}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-gray-500">
                        {new Date(order.createdAt).toLocaleString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>

      {/* Payment Modal */}
      {paymentModal && (
        <PaymentModal
          order={paymentModal.order}
          campaignId={campaignId}
          item={paymentModal.item}
          onClose={handlePaymentClose}
          onSuccess={handlePaymentSuccess}
          onFailed={handlePaymentFailed}
        />
      )}
    </div>
  );
};

export default CampaignDetail;