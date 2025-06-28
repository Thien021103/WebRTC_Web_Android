#ifndef DISABLE_PEER_SIGNALING
#include <assert.h>
#include <signal.h>
#include <string.h>
#include <unistd.h>

#include "config.h"
#include "peer_signaling.h"
#include "ports.h"
#include "utils.h"
#include <libwebsockets.h>
#include "agent.h"

#define TOPIC_SIZE 128
#define HOST_LEN 64
#define CRED_LEN 128
#define PEER_ID_SIZE 80
#define TOKEN_SIZE 512

typedef struct PeerSignaling {

    uint16_t packet_id;
    char camera_id[PEER_ID_SIZE];
    char camera_token[TOKEN_SIZE];
    char ws_host[HOST_LEN];
    int  ws_port;
    char client_id[CRED_LEN];
    char secret_key[CRED_LEN];
    char connect_hmac_hex[41];

    void (*on_light_switch)(int close);

    PeerConnection* pc;

} PeerSignaling;

enum MsgType {
    ICE_CANDIDATE = 1,
    SDP_OFFER = 3,
    SDP_ANSWER = 4,
    REGISTER_WITH_SERVER = 6,
    PONG = 7,
    HUMAN = 8,
};

// Signaling information
enum SignalingState {
  Active = 111,     // Offer and Answer messages has been sent
  Creating = 110,   // Creating session, offer has been sent
  Ready = 101,      // Both clients available and ready to initiate session
  Impossible = 100, // We have less than two clients connected to the server
  Offline = 010,    // unable to connect signaling server
};
 
static enum SignalingState signaling_state = Impossible;

static PeerSignaling g_ps;

static struct lws_context *lws_context = NULL;
static struct lws* web_socket = NULL;

static int retry_delay = 1000000; // Start with 1s
static int mainloop = 1;
static int token_received = 0;

