import axios from 'axios';

// Base client with no auth — used outside React components (rare).
export const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
});

/**
 * Hook version: call this once near the top of a component/page that needs
 * authenticated requests, then use the returned client for calls in that
 * component. getToken() pulls Clerk's current session JWT (same one your
 * Spring Boot SecurityConfig verifies against the JWKS endpoint).
 *
 * Usage:
 *   const api = useApiClient();
 *   const { data } = await api.get('/api/tenant/bills');
 */
import { useAuth } from '@clerk/clerk-react';
import { useMemo } from 'react';

export function useApiClient() {
    const { getToken } = useAuth();

    return useMemo(() => {
        const instance = axios.create({
            baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
        });

        instance.interceptors.request.use(async (config) => {
            const token = await getToken(); // uses the Clerk JWT template you configure
            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
            }
            return config;
        });

        return instance;
    }, [getToken]);
}