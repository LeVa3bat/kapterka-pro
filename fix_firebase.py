with open('app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('Подключение к каналу подразделения', 'Подключение к каналу')
content = content.replace('базы подразделения', 'удаленной базы')
content = content.replace('База подразделения синхронизирована', 'База синхронизирована')

with open('app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt', 'w', encoding='utf-8') as f:
    f.write(content)