// Embed the GTS Root R4 CA certificate as a null-terminated string
const char *ca_pem = 
    "-----BEGIN CERTIFICATE-----\n"
    "MIIFBjCCAu6gAwIBAgIRAIp9PhPWLzDvI4a9KQdrNPgwDQYJKoZIhvcNAQELBQAw\n"
    "TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh\n"
    "cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMjQwMzEzMDAwMDAw\n"
    "WhcNMjcwMzEyMjM1OTU5WjAzMQswCQYDVQQGEwJVUzEWMBQGA1UEChMNTGV0J3Mg\n"
    "RW5jcnlwdDEMMAoGA1UEAxMDUjExMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB\n"
    "CgKCAQEAuoe8XBsAOcvKCs3UZxD5ATylTqVhyybKUvsVAbe5KPUoHu0nsyQYOWcJ\n"
    "DAjs4DqwO3cOvfPlOVRBDE6uQdaZdN5R2+97/1i9qLcT9t4x1fJyyXJqC4N0lZxG\n"
    "AGQUmfOx2SLZzaiSqhwmej/+71gFewiVgdtxD4774zEJuwm+UE1fj5F2PVqdnoPy\n"
    "6cRms+EGZkNIGIBloDcYmpuEMpexsr3E+BUAnSeI++JjF5ZsmydnS8TbKF5pwnnw\n"
    "SVzgJFDhxLyhBax7QG0AtMJBP6dYuC/FXJuluwme8f7rsIU5/agK70XEeOtlKsLP\n"
    "Xzze41xNG/cLJyuqC0J3U095ah2H2QIDAQABo4H4MIH1MA4GA1UdDwEB/wQEAwIB\n"
    "hjAdBgNVHSUEFjAUBggrBgEFBQcDAgYIKwYBBQUHAwEwEgYDVR0TAQH/BAgwBgEB\n"
    "/wIBADAdBgNVHQ4EFgQUxc9GpOr0w8B6bJXELbBeki8m47kwHwYDVR0jBBgwFoAU\n"
    "ebRZ5nu25eQBc4AIiMgaWPbpm24wMgYIKwYBBQUHAQEEJjAkMCIGCCsGAQUFBzAC\n"
    "hhZodHRwOi8veDEuaS5sZW5jci5vcmcvMBMGA1UdIAQMMAowCAYGZ4EMAQIBMCcG\n"
    "A1UdHwQgMB4wHKAaoBiGFmh0dHA6Ly94MS5jLmxlbmNyLm9yZy8wDQYJKoZIhvcN\n"
    "AQELBQADggIBAE7iiV0KAxyQOND1H/lxXPjDj7I3iHpvsCUf7b632IYGjukJhM1y\n"
    "v4Hz/MrPU0jtvfZpQtSlET41yBOykh0FX+ou1Nj4ScOt9ZmWnO8m2OG0JAtIIE38\n"
    "01S0qcYhyOE2G/93ZCkXufBL713qzXnQv5C/viOykNpKqUgxdKlEC+Hi9i2DcaR1\n"
    "e9KUwQUZRhy5j/PEdEglKg3l9dtD4tuTm7kZtB8v32oOjzHTYw+7KdzdZiw/sBtn\n"
    "UfhBPORNuay4pJxmY/WrhSMdzFO2q3Gu3MUBcdo27goYKjL9CTF8j/Zz55yctUoV\n"
    "aneCWs/ajUX+HypkBTA+c8LGDLnWO2NKq0YD/pnARkAnYGPfUDoHR9gVSp/qRx+Z\n"
    "WghiDLZsMwhN1zjtSC0uBWiugF3vTNzYIEFfaPG7Ws3jDrAMMYebQ95JQ+HIBD/R\n"
    "PBuHRTBpqKlyDnkSHDHYPiNX3adPoPAcgdF3H2/W0rmoswMWgTlLn1Wu0mrks7/q\n"
    "pdWfS6PJ1jty80r2VKsM/Dj3YIDfbjXKdaFU5C+8bhfJGqU3taKauuz0wHVGT3eo\n"
    "6FlWkWYtbt4pgdamlwVeZEW+LM7qZEJEsMNPrfC03APKmZsJgpWCDWOKZvkZcvjV\n"
    "uYkQ4omYCTX5ohy+knMjdOmdH9c7SpqEWBDC86fiNex+O0XOMEZSa8DA\n"
    "-----END CERTIFICATE-----";

char* pong = "PONG";

enum AppState {
    APP_STATE_UNKNOWN = 0,
    SERVER_CONNECTING = 1000,
    SERVER_CONNECTION_ERROR,
    SERVER_CONNECTED, /* Ready to register */
    SERVER_REGISTERED, /* Ready to call a peer */
    SERVER_CLOSED, /* server connection closed by us or the server */
};

static enum AppState app_state = APP_STATE_UNKNOWN;

#define BUFFER_BYTES (1024*100)

static int callback_ws(struct lws* wsi, enum lws_callback_reasons reason, void* user, void* in, size_t len);

static int lws_websocket_connection_send_text(struct lws* wsi_in, char* str, enum MsgType msgtype) {
    if (str == NULL || wsi_in == NULL)
        return -1;
    unsigned char buf[LWS_SEND_BUFFER_PRE_PADDING + BUFFER_BYTES + LWS_SEND_BUFFER_POST_PADDING];
    unsigned char* p = &buf[LWS_SEND_BUFFER_PRE_PADDING];
    size_t n;

    switch (msgtype) {
    case SDP_OFFER:
        n = sprintf((char*)p, "%s", str);
        LOGI("Sent: %s", (char*)p);
        lws_write(wsi_in, p, n, LWS_WRITE_TEXT);
        break;
    case REGISTER_WITH_SERVER:
        n = sprintf((char*)p, "%s", str);
        LOGI("Sent: %s", (char*)p);
        lws_write(wsi_in, p, n, LWS_WRITE_TEXT);
        break;
    case SDP_ANSWER:
        break;
    case ICE_CANDIDATE:
        break;
    case PONG:
        n = sprintf((char*)p, "%s", str);
        LOGD("Sent: %s", (char*)p);
        lws_write(wsi_in, p, n, LWS_WRITE_TEXT);
        break;
    case HUMAN:
        n = sprintf((char*)p, "%s", str);
        LOGI("Sent: %s", (char*)p);
        lws_write(wsi_in, p, n, LWS_WRITE_TEXT);
        break;
    default:
        break;
    }
    return n;
}

