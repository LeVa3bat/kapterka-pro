const {chromium} = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const base = (process.env.PAGE_BASE_URL || 'http://127.0.0.1:4173').replace(/\/$/,'');
const origin = new URL(base).origin;
const evidence = process.env.QA_OUTPUT || 'growth/qa';
const scenarios = [
  ['uchet-vydachi-imushchestva-android.html','issue-demo','issue-check',4],
  ['inventarizaciya-na-android.html','stock-demo','offline-stock',3],
  ['uchet-instrumenta-android.html','report-demo','report-export',3],
];

(async () => {
  fs.mkdirSync(evidence,{recursive:true});
  const report = {checkedAt:new Date().toISOString(),base,analyticsRequestsBlocked:true,checks:[]};
  const browser = await chromium.launch({headless:true,executablePath:process.env.CHROME_BIN || (process.platform === 'win32' ? 'C:/Program Files/Google/Chrome/Application/chrome.exe' : '/usr/bin/google-chrome')});
  try {
    for (const [device,viewport] of [['mobile',{width:360,height:780}],['desktop',{width:1280,height:900}]]) {
      const context = await browser.newContext({viewport,acceptDownloads:true});
      await context.route('**/*',route => new URL(route.request().url()).origin === origin ? route.continue() : route.abort());
      const page = await context.newPage();
      const errors=[];
      page.on('pageerror',error => errors.push(error.message));
      for (const [file,section,name,cueCount] of scenarios) {
        const requests=[];
        const listener = request => {if (request.url().endsWith('.mp4')) requests.push(request.url());};
        page.on('request',listener);
        await page.goto(base+'/'+file,{waitUntil:'networkidle'});
        assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth+1),device+': horizontal overflow on '+file);
        const video=page.locator('#'+section+' video');
        const setup=await video.evaluate(node => ({controls:node.controls,playsInline:node.playsInline,preload:node.preload,autoplay:node.autoplay,paused:node.paused,poster:node.poster,tracks:node.querySelectorAll('track').length}));
        assert.equal(setup.controls,true);
        assert.equal(setup.playsInline,true);
        assert.equal(setup.preload,'none');
        assert.equal(setup.autoplay,false);
        assert.equal(setup.paused,true);
        assert.equal(setup.tracks,1);
        assert.ok(setup.poster.endsWith('/videos/'+name+'-poster.webp'));
        assert.deepEqual(requests,[],device+': MP4 was fetched before play on '+file);
        const details=page.locator('#'+section+' details');
        await details.locator('summary').click();
        assert.equal(await details.locator('li').count(),cueCount);
        await details.locator('summary').click();
        // Centre the small player below the site's sticky header for the visual record.
        await video.evaluate(node => node.scrollIntoView({block:'center',behavior:'instant'}));
        await video.screenshot({path:evidence+'/'+device+'-'+name+'-video.png'});
        await video.evaluate(node => node.play());
        await page.waitForFunction(id => {
          const node=document.querySelector('#'+id+' video');
          return node?.readyState >= 2 && node.textTracks[0]?.cues?.length > 0;
        },section);
        const playback=await video.evaluate(node => {
          node.pause();
          const track=node.textTracks[0];
          return {duration:node.duration,width:node.videoWidth,height:node.videoHeight,error:node.error?.message ?? null,cues:[...track.cues].map(cue => ({start:cue.startTime,end:cue.endTime,text:cue.text})),captionMode:track.mode};
        });
        assert.equal(playback.error,null,device+': video decode failed on '+file);
        assert.equal(playback.duration,30);
        assert.equal(playback.width,1080);
        assert.equal(playback.height,1920);
        assert.equal(playback.cues.length,cueCount);
        assert.equal(playback.cues[0].start,0);
        assert.equal(playback.cues.at(-1).end,30);
        assert.equal(playback.captionMode,'showing');
        assert.ok(playback.cues.every(cue => cue.text.length>0 && cue.end>cue.start));
        await video.evaluate(node => new Promise((resolve,reject) => {
          node.addEventListener('seeked',resolve,{once:true});
          node.addEventListener('error',() => reject(new Error('Video seek failed')),{once:true});
          node.currentTime=25;
        }));
        assert.equal(await video.evaluate(node => node.currentTime),25);
        assert.ok(requests.every(url => new URL(url).origin===origin));
        report.checks.push({device,file,section,video:name+'.mp4',noVideoRequestBeforePlay:true,durationSeconds:playback.duration,dimensions:[playback.width,playback.height],russianSubtitleCues:playback.cues.length,seekSeconds:25,horizontalOverflow:false});
        page.off('request',listener);
      }
      assert.deepEqual(errors,[],device+': browser errors');
      await context.close();
    }
    report.status='PASS';
    fs.writeFileSync(evidence+'/site-videos-browser.json',JSON.stringify(report,null,2)+'\n');
    console.log('PASS: 3 local videos on mobile/desktop, no MP4 preload/autoplay, playback and seeking, Russian WebVTT, transcripts, responsive layout.');
  } finally {await browser.close();}
})().catch(error => {console.error(error);process.exitCode=1;});
