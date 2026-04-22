export const environment = {
  production: false,
  apiBaseUrl: '/api',
  auth: {
    enabled: false,
    domain: '',
    clientId: '',
    audience: '',
    useRefreshTokens: false,
    cacheLocation: 'memory' as const
  }
};