static int websocket_write_back(struct lws* wsi_in, char* str, int str_size_in) {

    PeerConnectionState state = peer_connection_get_state(g_ps.pc);

    LOGI("Message received: \n%s", str);
    if (str == NULL || wsi_in == NULL) {
        LOGW("Invalid arguments received");
        return -1;
    }
    if (strncmp(str, "TOKEN ", 6) == 0) {
        const char *value = str + 6; // Skip "TOKEN\n"
        memset(g_ps.camera_token, 0, TOKEN_SIZE);
        snprintf (g_ps.camera_token, TOKEN_SIZE, "%s", value);
        token_received = 1;
        if (signaling_state == Ready) {
            LOGI("Ready to start signaling, making Offer");
            peer_connection_create_offer(g_ps.pc);
        }
    } else if (strncmp(str, "PING", 4) == 0) {
        lws_websocket_connection_send_text(web_socket, pong, PONG);
    } else if (strncmp(str, "LIGHT", 5) == 0) {
        LOGI("LIGHT switch command received");
        // Handle light command
        g_ps.on_light_switch(0);
    } else if (strncmp(str, "STATE ", 6) == 0) {
        const char *value = strchr(str, ' '); // Find the first space
        if (value) {
            value++; // Move past the space
            if (strcmp(value, "Impossible") == 0) {
                signaling_state = Impossible;
                peer_connection_close(g_ps.pc); // Close old connection when client out 
                g_ps.on_light_switch(1); // Turn off light
                LOGI("Waiting for a client to signal...");
            } else if (strcmp(value, "Active") == 0) {
                signaling_state = Active;
                LOGI("Answer received, binding...");
            } else if (strcmp(value, "Creating") == 0) {
                signaling_state = Creating;
                LOGI("Offer sent...");
            } else if (strcmp(value, "Ready") == 0) {
                signaling_state = Ready;
                if (state == PEER_CONNECTION_CLOSED ||
                    state == PEER_CONNECTION_NEW    ||
                    state == PEER_CONNECTION_FAILED ||
                    state == PEER_CONNECTION_DISCONNECTED) {
                    if (!token_received) {
                        return -1;
                    } else {
                        LOGI("Ready to start signaling, making Offer");
                        peer_connection_create_offer(g_ps.pc);
                    }
                }
            } else if (strcmp(value, "Offline") == 0) {
                signaling_state = Offline;
                LOGI("Offline");
            }
        }
    } else if (strncmp(str, "ANSWER\n", 7) == 0) {
        LOGI("Recv: %s\n", str);
        const char *value = str + 7; // Skip "ANSWER\n"
        if (state == PEER_CONNECTION_NEW) {
            peer_connection_set_remote_description(g_ps.pc, value);
        }
    } else if (strncmp(str, "ICE\n", 4) == 0) {
        const char *value = str + 4; // Skip "ICE\n"
        LOGI("Recv: %s\n", str);
        if (state == PEER_CONNECTION_CHECKING) {
            char converted_candidate[1024];
            char *candidate = strstr(value, "candidate");
            snprintf(converted_candidate, sizeof(converted_candidate), "a=%s", candidate);
            LOGI("Adding client candidates");
            peer_connection_add_ice_candidate(g_ps.pc, converted_candidate);
        }
    }
    return 0;
}

