import { useState, useRef, useEffect, useCallback } from 'react';
import { Animated } from 'react-native';

interface CountdownTimerOptions {
    initialSeconds?: number;
    onComplete?: () => void;
}

export function useCountdownTimer({ initialSeconds = 10, onComplete }: CountdownTimerOptions = {}) {
    const [seconds, setSeconds] = useState(initialSeconds);
    const countdownAnimation = useRef(new Animated.Value(0)).current;
    // Use a ref to store the interval ID so we can clear it
    const intervalRef = useRef<any>(null);

    const stopTimer = useCallback(() => {
        if (intervalRef.current) {
            clearInterval(intervalRef.current);
            intervalRef.current = null;
        }
    }, []);

    const resetCountdown = useCallback(() => {
        stopTimer();
        countdownAnimation.setValue(0);
        setSeconds(initialSeconds);
    }, [initialSeconds, countdownAnimation, stopTimer]);

    const startCountdown = useCallback(() => {
        // Reset first to be safe
        resetCountdown();

        // Start Animation
        Animated.timing(countdownAnimation, {
            toValue: 1,
            duration: initialSeconds * 1000,
            useNativeDriver: false,
        }).start();

        // Start Interval
        intervalRef.current = setInterval(() => {
            setSeconds((prev) => {
                if (prev <= 1) {
                    stopTimer();
                    if (onComplete) {
                        onComplete();
                    }
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);
    }, [initialSeconds, countdownAnimation, onComplete, resetCountdown, stopTimer]);

    // Cleanup on unmount
    useEffect(() => {
        return () => {
            stopTimer();
        };
    }, [stopTimer]);

    return {
        countdownSeconds: seconds,
        countdownAnimation,
        startCountdown,
        resetCountdown,
    };
}
