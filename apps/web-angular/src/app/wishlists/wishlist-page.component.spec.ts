import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { WishlistPageComponent } from './wishlist-page.component';
import { WishlistService } from './wishlist.service';

describe('WishlistPageComponent', () => {
  let fixture: ComponentFixture<WishlistPageComponent>;
  const service = {
    listWishlists: jasmine.createSpy().and.returnValue(of([])),
    createWishlist: jasmine.createSpy().and.returnValue(of({
      id: 'wishlist-1',
      ownerId: 'owner',
      title: 'Beach',
      isPublic: false,
      owner: true,
      collaboratorCount: 0,
      itemCount: 0,
      updatedAt: new Date().toISOString()
    }))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WishlistPageComponent, RouterTestingModule, NoopAnimationsModule],
      providers: [{ provide: WishlistService, useValue: service }]
    }).compileComponents();
    fixture = TestBed.createComponent(WishlistPageComponent);
    fixture.detectChanges();
  });

  it('renders the wishlist grid and creates wishlists inline', () => {
    fixture.componentInstance.newTitle = 'Beach';
    fixture.componentInstance.createWishlist();
    expect(service.createWishlist).toHaveBeenCalled();
    expect(fixture.componentInstance.wishlists[0].title).toBe('Beach');
  });
});