void people_detection() {
    // Handle people detection events
    char message[512];
    snprintf(message, sizeof(message), "HUMAN camera %s", g_ps.camera_token);
    lws_websocket_connection_send_text(web_socket, message, HUMAN);
}

static struct lws_protocols protocols[] =
{
    {
        "protocol",
        callback_ws,
        0,
        BUFFER_BYTES,
    },
    { NULL, NULL, 0, 0 } /* terminator */
};

static int callback_ws(struct lws* wsi, enum lws_callback_reasons reason, void* user, void* in, size_t len)
{
    struct lws_client_connect_info i;

    LOGI("\tcallback_ws %d", reason);
    switch (reason)
    {
    case LWS_CALLBACK_CLIENT_ESTABLISHED:
    {
        app_state = SERVER_CONNECTED;
        lws_callback_on_writable(wsi);
        LOGI("--- CONNECTED ---");
        break;
    }
    case LWS_CALLBACK_CLIENT_RECEIVE:
    {
        // Handle incomming messages here
        unsigned char* buf = (unsigned char*) malloc(LWS_SEND_BUFFER_PRE_PADDING + len + LWS_SEND_BUFFER_POST_PADDING);
        unsigned int i;
        for (i = 0; i < len; i++) {
            buf[LWS_SEND_BUFFER_PRE_PADDING + (len - 1) - i] = ((char*)in)[i];
        }

        websocket_write_back(wsi, (char*)in, -1);

        free(buf);

        break;
    }
    case LWS_CALLBACK_CLIENT_WRITEABLE:
    {
        app_state = SERVER_REGISTERED;
        char message[100];

        snprintf(message, sizeof(message), "CONNECT camera %s", g_ps.camera_id);

        lws_websocket_connection_send_text(web_socket, message, REGISTER_WITH_SERVER);
        break;
    }
    case LWS_CALLBACK_CLOSED:
    case LWS_CALLBACK_CLIENT_CLOSED:
    {
        LOGI("--- CLOSED ---");

        app_state = SERVER_CLOSED;
        signaling_state = Offline;
        token_received = 0;
        usleep(retry_delay);

        memset(&i, 0 , sizeof(i));
        i.context = lws_context;
        i.port = g_ps.ws_port;
        i.address = g_ps.ws_host;
        i.path = "/";
        i.host = g_ps.ws_host;
        i.origin = g_ps.ws_host;
        i.ssl_connection = LCCSCF_USE_SSL;
        i.protocol = protocols[0].name;
        i.local_protocol_name = protocols[0].name;

        web_socket = lws_client_connect_via_info(&i);

        LOGI("--- RECONNECTING ---");
        app_state = SERVER_CONNECTING;
        break;
    }
    case LWS_CALLBACK_CLIENT_CONNECTION_ERROR:
    {
        LOGI("--- CLIENT CONNECTION ERROR ---");

        app_state = SERVER_CONNECTION_ERROR;
        signaling_state = Offline;
        token_received = 0;
        usleep(retry_delay);
        
        memset(&i, 0 , sizeof(i));
        i.context = lws_context;
        i.port = g_ps.ws_port;
        i.address = g_ps.ws_host;
        i.path = "/";
        i.host = g_ps.ws_host;
        i.origin = g_ps.ws_host;
        i.ssl_connection = LCCSCF_USE_SSL;
        i.protocol = protocols[0].name;
        i.local_protocol_name = protocols[0].name;

        web_socket = lws_client_connect_via_info(&i);

        LOGI("--- RECONNECTING ---");
        app_state = SERVER_CONNECTING;
        break;
    }
    default:
        break;
    }

    return 0;
}

static void peer_signaling_onicecandidate(char* description, void* userdata) {
    char offer[5000];

    snprintf(offer, sizeof(offer), "OFFER camera %s\n%s", g_ps.camera_token, description);

    // Send the offer over WebSocket
    lws_write(web_socket, (unsigned char*)offer, strlen(offer), LWS_WRITE_TEXT);
}

