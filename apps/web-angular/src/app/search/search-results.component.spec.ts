import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, BehaviorSubject } from 'rxjs';
import { SearchResultsComponent } from './search-results.component';
import { SearchStateService } from './search-state.service';
import { SearchApiService } from './search-api.service';
import { SavedListingsService } from '../saved/saved-listings.service';
import { AppAuthService } from '../core/auth.service';
import { ListingService } from '../listings/listing.service';
import { DEFAULT_SEARCH_FILTERS } from './search-filters.model';

describe('SearchResultsComponent', () => {
  let fixture: ComponentFixture<SearchResultsComponent>;
  let component: SearchResultsComponent;

  const filtersSubject = new BehaviorSubject(DEFAULT_SEARCH_FILTERS);

  const searchStateMock = {
    filters$: filtersSubject.asObservable(),
    mapBounds$: of(null),
    snapshot: DEFAULT_SEARCH_FILTERS,
    mapBoundsSnapshot: null,
    setFilters: jasmine.createSpy(),
    patchFilters: jasmine.createSpy(),
    setMapBounds: jasmine.createSpy(),
    removeFilter: jasmine.createSpy(),
    clearFilters: jasmine.createSpy(),
    parseQueryParams: jasmine.createSpy(),
    toQueryParams: jasmine.createSpy()
  };

  const searchApiMock = {
    search: jasmine.createSpy().and.returnValue(of({
      content: [],
      totalElements: 0,
      totalPages: 0,
      currentPage: 0,
      pageSize: 20,
      hasNext: false,
      hasPrevious: false,
      cursor: null
    })),
    getSuggestions: jasmine.createSpy().and.returnValue(of([])),
    recordClick: jasmine.createSpy().and.returnValue(of(undefined)),
    clearCache: jasmine.createSpy()
  };

  const savedListingsMock = {
    save: jasmine.createSpy().and.returnValue(of({})),
    unsave: jasmine.createSpy().and.returnValue(of({}))
  };

  const authMock = {
    isAuthenticated$: of(false),
    login: jasmine.createSpy(),
    logout: jasmine.createSpy()
  };

  const listingServiceMock = {
    search: jasmine.createSpy().and.returnValue(of({
      content: [],
      totalElements: 0,
      totalPages: 0,
      currentPage: 0,
      pageSize: 20,
      hasNext: false,
      hasPrevious: false,
      cursor: null
    }))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SearchResultsComponent],
      providers: [
        { provide: SearchStateService, useValue: searchStateMock },
        { provide: SearchApiService, useValue: searchApiMock },
        { provide: SavedListingsService, useValue: savedListingsMock },
        { provide: AppAuthService, useValue: authMock },
        { provide: ListingService, useValue: listingServiceMock },
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SearchResultsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('creates the component', () => {
    expect(component).toBeTruthy();
  });

  it('initializes with default filters', () => {
    expect(component.filters).toEqual(DEFAULT_SEARCH_FILTERS);
  });

  it('shows empty state header when no results', () => {
    component.totalElements = 0;
    component.filters = { ...DEFAULT_SEARCH_FILTERS, locationQuery: '' };
    expect(component.headerText).toBe('No stays found');
  });

  it('shows result count header when results exist', () => {
    component.totalElements = 5;
    component.filters = { ...DEFAULT_SEARCH_FILTERS, locationQuery: 'Miami' };
    expect(component.headerText).toBe('5 stays for "Miami"');
  });

  it('toggles view mode between list and map', () => {
    component.setViewMode('list');
    expect(component.viewMode).toBe('list');

    component.setViewMode('map');
    expect(component.viewMode).toBe('map');
  });
});
