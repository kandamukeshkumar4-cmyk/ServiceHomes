import { Routes } from '@angular/router';
import { authGuard } from '../core/auth.guard';
import { NotificationCenterComponent } from './notification-center.component';

export const notificationRoutes: Routes = [
  { path: '', canActivate: [authGuard], component: NotificationCenterComponent }
];
