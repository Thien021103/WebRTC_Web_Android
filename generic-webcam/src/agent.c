#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/select.h>
#include <unistd.h>

#include "agent.h"
#include "base64.h"
#include "ice.h"
#include "ports.h"
#include "socket.h"
#include "stun.h"
#include "utils.h"

#define AGENT_POLL_TIMEOUT 1
#define AGENT_CONNCHECK_MAX 500
#define AGENT_CONNCHECK_PERIOD 50
#define AGENT_PERMISSION_PERIOD 250
#define AGENT_STUN_RECV_MAXTIMES 1000

void agent_clear_candidates(Agent* agent) {
  agent->local_candidates_count = 0;
  agent->remote_candidates_count = 0;
  agent->candidate_pairs_num = 0;
}

int agent_create(Agent* agent) {
  int ret;
  if ((ret = udp_socket_open(&agent->udp_sockets[0], AF_INET, 0)) < 0) {
    LOGE("Failed to create UDP socket.");
    return ret;
  }
  char astr[30];
  addr_to_string(&agent->udp_sockets[0].bind_addr, astr, sizeof(astr));
  LOGI("create IPv4 UDP socket with address: %s and port: %d", astr ,agent->udp_sockets[0].bind_addr.port);

#if CONFIG_IPV6
  if ((ret = udp_socket_open(&agent->udp_sockets[1], AF_INET6, 0)) < 0) {
    LOGE("Failed to create IPv6 UDP socket.");
    return ret;
  }
  LOGI("create IPv6 UDP socket: %d", agent->udp_sockets[1].fd);
#endif

  agent_clear_candidates(agent);
  // agent->indication_sent = 0;
  agent->turn_permission = 0;
  agent->requested = 0;
  agent->responded = 0;
  agent->use_channel = 0;
  return 0;
}

void agent_destroy(Agent* agent) {
  if (agent->udp_sockets[0].fd > 0) {
    udp_socket_close(&agent->udp_sockets[0]);
  }

#if CONFIG_IPV6
  if (agent->udp_sockets[1].fd > 0) {
    udp_socket_close(&agent->udp_sockets[1]);
  }
#endif
  // agent->indication_sent = 0;
  agent->turn_permission = 0;
  agent->requested = 0;
  agent->responded = 0;
  agent->use_channel = 0;
}

static int agent_socket_recv(Agent* agent, Address* addr, uint8_t* buf, int len) {
  int ret = -1;
  int i = 0;
  int maxfd = -1;
  fd_set rfds;
  struct timeval tv;
  int addr_type[] = { AF_INET,
#if CONFIG_IPV6
                      AF_INET6,
#endif
  };

  tv.tv_sec = 0;
  tv.tv_usec = AGENT_POLL_TIMEOUT * 1000;
  FD_ZERO(&rfds);

  for (i = 0; i < sizeof(addr_type) / sizeof(addr_type[0]); i++) {
    if (agent->udp_sockets[i].fd > maxfd) {
      maxfd = agent->udp_sockets[i].fd;
    }
    if (agent->udp_sockets[i].fd >= 0) {
      FD_SET(agent->udp_sockets[i].fd, &rfds);
    }
  }

  ret = select(maxfd + 1, &rfds, NULL, NULL, &tv);
  if (ret < 0) {
    LOGE("select error");
  } else if (ret == 0) {
    // timeout
  } else {
    for (i = 0; i < 2; i++) {
      if (FD_ISSET(agent->udp_sockets[i].fd, &rfds)) {
        memset(buf, 0, len);
        ret = udp_socket_recvfrom(&agent->udp_sockets[i], addr, buf, len);
        break;
      }
    }
  }

  return ret;
}

static int agent_socket_recv_attempts(Agent* agent, Address* addr, uint8_t* buf, int len, int maxtimes) {
  int ret = -1;
  int i = 0;
  for (i = 0; i < maxtimes; i++) {
    if ((ret = agent_socket_recv(agent, addr, buf, len)) != 0) {
      break;
    }
    usleep(1000); // Sleep for 1 ms
  }
  return ret;
}

static int agent_socket_send(Agent* agent, Address* addr, const uint8_t* buf, int len) {
  switch (addr->family) {
    case AF_INET6:
      return udp_socket_sendto(&agent->udp_sockets[1], addr, buf, len);
    case AF_INET:
    default:
      return udp_socket_sendto(&agent->udp_sockets[0], addr, buf, len);
  }
  return -1;
}

