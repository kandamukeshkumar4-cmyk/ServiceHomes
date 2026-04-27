import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { SaveToWishlistComponent } from './save-to-wishlist.component';
import { WishlistService } from './wishlist.service';

describe('SaveToWishlistComponent', () => {
  let fixture: ComponentFixture<SaveToWishlistComponent>;
  const wishlist = { id: 'wishlist-1', ownerId: 'owner', title: 'Trip', isPublic: false, owner: true, collaboratorCount: 0, itemCount: 0, updatedAt: '' };
  const service = {
    listWishlists: jasmine.createSpy().and.returnValue(of([wishlist])),
    wishlistIdsContainingListing: jasmine.createSpy().and.returnValue(of({ wishlistIds: [] })),
    addItem: jasmine.createSpy().and.returnValue(of({})),
    removeListing: jasmine.createSpy().and.returnValue(of(void 0)),
    createWishlist: jasmine.createSpy().and.returnValue(of(wishlist))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SaveToWishlistComponent, NoopAnimationsModule],
      providers: [{ provide: WishlistService, useValue: service }]
    }).compileComponents();
    fixture = TestBed.createComponent(SaveToWishlistComponent);
    fixture.componentRef.setInput('listingId', 'listing-1');
    fixture.detectChanges();
  });

  it('toggles a listing into a wishlist and creates inline', () => {
    fixture.componentInstance.toggleWishlist(wishlist);
    fixture.componentInstance.newTitle = 'Trip';
    fixture.componentInstance.createWishlist();
    expect(service.addItem).toHaveBeenCalledWith('wishlist-1', 'listing-1');
    expect(service.createWishlist).toHaveBeenCalled();
  });

  it('removes a listing from an already selected wishlist on the server', () => {
    fixture.componentInstance.savedWishlistIds.add('wishlist-1');

    fixture.componentInstance.toggleWishlist(wishlist);

    expect(service.removeListing).toHaveBeenCalledWith('wishlist-1', 'listing-1');
  });
});
