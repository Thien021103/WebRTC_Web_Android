#ifndef PEER_SIGNALING_H_
#define PEER_SIGNALING_H_

#include "peer_connection.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifndef DISABLE_PEER_SIGNALING

typedef struct ServiceConfiguration {
  char* camera_id;
  char* secret_key;
  const char* client_id;
  const char* ws_url;
  int ws_port;
  PeerConnection* pc;
} ServiceConfiguration;

#define SERVICE_CONFIG_DEFAULT()                    \
  {                                                 \
    .ws_url = "thientranduc.id.vn",                 \
    .ws_port = 444,                                 \
    .secret_key = "secret_key",                     \
    .client_id = "peer",                            \
    .pc = NULL                                      \
  }

void peer_signaling_set_config(ServiceConfiguration* config);

void peer_signaling_leave_channel();

int peer_signaling_loop();

void connect_to_ws_server();

#endif  // DISABLE_PEER_SIGNALING

#ifdef __cplusplus
}
#endif

#endif  // PEER_SIGNALING_H_
