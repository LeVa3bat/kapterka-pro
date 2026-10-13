const {chromium} = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const base = process.env.PAGE_BASE_URL || 'http://127.0.0.1:4173';
const evidence = process.env.QA_OUTPUT || 'growth/qa';
(async () => {
  fs.mkdirSync(evidence,{recursive:true});
  const browser = await chromium.launch({headless:true,executablePath:process.env.CHROME_BIN || (process.platform === 'win32' ? 'C:/Program Files/Google/Chrome/Application/chrome.exe' : '/usr/bin/google-chrome')});
  try {
    for (const [device,viewport] of [['mobile',{width:412,height:915}],['desktop',{width:1280,height:800}]]) {
      const context = await browser.newContext({viewport,acceptDownloads:true});
      await context.route('**/*',route=>new URL(route.request().url()).origin === base ? route.continue() : route.abort());
      const page = await context.newPage(); const errors=[]; page.on('pageerror',error=>errors.push(error.message));
      for (const file of ['','uchet-instrumenta-android.html','inventarizaciya-na-android.html','peremeshchenie-mezhdu-skladami-android.html']) {
        await page.goto(base+'/'+file); await page.locator('h1').waitFor();
        assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),device+': horizontal overflow on '+file);
        await page.screenshot({path:evidence+'/'+device+'-'+(file.replace('.html','')||'home')+'.png',fullPage:true});
      }
      await page.goto(base+'/');
      for (const tab of ['tabCabinet','tabPayment','tabSync','tabDownload','tabOverview']) {
        await page.evaluate(id=>window.switchMainTab(id),tab);
        await page.locator('#'+tab+'.active').waitFor({state:'visible'});
      }
      await page.getByRole('button',{name:'Открыть в RuStore',exact:true}).first().click();
      await page.locator('#modalRuStore').waitFor({state:'visible'});
      await page.evaluate(()=>window.closeModal('modalRuStore'));
      await page.goto(base+'/inventarizaciya-na-android.html#reconcile');
      const fields = page.locator('.reconcile-row').first();
      for (const [key,value] of Object.entries({name:'=HYPERLINK("вымышленный пример")',unit:'шт.',book:'10',actual:'8'})) await fields.locator('[data-field="'+key+'"]').fill(value);
      await page.getByRole('button',{name:'Рассчитать',exact:true}).click();
      assert.match(await page.locator('#reconcile-results').innerText(),/недостача 2/);
      const download = page.waitForEvent('download'); await page.locator('#reconcile-export').click();
      const data=fs.readFileSync(await (await download).path(),'utf8'); assert.ok(data.includes('"\'=HYPERLINK')); assert.ok(data.includes('"-2"'));
      await fields.locator('[data-field="actual"]').fill('-1'); assert.equal(await page.locator('#reconcile-export').isDisabled(),true);
      await page.getByRole('button',{name:'Рассчитать',exact:true}).click(); assert.match(await page.locator('#reconcile-status').innerText(),/Введите число/);
      await fields.locator('[data-field="actual"]').fill('10'); await page.getByRole('button',{name:'Рассчитать',exact:true}).click(); assert.match(await page.locator('#reconcile-results').innerText(),/совпадает/);
      await page.evaluate(()=>{for(let i=0;i<99;i++)document.getElementById('reconcile-add').click();});
      assert.equal(await page.locator('.reconcile-row').count(),100); assert.equal(await page.locator('#reconcile-add').isDisabled(),true);
      await page.locator('.reconcile-row').last().getByRole('button',{name:'Убрать'}).click(); assert.equal(await page.locator('#reconcile-add').isDisabled(),false);
      await page.locator('#reconcile-clear').click(); assert.equal(await page.locator('.reconcile-row').count(),1);
      // Сохраняем только наши тестовые запросы аналитики; системное меню заменено тестовым API.
      await page.goto(base+'/uchet-instrumenta-android.html#workshop-kit');
      await page.evaluate(()=>{window.testShares=[];window.testEvents=[];window.trackPublicTool=name=>window.testEvents.push(name);Object.defineProperty(navigator,'share',{configurable:true,value:async payload=>{window.testShares.push(payload);}});});
      await page.getByRole('button',{name:'Поделиться пустым шаблоном',exact:true}).click();
      await page.getByRole('button',{name:'Посоветовать приложение',exact:true}).click();
      const shares=await page.evaluate(()=>({payloads:window.testShares,events:window.testEvents}));
      assert.equal(shares.payloads.length,2); assert.deepEqual(shares.events,['share_menu_requested','share_menu_requested']);
      assert.ok(shares.payloads.every(p=>p.url.startsWith('https://kapterka-pro.ru/')&&!p.files));
      assert.deepEqual(errors,[],device+': browser errors');
      await context.close();
    }
    for (const file of ['workshop-blank.xlsx','workshop-example.xlsx','workshop-blank.pdf','workshop-example.pdf','inventory-checklist.pdf']) {
      const response = await fetch(base+'/downloads/'+file); assert.equal(response.status,200); assert.ok((await response.arrayBuffer()).byteLength>1000);
    }
    console.log('PASS: mobile/desktop layout, tabs, RuStore modal, calculator/CSV/validation/100 rows, safe share payloads, five downloads.');
  } finally { await browser.close(); }
})().catch(error=>{console.error(error);process.exitCode=1;});
