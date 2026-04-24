import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SearchMapComponent, SearchMapBounds } from './search-map.component';

describe('SearchMapComponent', () => {
  let fixture: ComponentFixture<SearchMapComponent>;
  let component: SearchMapComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SearchMapComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(SearchMapComponent);
    component = fixture.componentInstance;
  });

  it('creates the component', () => {
    expect(component).toBeTruthy();
  });

  it('has empty results initially', () => {
    expect(component.results).toEqual([]);
    expect(component.visibleResultCount).toBe(0);
    expect(component.hasResults).toBe(false);
  });

  it('detects results with valid coordinates', () => {
    component.results = [
      {
        id: '1',
        title: 'Test',
        coverUrl: '',
        city: 'Miami',
        country: 'USA',
        nightlyPrice: 100,
        categoryName: 'Beach',
        latitude: 25.76,
        longitude: -80.19,
        maxGuests: 2,
        bedrooms: 1,
        beds: 1,
        bathrooms: 1,
        reviewCount: 0,
        isSaved: false
      }
    ];
    fixture.detectChanges();
    expect(component.hasResults).toBe(true);
    expect(component.visibleResultCount).toBe(1);
  });

  it('emits searchAreaRequested when requestSearchArea is called', () => {
    const bounds: SearchMapBounds = { swLat: 40.0, swLng: -74.0, neLat: 41.0, neLng: -73.0 };
    let emitted: SearchMapBounds | undefined;

    component.searchAreaRequested.subscribe((b) => {
      emitted = b;
    });

    (component as any).pendingBounds = bounds;
    component.requestSearchArea();

    expect(emitted).toEqual(bounds);
    expect(component.isSearchAreaDirty).toBe(false);
  });

  it('does not emit searchAreaRequested when pendingBounds is null', () => {
    const spy = jasmine.createSpy();
    component.searchAreaRequested.subscribe(spy);
    component.requestSearchArea();
    expect(spy).not.toHaveBeenCalled();
  });

  it('cleans up map on destroy', () => {
    const removeSpy = jasmine.createSpy();
    (component as any).map = { remove: removeSpy };
    component.ngOnDestroy();
    expect(removeSpy).toHaveBeenCalled();
    expect((component as any).map).toBeNull();
  });
});
