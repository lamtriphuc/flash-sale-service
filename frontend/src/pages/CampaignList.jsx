import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { campaignApi } from '../api/campaigns';
import Countdown from '../components/Countdown';
import useAuthStore from '../store/authStore';

const PLACEHOLDER_IMG = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300"><rect fill="#e5e7eb" width="400" height="300"/><text fill="#9ca3af" font-family="Arial" font-size="16" x="50" y="155">No Image Available</text></svg>'
);

const CampaignList = () => {
  const [campaigns, setCampaigns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { logout, user } = useAuthStore();
  const navigate = useNavigate();

  useEffect(() => {
    fetchCampaigns();
  }, []);

  const fetchCampaigns = async () => {
    try {
      setLoading(true);
      const res = await campaignApi.getAllCampaigns();
      setCampaigns(res.data);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load campaigns');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="text-xl text-gray-600 animate-pulse">Loading campaigns...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Navbar */}
      <nav className="bg-white shadow-md px-6 py-4">
        <div className="max-w-6xl mx-auto flex items-center justify-between">
          <h1 className="text-xl font-bold text-gray-800">FlashDeal</h1>
          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-600">
              {user?.fullName || user?.email}
            </span>
            {user?.role === 'ADMIN' && (
              <button
                onClick={() => navigate('/admin')}
                className="bg-purple-600 text-white px-4 py-1.5 rounded-md text-sm hover:bg-purple-700 transition-colors"
              >
                Admin Panel
              </button>
            )}
            <button
              onClick={handleLogout}
              className="bg-gray-200 text-gray-700 px-4 py-1.5 rounded-md text-sm hover:bg-gray-300 transition-colors"
            >
              Logout
            </button>
          </div>
        </div>
      </nav>

      {/* Content */}
      <div className="max-w-6xl mx-auto px-6 py-8">
        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-6">
            {error}
          </div>
        )}

        {campaigns.length === 0 && !error && (
          <div className="text-center py-16">
            <h2 className="text-2xl font-semibold text-gray-700 mb-2">
              No Campaigns Yet
            </h2>
            <p className="text-gray-500">
              Check back later for upcoming flash sales!
            </p>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {campaigns.map((campaign) => {
            const now = new Date();
            const start = new Date(campaign.startTime);
            const end = new Date(campaign.endTime);
            const isOngoing = now >= start && now <= end;
            const isUpcoming = now < start;
            const isEnded = now > end;

            return (
              <div
                key={campaign.id}
                onClick={() => navigate(`/campaigns/${campaign.id}`)}
                className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow cursor-pointer"
              >
                {/* Thumbnail */}
                <div className="h-48 overflow-hidden bg-gray-100">
                  <img
                    src={campaign.thumbnailUrl || PLACEHOLDER_IMG}
                    alt={campaign.name}
                    className="w-full h-full object-cover"
                    loading="lazy"
                    onError={(e) => {
                      e.target.src = PLACEHOLDER_IMG;
                    }}
                  />
                </div>

                {/* Content */}
                <div className="p-6">
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="text-lg font-semibold text-gray-800">
                      {campaign.name}
                    </h3>
                    <span
                      className={`text-xs font-medium px-2 py-1 rounded-full ${
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
                  <p className="text-sm text-gray-500 mb-3">
                    Code: {campaign.code}
                  </p>
                  {isUpcoming && (
                    <div className="flex items-center gap-2 text-sm">
                      <span className="text-gray-600">Starts in:</span>
                      <Countdown targetTime={campaign.startTime} />
                    </div>
                  )}
                  {isOngoing && (
                    <div className="flex items-center gap-2 text-sm">
                      <span className="text-gray-600">Ends in:</span>
                      <Countdown targetTime={campaign.endTime} />
                    </div>
                  )}
                  {isEnded && (
                    <p className="text-sm text-gray-400 italic">
                      This campaign has ended
                    </p>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default CampaignList;