static int agent_create_host_addr(Agent* agent) {
  int i, j;
  const char* iface_prefx[] = {CONFIG_IFACE_PREFIX};
  IceCandidate* ice_candidate;
  // char astr[26];
  int addr_type[] = { AF_INET,
#if CONFIG_IPV6
                      AF_INET6,
#endif
  };

  for (i = 0; i < sizeof(addr_type) / sizeof(addr_type[0]); i++) {
    for (j = 0; j < sizeof(iface_prefx) / sizeof(iface_prefx[0]); j++) {
      ice_candidate = agent->local_candidates + agent->local_candidates_count;
      // only copy port and family to addr of ice candidate
      ice_candidate_create(ice_candidate, agent->local_candidates_count, ICE_CANDIDATE_TYPE_HOST,
                           &agent->udp_sockets[i].bind_addr);
        // addr_to_string(&agent->udp_sockets[i].bind_addr, astr, sizeof(astr));
        // LOGI("ice_candidate_create with bind address: %s\n", astr);
      // if resolve host addr, add to local candidate
      if (ports_get_host_addr(&ice_candidate->addr, iface_prefx[j])) {
        // addr_to_string(&ice_candidate->addr, astr, sizeof(astr));
          // LOGI("Resolved host address: %s\n", astr);
          // LOGI("Resolved host port: %d\n", ice_candidate->addr.port);

        agent->local_candidates_count++;
      }
    }
  }

  return 0;
}

static int agent_create_stun_addr(Agent* agent, Address* serv_addr) {
  int ret = -1;
  Address mapped_addr;
  StunMessage send_msg;
  StunMessage recv_msg;
  memset(&send_msg, 0, sizeof(send_msg));
  memset(&recv_msg, 0, sizeof(recv_msg));

  stun_msg_create(&send_msg, STUN_CLASS_REQUEST | STUN_METHOD_BINDING);

  ret = agent_socket_send(agent, serv_addr, send_msg.buf, send_msg.size);

  if (ret == -1) {
    LOGE("Failed to send STUN Binding Request.");
    return ret;
  }

  ret = agent_socket_recv_attempts(agent, NULL, recv_msg.buf, sizeof(recv_msg.buf), AGENT_STUN_RECV_MAXTIMES);
  if (ret <= 0) {
    LOGE("Failed to receive STUN Binding Response.");
    return ret;
  }

  stun_parse_msg_buf(&recv_msg);
  memcpy(&mapped_addr, &recv_msg.mapped_addr, sizeof(Address));
  IceCandidate* ice_candidate = agent->local_candidates + agent->local_candidates_count++;
  ice_candidate_create(ice_candidate, agent->local_candidates_count, ICE_CANDIDATE_TYPE_SRFLX, &mapped_addr);
  return ret;
}

