import { Component, computed, input, Pipe, PipeTransform } from '@angular/core';
import { label, tone } from '../core/util';

/** Coloured pill for an enum value, e.g. <cc-status value="PENDING_VERIFICATION" />. */
@Component({
  selector: 'cc-status',
  template: `<span class="chip" [class]="'chip ' + toneClass()">{{ text() }}</span>`,
})
export class StatusChip {
  readonly value = input<string | null | undefined>();
  readonly text = computed(() => label(this.value()));
  readonly toneClass = computed(() => tone(this.value()));
}

/** "IN_PROGRESS" -> "In progress". */
@Pipe({ name: 'label' })
export class LabelPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return label(value);
  }
}
