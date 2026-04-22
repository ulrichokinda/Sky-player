import { chromium } from 'playwright-chromium';
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  
  page.on('console', msg => console.log('BROWSER CONSOLE:', msg.type(), msg.text()));
  page.on('pageerror', err => console.log('BROWSER ERROR:', err.message));
  
  await page.goto('http://localhost:3000');
  
  // wait 2s
  await new Promise(r => setTimeout(r, 2000));
  
  const content = await page.content();
  console.log(content.substring(0, 500));
  
  // Also check if ErrorBoundary red screen is shown
  const rootHtml = await page.evaluate(() => document.getElementById('root')?.innerHTML);
  console.log('ROOT HTML LENGTH:', rootHtml?.length);
  
  await browser.close();
})();
