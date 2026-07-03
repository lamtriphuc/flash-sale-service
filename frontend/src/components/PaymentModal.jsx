import { useState, useEffect } from 'react';
import { paymentApi } from '../api/payments';

const PAYMENT_TTL_SECONDS = 5 * 60; // 5 minutes

const PaymentModal = ({ order, campaignId, item, onClose, onSuccess, onFailed }) => {
  const [processing, setProcessing] = useState(false);
  const [timeLeft, setTimeLeft] = useState(PAYMENT_TTL_SECONDS);
  const [result, setResult] = useState(null);

  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          setResult('TIMEOUT');
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    if (result === 'TIMEOUT') {
      const timeoutId = setTimeout(() => {
        onClose();
      }, 2000);
      return () => clearTimeout(timeoutId);
    }
  }, [result, onClose]);

  const handleConfirm = async (paymentResult) => {
    if (processing) return;
    setProcessing(true);

    try {
      const res = await paymentApi.mockConfirm(order.id, paymentResult);
      const updatedOrder = res.data;

      if (paymentResult === 'SUCCESS') {
        setResult('SUCCESS');
        setTimeout(() => onSuccess(updatedOrder), 1500);
      } else {
        setResult('FAILED');
        setTimeout(() => onFailed(updatedOrder), 1500);
      }
    } catch (err) {
      setProcessing(false);
      setResult('ERROR');
    }
  };

  const formatTime = (seconds) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  if (result === 'SUCCESS') {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
        <div className="bg-white rounded-xl shadow-2xl p-8 max-w-md w-full mx-4 text-center">
          <div className="text-6xl mb-4">✅</div>
          <h3 className="text-2xl font-bold text-green-600 mb-2">Payment Successful!</h3>
          <p className="text-gray-600 mb-4">Your order has been confirmed.</p>
          <div className="animate-pulse text-sm text-gray-400">Redirecting...</div>
        </div>
      </div>
    );
  }

  if (result === 'FAILED') {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
        <div className="bg-white rounded-xl shadow-2xl p-8 max-w-md w-full mx-4 text-center">
          <div className="text-6xl mb-4">❌</div>
          <h3 className="text-2xl font-bold text-red-600 mb-2">Payment Failed</h3>
          <p className="text-gray-600 mb-2">The transaction was not completed.</p>
          <p className="text-sm text-gray-500">Inventory has been restored. You can try again.</p>
          <div className="animate-pulse text-sm text-gray-400 mt-4">Redirecting...</div>
        </div>
      </div>
    );
  }

  if (result === 'TIMEOUT') {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
        <div className="bg-white rounded-xl shadow-2xl p-8 max-w-md w-full mx-4 text-center">
          <div className="text-6xl mb-4">⏰</div>
          <h3 className="text-2xl font-bold text-orange-600 mb-2">Reservation Expired</h3>
          <p className="text-gray-600 mb-2">The payment time has expired.</p>
          <p className="text-sm text-gray-500">Inventory has been released.</p>
          <div className="animate-pulse text-sm text-gray-400 mt-4">Closing...</div>
        </div>
      </div>
    );
  }

  if (result === 'ERROR') {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
        <div className="bg-white rounded-xl shadow-2xl p-8 max-w-md w-full mx-4 text-center">
          <div className="text-6xl mb-4">⚠️</div>
          <h3 className="text-2xl font-bold text-red-600 mb-2">Error</h3>
          <p className="text-gray-600 mb-2">Something went wrong. Please try again.</p>
          <button
            onClick={onClose}
            className="bg-gray-600 text-white px-6 py-2 rounded-md hover:bg-gray-700"
          >
            Close
          </button>
        </div>
      </div>
    );
  }

  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;
  const isLowTime = timeLeft <= 60;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      {/* Prevent closing by clicking outside - need to stop propagation */}
      <div
        className="bg-white rounded-xl shadow-2xl p-8 max-w-md w-full mx-4"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="text-center mb-6">
          <div className="text-4xl mb-2">💳</div>
          <h3 className="text-xl font-bold text-gray-800">Mock Payment</h3>
          <p className="text-xs text-gray-400 mt-1">
            This is a demo — no real transaction will occur
          </p>
        </div>

        {/* Order Summary */}
        <div className="bg-gray-50 rounded-lg p-4 mb-6 space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">Product:</span>
            <span className="font-medium text-gray-800">{item?.itemName}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">Code:</span>
            <span className="font-mono text-gray-800">{item?.itemCode}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">Quantity:</span>
            <span className="font-medium text-gray-800">1</span>
          </div>
          <div className="flex justify-between text-sm border-t pt-2">
            <span className="text-gray-600">Total:</span>
            <span className="font-bold text-red-600">
              ${(item?.salePrice / 100).toFixed(2)}
            </span>
          </div>
        </div>

        {/* Timer */}
        <div className="text-center mb-6">
          <div className={`text-sm mb-1 ${isLowTime ? 'text-red-600' : 'text-gray-500'}`}>
            Time remaining to pay:
          </div>
          <div
            className={`text-3xl font-mono font-bold ${
              isLowTime ? 'text-red-600 animate-pulse' : 'text-gray-800'
            }`}
          >
            {formatTime(timeLeft)}
          </div>
        </div>

        {/* Buttons */}
        <div className="space-y-3">
          <button
            onClick={() => handleConfirm('SUCCESS')}
            disabled={processing}
            className="w-full bg-green-600 text-white py-3 rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-medium text-sm"
          >
            {processing ? 'Processing...' : '✅ Confirm Payment (Simulate Success)'}
          </button>

          <button
            onClick={() => handleConfirm('FAILED')}
            disabled={processing}
            className="w-full bg-red-100 text-red-700 py-3 rounded-lg hover:bg-red-200 disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-medium text-sm border border-red-200"
          >
            {processing ? 'Processing...' : '❌ Simulate Failed Payment'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default PaymentModal;