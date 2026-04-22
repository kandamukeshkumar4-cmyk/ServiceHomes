import { ComponentFixture, TestBed, discardPeriodicTasks, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, Subject, of } from 'rxjs';
import { MessageThreadComponent } from './message-thread.component';
import { MessagingThread } from './messaging.models';
import { MessagingService } from './messaging.service';

describe('MessageThreadComponent', () => {
  let fixture: ComponentFixture<MessageThreadComponent>;
  let component: MessageThreadComponent;
  let routeParamMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let sendMessageSubject: Subject<MessagingThread>;

  const baseThread: MessagingThread = {
    threadId: 'thread-1',
    reservationId: 'reservation-1',
    listingId: 'listing-1',
    listingTitle: 'City Loft',
    listingCoverUrl: null,
    guestId: 'guest-1',
    hostId: 'host-1',
    counterpartId: 'host-1',
    counterpartName: 'Morgan Host',
    counterpartAvatarUrl: null,
    unreadCount: 0,
    messages: [
      {
        id: 'message-1',
        senderId: 'host-1',
        senderDisplayName: 'Morgan Host',
        senderAvatarUrl: null,
        content: 'Welcome to the neighborhood.',
        createdAt: '2026-04-22T12:00:00Z',
        readAt: null,
        mine: false
      }
    ]
  };

  beforeEach(async () => {
    routeParamMap$ = new BehaviorSubject(convertToParamMap({ id: 'reservation-1' }));
    sendMessageSubject = new Subject<MessagingThread>();

    await TestBed.configureTestingModule({
      imports: [MessageThreadComponent],
      providers: [
        {
          provide: MessagingService,
          useValue: {
            getThread: jasmine.createSpy().and.returnValue(of(baseThread)),
            sendMessage: jasmine.createSpy().and.returnValue(sendMessageSubject.asObservable())
          }
        },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: routeParamMap$.asObservable()
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MessageThreadComponent);
    component = fixture.componentInstance;
  });

  it('shows an optimistic message and replaces it with server state after send success', fakeAsync(() => {
    const messagingService = TestBed.inject(MessagingService) as jasmine.SpyObj<MessagingService>;

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    component.draft = 'Looking forward to arrival details.';
    component.sendMessage();
    fixture.detectChanges();

    expect(component.pendingMessages.length).toBe(1);
    expect(messagingService.sendMessage).toHaveBeenCalledWith('reservation-1', {
      content: 'Looking forward to arrival details.'
    });
    expect(fixture.nativeElement.textContent).toContain('Sending…');

    sendMessageSubject.next({
      ...baseThread,
      messages: [
        ...baseThread.messages,
        {
          id: 'message-2',
          senderId: 'guest-1',
          senderDisplayName: 'You',
          senderAvatarUrl: null,
          content: 'Looking forward to arrival details.',
          createdAt: '2026-04-22T12:05:00Z',
          readAt: null,
          mine: true
        }
      ]
    });
    sendMessageSubject.complete();
    tick();
    fixture.detectChanges();

    expect(component.pendingMessages.length).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('Looking forward to arrival details.');

    fixture.destroy();
    discardPeriodicTasks();
  }));
});
