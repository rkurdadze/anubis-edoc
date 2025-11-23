1

dotnet-svcutil \
"https://edocument.ge/TEST/integrationservice/eDocumentExportService.svc?wsdl" \
-n "* , EdocBridge.edoc" \
-o edoc/EdocReference.cs



2
cd edoc-bridge
docker compose build --no-cache
docker compose up


Тестируем LogOn через curl:
curl -X POST http://localhost:5080/api/edoc/logon \
-H "Content-Type: application/json" \
-d '{}'


curl -X POST "http://localhost:5080/api/edoc/logon" \
-H "Content-Type: application/json" \
-d '{
"token": "BD081743-C0C4-43B6-A0C3-30914FC9888F",
"version": "1.0.0.0"
}'



Должно вернуть что-то вроде:
{
"sessionId": "GUID-HERE"
}
