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
| `edoc.export-service-url` | SOAP endpoint | `https://edocument.ge/TEST/integrationservice/eDocumentExportService.svc` |
| `edoc.wsdl-url` | WSDL | `https://edocument.ge/TEST/integrationservice/eDocumentExportService.svc?wsdl` |
| `edoc.client-auth-token` | Токен клиента | требуется задать |
| `edoc.service-version` | Версия сервиса | `1.0.0.0` |
| `edoc.connect-timeout-millis` | Таймаут подключения | `10000` |
| `edoc.read-timeout-millis` | Таймаут чтения | `60000` |

## Сборка и запуск

```bash
mvn clean package
java -jar target/edoc-0.0.1-SNAPSHOT.jar
```

Docker:
```bash
docker build -t edoc-service .
docker run -p 4102:4102 -e EDOC_CLIENT_AUTH_TOKEN=... edoc-service
```
или `docker-compose up --build`.

## REST API
- `GET /api/edoc/documents?type=Incoming&from=2024-01-01&to=2024-01-31`
- `GET /api/edoc/documents/{id}?full=true`
- `POST /api/edoc/documents/{id}/exported`
- `GET /api/edoc/contacts/physical/by-personalNumber?personalNumber=...`
- `GET /api/edoc/contacts/physical/by-name?lastName=...&firstName=...`
- `GET /api/edoc/contacts/organizations/by-identificationNumber?identificationNumber=...`
- `GET /api/edoc/contacts/organizations/by-name?name=...`
- `GET /api/edoc/contacts/stateStructures?name=...`

## Тестовые коллекции
В `src/test/resources/postman/Edoc.postman_collection.json` размещены готовые запросы для Postman, охватывающие все конечные точки.

## Документация
Исходная спецификация: `eDocument_ExportService_Documentation_2019_11_05_v1.0.0.0.pdf`.