static int agent_create_turn_addr(Agent* agent, Address* serv_addr, const char* username, const char* credential) {
  int ret = -1;
  uint32_t attr = ntohl(0x11000000);
  Address turn_addr;
  StunMessage send_msg;
  StunMessage recv_msg;
  memset(&recv_msg, 0, sizeof(recv_msg));
  memset(&send_msg, 0, sizeof(send_msg));
  stun_msg_create(&send_msg, STUN_METHOD_ALLOCATE);
  stun_msg_write_attr(&send_msg, STUN_ATTR_TYPE_REQUESTED_TRANSPORT, sizeof(attr), (char*)&attr);  // UDP
  stun_msg_write_attr(&send_msg, STUN_ATTR_TYPE_USERNAME, strlen(username), (char*)username);

  ret = agent_socket_send(agent, serv_addr, send_msg.buf, send_msg.size);
  if (ret == -1) {
    LOGE("Failed to send TURN Binding Request.");
    return -1;
  }

  ret = agent_socket_recv_attempts(agent, NULL, recv_msg.buf, sizeof(recv_msg.buf), AGENT_STUN_RECV_MAXTIMES);
  if (ret <= 0) {
    LOGD("Failed to receive STUN Binding Response.");
    return ret;
  }

  stun_parse_msg_buf(&recv_msg);

  if (recv_msg.stunclass == STUN_CLASS_ERROR && recv_msg.stunmethod == STUN_METHOD_ALLOCATE) {
    memset(&send_msg, 0, sizeof(send_msg));
    stun_msg_create(&send_msg, STUN_METHOD_ALLOCATE);
    stun_msg_write_attr(&send_msg, STUN_ATTR_TYPE_REQUESTED_TRANSPORT, sizeof(attr), (char*)&attr);  // UDP
    stun_msg_write_attr(&send_msg, STUN_ATTR_TYPE_USERNAME, strlen(username), (char*)username);
    stun_msg_write_attr(&send_msg, STUN_ATTR_TYPE_NONCE, strlen(recv_msg.nonce), recv_msg.nonce);
    stun_msg_write_attr(&send_msg, STUN_ATTR_TYPE_REALM, strlen(recv_msg.realm), recv_msg.realm);

    // For further creation of STUN message
    memset(&(agent->turn_ser_addr), 0, sizeof(Address));
    memset(agent->turn_nonce, 0, sizeof(agent->turn_nonce));
    memset(agent->turn_realm, 0, sizeof(agent->turn_realm));

    memcpy(&(agent->turn_ser_addr), serv_addr, sizeof(Address));
    agent->turn_username = username;
    agent->turn_password = credential;
    memcpy(agent->turn_nonce, recv_msg.nonce, sizeof(recv_msg.nonce));
    memcpy(agent->turn_realm, recv_msg.realm, sizeof(recv_msg.realm));
    stun_msg_finish(&send_msg, STUN_CREDENTIAL_LONG_TERM, agent->turn_password, strlen(agent->turn_password));

  } else {
    LOGE("Invalid TURN Binding Response.");
    return -1;
  }

  ret = agent_socket_send(agent, serv_addr, send_msg.buf, send_msg.size);
  if (ret < 0) {
    LOGE("Failed to send TURN Binding Request.");
    return -1;
  }

  ret = agent_socket_recv_attempts(agent, NULL, recv_msg.buf, sizeof(recv_msg.buf), AGENT_STUN_RECV_MAXTIMES);
  if (ret <= 0) {
    LOGD("Failed to receive TURN Binding Response.");
    return ret;
  }

  stun_parse_msg_buf(&recv_msg);
  memcpy(&turn_addr, &recv_msg.relayed_addr, sizeof(Address));
  IceCandidate* ice_candidate = agent->local_candidates + agent->local_candidates_count++;
  ice_candidate_create(ice_candidate, agent->local_candidates_count, ICE_CANDIDATE_TYPE_RELAY, &turn_addr);
  return ret;
}

void agent_gather_candidate(Agent* agent, const char* urls, const char* username, const char* credential) {
  char* pos;
  int port;
  char hostname[64];
  char addr_string[ADDRSTRLEN];
  int i;
  int addr_type[1] = {AF_INET};  // ipv6 no need stun
  Address resolved_addr;
  memset(hostname, 0, sizeof(hostname));

  if (urls == NULL) {
    agent_create_host_addr(agent);
    return;
  }

  if ((pos = strstr(urls + 5, ":")) == NULL) {
    LOGE("Invalid URL");
    return;
  }

  port = atoi(pos + 1);
  if (port <= 0) {
    LOGE("Cannot parse port");
    return;
  }

  snprintf(hostname, pos - urls - 5 + 1, "%s", urls + 5);

  for (i = 0; i < sizeof(addr_type) / sizeof(addr_type[0]); i++) {
    if (ports_resolve_addr(hostname, &resolved_addr) == 0) {
      addr_set_port(&resolved_addr, port);
      addr_to_string(&resolved_addr, addr_string, sizeof(addr_string));
      LOGI("Resolved stun/turn server %s:%d", addr_string, port);

      if (strncmp(urls, "stun:", 5) == 0) {
        LOGD("Create stun addr");
        agent_create_stun_addr(agent, &resolved_addr);
      } else if (strncmp(urls, "turn:", 5) == 0) {
        LOGD("Create turn addr");
        agent_create_turn_addr(agent, &resolved_addr, username, credential);
      }
    }
  }
}

void agent_get_local_description(Agent* agent, char* description, int length) {
  memset(description, 0, length);
  memset(agent->local_ufrag, 0, sizeof(agent->local_ufrag));
  memset(agent->local_upwd, 0, sizeof(agent->local_upwd));

  utils_random_string(agent->local_ufrag, 4);
  utils_random_string(agent->local_upwd, 24);

  snprintf(description, length, "a=ice-ufrag:%s\r\na=ice-pwd:%s\r\n", agent->local_ufrag, agent->local_upwd);

  for (int i = 0; i < agent->local_candidates_count; i++) {
    ice_candidate_to_description(&agent->local_candidates[i], description + strlen(description), length - strlen(description));
  }

  // remove last \n
  description[strlen(description)] = '\0';
  LOGI("local description:\n%s", description);
}