void peer_signaling_set_config(ServiceConfiguration* service_config) {

    unsigned char hmac[20];
    memset(&g_ps, 0, sizeof(g_ps));
    do {
        if (service_config->ws_url == NULL || strlen(service_config->ws_url) == 0) {
            break;
        }

        strncpy(g_ps.ws_host, service_config->ws_url, HOST_LEN);
        g_ps.ws_port = service_config->ws_port;
        LOGI("WS Host: %s, Port: %d", g_ps.ws_host, g_ps.ws_port);
    } while (0);
    
    // ID
    if (service_config->camera_id != NULL && strlen(service_config->camera_id) > 0) {
        strncpy(g_ps.camera_id, service_config->camera_id, PEER_ID_SIZE);
    }

    if (service_config->client_id != NULL && strlen(service_config->client_id) > 0) {
        strncpy(g_ps.client_id, service_config->client_id, CRED_LEN);
    }

    if (service_config->secret_key != NULL && strlen(service_config->secret_key) > 0) {
        strncpy(g_ps.secret_key, service_config->secret_key, PEER_ID_SIZE);
    }

    if (service_config->on_light_switch != NULL) {
        g_ps.on_light_switch = service_config->on_light_switch;
    }

    utils_get_hmac_sha1(g_ps.camera_id, strlen(g_ps.camera_id), g_ps.secret_key, strlen(g_ps.secret_key), hmac);
    for (int i = 0; i < 20; i++) {
        sprintf(&g_ps.connect_hmac_hex[i*2], "%02x", hmac[i]);
    }

    g_ps.pc = service_config->pc;
    peer_connection_onicecandidate(g_ps.pc, peer_signaling_onicecandidate);
}
#endif  // DISABLE_PEER_SIGNALING

void disconnect_websocket() {
    mainloop = 0;  // Set flag to interrupt the loop
    if (web_socket) {
        LOGI("--- CLOSING ---");
        // lws_close_reason(web_socket, LWS_CLOSE_STATUS_NORMAL, (unsigned char *)"Normal closure", strlen("Normal closure"));
        lws_callback_on_writable(web_socket);  // Trigger writable callback for closure
    }
}

void connect_to_ws_server()
{
    signaling_state = Offline;
    token_received = 0;
    // lws_set_log_level(LLL_ERR | LLL_WARN | LLL_NOTICE | LLL_INFO | LLL_DEBUG | LLL_PARSER | LLL_HEADER | LLL_EXT | LLL_CLIENT, NULL);
    struct lws_context_creation_info info;
    memset(&info, 0, sizeof(info));

    info.options = LWS_SERVER_OPTION_DO_SSL_GLOBAL_INIT;
    info.port = CONTEXT_PORT_NO_LISTEN;
    info.protocols = protocols;
    info.fd_limit_per_thread = 3;
    info.client_ssl_ca_mem = ca_pem;
    info.client_ssl_ca_mem_len = strlen(ca_pem);
    lws_context = lws_create_context(&info);
    if (!lws_context) {
        lwsl_err("lws init failed!\n");
        return;
    }

    struct lws_client_connect_info i;
    memset(&i, 0 , sizeof(i));

    i.context = lws_context;
    i.port = g_ps.ws_port;
    i.address = g_ps.ws_host;
    i.path = "/";
    i.host = g_ps.ws_host;
    i.origin = g_ps.ws_host;
    i.ssl_connection = LCCSCF_USE_SSL;
    i.protocol = protocols[0].name;
    i.local_protocol_name = protocols[0].name;

    web_socket = lws_client_connect_via_info(&i);

    app_state = SERVER_CONNECTING;

    while (mainloop) {
        lws_service(lws_context, /* timeout_ms = */ 5000);
        usleep(10000);
    }
    lws_context_destroy(lws_context);
}

int peer_signaling_loop() {
    connect_to_ws_server();
    return 0;
}

void peer_signaling_leave_channel() {
    disconnect_websocket();
}