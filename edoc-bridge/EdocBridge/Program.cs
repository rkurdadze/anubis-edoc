using EdocBridge.edoc;
using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Configuration;
using Microsoft.AspNetCore.Http;
using System;

var builder = WebApplication.CreateBuilder(args);

// load config
builder.Services.Configure<EdocConfig>(builder.Configuration.GetSection("Edoc"));
builder.Services.AddSingleton<EdocConfig>(sp =>
{
    var cfg = new EdocConfig();
    builder.Configuration.GetSection("Edoc").Bind(cfg);
    return cfg;
});

// SOAP client
builder.Services.AddSingleton<EdocServiceClient>();

var app = builder.Build();

app.MapPost("/api/edoc/logon", async (LogOnRequest req, EdocServiceClient client) =>
{
    try
    {
        var sessionId = await client.LogOnAsync(req.Token, req.Version);
        return Results.Ok(new { sessionId });
    }
    catch (System.ServiceModel.FaultException<SecurityFault> sf)
    {
        return Results.Problem(
            title: "SecurityFault from EDOC",
            detail: sf.Message,
            statusCode: 500
        );
    }
    catch (Exception ex)
    {
        return Results.Problem(ex.Message, statusCode: 500);
    }
});


app.Run();
