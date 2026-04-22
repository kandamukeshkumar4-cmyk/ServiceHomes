export interface MessagingInboxThread {
  threadId: string;
  reservationId: string;
  listingId: string;
  listingTitle: string;
  listingCoverUrl: string | null;
  counterpartId: string;
  counterpartName: string;
  counterpartAvatarUrl: string | null;
  lastMessagePreview: string | null;
  lastMessageAt: string | null;
  unreadCount: number;
}

export interface MessagingMessage {
  id: string;
  senderId: string;
  senderDisplayName: string;
  senderAvatarUrl: string | null;
  content: string;
  createdAt: string;
  readAt: string | null;
  mine: boolean;
}

export interface MessagingThread {
  threadId: string | null;
  reservationId: string;
  listingId: string;
  listingTitle: string;
  listingCoverUrl: string | null;
  guestId: string;
  hostId: string;
  counterpartId: string;
  counterpartName: string;
  counterpartAvatarUrl: string | null;
  unreadCount: number;
  messages: MessagingMessage[];
}

export interface SendMessagePayload {
  content: string;
}

export interface PendingMessagingMessage extends MessagingMessage {
  pending: true;
}