int agent_send(Agent* agent, const uint8_t* buf, int len) {
  if (agent->use_channel) {
    // Send as ChannelData to TURN server
    uint8_t channel_buf[CONFIG_MTU + 128]; // Fixed-size buffer, adjust as needed
    if (len + 4 > sizeof(channel_buf)) {
        return -1; // Buffer too small
    }

    // ChannelData header
    channel_buf[0] = agent->channel[0]; // 0x40
    channel_buf[1] = agent->channel[1]; // 0x05
    *(uint16_t*)(channel_buf + 2) = htons(len); // Data length
    memcpy(channel_buf + 4, buf, len);          // Payload

    // Pad to 4-byte boundary
    int padded_len = 4 * ((len + 3) / 4);
    if (padded_len > len) {
      memset(channel_buf + 4 + len, 0, padded_len - len);
    }

    int total_len = 4 + padded_len;
    int sent = agent_socket_send(agent, &agent->turn_ser_addr, channel_buf, total_len);
    if (sent != total_len) {
      return -1;
    }
    else {
      LOGD("Sent ChannelData: channel=0x%04x, data_len=%d\n", 
               (agent->channel[0] << 8) | agent->channel[1], len);
      return len;
    }
  }
  else {
    return agent_socket_send(agent, &agent->nominated_pair->remote->addr, buf, len);
  }
}

static void agent_create_permission_request(Agent* agent, StunMessage* msg, const Address* peer_addr) {
  char xor_peer_addr[32];
  int size = 0;
  uint8_t mask[16];
  StunHeader* header;

  stun_msg_create(msg, STUN_METHOD_CREATE_PERMISSION);
  header = (StunHeader*)msg->buf;
  *((uint32_t*)mask) = htonl(MAGIC_COOKIE);
  memcpy(mask + 4, header->transaction_id, sizeof(header->transaction_id));
  
  memset(xor_peer_addr, 0, sizeof(xor_peer_addr));
  size = stun_set_mapped_address(xor_peer_addr, mask, peer_addr); // XOR with transaction ID (per STUN spec) 
  stun_msg_write_attr(msg, STUN_ATTR_TYPE_XOR_PEER_ADDRESS, size, xor_peer_addr);
  stun_msg_write_attr(msg, STUN_ATTR_TYPE_USERNAME, strlen(agent->turn_username), agent->turn_username);
  stun_msg_write_attr(msg, STUN_ATTR_TYPE_REALM, strlen(agent->turn_realm), agent->turn_realm);
  stun_msg_write_attr(msg, STUN_ATTR_TYPE_NONCE, strlen(agent->turn_nonce), agent->turn_nonce);
  stun_msg_finish(msg, STUN_CREDENTIAL_LONG_TERM, agent->turn_password, strlen(agent->turn_password));
}

static void agent_create_binding_response(Agent* agent, StunMessage* msg, Address* addr) {
  int size = 0;
  char username[584];
  char mapped_address[32];
  uint8_t mask[16];
  StunHeader* header;
  
  stun_msg_create(msg, STUN_CLASS_RESPONSE | STUN_METHOD_BINDING);
  header = (StunHeader*)msg->buf;
  memcpy(header->transaction_id, agent->transaction_id, sizeof(header->transaction_id));
  snprintf(username, sizeof(username), "%s:%s", agent->local_ufrag, agent->remote_ufrag);
  *((uint32_t*)mask) = htonl(MAGIC_COOKIE);
  memcpy(mask + 4, agent->transaction_id, sizeof(agent->transaction_id));
  
  memset(mapped_address, 0, sizeof(mapped_address));
  size = stun_set_mapped_address(mapped_address, mask, addr);
  stun_msg_write_attr(msg, STUN_ATTR_TYPE_XOR_MAPPED_ADDRESS, size, mapped_address);
  stun_msg_write_attr(msg, STUN_ATTR_TYPE_USERNAME, strlen(username), username);
  stun_msg_finish(msg, STUN_CREDENTIAL_SHORT_TERM, agent->local_upwd, strlen(agent->local_upwd));
}

