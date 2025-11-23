using EdocBridge.edoc;
using System.Threading.Tasks;
using System;


namespace EdocBridge.edoc;

public class EdocServiceClient
{
    private readonly EdocConfig _config;
    private readonly IeDocumentExportServiceClient _client;

    public EdocServiceClient(EdocConfig config)
    {
        _config = config;

        _client = new IeDocumentExportServiceClient(
            IeDocumentExportServiceClient.EndpointConfiguration.CustomBinding_IeDocumentExportService,
            config.ServiceUrl
        );
    }

    /// <summary>
    /// Логон с возможностью передать token/version из API запроса
    /// </summary>
    public async Task<string> LogOnAsync(string? tokenOverride, string? versionOverride)
    {
        var token = string.IsNullOrWhiteSpace(tokenOverride)
            ? _config.Token
            : tokenOverride;

        var version = string.IsNullOrWhiteSpace(versionOverride)
            ? _config.ServiceVersion
            : versionOverride;

        if (string.IsNullOrWhiteSpace(token))
            throw new InvalidOperationException("Token is missing (empty).");

        var result = await _client.LogOnAsync(token, version);
        return result.ToString();
    }
}
