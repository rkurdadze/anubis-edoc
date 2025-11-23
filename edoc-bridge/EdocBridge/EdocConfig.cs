using Microsoft.Extensions.Configuration;

namespace EdocBridge.edoc;

public class EdocConfig
{
    public string ServiceUrl { get; set; }
    public string Token { get; set; }
    public string ServiceVersion { get; set; }
    public int Timeout { get; set; }

    public static EdocConfig Load()
    {
        var config = new ConfigurationBuilder()
            .AddJsonFile("appsettings.json")
            .Build();

        return config.GetSection("Edoc").Get<EdocConfig>();
    }
}