static void agent_create_binding_request(Agent* agent, StunMessage* msg) {
  uint64_t tie_breaker = 0;  // always be controlled
  char username[584];
  memset(username, 0, sizeof(username));
  snprintf(username, sizeof(username), "%s:%s", agent->remote_ufrag, agent->local_ufrag);
  
  // Send binding request
  stun_msg_create(msg, STUN_CLASS_REQUEST | STUN_METHOD_BINDING);
  stun_msg_write_attr(msg, STUN_ATTR_TYPE_USERNAME, strlen(username), username);
  stun_msg_write_attr(msg, STUN_ATTR_TYPE_PRIORITY, 4, (char*)&agent->nominated_pair->priority);
  if (agent->mode == AGENT_MODE_CONTROLLING) {
    stun_msg_write_attr(msg, STUN_ATTR_TYPE_USE_CANDIDATE, 0, NULL);
    stun_msg_write_attr(msg, STUN_ATTR_TYPE_ICE_CONTROLLING, 8, (char*)&tie_breaker);
  } else {
    stun_msg_write_attr(msg, STUN_ATTR_TYPE_ICE_CONTROLLED, 8, (char*)&tie_breaker);
  }
  stun_msg_finish(msg, STUN_CREDENTIAL_SHORT_TERM, agent->remote_upwd, strlen(agent->remote_upwd));
}

static void agent_create_send_indication(Agent* agent, StunMessage* msg, const Address* peer_addr, StunMessage* inner_msg) {
  int size = 0;
  char xor_peer_addr[32];
  char data[512];
  uint8_t mask[16]; 
  StunHeader* header;

  stun_msg_create(msg, STUN_CLASS_INDICATION | STUN_METHOD_SEND);
  header = (StunHeader*)msg->buf;
  *((uint32_t*)mask) = htonl(MAGIC_COOKIE);
  memcpy(mask + 4, header->transaction_id, sizeof(header->transaction_id));
  
  memset(xor_peer_addr, 0, sizeof(xor_peer_addr));
  memset(data, 0, sizeof(data));

  size = stun_set_mapped_address(xor_peer_addr, mask, peer_addr); // XOR with transaction ID (per STUN spec) 
  memcpy(data, inner_msg->buf, sizeof(inner_msg->buf));

  stun_msg_write_attr(msg, STUN_ATTR_TYPE_XOR_PEER_ADDRESS, size, xor_peer_addr);
  stun_msg_write_attr(msg, STUN_ATTR_TYPE_DATA, inner_msg->size, data);
  stun_msg_finish(msg, STUN_CREDENTIAL_LONG_TERM, agent->turn_password, strlen(agent->turn_password));
}

static void agent_create_channel_bind_request(Agent* agent, StunMessage* msg, const Address* peer_addr) {
  char xor_peer_addr[32];
  int size = 0;
  uint8_t mask[16];
  char channel[4];
  StunHeader* header;

  // Channel 0x4005
  channel[0] = 0x40;
  channel[1] = 0x05;
  channel[2] = 0x00;
  channel[3] = 0x00;

  stun_msg_create(msg, STUN_CLASS_REQUEST | STUN_METHOD_CHANNEL_BIND);
  header = (StunHeader*)msg->buf;
  *((uint32_t*)mask) = htonl(MAGIC_COOKIE);
  memcpy(mask + 4, header->transaction_id, sizeof(header->transaction_id));

  memset(xor_peer_addr, 0, sizeof(xor_peer_addr));
  size = stun_set_mapped_address(xor_peer_addr, mask, peer_addr); // XOR with transaction ID (per STUN spec) 
  
  // Send channel binding request
  stun_msg_write_attr(msg, STUN_ATTR_TYPE_CHANNEL_NUMBER, sizeof(channel), channel);
  stun_msg_write_attr(msg, STUN_ATTR_TYPE_XOR_PEER_ADDRESS, size, xor_peer_addr);
  stun_msg_write_attr(msg, STUN_ATTR_TYPE_USERNAME, strlen(agent->turn_username), agent->turn_username);
  stun_msg_write_attr(msg, STUN_ATTR_TYPE_REALM, strlen(agent->turn_realm), agent->turn_realm);
  stun_msg_write_attr(msg, STUN_ATTR_TYPE_NONCE, strlen(agent->turn_nonce), agent->turn_nonce);
  stun_msg_finish(msg, STUN_CREDENTIAL_LONG_TERM, agent->turn_password, strlen(agent->turn_password));
}


void agent_process_stun_request(Agent* agent, StunMessage* stun_msg, Address* addr) {
  StunMessage msg;
  StunHeader* header;
  StunMessage outer_msg;
  switch (stun_msg->stunmethod) {
    case STUN_METHOD_BINDING:
      if (stun_msg_is_valid(stun_msg->buf, stun_msg->size, agent->local_upwd) == 0) {
        
        header = (StunHeader*)stun_msg->buf;
        memcpy(agent->transaction_id, header->transaction_id, sizeof(header->transaction_id));
        LOGI("Sending binding RESPONSE and INDICATION to remote ip");

        agent->responded = 1; // Sent response to client, switch to channel binding

        if(agent->use_channel) {
          agent_create_binding_response(agent, &msg, addr);
          agent_send(agent, msg.buf, msg.size);
        }
        else {
          agent_create_binding_response(agent, &msg, addr);
          agent_socket_send(agent, addr, msg.buf, msg.size);
  
          memset(&outer_msg, 0, sizeof(outer_msg));
          agent_create_send_indication(agent, &outer_msg, addr, &msg);
          agent_socket_send(agent, &agent->turn_ser_addr, outer_msg.buf, outer_msg.size);
        }
        agent->binding_request_time = ports_get_epoch_time();
      }
      break;
    default:
      break;
  }
}

