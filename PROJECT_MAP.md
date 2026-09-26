# Карта проекта «Каптёрка PRO»

Этот файл описывает, что где лежит. Он нужен только для навигации: на сайт, в приложение и в сборку не попадает.
Актуальная версия релиза — в `docs/release.json` (на момент составления карты: **3.6.1**, build **34**, Room DB **3**).
Строка «Текущий релиз» в `README.md` приведена в соответствие с этой версией.

## Корень репозитория

| Файл / папка | Назначение |
|---|---|
| `app/` | Android-приложение (Kotlin, Jetpack Compose, Room) |
| `docs/` | Сайт. **Эту папку раздаёт GitHub Pages**, всё, что в неё попадает, становится публичным |
| `server/` | Серверная часть: Cloudflare Worker и правила Firestore |
| `tools/` | Служебные Java-скрипты для генерации иконок и скриншотов |
| `gradle/` | Обёртка Gradle и каталог версий зависимостей (`libs.versions.toml`) |
| `.github/` | Автоматизация: workflow'ы GitHub Actions, скрипты проверок, аудиты |
| `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` | Настройки сборки Android-проекта |
| `package.json`, `package-lock.json` | Зависимости для скриптов проверки на Node.js |
| `metadata.json` | Название и описание проекта (служебный файл платформы, где создавалось приложение) |
| `sitemap.xml` | Карта сайта в корне. Сайт использует свою версию `docs/sitemap.xml`, они не совпадают |
| `RELEASE_NOTES_v3.4.8.md`, `v3.4.9.md`, `v3.5.0.md`, `v3.7.0.md` | Заметки к релизам |
| `.env.example` | Образец переменных окружения. Настоящий `.env` не хранится в git |
| `.gitignore` | Список того, что нельзя коммитить (ключи, `.env`, сборки, `*.apk` кроме `docs/*.apk`) |

## `app/`: приложение

| Путь | Что там |
|---|---|
| `app/build.gradle.kts` | Настройки модуля: `applicationId`, `versionCode`/`versionName`, подпись релиза, тестовая сборка `nextsafe` |
| `app/proguard-rules.pro` | Правила сжатия и обфускации кода |
| `app/src/main/java/com/example/` | Исходный код |
| `…/data/local` | Локальная база (Room) |
| `…/data/model` | Модели данных |
| `…/data/repository` | Репозиторий: единая точка доступа к данным |
| `…/data/sync` | Синхронизация между устройствами |
| `…/data/license` | Лицензии и PRO-доступ |
| `…/data/payment` | Оплата |
| `…/data/auth` | Авторизация |
| `…/data/admin` | Админ-функции |
| `…/data/notification` | Уведомления |
| `…/ui/screens` | Экраны |
| `…/ui/components` | Переиспользуемые элементы интерфейса |
| `…/ui/theme` | Тема оформления |
| `…/ui/viewmodel` | Состояние экранов (ViewModel) |
| `…/util` | Вспомогательный код |
| `app/src/main/res/` | Ресурсы: иконки (`mipmap-*`), `drawable`, `values`, `xml` |
| `app/src/test/` | Модульные тесты и снимки экранов |
| `app/src/androidTest/` | Тесты на устройстве |

## `docs/`: сайт (публикуется)

| Что | Файлы |
|---|---|
| Главная и служебные страницы | `index.html`, `404.html`, `help.html`, `guides.html`, `updates.html`, `pricing.html`, `privacy.html`, `terms.html`, `security.html`, `approve.html` |
| SEO-страницы и инструкции | остальные `*.html` (учёт склада, инвентаризация, перенос на новый телефон, резервная копия и т.д.) |
| Стили | `style.css`, `site36.css`, `tabs_and_cabinet.css`, `guide.css` |
| Скрипты | `app.js`, `auth-v2.js`, `auth-config.js`, `site36.js`, `guide-analytics.js`, `release-meta.js` |
| Данные о релизе | `release.json` (версия, размер и хэши APK), `version.json` (версия сайта) |
| APK для скачивания | `kapterka-pro.apk` (подписанный релиз) |
| Картинки | `screens/` (скриншоты), `app_icon_512.png`, `icon_*.png`, `rustore_*`, `favicon.svg` |
| Для поисковиков | `robots.txt`, `sitemap.xml`, `site.webmanifest`, `google*.html`, `*.txt`-файл проверки |
| Домен | `CNAME` |
| `.well-known/` | `security.txt`: контакт для сообщений об уязвимостях |

## `server/`: серверная часть

Подробнее в `server/README.md`.

| Путь | Что там |
|---|---|
| `server/cloudflare/worker.mjs` | Cloudflare Worker `kapterka-api`: оплата, лицензии, вход, админ |
| `server/cloudflare/worker.test.mjs` | Тесты Worker'а (работают без сети) |
| `server/cloudflare/wrangler.toml` | Настройки развёртывания Worker'а |
| `server/firestore/` | Правила базы Firestore (`transition.rules`, `strict.rules`), их тесты и конфиг эмулятора |
| `server/google-apps-script-*.js` | Скрипты Google Apps Script для почты и входа |
| `server/yookassa-webhook.js` | Обработчик платежей ЮKassa |
| `server/WEB_AUTH_DEPLOY.md` | Заметки по развёртыванию входа |

## `tools/graphics/`

Java-скрипты для генерации иконок (`Generate*Icon`, `Render*Icon`) и скриншотов (`Generate*Screenshots`). На работу приложения не влияют.

## `.github/`

| Путь | Что там |
|---|---|
| `workflows/` | Сборки, тесты и проверки в GitHub Actions: `android-*` (компиляция, тесты, тестовый APK), `release-*` (релизы), `kapterka-api` (Worker), `firestore-rules*`, `site-*` (проверки сайта), `indexnow` (уведомление поисковиков) |
| `scripts/` | Скрипты проверок и обновления сайта под релиз (`site-smoke`, `android-invariants`, `lighthouse-budget`, `update-release-*`) |
| `release-apk-baseline.json` | Эталон для проверки APK |
| `*_AUDIT.md`, `SEO_RESEARCH_*.md` | Отчёты и исследования |

## Что нельзя трогать без отдельного решения

- Подпись релиза, `applicationId`, `versionCode`. Иначе новая версия не установится поверх старой.
- Ключи и пароли (`*.keystore`, `*.jks`, `.env`) не коммитим.
- `docs/kapterka-pro.apk` и `docs/release.json` меняются только при настоящем релизе.
- Правила Firestore, фаза `strict`: только по команде владельца.

## Проверки без сети (Node.js)

```
node server/cloudflare/worker.test.mjs
node .github/scripts/site-smoke.mjs
node .github/scripts/android-invariants.mjs
```

Сборка APK и тесты приложения выполняются в GitHub Actions. Локально для них нужны Android Studio и Android SDK.
