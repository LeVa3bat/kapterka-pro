// Создаёт небольшие превью; оригиналы остаются для просмотра крупным планом.
const fs = require('node:fs');
const sharp = require('sharp');
(async () => {
  for (const file of fs.readdirSync('docs/screens').filter(name => /^36-[2-8]-.+\.jpg$/.test(name))) {
    const target = 'docs/screens/' + file.replace('.jpg','-preview.webp');
    await sharp('docs/screens/'+file).resize({width:540}).webp({quality:82}).toFile(target);
    console.log(target+': '+fs.statSync(target).size+' bytes');
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