void agent_process_stun_response(Agent* agent, StunMessage* stun_msg) {
  switch (stun_msg->stunmethod) {
    case STUN_METHOD_BINDING:
      if (stun_msg_is_valid(stun_msg->buf, stun_msg->size, agent->remote_upwd) == 0) {
        agent->nominated_pair->state = ICE_CANDIDATE_STATE_SUCCEEDED;

        agent->requested = 1; // Sent request and received response from client, listening for request

      }
      break;
    default:
      break;
  }
}

int agent_recv(Agent* agent, uint8_t* buf, int len) {
  int ret = -1;
  StunMessage stun_msg;
  StunMessage inner_msg;
  Address addr;
  char addr_string[ADDRSTRLEN];

  // Receive ChannelData from TURN server
  if (agent->use_channel) {
    ret = agent_socket_recv(agent, &addr, buf, len);
    if (ret < 0) {
        return -1; // Receive error
    }
    if (ret < 4) {
        return -1; // Too short for ChannelData
    }
    // Check if it's ChannelData with 0x4005
    uint16_t received_channel = ntohs(*(uint16_t*)buf);
    uint16_t expected_channel = (agent->channel[0] << 8) | agent->channel[1]; // 0x4005
    if (received_channel == expected_channel) {
        uint16_t data_len = ntohs(*(uint16_t*)(buf + 2));
        if (ret < 4 + data_len) {
            return -1; // Incomplete message
        }
        // Extract payload (skip 4-byte header)
        memmove(buf, buf + 4, data_len);
        // Receive STUN packets from peer in ChannelData
        if (stun_probe(buf, len) == 0) {
          memcpy(stun_msg.buf, buf, ret);
          stun_msg.size = data_len;
          stun_parse_msg_buf(&stun_msg);
          switch (stun_msg.stunclass) {
            case STUN_CLASS_REQUEST:
              LOGI("Received stun binding request!!!");
              agent_process_stun_request(agent, &stun_msg, &addr);
              break;
            case STUN_CLASS_RESPONSE:
              agent_process_stun_response(agent, &stun_msg);
              break;
            case STUN_CLASS_ERROR:
              break;
            default:
              break;
          }
        }
        return data_len; // Return payload length
    }
    return -1; // Not a matching ChannelData message
  }
  // Receive STUN packets from peer
  else if ((ret = agent_socket_recv(agent, &addr, buf, len)) > 0 && stun_probe(buf, len) == 0) {
    memcpy(stun_msg.buf, buf, ret);
    stun_msg.size = ret;
    stun_parse_msg_buf(&stun_msg);
    switch (stun_msg.stunclass) {
      case STUN_CLASS_REQUEST:
        // Only response when already checked with your own request first
        if(agent->requested) {
          addr_to_string(&addr, addr_string, sizeof(addr_string));
          LOGD("Received binding REQUEST from address ip: %s, port: %d", addr_string, agent->nominated_pair->remote->addr.port);
          agent_process_stun_request(agent, &stun_msg, &addr);
        }
        break;
      case STUN_CLASS_RESPONSE:
        addr_to_string(&agent->nominated_pair->remote->addr, addr_string, sizeof(addr_string));
        LOGD("Received binding RESPONSE from remote ip: %s, port: %d", addr_string, agent->nominated_pair->remote->addr.port);
        agent_process_stun_response(agent, &stun_msg);
        break;
      case STUN_CLASS_ERROR:
        break;
      case STUN_CLASS_INDICATION:
        addr_to_string(&addr, addr_string, sizeof(addr_string));
        LOGI("Received DATA INDICATION from remote ip: %s, port: %d", addr_string, addr.port);
        memcpy(inner_msg.buf, stun_msg.turn_data, sizeof(stun_msg.turn_data));
        inner_msg.size = stun_msg.turn_data_size;
        stun_parse_msg_buf(&inner_msg);
        // Only response when already checked with your own request first
        if (inner_msg.stunclass == STUN_CLASS_REQUEST && agent->requested) {
          LOGI("Received BINDING request in DATA INDICATION");
          agent_process_stun_request(agent, &inner_msg, &agent->nominated_pair->remote->addr);
        }
        else if(inner_msg.stunclass == STUN_CLASS_RESPONSE) {
          LOGI("Received BINDING response in DATA INDICATION");
          agent_process_stun_response(agent, &inner_msg);
        }
      default:
        break;
    }
    ret = 0;
  }
  return ret;
}

