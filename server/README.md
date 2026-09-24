# Серверная часть «Каптёрка ПРО»

| Что | Где | Файлы |
|---|---|---|
| Оплата, лицензии, реестр бойцов, вступление в подразделение | Cloudflare Worker `kapterka-api` | `cloudflare/worker.mjs`, `cloudflare/wrangler.toml` |
| Правила доступа к базе | Firebase Firestore, проект `kapterka-pro` | `firestore/transition.rules`, `firestore/strict.rules` |

Секретов в репозитории нет и быть не должно.

## Как это защищено

- **Оплата.** Цену задаёт сервер. После оплаты сервер сам спрашивает ЮKassa, сверяет сумму,
  товар и бойца. Один платёж даёт одну лицензию, повторная проверка ничего не продлевает.
- **Лицензии.** Записи лежат в `srv_licenses` / `srv_fighters`, и приложения туда не имеют доступа.
  Старые коллекции `licenses` / `fighters` сервер не считает достоверными.
- **Синхронизация.** Каждый телефон получает анонимную личность Firebase. Сервер (`unit_join`,
  с ограничением числа попыток) записывает его участником подразделения. При строгих
  правилах (фаза B) данные подразделения доступны только его участникам.
- **Админ.** В приложении нет пароля. Сервер сверяет SHA-256 и выдаёт сессию на 15 минут.
  После 5 ошибок вход блокируется.
- Открытый ретранслятор в Telegram (`send_telegram`) убран.

## Разовая настройка (владелец, через панели, секреты не пересылать в чат)

1. **Firebase → Authentication → Sign-in method → Anonymous → Включить.**
2. **Firebase → Настройки проекта → Сервисные аккаунты → «Создать закрытый ключ».**
   Скачается JSON-файл. Он нужен в двух местах:
   - GitHub → Settings → Secrets and variables → Actions → `FIREBASE_SERVICE_ACCOUNT`
     (вставить содержимое файла целиком);
   - Cloudflare → Workers → kapterka-api → Settings → Variables and Secrets →
     Secret `FIREBASE_SERVICE_ACCOUNT_JSON` (то же содержимое).
   После этого удалите файл с телефона или компьютера.
3. Cloudflare → kapterka-api → Secrets:
   - `ADMIN_PASSWORD` — пароль админ-панели, не короче 12 символов (либо, для
     продвинутых, `ADMIN_API_SECRET_SHA256` + `ADMIN_SESSION_SECRET`);
   - по желанию: `BREVO_API_KEY` + `EMAIL_SENDER_EMAIL` (письма с ключом),
     `TG_BOT_TOKEN` + `TG_ADMIN_CHAT_ID` (уведомления).
4. Cloudflare → My Profile → API Tokens → Create Token → шаблон **«Edit Cloudflare Workers»**.
   В GitHub Secrets добавить `CLOUDFLARE_API_TOKEN` (токен) и `CLOUDFLARE_ACCOUNT_ID`
   (Account ID с главной страницы Workers).

## Выкладка

- **Сервер:** Actions → «kapterka-api (Cloudflare Worker)» → Run workflow → deploy = true.
  Перед выкладкой прогоняются тесты. После выкладки выполняется живая проверка.
  Ключи для 3.5.0 не меняются.
- **Правила, фаза A:** Actions → «Firestore rules» → phase = `transition`. Выкладывается вместе с 3.6.
- **Правила, фаза B:** phase = `strict`. Только по команде владельца, когда почти все
  пользователи обновились. После этого версии ≤3.5.0 перестанут синхронизироваться,
  но их данные на телефонах сохранятся.

## Проверки

```
node server/cloudflare/worker.test.mjs                  # сервер, без сети
npx firebase emulators:exec --only firestore \
  --config server/firestore/firebase.json --project demo-kapterka \
  "node server/firestore/rules.test.mjs"                 # правила в эмуляторе
```
