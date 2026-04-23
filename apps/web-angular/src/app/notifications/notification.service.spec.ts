import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { BehaviorSubject, of } from 'rxjs';
import { AppAuthService } from '../core/auth.service';
import { NotificationService } from './notification.service';

class FakeWebSocket {
  static readonly instances: FakeWebSocket[] = [];
  static readonly CONNECTING = 0;
  static readonly OPEN = 1;
  static readonly CLOSING = 2;
  static readonly CLOSED = 3;

  onopen: (() => void) | null = null;
  onmessage: ((event: MessageEvent<string>) => void) | null = null;
  onclose: (() => void) | null = null;
  onerror: (() => void) | null = null;
  readyState = FakeWebSocket.CONNECTING;

  constructor(readonly url: string) {
    FakeWebSocket.instances.push(this);
  }

  close(): void {
    this.readyState = FakeWebSocket.CLOSED;
  }
}

describe('NotificationService', () => {
  let service: NotificationService;
  let http: HttpTestingController;
  let authState: BehaviorSubject<boolean>;
  let originalWebSocket: typeof WebSocket;

  beforeEach(() => {
    authState = new BehaviorSubject(false);
    originalWebSocket = window.WebSocket;
    FakeWebSocket.instances.length = 0;
    (window as unknown as { WebSocket: unknown }).WebSocket = FakeWebSocket;

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        {
          provide: AppAuthService,
          useValue: {
            isAuthenticated$: authState.asObservable(),
            getAccessToken: jasmine.createSpy().and.returnValue(of('token-123')),
            me: of({ id: 'local-user' })
          }
        }
      ]
    });

    service = TestBed.inject(NotificationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    window.WebSocket = originalWebSocket;
  });

  it('loads the unread notification count', () => {
    let count = 0;
    service.getUnreadCount().subscribe((value) => count = value);

    const request = http.expectOne('/api/notifications/unread-count');
    expect(request.request.method).toBe('GET');
    request.flush({ count: 4 });

    expect(count).toBe(4);
  });

  it('connects to the backend WebSocket endpoint with the current token', () => {
    service.connectWebSocket();

    expect(FakeWebSocket.instances.length).toBe(1);
    expect(FakeWebSocket.instances[0].url).toContain('/ws/notifications?token=token-123');
  });
});
