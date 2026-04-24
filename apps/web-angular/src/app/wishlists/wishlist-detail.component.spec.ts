import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { WishlistDetailComponent } from './wishlist-detail.component';
import { WishlistService } from './wishlist.service';

describe('WishlistDetailComponent', () => {
  let fixture: ComponentFixture<WishlistDetailComponent>;
  const wishlist = {
    id: 'wishlist-1',
    ownerId: 'owner',
    title: 'Trip',
    isPublic: false,
    owner: true,
    collaboratorCount: 0,
    itemCount: 2,
    updatedAt: '',
    collaboratorIds: [],
    items: [
      { id: 'a', listing: { id: 'l1', title: 'A', price: 100 }, sortOrder: 0, addedAt: '' },
      { id: 'b', listing: { id: 'l2', title: 'B', price: 120 }, sortOrder: 1, addedAt: '' }
    ],
    totalItems: 2,
    editable: true
  };
  const service = {
    getWishlist: jasmine.createSpy().and.returnValue(of(wishlist)),
    reorderItems: jasmine.createSpy().and.returnValue(of(void 0)),
    updateItem: jasmine.createSpy().and.returnValue(of(wishlist.items[0])),
    updateCollaborators: jasmine.createSpy().and.returnValue(of({ ...wishlist, collaboratorIds: ['guest'] })),
    generateShareLink: jasmine.createSpy().and.returnValue(of({ token: 't', url: '/wishlists/share/t' })),
    updatePrivacy: jasmine.createSpy().and.returnValue(of(wishlist)),
    removeItem: jasmine.createSpy().and.returnValue(of(void 0))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WishlistDetailComponent, NoopAnimationsModule],
      providers: [
        { provide: WishlistService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'wishlist-1' } } } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(WishlistDetailComponent);
    fixture.detectChanges();
  });

  it('persists reorder and collaborator edits', () => {
    fixture.componentInstance.moveItem(1, -1);
    fixture.componentInstance.collaboratorText = 'guest';
    fixture.componentInstance.saveCollaborators();
    expect(service.reorderItems).toHaveBeenCalledWith('wishlist-1', ['b', 'a']);
    expect(service.updateCollaborators).toHaveBeenCalledWith('wishlist-1', ['guest']);
  });
});
