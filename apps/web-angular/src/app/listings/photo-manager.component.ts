import { Component, inject, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ListingPhoto } from './listing.model';

@Component({
  selector: 'app-photo-manager',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="surface-0 shadow-1 border-round-lg p-3">
      <h2 class="text-lg font-bold mb-2">Photos</h2>

      <div class="flex gap-2 mb-3">
        <input type="file" accept="image/*" multiple (change)="onFilesSelected($event)" #fileInput hidden />
        <button class="p-button p-button-sm p-button-outlined" (click)="fileInput.click()">Upload files</button>
        <button class="p-button p-button-sm p-button-outlined" (click)="showUrlInput = !showUrlInput">Add by URL</button>
      </div>

      <div *ngIf="showUrlInput" class="flex gap-2 mb-3">
        <input class="p-inputtext flex-1" [(ngModel)]="urlInput" placeholder="https://example.com/image.jpg" />
        <button class="p-button p-button-sm" (click)="addByUrl()">Add</button>
      </div>

      <div class="grid gap-2">
        <div *ngFor="let photo of photos; let i = index" class="col-4 md:col-3 relative">
          <div class="border-round-lg overflow-hidden shadow-1" [class.border-2]="photo.isCover" [class.border-primary]="photo.isCover">
            <img [src]="photo.url" class="w-full h-6rem object-cover" />
            <div class="absolute top-1 right-1 flex gap-1">
              <button *ngIf="!photo.isCover" class="p-button p-button-sm p-button-text p-1" (click)="setCover(photo.id)" title="Set as cover">
                <i class="pi pi-star text-yellow-500"></i>
              </button>
              <button *ngIf="i > 0" class="p-button p-button-sm p-button-text p-1" (click)="moveUp(i)" title="Move up">
                <i class="pi pi-arrow-up"></i>
              </button>
              <button *ngIf="i < photos.length - 1" class="p-button p-button-sm p-button-text p-1" (click)="moveDown(i)" title="Move down">
                <i class="pi pi-arrow-down"></i>
              </button>
              <button class="p-button p-button-sm p-button-text p-button-danger p-1" (click)="delete(photo.id)" title="Delete">
                <i class="pi pi-trash"></i>
              </button>
            </div>
            <div *ngIf="photo.isCover" class="absolute bottom-1 left-1 px-2 py-1 bg-primary text-white text-xs border-round-lg">Cover</div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class PhotoManagerComponent implements OnInit {
  private http = inject(HttpClient);

  @Input() listingId = '';
  photos: ListingPhoto[] = [];
  showUrlInput = false;
  urlInput = '';

  ngOnInit() {
    this.loadPhotos();
  }

  loadPhotos() {
    // In a real implementation we'd fetch from a dedicated endpoint
    // For now we rely on the parent component passing photos or we fetch listing
  }

  onFilesSelected(event: Event) {
    const files = (event.target as HTMLInputElement).files;
    if (!files) return;
    Array.from(files).forEach(file => this.uploadFile(file));
  }

  uploadFile(file: File) {
    this.http.post<{ uploadUrl: string; s3Key: string; publicUrl: string }>(
      `/api/listings/${this.listingId}/photos/presigned-url`,
      { contentType: file.type, fileName: file.name }
    ).subscribe(res => {
      this.http.put(res.uploadUrl, file, {
        headers: { 'Content-Type': file.type }
      }).subscribe(() => {
        this.photos.push({
          id: crypto.randomUUID(),
          url: res.publicUrl,
          orderIndex: this.photos.length,
          isCover: this.photos.length === 0
        });
        this.emitChanges();
      });
    });
  }

  addByUrl() {
    if (!this.urlInput) return;
    this.http.post<ListingPhoto>(
      `/api/listings/${this.listingId}/photos/upload-by-url`,
      { url: this.urlInput }
    ).subscribe(photo => {
      this.photos.push(photo);
      this.urlInput = '';
      this.showUrlInput = false;
      this.emitChanges();
    });
  }

  setCover(photoId: string) {
    this.http.post(`/api/listings/${this.listingId}/photos/${photoId}/cover`, {})
      .subscribe(() => {
        this.photos.forEach(p => p.isCover = false);
        const photo = this.photos.find(p => p.id === photoId);
        if (photo) photo.isCover = true;
        this.emitChanges();
      });
  }

  moveUp(index: number) {
    if (index === 0) return;
    [this.photos[index - 1], this.photos[index]] = [this.photos[index], this.photos[index - 1]];
    this.emitChanges();
  }

  moveDown(index: number) {
    if (index >= this.photos.length - 1) return;
    [this.photos[index], this.photos[index + 1]] = [this.photos[index + 1], this.photos[index]];
    this.emitChanges();
  }

  delete(photoId: string) {
    this.http.delete(`/api/listings/${this.listingId}/photos/${photoId}`)
      .subscribe(() => {
        this.photos = this.photos.filter(p => p.id !== photoId);
        this.emitChanges();
      });
  }

  emitChanges() {
    // Parent component can listen if needed
  }
}
