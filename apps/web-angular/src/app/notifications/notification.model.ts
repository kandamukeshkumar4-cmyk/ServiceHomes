export type NotificationType =
  | 'RESERVATION_CREATED'
  | 'RESERVATION_CONFIRMED'
  | 'MESSAGE_RECEIVED'
  | 'REVIEW_RECEIVED'
  | 'PAYOUT_SCHEDULED'
  | 'LISTING_PUBLISHED'
  | 'SYSTEM';

export type NotificationChannel = 'IN_APP' | 'EMAIL' | 'PUSH';

export interface AppNotification {
  id: string;
  type: NotificationType;
  title: string;
  body: string;
  data: Record<string, unknown>;
  channel?: NotificationChannel;
  createdAt: string;
  readAt?: string | null;
  read: boolean;
}

export interface NotificationPage {
  content: AppNotification[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface NotificationPreference {
  type: NotificationType;
  channel: NotificationChannel;
  enabled: boolean;
}