void agent_set_remote_description(Agent* agent, char* description) {
  /*
  a=ice-ufrag:Iexb
  a=ice-pwd:IexbSoY7JulyMbjKwISsG9
  a=candidate:1 1 UDP 1 36.231.28.50 38143 typ srflx
  */
  int i, j;

  // LOGD("Seting remote description (candidate):\n%s", description);

  char* line_start = description;
  char* line_end = NULL;

  while ((line_end = strstr(line_start, "\r\n")) != NULL) {
    if (strncmp(line_start, "a=ice-ufrag:", strlen("a=ice-ufrag:")) == 0) {
      strncpy(agent->remote_ufrag, line_start + strlen("a=ice-ufrag:"), line_end - line_start - strlen("a=ice-ufrag:"));

    } else if (strncmp(line_start, "a=ice-pwd:", strlen("a=ice-pwd:")) == 0) {
      strncpy(agent->remote_upwd, line_start + strlen("a=ice-pwd:"), line_end - line_start - strlen("a=ice-pwd:"));

    } else if (strncmp(line_start, "a=candidate:", strlen("a=candidate:")) == 0) {
      if (ice_candidate_from_description(&agent->remote_candidates[agent->remote_candidates_count], line_start, line_end) == 0) {
        agent->remote_candidates_count++;
      }
    }

    line_start = line_end + 2;
  }

  LOGD("remote ufrag: %s", agent->remote_ufrag);
  LOGD("remote upwd: %s", agent->remote_upwd);

  // Please set gather candidates before set remote description
  for (i = 0; i < agent->local_candidates_count; i++) {
    for (j = 0; j < agent->remote_candidates_count; j++) {
      if (agent->local_candidates[i].addr.family == agent->remote_candidates[j].addr.family) {
        agent->candidate_pairs[agent->candidate_pairs_num].local = &agent->local_candidates[i];
        agent->candidate_pairs[agent->candidate_pairs_num].remote = &agent->remote_candidates[j];
        agent->candidate_pairs[agent->candidate_pairs_num].priority = agent->local_candidates[i].priority + agent->remote_candidates[j].priority;
        agent->candidate_pairs[agent->candidate_pairs_num].state = ICE_CANDIDATE_STATE_FROZEN;
        agent->candidate_pairs_num++;
      }
    }
  }
  LOGI("candidate pairs num: %d", agent->candidate_pairs_num);
}

int agent_create_permission(Agent* agent) {
  StunMessage cre_per_msg;
  StunMessage recv_msg;
  uint8_t buf[512];
  char addr_string[ADDRSTRLEN];
  memset(&recv_msg, 0, sizeof(recv_msg));

  if(agent->turn_permission) return 0;

  if (agent->nominated_pair->conncheck % AGENT_PERMISSION_PERIOD == 0) {
    LOGI("Concheck: %d", agent->nominated_pair->conncheck);

  // Create and send the request
    addr_to_string(&agent->nominated_pair->remote->addr, addr_string, sizeof(addr_string)); 
    LOGI("Setting permission for remote ip %s, port: %d", addr_string, agent->nominated_pair->remote->addr.port);
    agent_create_permission_request(agent, &cre_per_msg, &agent->nominated_pair->remote->addr);
    agent_socket_send(agent, &agent->turn_ser_addr, cre_per_msg.buf, cre_per_msg.size);

  // Wait for response
    int ret = agent_socket_recv_attempts(agent, NULL, recv_msg.buf, sizeof(recv_msg.buf), AGENT_STUN_RECV_MAXTIMES); 
    if (ret > 0) {
      stun_parse_msg_buf(&recv_msg);
      if (recv_msg.stunclass == STUN_CLASS_RESPONSE) {
        LOGI("CreatePermission succeeded");
        agent->turn_permission = 1;
        return 0;
      } else {
        LOGE("CreatePermission failed");
        return -1;
      }
    }
    else {
      LOGE("No response from TURN server");
      return -1;
    }
  }
  return 0;
}

