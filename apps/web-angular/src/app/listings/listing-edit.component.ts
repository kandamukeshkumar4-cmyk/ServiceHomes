import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ListingService } from './listing.service';
import { Listing, ListingCategory, ListingAmenity } from './listing.model';

@Component({
  selector: 'app-listing-edit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="p-4 max-w-40rem mx-auto" *ngIf="listing">
      <h1 class="text-2xl font-bold mb-4">Edit listing</h1>

      <form [formGroup]="form" (ngSubmit)="submit()" class="flex flex-column gap-3">
        <div class="surface-0 shadow-1 border-round-lg p-3">
          <h2 class="text-lg font-bold mb-2">Basics</h2>
          <div class="flex flex-column gap-2">
            <label>Title</label>
            <input class="p-inputtext" formControlName="title" />
          </div>
          <div class="flex flex-column gap-2 mt-2">
            <label>Description</label>
            <textarea class="p-inputtext" formControlName="description" rows="4"></textarea>
          </div>
          <div class="flex flex-column gap-2 mt-2">
            <label>Category</label>
            <select class="p-inputtext" formControlName="categoryId">
              <option *ngFor="let cat of categories" [value]="cat.id">{{ cat.name }}</option>
            </select>
          </div>
          <div class="flex flex-column gap-2 mt-2">
            <label>Property Type</label>
            <select class="p-inputtext" formControlName="propertyType">
              <option value="APARTMENT">Apartment</option>
              <option value="HOUSE">House</option>
              <option value="VILLA">Villa</option>
              <option value="CABIN">Cabin</option>
              <option value="COTTAGE">Cottage</option>
              <option value="TINY_HOME">Tiny Home</option>
              <option value="TREEHOUSE">Treehouse</option>
              <option value="BOAT">Boat</option>
              <option value="CAMPER">Camper</option>
            </select>
          </div>
        </div>

        <div class="surface-0 shadow-1 border-round-lg p-3">
          <h2 class="text-lg font-bold mb-2">Capacity</h2>
          <div class="grid gap-2">
            <div class="col-6 flex flex-column gap-2"><label>Max guests</label><input type="number" class="p-inputtext" formControlName="maxGuests" /></div>
            <div class="col-6 flex flex-column gap-2"><label>Bedrooms</label><input type="number" class="p-inputtext" formControlName="bedrooms" /></div>
            <div class="col-6 flex flex-column gap-2"><label>Beds</label><input type="number" class="p-inputtext" formControlName="beds" /></div>
            <div class="col-6 flex flex-column gap-2"><label>Bathrooms</label><input type="number" class="p-inputtext" formControlName="bathrooms" /></div>
          </div>
        </div>

        <div class="surface-0 shadow-1 border-round-lg p-3">
          <h2 class="text-lg font-bold mb-2">Pricing</h2>
          <div class="grid gap-2">
            <div class="col-4 flex flex-column gap-2"><label>Nightly price</label><input type="number" class="p-inputtext" formControlName="nightlyPrice" /></div>
            <div class="col-4 flex flex-column gap-2"><label>Cleaning fee</label><input type="number" class="p-inputtext" formControlName="cleaningFee" /></div>
            <div class="col-4 flex flex-column gap-2"><label>Service fee</label><input type="number" class="p-inputtext" formControlName="serviceFee" /></div>
          </div>
        </div>

        <div class="surface-0 shadow-1 border-round-lg p-3" formGroupName="location">
          <h2 class="text-lg font-bold mb-2">Location</h2>
          <div class="flex flex-column gap-2"><label>Address line 1</label><input class="p-inputtext" formControlName="addressLine1" /></div>
          <div class="flex flex-column gap-2 mt-2"><label>Address line 2</label><input class="p-inputtext" formControlName="addressLine2" /></div>
          <div class="grid gap-2 mt-2">
            <div class="col-6 flex flex-column gap-2"><label>City</label><input class="p-inputtext" formControlName="city" /></div>
            <div class="col-6 flex flex-column gap-2"><label>State</label><input class="p-inputtext" formControlName="state" /></div>
            <div class="col-6 flex flex-column gap-2"><label>Postal code</label><input class="p-inputtext" formControlName="postalCode" /></div>
            <div class="col-6 flex flex-column gap-2"><label>Country</label><input class="p-inputtext" formControlName="country" /></div>
          </div>
        </div>

        <div class="surface-0 shadow-1 border-round-lg p-3" formGroupName="policy">
          <h2 class="text-lg font-bold mb-2">Policies</h2>
          <div class="grid gap-2">
            <div class="col-6 flex flex-column gap-2"><label>Check-in time</label><input type="time" class="p-inputtext" formControlName="checkInTime" /></div>
            <div class="col-6 flex flex-column gap-2"><label>Check-out time</label><input type="time" class="p-inputtext" formControlName="checkOutTime" /></div>
            <div class="col-6 flex flex-column gap-2"><label>Min nights</label><input type="number" class="p-inputtext" formControlName="minNights" /></div>
            <div class="col-6 flex flex-column gap-2"><label>Max nights</label><input type="number" class="p-inputtext" formControlName="maxNights" /></div>
            <div class="col-6 flex flex-column gap-2">
              <label>Cancellation policy</label>
              <select class="p-inputtext" formControlName="cancellationPolicy">
                <option value="FLEXIBLE">Flexible</option>
                <option value="MODERATE">Moderate</option>
                <option value="STRICT">Strict</option>
              </select>
            </div>
            <div class="col-6 flex align-items-center gap-2 mt-3">
              <input type="checkbox" id="instantBook" formControlName="instantBook" />
              <label for="instantBook">Instant book</label>
            </div>
          </div>
        </div>

        <div class="surface-0 shadow-1 border-round-lg p-3">
          <h2 class="text-lg font-bold mb-2">Amenities</h2>
          <div class="flex flex-wrap gap-2">
            <label *ngFor="let amenity of amenities" class="flex align-items-center gap-2 px-3 py-2 border-1 surface-border border-round-lg cursor-pointer hover:surface-100">
              <input type="checkbox" [value]="amenity.id" [checked]="selectedAmenityIds.includes(amenity.id)" (change)="toggleAmenity(amenity.id, $event)" />
              <span>{{ amenity.name }}</span>
            </label>
          </div>
        </div>

        <button type="submit" class="p-button p-button-primary" [disabled]="form.invalid">Save changes</button>
      </form>
    </div>
  `,
  styles: []
})
export class ListingEditComponent implements OnInit {
  private fb = inject(FormBuilder);
  private listingService = inject(ListingService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  listingId = '';
  listing: Listing | null = null;
  categories: ListingCategory[] = [];
  amenities: ListingAmenity[] = [];
  selectedAmenityIds: string[] = [];

  form = this.fb.group({
    title: ['', Validators.required],
    description: ['', Validators.required],
    categoryId: ['', Validators.required],
    propertyType: ['', Validators.required],
    maxGuests: [1, [Validators.required, Validators.min(1)]],
    bedrooms: [0, Validators.required],
    beds: [0, Validators.required],
    bathrooms: [0, Validators.required],
    nightlyPrice: [0, [Validators.required, Validators.min(0)]],
    cleaningFee: [0],
    serviceFee: [0],
    location: this.fb.group({
      addressLine1: ['', Validators.required],
      addressLine2: [''],
      city: ['', Validators.required],
      state: [''],
      postalCode: [''],
      country: ['', Validators.required]
    }),
    policy: this.fb.group({
      checkInTime: [''],
      checkOutTime: [''],
      minNights: [1, Validators.required],
      maxNights: [null],
      cancellationPolicy: ['FLEXIBLE'],
      instantBook: [false]
    })
  });

  ngOnInit() {
    this.listingId = this.route.snapshot.paramMap.get('id') || '';
    this.listingService.getCategories().subscribe(c => this.categories = c);
    this.listingService.getAmenities().subscribe(a => this.amenities = a);
    this.listingService.getById(this.listingId).subscribe(l => {
      this.listing = l;
      this.selectedAmenityIds = l.amenities.map(a => a.id);
      this.form.patchValue({
        title: l.title,
        description: l.description,
        categoryId: l.category.id,
        propertyType: l.propertyType,
        maxGuests: l.maxGuests,
        bedrooms: l.bedrooms,
        beds: l.beds,
        bathrooms: l.bathrooms,
        nightlyPrice: Number(l.nightlyPrice),
        cleaningFee: Number(l.cleaningFee || 0),
        serviceFee: Number(l.serviceFee || 0),
        location: {
          addressLine1: l.location.addressLine1,
          addressLine2: l.location.addressLine2 || '',
          city: l.location.city,
          state: l.location.state || '',
          postalCode: l.location.postalCode || '',
          country: l.location.country
        },
        policy: {
          checkInTime: l.policy.checkInTime || '',
          checkOutTime: l.policy.checkOutTime || '',
          minNights: l.policy.minNights,
          maxNights: l.policy.maxNights || null,
          cancellationPolicy: l.policy.cancellationPolicy,
          instantBook: l.policy.instantBook
        }
      });
    });
  }

  toggleAmenity(id: string, event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      this.selectedAmenityIds.push(id);
    } else {
      this.selectedAmenityIds = this.selectedAmenityIds.filter(x => x !== id);
    }
  }

  submit() {
    if (this.form.invalid) return;
    const value = this.form.value;
    this.listingService.update(this.listingId, {
      ...value,
      location: value.location!,
      policy: {
        ...value.policy!,
        instantBook: !!value.policy!.instantBook
      },
      amenityIds: this.selectedAmenityIds
    } as any).subscribe(() => this.router.navigate(['/host/accommodations']));
  }
}
