# Web Auth v2 — развёртывание

Этот модуль заменяет браузерную регистрацию на вход по одноразовому 6-значному коду из Email.

## Что уже подготовлено
- backend: `server/google-apps-script-auth.js`
- frontend: `docs/auth-v2.js`
- config: `docs/auth-config.js`
- пока URL в auth-config.js пустой, старая авторизация продолжает работать.

## Публикация Google Apps Script
1. Открыть Google Таблицу, к которой будет привязана база аккаунтов.
2. Расширения → Apps Script.
3. Заменить содержимое Code.gs кодом из `server/google-apps-script-auth.js`.
4. Нажать Deploy → New deployment → Web app.
5. Execute as: Me.
6. Who has access: Anyone.
7. Скопировать URL, заканчивающийся на `/exec`.
8. В `docs/auth-config.js` вставить этот URL в `window.KAPTERKA_AUTH_API_URL`.
9. Открыть сайт и проверить регистрацию на тестовом Email.

Секреты Telegram хранятся только в Script Properties:
- TG_BOT_TOKEN
- TG_ADMIN_CHAT_ID

Никогда не размещать токены в GitHub или в папке docs.