int agent_connectivity_check(Agent* agent) {
  char addr_string[ADDRSTRLEN];
  uint8_t buf[1400];
  StunMessage inner_msg;
  StunMessage outer_msg;

  if (agent->nominated_pair->state != ICE_CANDIDATE_STATE_INPROGRESS) {
    if (agent->nominated_pair->state == ICE_CANDIDATE_STATE_SUCCEEDED && agent->responded) {
      return 0;
    }
    else if (!agent->requested) {
      LOGD("Nominated pair is not in progress");
      return -1;
    }
  }

  memset(&inner_msg, 0, sizeof(inner_msg));
  memset(&outer_msg, 0, sizeof(outer_msg));

  if (agent->nominated_pair->conncheck % AGENT_CONNCHECK_PERIOD == 0 && !agent->requested) {
    LOGI("Concheck: %d", agent->nominated_pair->conncheck);
    addr_to_string(&agent->nominated_pair->remote->addr, addr_string, sizeof(addr_string));
    LOGI("Sending binding REQUEST and SEND INDICATION to remote ip: %s, port: %d", addr_string, agent->nominated_pair->remote->addr.port);

    agent_create_binding_request(agent, &inner_msg);
    agent_socket_send(agent, &agent->nominated_pair->remote->addr, inner_msg.buf, inner_msg.size);
    
    agent_create_send_indication(agent, &outer_msg, &agent->nominated_pair->remote->addr, &inner_msg);
    agent_socket_send(agent, &agent->turn_ser_addr, outer_msg.buf, outer_msg.size);
  }

  agent_recv(agent, buf, sizeof(buf));

  if (agent->nominated_pair->state == ICE_CANDIDATE_STATE_SUCCEEDED && agent->responded) {
    agent->selected_pair = agent->nominated_pair;
    LOGI("Next phase, channel");
    return 0;
  }

  return -1;
}

int agent_channel_bind(Agent* agent) {
  StunMessage chan_bind_msg;
  StunMessage recv_msg;
  uint8_t buf[512];
  char addr_string[ADDRSTRLEN];
  memset(&recv_msg, 0, sizeof(recv_msg));

  // Create and send the request
  addr_to_string(&agent->nominated_pair->remote->addr, addr_string, sizeof(addr_string)); 
  LOGI("Binding channel with remote ip %s, port: %d", addr_string, agent->nominated_pair->remote->addr.port);
  agent_create_channel_bind_request(agent, &chan_bind_msg, &agent->nominated_pair->remote->addr);
  agent_socket_send(agent, &agent->turn_ser_addr, chan_bind_msg.buf, chan_bind_msg.size);

  // Wait for response
  int ret = agent_socket_recv_attempts(agent, NULL, recv_msg.buf, sizeof(recv_msg.buf), AGENT_STUN_RECV_MAXTIMES);
  if (ret > 0) {
    stun_parse_msg_buf(&recv_msg);
    if (recv_msg.stunclass == STUN_CLASS_RESPONSE) {
      LOGI("Binding channel succeeded");
      // Start using Channel 0x4005
      agent->channel[0] = 0x40;
      agent->channel[1] = 0x05;
      agent->channel[2] = 0x00;
      agent->channel[3] = 0x00;
      agent->use_channel = 1;
      return 0;
    } else {
      LOGE("Binding channel failed");
      return -1;
    }
  }
  else {
    LOGE("No response from TURN server");
    return -1;
  }

}

int agent_select_candidate_pair(Agent* agent) {
  int i;
  for (i = 0; i < agent->candidate_pairs_num; i++) {
    if (agent->candidate_pairs[i].state == ICE_CANDIDATE_STATE_FROZEN) {
      // nominate this pair
      agent->nominated_pair = &agent->candidate_pairs[i];
      agent->candidate_pairs[i].conncheck = 0;
      agent->candidate_pairs[i].state = ICE_CANDIDATE_STATE_INPROGRESS;
      return 0;
    } else if (agent->candidate_pairs[i].state == ICE_CANDIDATE_STATE_INPROGRESS) {
      agent->candidate_pairs[i].conncheck++;
      if (agent->candidate_pairs[i].conncheck < AGENT_CONNCHECK_MAX) {
        return 0;
      }
      agent->candidate_pairs[i].state = ICE_CANDIDATE_STATE_FAILED;
    } else if (agent->candidate_pairs[i].state == ICE_CANDIDATE_STATE_FAILED) {
    } else if (agent->candidate_pairs[i].state == ICE_CANDIDATE_STATE_SUCCEEDED) {
      agent->selected_pair = &agent->candidate_pairs[i];
      return 0;
    }
  }
  // all candidate pairs are failed
  return -1;
}
