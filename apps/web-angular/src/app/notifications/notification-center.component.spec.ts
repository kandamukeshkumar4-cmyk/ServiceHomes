import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject, of } from 'rxjs';
import { NotificationCenterComponent } from './notification-center.component';
import { AppNotification } from './notification.model';
import { NotificationService } from './notification.service';

describe('NotificationCenterComponent', () => {
  let fixture: ComponentFixture<NotificationCenterComponent>;
  let component: NotificationCenterComponent;
  let notifications$: BehaviorSubject<AppNotification[]>;
  let unreadCount$: BehaviorSubject<number>;
  let serviceMock: jasmine.SpyObj<Pick<NotificationService, 'refresh' | 'markAllAsRead' | 'markAsRead' | 'dismiss'>>;

  beforeEach(async () => {
    notifications$ = new BehaviorSubject<AppNotification[]>([
      {
        id: 'notification-1',
        type: 'MESSAGE_RECEIVED',
        title: 'New message',
        body: 'Morgan sent you a message.',
        createdAt: '2026-04-22T12:00:00Z',
        readAt: null,
        read: false,
        data: { path: '/inbox' }
      }
    ]);
    unreadCount$ = new BehaviorSubject(3);
    serviceMock = jasmine.createSpyObj('NotificationService', ['refresh', 'markAllAsRead', 'markAsRead', 'dismiss'], {
      notifications$: notifications$.asObservable(),
      unreadCount$: unreadCount$.asObservable()
    });
    serviceMock.markAllAsRead.and.returnValue(of({ updated: 1 }));
    serviceMock.markAsRead.and.returnValue(of({ ...notifications$.value[0], read: true, readAt: '2026-04-22T12:01:00Z' }));
    serviceMock.dismiss.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [NotificationCenterComponent, NoopAnimationsModule, RouterTestingModule],
      providers: [
        { provide: NotificationService, useValue: serviceMock }
      ]
    }).compileComponents();

    spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
    fixture = TestBed.createComponent(NotificationCenterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders the unread badge', () => {
    component.toggle();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('3');
    expect(serviceMock.refresh).toHaveBeenCalled();
  });

  it('delegates read actions to the notification service', () => {
    component.markAllAsRead();
    component.openNotification(notifications$.value[0]);

    expect(serviceMock.markAllAsRead).toHaveBeenCalled();
    expect(serviceMock.markAsRead).toHaveBeenCalledWith('notification-1');
  });
});
