#ifndef DISABLE_PEER_SIGNALING
#include <assert.h>
#include <signal.h>
#include <string.h>
#include <unistd.h>

#include "base64.h"
#include "config.h"
#include "peer_signaling.h"
#include "ports.h"
#include "utils.h"
#include <libwebsockets.h>
#include "agent.h"
// #include <json-glib/json-glib.h>

#define KEEP_ALIVE_TIMEOUT_SECONDS 60
#define CONNACK_RECV_TIMEOUT_MS 1000

#define TOPIC_SIZE 128
#define HOST_LEN 64
#define CRED_LEN 128
#define PEER_ID_SIZE 100

// #define RPC_VERSION "2.0"

#define RPC_METHOD_STATE "state"
#define RPC_METHOD_OFFER "offer"
#define RPC_METHOD_ANSWER "answer"
#define RPC_METHOD_CLOSE "close"

// #define RPC_ERROR_PARSE_ERROR "{\"code\":-32700,\"message\":\"Parse error\"}"
// #define RPC_ERROR_INVALID_REQUEST "{\"code\":-32600,\"message\":\"Invalid Request\"}"
// #define RPC_ERROR_METHOD_NOT_FOUND "{\"code\":-32601,\"message\":\"Method not found\"}"
// #define RPC_ERROR_INVALID_PARAMS "{\"code\":-32602,\"message\":\"Invalid params\"}"
// #define RPC_ERROR_INTERNAL_ERROR "{\"code\":-32603,\"message\":\"Internal error\"}"

