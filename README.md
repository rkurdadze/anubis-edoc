# eDocument Export Service (TEST)

Spring Boot 3 приложение, инкапсулирующее SOAP eDocument Export Service и предоставляющее REST-обёртку для внутренних систем.

## Основные возможности
- Управление сессией LogOn/LogOut, автоматический ре-логин при сбросе сессии.
- Получение списка документов и детальной информации (файлы, связи).
- Пометка документа как экспортированного.
- Поиск контактов: физические лица, организации, госструктуры.
- REST API `/api/edoc/**` с JSON-ответами.

## Конфигурация
Настройки в `application.yml` или переменных окружения:

| Свойство | Описание | По умолчанию |
| --- | --- | --- |
| `edoc.base-url` | Базовый портал | `https://edocument.ge/TEST/` |
| `edoc.export-service-url` | SOAP endpoint (BasicHttpBinding) | `https://edocument.ge/TEST/integrationservice/eDocumentExportService.svc/bhb` |
| `edoc.wsdl-url` | WSDL | `https://edocument.ge/TEST/integrationservice/eDocumentExportService.svc?wsdl` |
| `edoc.client-auth-token` | Токен клиента | `{BD081743-C0C4-43B6-A0C3-30914FC9888F}` |
| `edoc.service-version` | Версия сервиса | `1.0.0.0` |
| `edoc.connect-timeout-millis` | Таймаут подключения | `10000` |
| `edoc.read-timeout-millis` | Таймаут чтения | `60000` |

## Сборка и запуск

Docker:
```bash
    docker build --platform linux/amd64 -t anubis/edoc -f Dockerfile .
```

Save:
```bash
    docker save -o anubis-edoc.tar anubis/edoc
```

#### -> Отправьте эти файлы на сервер и загрузите
```bash
    docker load -i /var/www/anubis-edoc.tar
```

Run:
```bash
    docker run -d \
        -p 4102:4102 \
        --name edoc-service \
        --network anubis-net \
        -e EDOC_CLIENT_AUTH_TOKEN="{A362EFA8-3C36-473F-B79C-5EAA65B68EF6}" \
        -e EDOC_DB_URL=jdbc:postgresql://postgis:5432/edoc \
        -e EDOC_DB_USER=postgres \
        -e EDOC_DB_PASSWORD=saadmin \
        anubis/edoc
```
или `docker-compose up --build`.

## REST API
REST-обёртка предоставляет прикладные эндпоинты `/api/edoc/**`; управление `sessionId` выполняется сервисом автоматически (явный `logon` endpoint не используется).
- `POST /api/edoc/session/logout` — явный LogOut текущей активной сессии.
- `GET /api/edoc/documents?type=INCOMING&from=2024-01-01&to=2024-01-31` — GetDocuments (обязательны тип и период; фильтр по связанному контакту `contactType + contactId` опционален и учитывается только если заданы оба параметра).
- `GET /api/edoc/documents/{id}` — детали документа **только из локального кэша** (`404 EDOC_NOT_CACHED`, если документа нет в кэше).
- `POST /api/edoc/documents/{id}/fetch` — ручная загрузка документа с remote eDocument (consumes remote read cycle), затем сохранение в локальный кэш.
- `GET /api/edoc/documents/{id}/cache-status` — статус локального кэша документа.
- `GET /api/edoc/documents/{id}/files/{fileId}/content` — скачать содержимое файла документа из локального кэша.
- `POST /api/edoc/documents/{id}/exported` — обновление локального статуса экспорта.
- `GET /api/edoc/contacts/physical/by-personalNumber?personalNumber=...` — GetPhysicalPersonsByPersonalNumber.
- `GET /api/edoc/contacts/physical/by-name?lastName=...&firstName=...` — GetPhysicalPersonsByName.
- `GET /api/edoc/contacts/organizations/by-identificationNumber?identificationNumber=...` — GetOrganizationsByIdentificationNumber.
- `GET /api/edoc/contacts/organizations/by-name?name=...` — GetOrganizationsByName.
- `GET /api/edoc/contacts/stateStructures?name=...` — GetStateStructures.

## Swagger / OpenAPI
Swagger UI доступен после запуска по адресу `http://localhost:4102/swagger-ui/index.html`.

- Описание включает все контроллеры и DTO с примерами.
- Значения по умолчанию подхватывают дефолтный токен `{BD081743-C0C4-43B6-A0C3-30914FC9888F}` и версию `1.0.0.0`.
- Для генерации OpenAPI-спецификации в JSON: `curl http://localhost:4102/v3/api-docs`.

## Тестовые коллекции
В `postman/anubis-edoc.postman_collection.json` (идентичный файл лежит в `src/test/resources/postman/Edoc.postman_collection.json`) собраны готовые сценарии Postman.

Коллекция включает:
- проверки REST-методов (logout, документы, подтверждение экспорта, поиски контактов);
- тесты наличия ключевых полей и сохранение промежуточных переменных для последующих запросов;
- workflow "export first document from list", позволяющий прогнать полный цикл: подбор по параметрам → детальный просмотр → отметка об экспорте.

Для работы достаточно указать `baseUrl` (`http://localhost:4102` по умолчанию) и при необходимости значения для фильтров (период, тип документа, идентификаторы). Коллекция рассчитана на импорт в Postman Collection Runner или в качестве тестов в CI.

## Документация
Исходная спецификация: `eDocument_ExportService_Documentation_2019_11_05_v1.0.0.0.pdf`.
