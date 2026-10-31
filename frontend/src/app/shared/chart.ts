import { Component, effect, ElementRef, input, OnDestroy, viewChild } from '@angular/core';
import Chart, { ChartConfiguration } from 'chart.js/auto';

export const CHART_COLORS = ['#0b5cad', '#0e9f8e', '#f29d38', '#c62828', '#7e57c2', '#26a69a', '#8d6e63', '#5c6bc0', '#ec407a'];

/** Thin wrapper around Chart.js; re-renders whenever the config input changes. */
@Component({
  selector: 'cc-chart',
  template: `<div class="box" [style.height]="height()"><canvas #canvas></canvas></div>`,
  styles: `.box { position: relative; width: 100%; }`,
})
export class ChartComponent implements OnDestroy {
  readonly config = input.required<ChartConfiguration>();
  readonly height = input('260px');
  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private chart?: Chart;

  constructor() {
    effect(() => {
      const config = this.config();
      this.chart?.destroy();
      this.chart = new Chart(this.canvas().nativeElement, {
        ...config,
        options: { responsive: true, maintainAspectRatio: false, ...config.options },
      });
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }
}