static char* rand_string(char* str, size_t size)
{
    const char charset[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJK...";
    if (size) {
        --size;
        for (size_t n = 0; n < size; n++) {
            int key = rand() % (int)(sizeof charset - 1);
            str[n] = charset[key];
        }
        str[size] = '\0';
    }
    return str;
}

typedef struct PeerSignaling {

    char subtopic[TOPIC_SIZE];
    char pubtopic[TOPIC_SIZE];

    uint16_t packet_id;
    int id;

    int mqtt_port;
    int http_port;
    char mqtt_host[HOST_LEN];
    char http_host[HOST_LEN];
    char http_path[HOST_LEN];
    char ws_host[HOST_LEN];
    int  ws_port;
    char username[CRED_LEN];
    char password[CRED_LEN];
    char client_id[CRED_LEN];
    PeerConnection* pc;

} PeerSignaling;

enum MsgType {
    JANUS_MSS_UNKNOWN = 0,
    JANUS_MSS_ICE_CANDIDATE = 1,
    JANUS_MSS_ICE_CANDIDATE_GATHERED = 2,
    JANUS_MSS_SDP_OFFER = 3,
    JANUS_MSS_SDP_ANSWER = 4,
    JANUS_MSS_REGISTER = 5,
    JANUS_MSS_REGISTER_WITH_SERVER = 6,
    JANUS_MSS_ROOM_REQUEST = 7,
    JANUS_MSS_KEEP_ALIVE = 8,
    JANUS_MSS_ROOM_CALL = 9,
    ///////////////////////////////////////////////////////////
    JANUS_MSS_ROOM_PARTICIPANTS = 10,
    //////////////////////////////////////////////////////////
};

// Signaling information
enum SignalingState {
  Active = 111,     // Offer and Answer messages has been sent
  Creating = 110,   // Creating session, offer has been sent
  Ready = 101,      // Both clients available and ready to initiate session
  Impossible = 100, // We have less than two clients connected to the server
  Offline = 010,    // unable to connect signaling server
};
enum SignalingCommand {
  STATE = 200, // Command for WebRTCSessionState
  OFFER = 202, // to send or receive offer
  ANSWER = 220, // to send or receive answer
  ICE = 222,// to send and receive ice candidates
};
static enum SignalingState signaling_state = Impossible;

static PeerSignaling g_ps;

//Janus information
long long feed_id = 0;
long long session_id = 0;
long long handle_id = 0;
char transaction[12];

static struct lws_context *lws_context = NULL;
static struct lws* web_socket = NULL;

static const char* room = "1234";
static const char* token = "";
// static const char* receiver_id = "2002";
static const char* peer_id = "1001";

static int retry_delay = 1000000; // Start with 1s
static int mainloop = 1;

// Embed the GTS Root R4 CA certificate as a null-terminated string
const char *ca_pem = 
    "-----BEGIN CERTIFICATE-----\n"
    "MIIDejCCAmKgAwIBAgIQf+UwvzMTQ77dghYQST2KGzANBgkqhkiG9w0BAQsFADBX\n"
    "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQR2xvYmFsU2lnbiBudi1zYTEQMA4GA1UE\n"
    "CxMHUm9vdCBDQTEbMBkGA1UEAxMSR2xvYmFsU2lnbiBSb290IENBMB4XDTIzMTEx\n"
    "NTAzNDMyMVoXDTI4MDEyODAwMDA0MlowRzELMAkGA1UEBhMCVVMxIjAgBgNVBAoT\n"
    "GUdvb2dsZSBUcnVzdCBTZXJ2aWNlcyBMTEMxFDASBgNVBAMTC0dUUyBSb290IFI0\n"
    "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE83Rzp2iLYK5DuDXFgTB7S0md+8Fhzube\n"
    "Rr1r1WEYNa5A3XP3iZEwWus87oV8okB2O6nGuEfYKueSkWpz6bFyOZ8pn6KY019e\n"
    "WIZlD6GEZQbR3IvJx3PIjGov5cSr0R2Ko4H/MIH8MA4GA1UdDwEB/wQEAwIBhjAd\n"
    "BgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwDwYDVR0TAQH/BAUwAwEB/zAd\n"
    "BgNVHQ4EFgQUgEzW63T/STaj1dj8tT7FavCUHYwwHwYDVR0jBBgwFoAUYHtmGkUN\n"
    "l8qJUC99BM00qP/8/UswNgYIKwYBBQUHAQEEKjAoMCYGCCsGAQUFBzAChhpodHRw\n"
    "Oi8vaS5wa2kuZ29vZy9nc3IxLmNydDAtBgNVHR8EJjAkMCKgIKAehhxodHRwOi8v\n"
    "Yy5wa2kuZ29vZy9yL2dzcjEuY3JsMBMGA1UdIAQMMAowCAYGZ4EMAQIBMA0GCSqG\n"
    "SIb3DQEBCwUAA4IBAQAYQrsPBtYDh5bjP2OBDwmkoWhIDDkic574y04tfzHpn+cJ\n"
    "odI2D4SseesQ6bDrarZ7C30ddLibZatoKiws3UL9xnELz4ct92vID24FfVbiI1hY\n"
    "+SW6FoVHkNeWIP0GCbaM4C6uVdF5dTUsMVs/ZbzNnIdCp5Gxmx5ejvEau8otR/Cs\n"
    "kGN+hr/W5GvT1tMBjgWKZ1i4//emhA1JG1BbPzoLJQvyEotc03lXjTaCzv8mEbep\n"
    "8RqZ7a2CPsgRbuvTPBwcOMBBmuFeU88+FSBX6+7iP0il8b4Z0QFqIwwMHfs/L6K1\n"
    "vepuoxtGzi4CZ68zJpiq1UvSqTbFJjtbD4seiMHl\n"
    "-----END CERTIFICATE-----";

enum AppState {
    APP_STATE_UNKNOWN = 0,
    APP_STATE_ERROR = 1, /* generic error */
    SERVER_CONNECTING = 1000,
    SERVER_CONNECTION_ERROR,
    SERVER_CONNECTED, /* Ready to register */
    SERVER_REGISTERING = 2000,
    SERVER_REGISTRATION_ERROR,
    SERVER_REGISTERED, /* Ready to call a peer */
    SERVER_REGISTERING_2 = 2500,
    SERVER_REGISTRATION_ERROR_2,
    SERVER_REGISTERED_2, /* Ready to call a peer */
    SERVER_REGISTERING_3 = 2700,
    SERVER_REGISTRATION_ERROR_3,
    SERVER_REGISTERED_3, /* Ready to call a peer */
    SERVER_CLOSED, /* server connection closed by us or the server */
    PEER_CONNECTING = 3000,
    PEER_CONNECTION_ERROR,
    PEER_CONNECTED,
    PEER_CALL_NEGOTIATING = 3500,
    PEER_CALL_NEGOTIATED = 4000,
    PEER_CALL_STARTED,
    PEER_CALL_STOPPING,
    PEER_CALL_STOPPED,
    PEER_CALL_ERROR,
};

static enum AppState app_state = APP_STATE_UNKNOWN;

#define JANUS_RX_BUFFER_BYTES (1024*100)
// enum protocols
// {
//     PROTOCOL_JANUS = 0,
//     PROTOCOL_COUNT
// };

static int callback_janus(struct lws* wsi, enum lws_callback_reasons reason, void* user, void* in, size_t len);

static int lws_websocket_connection_send_text(struct lws* wsi_in, char* str, enum MsgType msgtype) {
    if (str == NULL || wsi_in == NULL)
        return -1;
    unsigned char buf[LWS_SEND_BUFFER_PRE_PADDING + JANUS_RX_BUFFER_BYTES + LWS_SEND_BUFFER_POST_PADDING];
    unsigned char* p = &buf[LWS_SEND_BUFFER_PRE_PADDING];
    size_t n;

    switch (msgtype) {
    case JANUS_MSS_ICE_CANDIDATE_GATHERED:
        break;
    case JANUS_MSS_ICE_CANDIDATE:
        break;
    case JANUS_MSS_SDP_OFFER:
        n = sprintf((char*)p, "%s", str);
        LOGI("Sent: %s\n", (char*)p);
        lws_write(wsi_in, p, n, LWS_WRITE_TEXT);
        break;
    case JANUS_MSS_SDP_ANSWER:
        break;
    case JANUS_MSS_REGISTER:
        n = sprintf((char*)p, "%s", str);
        LOGI("Sent: %s\n", (char*)p);
        lws_write(wsi_in, p, n, LWS_WRITE_TEXT);
        break;
    case JANUS_MSS_REGISTER_WITH_SERVER:
        n = sprintf((char*)p, "%s", str);
        LOGI("Sent: %s\n", (char*)p);
        lws_write(wsi_in, p, n, LWS_WRITE_TEXT);
        break;
    case JANUS_MSS_ROOM_REQUEST:
        break;
    case JANUS_MSS_KEEP_ALIVE:
        break;
    case JANUS_MSS_ROOM_PARTICIPANTS:
        break;
    case JANUS_MSS_ROOM_CALL:
        break;
    default:
        break;
    }
    return n;
}

static int websocket_write_back(struct lws* wsi_in, char* str, int str_size_in) {

    PeerConnectionState state = peer_connection_get_state(g_ps.pc);

    LOGD("Message received: \n%s\n", str);
    if (str == NULL || wsi_in == NULL) {
        LOGW("Invalid arguments received");
        return -1;
    }
    if (strncmp(str, "STATE ", 6) == 0) {
        const char *value = strchr(str, ' '); // Find the first space
        if (value) {
            value++; // Move past the space
            if (strcmp(value, "Impossible") == 0) {
                signaling_state = Impossible;
                peer_connection_close(g_ps.pc); // Close old connection when client out 
                LOGI("Waiting for a client to signal...");
            } else if (strcmp(value, "Active") == 0) {
                signaling_state = Active;
                LOGI("Answer received, binding...");
            } else if (strcmp(value, "Creating") == 0) {
                signaling_state = Creating;
                LOGI("Offer sent...");
            } else if (strcmp(value, "Ready") == 0) {
                signaling_state = Ready;
                LOGI("Ready to start signaling, making Offer");
                if (state == PEER_CONNECTION_CLOSED ||
                    state == PEER_CONNECTION_NEW    ||
                    state == PEER_CONNECTION_FAILED ||
                    state == PEER_CONNECTION_DISCONNECTED) {
                    peer_connection_create_offer(g_ps.pc);
                }
            } else if (strcmp(value, "Offline") == 0) {
                signaling_state = Offline;
                LOGI("Offline");
            }
        }
    } else if (strncmp(str, "ANSWER\n", 7) == 0) {
        const char *value = str + 7; // Skip "ANSWER\n"
        if (state == PEER_CONNECTION_NEW) {
            peer_connection_set_remote_description(g_ps.pc, value);
        }
    } else if (strncmp(str, "ICE\n", 4) == 0) {
        const char *value = str + 4; // Skip "ICE\n"
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

static struct lws_protocols protocols[] =
{
    {
        "janus-protocol",
        callback_janus,
        0,
        JANUS_RX_BUFFER_BYTES,
    },
    { NULL, NULL, 0, 0 } /* terminator */
};

static int callback_janus(struct lws* wsi, enum lws_callback_reasons reason, void* user, void* in, size_t len)
{
    struct lws_client_connect_info i;

    LOGI("\tcallback_janus %d", reason);
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

        snprintf(message, sizeof(message), "CONNECT user %d", g_ps.id);

        lws_websocket_connection_send_text(web_socket, message, JANUS_MSS_REGISTER_WITH_SERVER);
        break;
    }
    case LWS_CALLBACK_CLOSED:
    case LWS_CALLBACK_CLIENT_CLOSED:
    {
        LOGI("--- CLOSED ---");

        app_state = SERVER_CLOSED;
        usleep(retry_delay);

        memset(&i, 0 , sizeof(i));
        i.context = lws_context;
        i.port = 443;
        i.address = "webrtc-websocket-lc03.onrender.com";
        i.path = "/";
        i.host = "webrtc-websocket-lc03.onrender.com";
        i.origin = "webrtc-websocket-lc03.onrender.com";
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
        usleep(retry_delay);
        
        memset(&i, 0 , sizeof(i));
        i.context = lws_context;
        i.port = 443;
        i.address = "webrtc-websocket-lc03.onrender.com";
        i.path = "/";
        i.host = "webrtc-websocket-lc03.onrender.com";
        i.origin = "webrtc-websocket-lc03.onrender.com";
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
    // LOGI("Making initial offer:\n%s\n", description);
    // Remove host candidates from SDP
    char sdp_without_host_candidate[5000];
    memset(sdp_without_host_candidate, 0, sizeof(sdp_without_host_candidate));

    char *line = strtok(description, "\n");
    while (line != NULL) {
        if (strstr(line, "typ host") == NULL) {
            strcat(sdp_without_host_candidate, line);
            strcat(sdp_without_host_candidate, "\n");
        }
        line = strtok(NULL, "\n");
    }
    strcpy(description, sdp_without_host_candidate);

    char offer[5000];
    // int a = strlen(description);
    // LOGI("%d", a);

    snprintf(offer, sizeof(offer), "OFFER user %d\n%s \n ", g_ps.id, description);

    // Send the offer over WebSocket
    lws_websocket_connection_send_text(web_socket, offer, JANUS_MSS_SDP_OFFER);

    // g_free(text);
}

int peer_signaling_loop() {
    connect_to_janus_server();
    return 0;
}

void peer_signaling_leave_channel() {
    disconnect_websocket();
}

void peer_signaling_set_config(ServiceConfiguration* service_config) {

    memset(&g_ps, 0, sizeof(g_ps));

    //congnv
    do {
        if (service_config->ws_url == NULL || strlen(service_config->ws_url) == 0) {
            break;
        }

        strncpy(g_ps.ws_host, service_config->ws_url, HOST_LEN);
        g_ps.ws_port = service_config->ws_port;
        LOGI("WS Host: %s, Port: %d", g_ps.ws_host, g_ps.ws_port);
    } while (0);

    // Username, password, id

    g_ps.id = service_config->id;
    if (service_config->client_id != NULL && strlen(service_config->client_id) > 0) {
        strncpy(g_ps.client_id, service_config->client_id, CRED_LEN);
    }

    if (service_config->username != NULL && strlen(service_config->username) > 0) {
        strncpy(g_ps.username, service_config->username, CRED_LEN);
    }

    if (service_config->password != NULL && strlen(service_config->password) > 0) {
        strncpy(g_ps.password, service_config->password, CRED_LEN);
    }

    g_ps.pc = service_config->pc;
    peer_connection_onicecandidate(g_ps.pc, peer_signaling_onicecandidate);
}
#endif  // DISABLE_PEER_SIGNALING

void disconnect_websocket() {
    mainloop = 0;  // Set flag to interrupt the loop
    if (web_socket) {
        LOGI("CLOSING");
        // lws_close_reason(web_socket, LWS_CLOSE_STATUS_NORMAL, (unsigned char *)"Normal closure", strlen("Normal closure"));
        lws_callback_on_writable(web_socket);  // Trigger writable callback for closure
    }
}

void connect_to_janus_server()
{
    // lws_set_log_level(LLL_ERR | LLL_WARN | LLL_NOTICE | LLL_INFO | LLL_DEBUG | LLL_PARSER | LLL_HEADER | LLL_EXT | LLL_CLIENT, NULL);
    struct lws_context_creation_info info;
    memset(&info, 0, sizeof(info));

    info.options = LWS_SERVER_OPTION_DO_SSL_GLOBAL_INIT;
    info.port = CONTEXT_PORT_NO_LISTEN;
    info.protocols = protocols;
    info.fd_limit_per_thread = 3;
    // info.client_ssl_ca_filepath = "/home/thien-gay/WebRTC_Web_Android/generic-webcam/src/ca.cert";
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
    i.port = 443;
    i.address = "webrtc-websocket-lc03.onrender.com";
    i.path = "/";
    i.host = "webrtc-websocket-lc03.onrender.com";
    i.origin = "webrtc-websocket-lc03.onrender.com";
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
