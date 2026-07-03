import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { campaignApi } from '../api/campaigns';
import useAuthStore from '../store/authStore';

const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

const PLACEHOLDER_IMG = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300"><rect fill="#e5e7eb" width="400" height="300"/><text fill="#9ca3af" font-family="Arial" font-size="16" x="50" y="155">No Image Available</text></svg>'
);

const Admin = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const [campaigns, setCampaigns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  // Campaign form state
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newCampaign, setNewCampaign] = useState({
    code: '',
    name: '',
    startTime: '',
    endTime: '',
  });

  // Thumbnail state
  const [thumbnailFile, setThumbnailFile] = useState(null);
  const [thumbnailPreview, setThumbnailPreview] = useState(null);
  const [uploadingThumbnail, setUploadingThumbnail] = useState(false);
  const fileInputRef = useRef(null);

  // Item form state
  const [showItemForm, setShowItemForm] = useState(null);
  const [newItem, setNewItem] = useState({
    itemCode: '',
    itemName: '',
    originalPrice: '',
    salePrice: '',
    totalQuantity: '',
  });

  useEffect(() => {
    fetchCampaigns();
  }, []);

  useEffect(() => {
    // Cleanup preview URL on unmount
    return () => {
      if (thumbnailPreview && thumbnailPreview.startsWith('blob:')) {
        URL.revokeObjectURL(thumbnailPreview);
      }
    };
  }, [thumbnailPreview]);

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

  const validateFile = (file) => {
    if (!file) return 'No file selected';
    if (!ALLOWED_TYPES.includes(file.type)) {
      return 'Only JPG, PNG, and WebP images are allowed';
    }
    if (file.size > MAX_FILE_SIZE) {
      return 'File size must be less than 5MB';
    }
    return null;
  };

  const handleFileSelect = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const validationError = validateFile(file);
    if (validationError) {
      setError(validationError);
      setThumbnailFile(null);
      setThumbnailPreview(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }

    setError(null);
    setThumbnailFile(file);
    // Create preview
    const previewUrl = URL.createObjectURL(file);
    setThumbnailPreview(previewUrl);
  };

  const handleCreateCampaign = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);
    try {
      const res = await campaignApi.createCampaign({
        code: newCampaign.code,
        name: newCampaign.name,
        startTime: new Date(newCampaign.startTime).toISOString(),
        endTime: new Date(newCampaign.endTime).toISOString(),
      });
      const createdCampaign = res.data;

      // If thumbnail was selected, upload it
      if (thumbnailFile) {
        setUploadingThumbnail(true);
        try {
          await campaignApi.uploadThumbnail(createdCampaign.id, thumbnailFile);
          setSuccessMsg('Campaign created and thumbnail uploaded successfully!');
        } catch (uploadErr) {
          setError(
            'Campaign created but thumbnail upload failed. You can retry upload later.'
          );
        } finally {
          setUploadingThumbnail(false);
        }
      } else {
        setSuccessMsg('Campaign created successfully!');
      }

      // Reset form
      setShowCreateForm(false);
      setNewCampaign({ code: '', name: '', startTime: '', endTime: '' });
      setThumbnailFile(null);
      setThumbnailPreview(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
      fetchCampaigns();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create campaign');
    }
  };

  const handleAddItem = async (campaignId, e) => {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);
    try {
      await campaignApi.addItem(campaignId, {
        itemCode: newItem.itemCode,
        itemName: newItem.itemName,
        originalPrice: parseInt(newItem.originalPrice, 10),
        salePrice: parseInt(newItem.salePrice, 10),
        totalQuantity: parseInt(newItem.totalQuantity, 10),
      });
      setSuccessMsg('Item added successfully!');
      setShowItemForm(null);
      setNewItem({
        itemCode: '',
        itemName: '',
        originalPrice: '',
        salePrice: '',
        totalQuantity: '',
      });
      fetchCampaigns();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to add item');
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (user?.role !== 'ADMIN') {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="bg-white p-8 rounded-lg shadow-md text-center">
          <p className="text-red-600 mb-4">Access denied. Admin only.</p>
          <button
            onClick={() => navigate('/campaigns')}
            className="bg-blue-600 text-white px-6 py-2 rounded-md hover:bg-blue-700"
          >
            Go to Campaigns
          </button>
        </div>
      </div>
    );
  }

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
            <h1 className="text-xl font-bold text-gray-800">Admin Panel</h1>
          </div>
          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-600">
              {user?.fullName || user?.email}
            </span>
            <button
              onClick={handleLogout}
              className="bg-gray-200 text-gray-700 px-4 py-1.5 rounded-md text-sm hover:bg-gray-300"
            >
              Logout
            </button>
          </div>
        </div>
      </nav>

      <div className="max-w-6xl mx-auto px-6 py-8">
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

        {/* Create Campaign Button */}
        <div className="mb-6">
          <button
            onClick={() => {
              setShowCreateForm(!showCreateForm);
              setError(null);
              setSuccessMsg(null);
            }}
            className="bg-blue-600 text-white px-6 py-2 rounded-md hover:bg-blue-700 transition-colors"
          >
            {showCreateForm ? 'Cancel' : '+ Create Campaign'}
          </button>
        </div>

        {/* Create Campaign Form */}
        {showCreateForm && (
          <div className="bg-white rounded-lg shadow-md p-6 mb-6">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">
              New Campaign
            </h3>
            <form onSubmit={handleCreateCampaign} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Code
                  </label>
                  <input
                    type="text"
                    value={newCampaign.code}
                    onChange={(e) =>
                      setNewCampaign({ ...newCampaign, code: e.target.value })
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="FLASH-001"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Name
                  </label>
                  <input
                    type="text"
                    value={newCampaign.name}
                    onChange={(e) =>
                      setNewCampaign({ ...newCampaign, name: e.target.value })
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Summer Flash Sale"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Start Time
                  </label>
                  <input
                    type="datetime-local"
                    value={newCampaign.startTime}
                    onChange={(e) =>
                      setNewCampaign({
                        ...newCampaign,
                        startTime: e.target.value,
                      })
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    End Time
                  </label>
                  <input
                    type="datetime-local"
                    value={newCampaign.endTime}
                    onChange={(e) =>
                      setNewCampaign({
                        ...newCampaign,
                        endTime: e.target.value,
                      })
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  />
                </div>
              </div>

              {/* Thumbnail Upload */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Campaign Thumbnail (optional)
                </label>
                <div className="flex items-start gap-4">
                  <div className="flex-1">
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      onChange={handleFileSelect}
                      className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
                    />
                    <p className="text-xs text-gray-400 mt-1">
                      JPG, PNG, or WebP. Max 5MB.
                    </p>
                  </div>
                  {thumbnailPreview && (
                    <div className="w-32 h-24 rounded-lg overflow-hidden border border-gray-200 flex-shrink-0">
                      <img
                        src={thumbnailPreview}
                        alt="Preview"
                        className="w-full h-full object-cover"
                      />
                    </div>
                  )}
                </div>
              </div>

              <button
                type="submit"
                disabled={uploadingThumbnail}
                className="bg-green-600 text-white px-6 py-2 rounded-md hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                {uploadingThumbnail
                  ? 'Uploading thumbnail...'
                  : 'Create Campaign'}
              </button>
            </form>
          </div>
        )}

        {/* Campaigns List */}
        <h3 className="text-xl font-semibold text-gray-800 mb-4">
          All Campaigns
        </h3>

        {loading ? (
          <div className="text-center py-8 text-gray-600 animate-pulse">
            Loading campaigns...
          </div>
        ) : campaigns.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            No campaigns yet. Create one!
          </div>
        ) : (
          <div className="space-y-4">
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
                  className="bg-white rounded-lg shadow-md overflow-hidden"
                >
                  {/* Thumbnail Row */}
                  {campaign.thumbnailUrl && (
                    <div className="h-48 overflow-hidden bg-gray-100">
                      <img
                        src={campaign.thumbnailUrl}
                        alt={campaign.name}
                        className="w-full h-full object-cover"
                        loading="lazy"
                      />
                    </div>
                  )}

                  <div className="p-6">
                    <div className="flex items-center justify-between mb-4">
                      <div>
                        <h4 className="text-lg font-semibold text-gray-800">
                          {campaign.name}
                        </h4>
                        <p className="text-sm text-gray-500">
                          Code: {campaign.code} | ID: {campaign.id}
                        </p>
                      </div>
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

                    <div className="text-sm text-gray-600 mb-4">
                      <p>
                        Start: {new Date(campaign.startTime).toLocaleString()}
                      </p>
                      <p>End: {new Date(campaign.endTime).toLocaleString()}</p>
                    </div>

                    {/* Add Item Button */}
                    <button
                      onClick={() =>
                        setShowItemForm(
                          showItemForm === campaign.id ? null : campaign.id
                        )
                      }
                      className="bg-purple-600 text-white px-4 py-1.5 rounded-md text-sm hover:bg-purple-700 transition-colors"
                    >
                      {showItemForm === campaign.id ? 'Cancel' : '+ Add Item'}
                    </button>

                    {/* Add Item Form */}
                    {showItemForm === campaign.id && (
                      <form
                        onSubmit={(e) => handleAddItem(campaign.id, e)}
                        className="mt-4 p-4 bg-gray-50 rounded-md space-y-4"
                      >
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                          <div>
                            <label className="block text-xs font-medium text-gray-700 mb-1">
                              Item Code
                            </label>
                            <input
                              type="text"
                              value={newItem.itemCode}
                              onChange={(e) =>
                                setNewItem({
                                  ...newItem,
                                  itemCode: e.target.value,
                                })
                              }
                              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                              placeholder="ITEM-001"
                              required
                            />
                          </div>
                          <div>
                            <label className="block text-xs font-medium text-gray-700 mb-1">
                              Item Name
                            </label>
                            <input
                              type="text"
                              value={newItem.itemName}
                              onChange={(e) =>
                                setNewItem({
                                  ...newItem,
                                  itemName: e.target.value,
                                })
                              }
                              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                              placeholder="Product Name"
                              required
                            />
                          </div>
                          <div>
                            <label className="block text-xs font-medium text-gray-700 mb-1">
                              Original Price (cents)
                            </label>
                            <input
                              type="number"
                              value={newItem.originalPrice}
                              onChange={(e) =>
                                setNewItem({
                                  ...newItem,
                                  originalPrice: e.target.value,
                                })
                              }
                              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                              placeholder="10000"
                              required
                              min="1"
                            />
                          </div>
                          <div>
                            <label className="block text-xs font-medium text-gray-700 mb-1">
                              Sale Price (cents)
                            </label>
                            <input
                              type="number"
                              value={newItem.salePrice}
                              onChange={(e) =>
                                setNewItem({
                                  ...newItem,
                                  salePrice: e.target.value,
                                })
                              }
                              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                              placeholder="5000"
                              required
                              min="1"
                            />
                          </div>
                          <div>
                            <label className="block text-xs font-medium text-gray-700 mb-1">
                              Total Quantity
                            </label>
                            <input
                              type="number"
                              value={newItem.totalQuantity}
                              onChange={(e) =>
                                setNewItem({
                                  ...newItem,
                                  totalQuantity: e.target.value,
                                })
                              }
                              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                              placeholder="100"
                              required
                              min="1"
                            />
                          </div>
                        </div>
                        <button
                          type="submit"
                          className="bg-green-600 text-white px-4 py-1.5 rounded-md text-sm hover:bg-green-700 transition-colors"
                        >
                          Add Item
                        </button>
                      </form>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default Admin;