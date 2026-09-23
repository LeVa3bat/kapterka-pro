# Склад ПРО — отдельный backend

Эта папка относится только к новому приложению Склад ПРО (com.aistudio.skladpro).

Она не должна использовать Firebase-проект, ключи, лицензии, коллекции или YooKassa-настройки рабочей «Каптёрки ПРО».

## Архитектура

### Регистрация

Android использует отдельный Firebase Authentication проект с Email/Password.

Backend не получает пароль пользователя. Android получает Firebase ID token, а backend проверяет его через Firebase Admin SDK. Защищённые API-методы принимают только аккаунт с подтверждённым email; до подтверждения сервер возвращает `EMAIL_VERIFICATION_REQUIRED`.

### Демо

При первом успешном POST /v1/bootstrap backend создаёт entitlement со статусом trial, серверным временем начала и окончанием через SKLAD_DEMO_DAYS дней.

Повторный bootstrap не перезапускает демо.

### Подписка

Планы и цены задаются только на сервере:

- SKLAD_PRO_MONTH_PRICE_RUB
- SKLAD_PRO_YEAR_PRICE_RUB

Android передаёт только planId.

YooKassa Shop ID и Secret Key существуют только на backend:

- SKLAD_YOOKASSA_SHOP_ID
- SKLAD_YOOKASSA_SECRET_KEY

Платёж создаётся через POST /v1/payments. Backend сохраняет привязку paymentId → uid → planId → expectedAmount.

Успешный платёж подтверждается сервером через API YooKassa. Webhook сам по себе не считается доказательством оплаты: backend повторно читает платёж из YooKassa и сверяет status, uid, planId, сумму и валюту.

Повторная обработка одного paymentId идемпотентна: grantApplied не позволяет второй раз начислить срок.

Если действующая подписка ещё не закончилась, новый срок добавляется к текущему paidUntil.

### Синхронизация

Планируемая структура Firestore:

~~~
users/{uid}
entitlements/{uid}
payments/{paymentId}

workspaces/{workspaceId}
workspaces/{workspaceId}/members/{uid}
workspaces/{workspaceId}/items/{itemId}
workspaces/{workspaceId}/stocks/{stockId}
workspaces/{workspaceId}/operations/{operationId}
workspaces/{workspaceId}/requisitions/{requestId}
workspaces/{workspaceId}/tombstones/{tombstoneId}
~~~

У каждого рабочего пространства отдельный список участников. Сервер создаёт workspace и первого member с ролью owner.

Клиентские правила не позволяют менять entitlement, payment или members.

Операции учёта предполагаются неизменяемыми после записи. Коррекции должны оформляться новой операцией.

Удаления синхронизируем через tombstone-подход, чтобы удалённая позиция не возвращалась с другого устройства.

## Переменные окружения

Firebase:

~~~
SKLAD_FIREBASE_PROJECT_ID=sklad-pro-a1ec0
~~~

Backend принимает только отдельный проект `sklad-pro-a1ec0`. В Cloud Run доступ к Firebase/Firestore выполняется через service identity и Application Default Credentials. Долгоживущий JSON-ключ service account в переменных окружения не используется. Для локальной разработки допустим стандартный `GOOGLE_APPLICATION_CREDENTIALS`, но файл ключа нельзя добавлять в Git.

YooKassa:

~~~
SKLAD_YOOKASSA_SHOP_ID=
SKLAD_YOOKASSA_SECRET_KEY=
SKLAD_PRO_MONTH_PRICE_RUB=
SKLAD_PRO_YEAR_PRICE_RUB=
SKLAD_PAYMENT_RETURN_URL=skladpro://payment_success
~~~

Демо:

~~~
SKLAD_DEMO_DAYS=7
~~~

Если для магазина требуется формирование чека через YooKassa:

~~~
SKLAD_RECEIPT_VAT_CODE=
~~~

Значение НДС нельзя выбирать наугад: оно должно соответствовать реальной налоговой настройке продавца.

## API

GET /health — готовность Firebase, YooKassa и планов без выдачи секретов.

GET /v1/plans — публичные планы и цены.

POST /v1/bootstrap — инициализация пользователя и демо. Требует Firebase Bearer token.

GET /v1/me — entitlement текущего пользователя. Требует Firebase Bearer token. Поле `status` вычисляется по серверному времени: `pro`, `trial` или `expired`; устаревшее сохранённое значение не даёт доступ после истечения срока.

POST /v1/workspaces — создание рабочего пространства. Требует Firebase Bearer token.

POST /v1/devices — регистрация или heartbeat текущей Android-установки. `uid` берётся только из Firebase token.

GET /v1/devices — список устройств текущего аккаунта.

DELETE /v1/devices/{installationId} — удалить устройство из списка аккаунта.

POST /v1/payments — создание платежа. Требует Firebase Bearer token. Рекомендуется UUID v4 в Idempotence-Key.

GET /v1/payments/status?payment_id=... — серверная проверка платежа.

POST /v1/webhooks/yookassa — webhook YooKassa с повторной серверной проверкой платежа.

## Release gate

До включения реальной синхронизации и оплаты в Android обязательно:

1. Создать отдельный Firebase project для Склад ПРО.
2. Включить Email/Password Authentication.
3. Развернуть candidate Firestore rules и прогнать emulator tests.
4. Развернуть отдельный backend URL.
5. Проверить /health.
6. Настроить реальные планы и цены на сервере.
7. Настроить YooKassa webhook.
8. Провести тестовый платёж end-to-end.
9. Проверить повторную обработку одного paymentId.
10. Проверить продление активной подписки.
11. Проверить истечение demo по серверному времени.
12. Только после этого добавить новый backend URL и Firebase-конфигурацию в release Android.

Никакие секреты из этой схемы не должны попадать в APK или Git.
