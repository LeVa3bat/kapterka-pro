# Web Auth v2 — развёртывание

Этот модуль заменяет браузерную регистрацию на вход по одноразовому 6-значному коду из Email.

## Что уже подготовлено
- backend: `server/google-apps-script-auth.js`
- frontend: `docs/auth-v2.js`
- config: `docs/auth-config.js`
- Google Таблица НЕ нужна.
- пока URL в auth-config.js пустой, старая авторизация продолжает работать.

## Публикация standalone Google Apps Script
1. Открыть https://script.google.com и создать новый проект.
2. В файле `Code.gs` удалить пример `myFunction`.
3. Вставить код из `server/google-apps-script-auth.js`.
4. Сохранить проект.
5. Нажать `Начать развертывание` → `Новое развертывание`.
6. Тип: `Веб-приложение`.
7. Выполнять от имени: `Я`.
8. Доступ: `Все` / `Anyone`.
9. Разрешить доступ Google Apps Script к отправке Email.
10. Скопировать URL, заканчивающийся на `/exec`.
11. Передать этот URL в чат — он будет вставлен в `docs/auth-config.js` через GitHub.

## Хранение данных
Пользователи, одноразовые коды и сессии хранятся в Script Properties.
Пароли не используются и не хранятся.

## Telegram
Если нужны служебные уведомления, токен и chat id хранятся только в Script Properties:
- TG_BOT_TOKEN
- TG_ADMIN_CHAT_ID

Никогда не размещать токены в GitHub или в папке docs.
