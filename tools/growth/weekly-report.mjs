// Только вручную переданные агрегаты; не читает ключи, контакты, склад и платёжные записи.
import fs from 'node:fs';
const input = process.argv[2] || 'growth/measurement-input.example.json';
const output = process.argv[3] || 'growth/WEEKLY_REPORT.md';
const data = JSON.parse(fs.readFileSync(input,'utf8'));
const labels = {siteVisits:'Визиты сайта',templateDownloadClicks:'Клики скачивания шаблонов',calculatorUses:'Успешные расчёты',rustoreClicks:'Переходы в RuStore',apkClicks:'Клики APK',shareMenuRequests:'Запросы системного меню отправки',rustoreInstalls:'Установки по данным RuStore',demoStarts:'Начала демо',confirmedPayments:'Подтверждённые оплаты'};
if (!/^\d{4}-\d{2}-\d{2}$/.test(data.periodStart) || !/^\d{4}-\d{2}-\d{2}$/.test(data.periodEnd) || data.periodStart>data.periodEnd) throw new Error('Invalid period');
const clean = text=>String(text).replace(/[\r\n|]/g,' ');
const lines = ['# Недельный отчёт','',`Период: ${data.periodStart} — ${data.periodEnd}. Сформирован из разрешённых агрегатов; отсутствие доступа обозначено явно.`,'','| Показатель | Значение | Источник |','| --- | --- | --- |'];
for (const [key,label] of Object.entries(labels)) {
  const metric=data.metrics?.[key];
  if (!metric || (metric.value!==null && (!Number.isInteger(metric.value)||metric.value<0)) || !metric.source) throw new Error('Invalid aggregate '+key);
  lines.push('| '+label+' | '+(metric.value===null?'Нет данных':metric.value)+' | '+clean(metric.source)+' |');
}
lines.push('','Клик скачивания не подтверждает получение файла. Переход в RuStore или APK не подтверждает установку. Запрос меню не подтверждает его открытие или отправку. Сквозная атрибуция установок, демо и оплат к UTM не установлена.','', '## Ошибки и недостающие данные','',...data.errors.map(error=>'- '+clean(error)),'','## Одно следующее улучшение','',clean(data.nextImprovement),'');
fs.writeFileSync(output,lines.join('\n'));
console.log('Report saved: '+output+'; missing values remain null.');
