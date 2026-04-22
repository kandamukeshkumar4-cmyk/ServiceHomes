import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-host-response-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './host-response-form.component.html',
  styles: [`
    .response-box {
      border: 1px solid var(--surface-border, #dfe7ef);
    }
  `]
})
export class HostResponseFormComponent {
  @Input() submitting = false;
  @Input() errorMessage = '';
  @Output() submitted = new EventEmitter<string>();
  @Output() cancelled = new EventEmitter<void>();

  response = '';

  submit() {
    const normalizedResponse = this.response.trim();
    if (!normalizedResponse || this.submitting) {
      return;
    }

    this.submitted.emit(normalizedResponse);
  }
}
