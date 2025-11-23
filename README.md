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
| `edoc.client-auth-token` | Токен клиента | `{BD081743-C0C4-43B6-A0C3-30914FC9888F}` |
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
Все методы из WCF-спецификации отображены один-к-одному:
- `POST /api/edoc/session/logon` — явный LogOn, в ответе `sessionId`.
- `POST /api/edoc/session/logout` — явный LogOut по текущей или указанной сессии.
- `GET /api/edoc/documents?type=Incoming&from=2024-01-01&to=2024-01-31&contactType=Organization&contactId=15f5b4a7-7ad6-4de6-9a8f-fb39c5ba6c44` — GetDocuments (период + фильтр по связанному контакту).
- `GET /api/edoc/documents/{id}?full=true` — GetDocument (с флагом getFullData).
- `POST /api/edoc/documents/{id}/exported` — SetDocumentExported.
- `GET /api/edoc/contacts/physical/by-personalNumber?personalNumber=...` — GetPhysicalPersonsByPersonalNumber.
- `GET /api/edoc/contacts/physical/by-name?lastName=...&firstName=...` — GetPhysicalPersonsByName.
- `GET /api/edoc/contacts/organizations/by-identificationNumber?identificationNumber=...` — GetOrganizationsByIdentificationNumber.
- `GET /api/edoc/contacts/organizations/by-name?name=...` — GetOrganizationsByName.
- `GET /api/edoc/contacts/stateStructures?name=...` — GetStateStructures.

## Swagger / OpenAPI
Swagger UI доступен после запуска по адресу `http://localhost:8080/swagger-ui/index.html`.

- Описание включает все контроллеры и DTO с примерами.
- Значения по умолчанию подхватывают дефолтный токен `{BD081743-C0C4-43B6-A0C3-30914FC9888F}` и версию `1.0.0.0`.
- Для генерации OpenAPI-спецификации в JSON: `curl http://localhost:8080/v3/api-docs`.

## Тестовые коллекции
В `postman/anubis-edoc.postman_collection.json` (идентичный файл лежит в `src/test/resources/postman/Edoc.postman_collection.json`) собраны готовые сценарии Postman.

Коллекция включает:
- проверки всех REST-методов (LogOn/LogOut, документы, подтверждение экспорта, поиски контактов);
- тесты наличия ключевых полей и сохранение промежуточных переменных для последующих запросов;
- workflow "export first document from list", позволяющий прогнать полный цикл: подбор по параметрам → детальный просмотр → отметка об экспорте.

Для работы достаточно указать `baseUrl` и при необходимости значения для фильтров (период, тип документа, идентификаторы). Коллекция рассчитана на импорт в Postman Collection Runner или в качестве тестов в CI.

## Документация
Исходная спецификация: `eDocument_ExportService_Documentation_2019_11_05_v1.0.0.0.pdf`.
