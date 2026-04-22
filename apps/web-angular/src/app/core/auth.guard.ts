import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { map, take } from 'rxjs/operators';
import { AppAuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AppAuthService);

  return auth.isAuthenticated$.pipe(
    take(1),
    map(isAuth => {
      if (!isAuth) {
        auth.login();
        return false;
      }
      return true;
    })
  );
};
