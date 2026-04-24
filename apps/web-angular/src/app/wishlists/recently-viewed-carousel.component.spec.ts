import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { RecentlyViewedCarouselComponent } from './recently-viewed-carousel.component';
import { WishlistService } from './wishlist.service';

describe('RecentlyViewedCarouselComponent', () => {
  let fixture: ComponentFixture<RecentlyViewedCarouselComponent>;
  const service = {
    recentlyViewed: jasmine.createSpy().and.returnValue(of([{ id: 'listing-1', title: 'Loft', price: 100 }])),
    clearRecentlyViewed: jasmine.createSpy().and.returnValue(of(void 0))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecentlyViewedCarouselComponent, RouterTestingModule],
      providers: [{ provide: WishlistService, useValue: service }]
    }).compileComponents();
    fixture = TestBed.createComponent(RecentlyViewedCarouselComponent);
    fixture.detectChanges();
  });

  it('renders horizontal history and clears it', () => {
    expect(fixture.componentInstance.listings.length).toBe(1);
    fixture.componentInstance.clearHistory();
    expect(fixture.componentInstance.listings.length).toBe(0);
    expect(service.clearRecentlyViewed).toHaveBeenCalled();
  });
});
