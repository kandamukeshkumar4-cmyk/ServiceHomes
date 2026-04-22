export const environment = {
  production: true,
  apiBaseUrl: '/api',
  auth: {
    enabled: true,
    domain: 'YOUR_AUTH0_DOMAIN',
    clientId: 'YOUR_AUTH0_CLIENT_ID',
    audience: 'YOUR_AUTH0_AUDIENCE',
    useRefreshTokens: true,
    cacheLocation: 'localstorage' as const
  }
};
