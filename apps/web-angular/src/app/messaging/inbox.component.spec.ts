import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';
import { InboxComponent } from './inbox.component';
import { MessagingService } from './messaging.service';

describe('InboxComponent', () => {
  let fixture: ComponentFixture<InboxComponent>;
  let component: InboxComponent;

  const messagingServiceMock = {
    getInbox: jasmine.createSpy().and.returnValue(of([
      {
        threadId: 'thread-1',
        reservationId: 'reservation-1',
        listingId: 'listing-1',
        listingTitle: 'City Loft',
        listingCoverUrl: null,
        counterpartId: 'user-2',
        counterpartName: 'Morgan Host',
        counterpartAvatarUrl: null,
        lastMessagePreview: 'See you at check-in.',
        lastMessageAt: '2026-04-22T12:00:00Z',
        unreadCount: 2
      }
    ]))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InboxComponent, RouterTestingModule],
      providers: [
        { provide: MessagingService, useValue: messagingServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InboxComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders inbox threads from the messaging service', () => {
    expect(messagingServiceMock.getInbox).toHaveBeenCalled();
    expect(component.threads.length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Morgan Host');
    expect(fixture.nativeElement.textContent).toContain('City Loft');
    expect(fixture.nativeElement.textContent).toContain('2 new');
  });
});
