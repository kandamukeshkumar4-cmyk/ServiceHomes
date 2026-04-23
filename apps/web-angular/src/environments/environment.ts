export const environment = {
  production: true,
  apiBaseUrl: '/api',
  auth: {
    enabled: true,
    domain: 'dev-whp2gausi3kglqlv.us.auth0.com',
    clientId: 'ycU9hs7LpN3wek1UCDEbQkEaUgU64Tqz',
    audience: 'https://api.servicehomes',
    useRefreshTokens: true,
    cacheLocation: 'localstorage' as const
  }
};
