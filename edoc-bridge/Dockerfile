# ============================
# 1. Build stage
# ============================
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

# Копируем CSPROJ для восстановления зависимостей
COPY ./EdocBridge/EdocBridge.csproj ./EdocBridge/

# Копируем ServiceReference полностью
COPY ./ServiceReference ./ServiceReference

# Restore
RUN dotnet restore ./EdocBridge/EdocBridge.csproj

# Теперь копируем весь код
COPY ./EdocBridge ./EdocBridge

# Publish
RUN dotnet publish ./EdocBridge/EdocBridge.csproj -c Release -o /app/publish

# ============================
# 2. Runtime stage
# ============================
FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS final
WORKDIR /app

COPY --from=build /app/publish .

EXPOSE 5080
ENV ASPNETCORE_URLS=http://0.0.0.0:5080

ENTRYPOINT ["dotnet", "EdocBridge.dll"]
