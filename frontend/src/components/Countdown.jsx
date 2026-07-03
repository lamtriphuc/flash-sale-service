import { useState, useEffect } from 'react';

const Countdown = ({ targetTime, onEnd }) => {
  const [timeLeft, setTimeLeft] = useState(calculateTimeLeft(targetTime));

  useEffect(() => {
    const timer = setInterval(() => {
      const remaining = calculateTimeLeft(targetTime);
      setTimeLeft(remaining);

      if (remaining.total <= 0) {
        clearInterval(timer);
        if (onEnd) onEnd();
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [targetTime, onEnd]);

  if (timeLeft.total <= 0) {
    return <span className="text-green-600 font-semibold">Started!</span>;
  }

  return (
    <span className="font-mono text-sm tabular-nums">
      {String(timeLeft.days).padStart(2, '0')}d{' '}
      {String(timeLeft.hours).padStart(2, '0')}h{' '}
      {String(timeLeft.minutes).padStart(2, '0')}m{' '}
      {String(timeLeft.seconds).padStart(2, '0')}s
    </span>
  );
};

function calculateTimeLeft(targetTime) {
  const difference = new Date(targetTime) - new Date();
  if (difference <= 0) {
    return { total: 0, days: 0, hours: 0, minutes: 0, seconds: 0 };
  }
  return {
    total: difference,
    days: Math.floor(difference / (1000 * 60 * 60 * 24)),
    hours: Math.floor((difference / (1000 * 60 * 60)) % 24),
    minutes: Math.floor((difference / (1000 * 60)) % 60),
    seconds: Math.floor((difference / 1000) % 60),
  };
}

export default Countdown;