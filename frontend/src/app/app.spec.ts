import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { App } from './app';

describe('App', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);

  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    httpTesting.expectOne('/api/transactions').flush([]);

    expect(fixture.componentInstance).toBeTruthy();
    fixture.destroy();

  });

  it('should render the Transaction Hub dashboard title', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    httpTesting.expectOne('/api/transactions').flush([]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('h1')?.textContent?.trim()).toBe(
      'Transactions',
    );

    fixture.destroy();

  });

  it('should display an error when loading transactions fails', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    httpTesting.expectOne('/api/transactions').flush('Server unavailable', {
      status: 500,
      statusText: 'Internal Server Error',
    });

    fixture.detectChanges();

    expect(fixture.componentInstance.errorMessage).toBe(
      'Could not load transactions. Check that the backend is running.',
    );

    fixture.destroy();

  });
});
